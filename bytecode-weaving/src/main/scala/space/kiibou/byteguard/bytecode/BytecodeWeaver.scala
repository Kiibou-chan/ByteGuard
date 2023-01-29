package space.kiibou.byteguard.bytecode

import org.opalj.ai.ValueOrigin
import org.opalj.ba.{CodeElement, InsertionPosition, LabeledCode, toDA}
import org.opalj.bc.Assembler
import org.opalj.bi.{ACC_PUBLIC, ACC_SYNTHETIC}
import org.opalj.br.MethodDescriptor.JustTakes
import org.opalj.br._
import org.opalj.br.analyses.{Project, SomeProject}
import org.opalj.br.instructions._
import org.opalj.br.reader.Java17Framework
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac._
import org.opalj.value.ValueInformation
import space.kiibou.byteguard.Guard
import space.kiibou.byteguard.bytecode.BytecodeWeaver.{GuardViolationExceptionType, PredicateViolationExceptionType, loadClassBytes, toClassFile}
import space.kiibou.byteguard.processor.info
import space.kiibou.byteguard.processor.info.{ClassSpecificationInfo, ClassifierType}
import space.kiibou.byteguard.specification.GuardSpec
import space.kiibou.byteguard.specification.method._

import java.io.ByteArrayInputStream
import java.lang.reflect.{Field => JField}
import java.net.URL
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.ListHasAsScala

class BytecodeWeaver(private val bytecode: Array[Byte], private val classSpecInfo: ClassSpecificationInfo) {
    private val classFile: ClassFile = toClassFile(bytecode)

    private val specClassFile = toClassFile(loadClassBytes(classSpecInfo.getSpecClassType))
    private val specClass: Class[_ <: GuardSpec] = classSpecInfo.getSpecClass
    private val guardFields: List[JField] = classSpecInfo.getGuardFields.asScala.toList

    private val spec: GuardSpec = classSpecInfo.getSpecInstance
    private val indexGuardNameMap: Map[Int, String] = getIndexGuardNameMap(spec, guardFields)
    private val methodSpecs: Map[(String, FieldTypes), MethodSpec] = getNameMethodSpecMap(specClass, spec)

    private val project: SomeProject = Project[URL](
        List(
            specClassFile -> new URL(s"file://${specClassFile.thisType.fqn}"),
            classFile -> new URL(s"file://${classFile.thisType.fqn}")
        ),
        // org.opalj.bytecode.RTJar
    )

    private var additionalMethods: Seq[MethodTemplate] = Seq.empty[MethodTemplate]

    private var symbolIndex = 0

    private def getUniqueSymbol(prefix: String): Symbol = {
        val index = symbolIndex
        symbolIndex += 1
        Symbol(prefix + s"_$index")
    }

    def weave(): Array[Byte] = {
        val newFields = addGuardFields()
        val newMethods = transformMethods() ++ additionalMethods

        val newBytecode = Assembler(toDA(classFile.copy(
            fields = newFields, methods = newMethods
        )))

        newBytecode
    }

    private def addGuardFields(): FieldTemplates = {
        classFile.fields.map(_.copy()) ++ indexGuardNameMap.values.map { name =>
            Field(
                ACC_PUBLIC.mask, // + ACC_SYNTHETIC.mask,
                name,
                BooleanType,
            )
        }
    }

    private def transformMethods(): MethodTemplates = {
        classFile.methodsWithBody.map { method =>
            val x = if (methodSpecs.contains(method.name -> method.parameterTypes)) {
                transformMethod(method)
            } else {
                method.copy()
            }

            x
        }.to(ArraySeq)
    }

    private def transformMethod(method: Method): MethodTemplate = {
        val code = method.body.get

        val labeledCode = LabeledCode(code)

        val methodSpecComponents = methodSpecs((method.name -> method.parameterTypes)).components()

        if (method.name == "<init>") {
            handleConstructorGuardFieldInits(labeledCode)
        }

        handleRequires(method, labeledCode, methodSpecComponents)
        handleWhenThenConsequence(method, labeledCode, methodSpecComponents)

        val (newCode, _) = labeledCode.result(classFile.version, method)

        method.copy(body = Some(newCode))
    }

    private def handleConstructorGuardFieldInits(labeledCode: LabeledCode): Unit = {
        guardFields.foreach { field =>
            labeledCode.insert(0, InsertionPosition.Before, Seq(
                ALOAD_0,
                if (field.get(spec).asInstanceOf[Guard].initialState() == Guard.State.SET) ICONST_1 else ICONST_0,
                PUTFIELD(classFile.thisType, field.getName, BooleanType)
            ))
        }
    }

    private def handleRequires(method: Method, labeledCode: LabeledCode, methodSpecComponents: Array[MethodSpecComponent]): Unit = {
        methodSpecComponents.foreach {
            case requires: MethodSpecComponent.RequiresGuardState => handleRequiresGuardState(method, labeledCode, requires)
            case _ =>
        }

        handleRequiresPredicates(
            method,
            labeledCode,
            methodSpecComponents.collect { case requires: MethodSpecComponent.RequiresPredicate => requires }
        )
    }

    private def handleRequiresGuardState(method: Method, labeledCode: LabeledCode, requires: MethodSpecComponent.RequiresGuardState): Unit = {
        val name = indexGuardNameMap(requires.guardState().guard().index())

        val labelName = getUniqueSymbol(s"${name}_correctly_set")

        labeledCode.insert(0, InsertionPosition.Before, Seq(
            ALOAD_0,
            GETFIELD(classFile.thisType, name, BooleanType), // push state of the guard
            if (requires.guardState().state() == Guard.State.SET) IFNE(labelName) else IFEQ(labelName),
            NEW(GuardViolationExceptionType),
            DUP,
            LDC_W(ConstantString(s"Guard Violation: Guard $name in method ${classFile.thisType.toJava}#${method.name} should be set to ${requires.guardState().state()} but was set to ${requires.guardState().state().negate()}")),
            INVOKESPECIAL(GuardViolationExceptionType, isInterface = false, "<init>", JustTakes(ObjectType.String)),
            ATHROW,
            labelName
        ))
    }

    private case class RequiresPredicateData(receiverType: ReferenceType, lambdaName: String, lambdaMethodDescriptor: MethodDescriptor, isInstanceCapturing: Boolean, capturedParameters: Seq[(Int, FieldType)])

    private def handleRequiresPredicates(method: Method, labeledCode: LabeledCode, predicates: Array[MethodSpecComponent.RequiresPredicate]): Unit = {
        if (predicates.isEmpty) {
            return
        }

        /*
        Get the spec method code
        Find all invocations to the requires(Ljava/util/function/BooleanSupplier;)LMethodSpecComponent;
        Find invokedynamic instruction

        If it does not capture anything
            add the lambda method to the specified class
            add a check with a call to the lambda method as guard predicate

        If it's parameter capturing
            find out which parameters it captures
            add the lambda method to the specified class
            add a check with a call to the lambda method as guard predicate with the parameters it would captures

        If it's instance capturing
            ...
         */


        // We assume, that there is always a matching method
        val specMethod = specClassFile.methodsWithBody.filter { m => m.name == method.name && m.descriptor.parameterTypes == method.descriptor.parameterTypes }.next()

        val tacKey = project.get(ComputeTACAIKey)

        val result: AITACode[TACMethodParameter, ValueInformation] = tacKey(specMethod)

        val originParameterMap: Map[ValueOrigin, (Int, FieldType)] = getOriginParameterMap(method)

        val predicateData: RequiresPredicateData = (for {
            Assignment(_, _, VirtualFunctionCall(_, _, _, "requires", requiresPredicateGuardMethodDescriptor, _, ArraySeq(UVar(_, defSites)))) <- result.stmts
            (Assignment(_, _, InvokedynamicFunctionCall(_, BootstrapMethod(_, ArraySeq(_, InvokeStaticMethodHandle(receiverType, _, lambdaName, lambdaMethodDescriptor), _)), _, _, capturedValues: Seq[UVar[ValueInformation]])), index) <- result.stmts.zipWithIndex if defSites.contains(index)
            // TODO (Svenja, 2023/01/24): Currently we only handle invokestatic calls to the lambda, so instance capturing lambdas would not be processed correctly
        } yield RequiresPredicateData(
            receiverType,
            lambdaName,
            lambdaMethodDescriptor,
            isInstanceCapturing(capturedValues),
            getCapturedParameters(specMethod, defSites, originParameterMap, capturedValues)
        )).apply(0)

        additionalMethods ++= specClassFile.findMethod(predicateData.lambdaName, predicateData.lambdaMethodDescriptor).map { m =>
            m.copy(accessFlags = m.accessFlags ^ ACC_SYNTHETIC.mask, name = m.name + "_guard_predicate")
        }

        val labelName = getUniqueSymbol("predicate_is_true")

        labeledCode.insert(0, InsertionPosition.Before, Seq(
            if (predicateData.isInstanceCapturing) Seq[CodeElement[AnyRef]](ALOAD_0) else Seq.empty[CodeElement[AnyRef]],
            predicateData.capturedParameters.map[CodeElement[AnyRef]] { case (index, paramType) =>
                paramType match {
                    case BooleanType | ByteType | CharType | ShortType | IntegerType => ILOAD(index)
                    case FloatType => FLOAD(index)
                    case DoubleType => DLOAD(index)
                    case LongType => LLOAD(index)
                    case _: ReferenceType => ALOAD(index)
                }
            },
            Seq[CodeElement[AnyRef]](
                INVOKESTATIC(classFile.thisType, isInterface = false, predicateData.lambdaName + "_guard_predicate", predicateData.lambdaMethodDescriptor),
                IFNE(labelName),
                NEW(PredicateViolationExceptionType),
                DUP,
                LDC_W(ConstantString(s"Predicate Violation: Guard Predicate in method ${classFile.thisType.toJava}#${method.name} failed.")),
                INVOKESPECIAL(PredicateViolationExceptionType, isInterface = false, "<init>", JustTakes(ObjectType.String)),
                ATHROW,
                labelName
            )
        ).flatten)
    }

    private def isInstanceCapturing(capturedValues: Seq[DUVar[ValueInformation]]): Boolean = {
        for (UVar(_, defSites) <- capturedValues) {
            if (defSites.contains(-1)) {
                return true
            }
        }

        false
    }

    private def getCapturedParameters(specMethod: Method, defSites: IntTrieSet, originParameterMap: Map[ValueOrigin, (Int, FieldType)], capturedValues: Seq[UVar[ValueInformation]]): Seq[(ValueOrigin, FieldType)] = {
        capturedValues.map { case UVar(_, capturedParameterDefSites) =>
            if (capturedParameterDefSites.size > 1) {
                throw new IllegalStateException(s"Method ${classFile.thisType.fqn}#${specMethod.name}(${specMethod.descriptor.parameterTypes.mkString(", ")}) contains invokedynamic with " +
                    s"argument with defSites $defSites. Expected only one defSite.")
            }

            val origin = capturedParameterDefSites.head

            originParameterMap(origin)
        }
    }

    /**
     * @return a map containing mappings from a `ValueOrigin` to the index and type of its corresponding argument
     */
    private def getOriginParameterMap(method: Method): Map[ValueOrigin, (Int, FieldType)] = {
        var currentIndex: ValueOrigin = -2

        method.parameterTypes.zipWithIndex.map { case (paramType, argIndex) =>
            val index = currentIndex

            currentIndex += paramType.operandSize

            index -> (argIndex + 1, paramType)
        }.toMap
    }

    private def handleWhenThenConsequence(method: Method, labeledCode: LabeledCode, methodSpecComponents: Array[MethodSpecComponent]): Unit = {
        val whenComponents = methodSpecComponents.collect {
            case whenReturns: MethodSpecComponent.WhenComponent => whenReturns
        }

        if (whenComponents.isEmpty) {
            return
        }

        val returnTarget = getUniqueSymbol("return_jump_target")

        val consequenceCode = getWhenComponentCode(method, whenComponents)

        val returnPCs = for (PCAndInstruction(pc, _: ReturnInstruction) <- labeledCode.originalCode) yield pc

        labeledCode.insert(
            returnPCs.last,
            InsertionPosition.Before,
            Seq[CodeElement[AnyRef]](returnTarget) :++ consequenceCode
        )

        for (pc <- returnPCs.toList.reverse.tail) {
            labeledCode.replace(pc, Seq(GOTO(returnTarget)))
        }
    }

    private def getWhenComponentCode(method: Method, components: Array[MethodSpecComponent.WhenComponent]): Seq[CodeElement[AnyRef]] = {
        components.flatMap { whenComponent =>
            val jumpTarget = getUniqueSymbol("predicate")
            val condition = whenComponent.condition()

            val conditionCode: Seq[CodeElement[AnyRef]] = getWhenConditionCode(method, jumpTarget, condition)

            val consequenceCode: Seq[CodeElement[AnyRef]] = whenComponent match {
                case consequence: MethodSpecComponent.WhenThenConsequence =>
                    getWhenConsequenceCode(method, consequence.consequence()) :++
                        Seq[CodeElement[AnyRef]](jumpTarget)
                case consequence: MethodSpecComponent.WhenThenElseConsequence =>
                    val endTarget = getUniqueSymbol("when_end")

                    getWhenConsequenceCode(method, consequence.thenConsequence()) :++
                        Seq[CodeElement[AnyRef]](GOTO(endTarget), jumpTarget) :++
                        getWhenConsequenceCode(method, consequence.elseConsequence()) :++
                        Seq[CodeElement[AnyRef]](endTarget)
            }

            conditionCode :++ consequenceCode
        }.toSeq
    }

    private def getWhenConditionCode(method: Method, jumpTarget: Symbol, condition: WhenCondition): Seq[CodeElement[AnyRef]] = {
        condition match {
            case condition: WhenCondition.GuardCondition => Seq(
                ALOAD_0,
                GETFIELD(method.classFile.thisType, indexGuardNameMap(condition.guardState().guard().index()), BooleanType),
                if (condition.guardState().state() == Guard.State.SET) IFEQ(jumpTarget) else IFNE(jumpTarget)
            )
            case returns: WhenCondition.Returns => returns.predicate() match {
                case _: ReturnsPredicate.NoArgs => Seq()
                case value: ReturnsPredicate.Value => getReturnsPredicateWithValueCode(method, value, jumpTarget)
            }
        }
    }

    private def getReturnsPredicateWithValueCode(method: Method, value: ReturnsPredicate.Value, jumpTarget: Symbol): Seq[CodeElement[AnyRef]] = {
        method.returnType match {
            case BooleanType =>
                Seq[CodeElement[AnyRef]](
                    DUP, // retV, retV
                    if (value.value().asInstanceOf[Boolean]) IFEQ(jumpTarget) else IFNE(jumpTarget), // retV
                )
            case ByteType | CharType | ShortType | IntegerType | CTIntType =>
                Seq[CodeElement[AnyRef]](
                    DUP, // retV, retV
                    LDC_W(ConstantInteger(value.value().asInstanceOf[Int])), // retV, retV, expV
                    IF_ICMPNE(jumpTarget), // retV
                )
            case LongType =>
                Seq[CodeElement[AnyRef]](
                    DUP2, // retV (2), retV (2)
                    LDC_W(ConstantLong(value.value().asInstanceOf[Long])), // retV (2), retV (2), expV (2)
                    LCMP, // retV (2), equ
                    IFNE(jumpTarget), // retV (2)
                )
            case FloatType =>
                Seq[CodeElement[AnyRef]](
                    DUP, // retV, retV
                    LDC_W(ConstantFloat(value.value().asInstanceOf[Float])), // retV, retV, expV
                    FCMPL, // retV, cmpV
                    IFNE(jumpTarget), // retV
                )
            case DoubleType =>
                Seq[CodeElement[AnyRef]](
                    DUP2, // retV (2), retV (2)
                    LDC_W(ConstantDouble(value.value().asInstanceOf[Double])), // retV (2), retV (2), expV (2)
                    DCMPL, // retV (2), equ
                    IFNE(jumpTarget), // retV (2)
                )
            case _ => ???
        }
    }

    private def getWhenConsequenceCode(method: Method, thenConsequence: WhenConsequence): Seq[CodeElement[AnyRef]] = {
        thenConsequence match {
            case resets: WhenConsequence.ResetsGuard => Seq(
                ALOAD_0,
                ICONST_0,
                PUTFIELD(method.classFile.thisType, indexGuardNameMap(resets.guard().index()), BooleanType)
            )
            case sets: WhenConsequence.SetsGuard => Seq(
                ALOAD_0,
                ICONST_1,
                PUTFIELD(method.classFile.thisType, indexGuardNameMap(sets.guard().index()), BooleanType)
            )
        }
    }

    /**
     * Returns a map associating the index of a guard in a `GuardSpec` with it's field name
     */
    private def getIndexGuardNameMap(spec: GuardSpec, guardFields: List[JField]): Map[Int, String] = {
        guardFields.map { field =>
            if (!field.trySetAccessible()) {
                throw new IllegalAccessException(s"Class ${getClass.getSimpleName} could not obtain access to ${field.getDeclaringClass.getSimpleName}#${field.getName}")
            }

            val guard = field.get(spec).asInstanceOf[Guard]

            guard.index() -> field.getName
        }.toMap
    }

    /**
     * Returns a map associating the name of the target method with it's `MethodSpec`
     */
    private def getNameMethodSpecMap(specClass: Class[_ <: GuardSpec], spec: GuardSpec): Map[(String, FieldTypes), MethodSpec] = {
        val methodSpecInfo = classSpecInfo.getMethodSpecs.asScala.toList

        methodSpecInfo.map { info =>
            (info.getMethodInfo.getTargetMethodName, ArraySeq.unsafeWrapArray(info.getMethodInfo.getArgumentTypes.asScala.map(toFieldType).toArray)) -> info.getMethodSpec(specClass, spec)
        }.toMap
    }

    def toFieldType(jType: info.Type): FieldType = {
        jType match {
            case arrayType: info.ArrayType => ArrayType(toFieldType(arrayType.getType))
            case classifierType: ClassifierType => ObjectType(classifierType.getJavaName.replace(".", "/"))
            case _: info.BooleanType => BooleanType
            case _: info.ShortType => ShortType
            case _: info.ByteType => ByteType
            case _: info.CharType => CharType
            case _: info.IntType => IntegerType
            case _: info.LongType => LongType
            case _: info.FloatType => FloatType
            case _: info.DoubleType => DoubleType
            case _: info.VoidType => throw new IllegalArgumentException("Can not convert void to FieldType")
        }
    }
}


object BytecodeWeaver {
    private val GuardViolationExceptionType = ObjectType("space/kiibou/byteguard/exception/GuardViolationException")
    private val PredicateViolationExceptionType = ObjectType("space/kiibou/byteguard/exception/PredicateViolationException")

    private def wrongReturnValueExceptionConstructor(dataType: FieldType) = MethodDescriptor(ArraySeq(
        ObjectType.String, ObjectType.String, dataType, dataType
    ), VoidType)

    private val requiresPredicateGuardMethodDescriptor = SingleArgumentMethodDescriptor(
        ObjectType("java/util/function/BooleanSupplier"),
        ObjectType("space/kiibou/byteguard/specification/method/MethodSpecComponent")
    )

    def toClassFile(bytecode: Array[Byte]): ClassFile = {
        val files: List[ClassFile] = Java17Framework.ClassFile(() => new ByteArrayInputStream(bytecode))

        if (files.size != 1) throw new IllegalArgumentException(
            s"Bytecode contains ${files.size} classes (${files.map(_.thisType.toJava).mkString(", ")}), but should contain exactly one. "
        )

        files.head
    }

    def loadClassBytes(classType: ClassifierType): Array[Byte] = {
        ClassLoader.getSystemClassLoader.getResourceAsStream(classType.getJavaName.replace('.', '/') + ".class").readAllBytes()
    }
}
