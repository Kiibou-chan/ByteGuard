package space.kiibou.jguard.bytecode

import org.opalj.ba.{CodeElement, InsertionPosition, LabeledCode, toDA}
import org.opalj.bc.Assembler
import org.opalj.bi.{ACC_PROTECTED, ACC_PUBLIC}
import org.opalj.br.MethodDescriptor.JustTakes
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.br.reader.Java17Framework
import space.kiibou.jguard.Guard
import space.kiibou.jguard.bytecode.BytecodeWeaver.{IllegalStateExceptionType, GuardViolationExceptionType, wrongReturnValueExceptionConstructor}
import space.kiibou.jguard.processor.info.ClassSpecificationInfo
import space.kiibou.jguard.specification.GuardSpec
import space.kiibou.jguard.specification.method.{MethodSpec, MethodSpecComponent, ReturnsPredicate, WhenConsequence}

import java.io.ByteArrayInputStream
import java.lang.reflect.{Field => JField}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.ListHasAsScala

class BytecodeWeaver(private val bytecode: Array[Byte], private val classSpecInfo: ClassSpecificationInfo) {

    private val specClass: Class[_ <: GuardSpec] = classSpecInfo.getSpecClass
    private val guardFields: List[JField] = classSpecInfo.getGuardFields.asScala.toList

    private val spec: GuardSpec = classSpecInfo.getSpecInstance
    private val indexGuardNameMap: Map[Int, String] = getIndexGuardNameMap(spec, guardFields)
    private val methodSpecs: Map[String, MethodSpec] = getNameMethodSpecMap(specClass, spec)

    private var symbolIndex = 0

    private def getUniqueSymbol(prefix: String): Symbol = {
        val index = symbolIndex
        symbolIndex += 1
        Symbol(prefix + s"_$index")
    }

    /*
    - Add a guard to a class
      - Add a new boolean field with the specified name to the class
      - Initialize it in the constructor to the initialState set by the spec
    - Add a requirement to a method
      - Requirement depends only on a guard
        - check if the guard is in the correct state
        - if not, throw an exception
    - Add a consequence to a method
      - Returns does not check for a value
        - Create a new block at the end which sets the guard to the correct state and returns
        - Change all return ops with a jump to the new block
     */

    def weave(): Array[Byte] = {
        val classFile: ClassFile = toClassFile(bytecode)

        val newFields = addGuardFields(classFile)
        val newMethods = transformMethods(classFile)

        val newBytecode = Assembler(toDA(classFile.copy(fields = newFields, methods = newMethods)))

        newBytecode
    }

    private def addGuardFields(classFile: ClassFile): FieldTemplates = {
        classFile.fields.map(_.copy()) ++ indexGuardNameMap.values.map { name =>
            Field(
                ACC_PROTECTED.mask,
                name,
                BooleanType
            )
        }
    }

    private def transformMethods(classFile: ClassFile): MethodTemplates = {
        classFile.methodsWithBody.map { method =>
            if (methodSpecs.contains(method.name)) {
                transformMethod(classFile, method)
            } else if (method.name == "<init>") {
                addGuardInitsToConstructor(classFile, method)
            } else {
                method.copy()
            }
        }.to(ArraySeq)
    }

    private def addGuardInitsToConstructor(classFile: ClassFile, method: Method): MethodTemplate = {
        val code = method.body.get

        val labeledCode = LabeledCode(code)

        guardFields.foreach { field =>
            labeledCode.insert(0, InsertionPosition.Before, Seq(
                ALOAD_0,
                if (field.get(spec).asInstanceOf[Guard].initialState() == Guard.State.SET) ICONST_1 else ICONST_0,
                PUTFIELD(classFile.thisType, field.getName, BooleanType)
            ))
        }

        method.copy(body = Some(labeledCode.result(classFile.version, method)._1))
    }

    private def transformMethod(classFile: ClassFile, method: Method): MethodTemplate = {
        val code = method.body.get

        val labeledCode = LabeledCode(code)

        val methodSpecComponents = methodSpecs(method.name).components()

        // handle RequiresGuardState
        methodSpecComponents.foreach {
            case requires: MethodSpecComponent.RequiresGuardState => addRequires(classFile, method, labeledCode, requires)
            case _ =>
        }

        // handle WhenReturnsThenConsequence
        val whenReturnsComponents = methodSpecComponents.collect {
            case whenReturns: MethodSpecComponent.WhenReturnsThenConsequence => whenReturns
        }

        if (whenReturnsComponents.length > 0) {
            val returnTarget = getUniqueSymbol("return_jump_target")

            val consequenceCode = getConsequenceCode(method, whenReturnsComponents)

            val returnPCs = for (PCAndInstruction(pc, _: ReturnInstruction) <- code) yield pc
            labeledCode.insert(returnPCs.last, InsertionPosition.Before, Seq[CodeElement[AnyRef]](returnTarget) :++ consequenceCode)
            for (pc <- returnPCs.toList.reverse.tail) labeledCode.replace(pc, Seq(GOTO(returnTarget)))
        }

        val (newCode, _) = labeledCode.result(classFile.version, method)

        method.copy(body = Some(newCode))
    }

    private def addRequires(classFile: ClassFile, method: Method, labeledCode: LabeledCode, requires: MethodSpecComponent.RequiresGuardState): Unit = {
        val name = indexGuardNameMap(requires.guardState().guard().index())

        val labelName = Symbol(s"${name}_correctly_set")

        labeledCode.insert(0, InsertionPosition.Before, Seq(
            ALOAD_0,
            GETFIELD(classFile.thisType, name, BooleanType), // push state of the guard
            if (requires.guardState().state() == Guard.State.SET) ICONST_1 else ICONST_0, // push state the guard should be in
            IF_ICMPEQ(InstructionLabel(labelName)),
            NEW(GuardViolationExceptionType),
            DUP,
            LDC_W(ConstantString(s"Guard Violation: Guard $name in method ${classFile.thisType.toJava}#${method.name} should be set to ${requires.guardState().state()} but was set to ${requires.guardState().state().negate()}")),
            INVOKESPECIAL(GuardViolationExceptionType, isInterface = false, "<init>", JustTakes(ObjectType.String)),
            ATHROW,
            labelName
        ))
    }

    private def getConsequenceCode(method: Method, consequences: Array[MethodSpecComponent.WhenReturnsThenConsequence]): Seq[CodeElement[AnyRef]] = {
        consequences.flatMap { whenReturns =>
            val predicate = whenReturns.predicate()
            val thenConsequence = whenReturns.consequence()

            val consequenceCode: Seq[CodeElement[AnyRef]] = thenConsequence match {
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

            predicate match {
                case _: ReturnsPredicate.NoArgs => consequenceCode
                case value: ReturnsPredicate.Value => getReturnsPredicateWithValueCode(method, value, consequenceCode)
            }
        }.toSeq
    }

    /*
    // Ints/Floats
    DUP, // retV, retV
    ISTORE(maxLocalVariableIndex), // retV
    NEW(WrongReturnValueExceptionType), // retV, exc
    DUP,
    LDC_W(ConstantString(method.classFile.thisType.toJava)), // retV, exc, exc, clsN
    LDC_W(ConstantString(method.name)), // retV, exc, exc, clsN, methN
    ILOAD(maxLocalVariableIndex), // retV, exc, exc, clsN, methN, retV
    LDC_W(expectedValue), // retV, exc, exc, clsN, methN, retV, expV
    INVOKESPECIAL(WrongReturnValueExceptionType, isInterface = false, "<init>", wrongReturnValueExceptionConstructor(BooleanType)), // retV, exc
    ATHROW, // retV


    // Longs/Doubles
    DUP2, // retV (2), retV (2)
    LSTORE(maxLocalVariableIndex), // retV (2)
    NEW(WrongReturnValueExceptionType), // retV (2), exc
    DUP, // retV(2), exc, exc
    LDC_W(ConstantString(method.classFile.thisType.toJava)), // retV (2), exc, exc, clsN
    LDC_W(ConstantString(method.name)), // retV (2), exc, exc, clsN, methN
    ILOAD(maxLocalVariableIndex), // retV (2), exc, exc, clsN, methN, retV (2)
    LDC_W(ConstantLong(value.value().asInstanceOf[Long])), // retV (2), exc, exc, clsN, methN, retV (2), expV (2)
    INVOKESPECIAL(WrongReturnValueExceptionType, isInterface = false, "<init>", wrongReturnValueExceptionConstructor(LongType)), // retV (2), exc
    ATHROW, // retV (2)

     */

    private def getReturnsPredicateWithValueCode(method: Method, value: ReturnsPredicate.Value, thenConsequenceCode: Seq[CodeElement[AnyRef]]): Seq[CodeElement[AnyRef]] = {
        val jumpTarget = getUniqueSymbol("value_incorrectly_set")

        val check: Seq[CodeElement[AnyRef]] = method.returnType match {
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

        check :++ thenConsequenceCode :++ Seq[CodeElement[AnyRef]](jumpTarget)
    }

    private def toClassFile(bytecode: Array[Byte]): ClassFile = {
        val files: List[ClassFile] = Java17Framework.ClassFile(() => new ByteArrayInputStream(bytecode))

        if (files.size != 1) throw new IllegalArgumentException(
            s"Bytecode contains ${files.size} classes (${files.map(_.thisType.toJava).mkString(", ")}), but should contain exactly one. "
        )

        files.head
    }

    /**
     * Returns a map associating the index of a guard in a `GuardSpec` with it's field name
     */
    private def getIndexGuardNameMap(spec: GuardSpec, guardFields: List[JField]): Map[Int, String] = {
        guardFields.map { field =>
            val guard = field.get(spec).asInstanceOf[Guard]

            guard.index() -> field.getName
        }.toMap
    }

    /**
     * Returns a map associating the name of the target method with it's `MethodSpec`
     */
    private def getNameMethodSpecMap(specClass: Class[_ <: GuardSpec], spec: GuardSpec): Map[String, MethodSpec] = {
        val methodSpecInfo = classSpecInfo.getMethodSpecs.asScala.toList

        methodSpecInfo.map { info =>
            info.getMethodInfo.getTargetMethodName -> info.getMethodSpec(specClass, spec)
        }.toMap
    }

}

object BytecodeWeaver {
    private val GuardViolationExceptionType = ObjectType("space/kiibou/jguard/exception/GuardViolationException")

    private def wrongReturnValueExceptionConstructor(dataType: FieldType) = MethodDescriptor(ArraySeq(
        ObjectType.String, ObjectType.String, dataType, dataType
    ), VoidType)

    private val IllegalStateExceptionType = ObjectType("java/lang/IllegalStateException")
}
