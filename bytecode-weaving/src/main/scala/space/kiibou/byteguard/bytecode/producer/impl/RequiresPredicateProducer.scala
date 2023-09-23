package space.kiibou.byteguard.bytecode.producer.impl

import org.opalj.ai.ValueOrigin
import org.opalj.ba.CodeElement
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.br.MethodDescriptor.JustTakes
import org.opalj.br._
import org.opalj.br.instructions.{ALOAD, ALOAD_0, ATHROW, DLOAD, DUP, FLOAD, IFNE, ILOAD, INVOKESPECIAL, INVOKESTATIC, LDC_W, LLOAD, NEW}
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac._
import org.opalj.value.ValueInformation
import space.kiibou.byteguard.bytecode.BytecodeWeaver.PredicateViolationExceptionType
import space.kiibou.byteguard.bytecode.producer.BytecodeProducer
import space.kiibou.byteguard.bytecode.{BytecodeWeaver, Location, PreCondition}
import space.kiibou.byteguard.specification.method.MethodSpecComponent

import scala.collection.immutable.ArraySeq

class RequiresPredicateProducer(private val weaver: BytecodeWeaver,
                                private val method: Method,
                                private val predicates: Array[MethodSpecComponent.RequiresPredicate]) extends BytecodeProducer {

    private var genMethods: MethodTemplates = ArraySeq.empty

    private val genBytecode: Seq[(Location, Seq[CodeElement[AnyRef]])] = generateBytecode()

    override def fields(): FieldTemplates = ArraySeq.empty

    override def methods(): MethodTemplates = genMethods

    private case class RequiresPredicateData(receiverType: ReferenceType, lambdaName: String, lambdaMethodDescriptor: MethodDescriptor, isInstanceCapturing: Boolean, capturedParameters: Seq[(Int, FieldType)])

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = genBytecode

    private def generateBytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = {
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
        val specMethod = weaver.specClassFile.methodsWithBody.filter {
            m => m.name == method.name && m.descriptor.parameterTypes == method.descriptor.parameterTypes
        }.next()

        val tacKey = weaver.project.get(ComputeTACAIKey)

        val result: AITACode[TACMethodParameter, ValueInformation] = tacKey(specMethod)

        val originParameterMap: Map[ValueOrigin, (Int, FieldType)] = getOriginParameterMap(method)

        val predicatesData: Seq[RequiresPredicateData] = (for {
            Assignment(_, _, VirtualFunctionCall(_, _, _, "requires", requiresPredicateGuardMethodDescriptor, _, ArraySeq(UVar(_, defSites)))) <- result.stmts
            (Assignment(_, _, InvokedynamicFunctionCall(_, BootstrapMethod(_, ArraySeq(_, InvokeStaticMethodHandle(receiverType, _, lambdaName, lambdaMethodDescriptor), _)), _, _, capturedValues: Seq[UVar[ValueInformation]])), index) <- result.stmts.zipWithIndex if defSites.contains(index)
            // TODO (Svenja, 2023/01/24): Currently we only handle invokestatic calls to the lambda, so instance capturing lambdas would not be processed correctly
        } yield RequiresPredicateData(
            receiverType,
            lambdaName,
            lambdaMethodDescriptor,
            isInstanceCapturing(capturedValues),
            getCapturedParameters(specMethod, defSites, originParameterMap, capturedValues)
        )).toSeq

        for (predicateData <- predicatesData) yield {
            genMethods :++= weaver.specClassFile.findMethod(predicateData.lambdaName, predicateData.lambdaMethodDescriptor).map { m =>
                m.copy(accessFlags = m.accessFlags ^ ACC_SYNTHETIC.mask, name = m.name + "_guard_predicate")
            }

            val labelName = weaver.getUniqueSymbol("predicate_is_true")

            PreCondition -> Seq(
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
                    INVOKESTATIC(weaver.classFile.thisType, isInterface = false, predicateData.lambdaName + "_guard_predicate", predicateData.lambdaMethodDescriptor),
                    IFNE(labelName),
                    NEW(PredicateViolationExceptionType),
                    DUP,
                    LDC_W(ConstantString(s"Predicate Violation: Guard Predicate in method ${weaver.classFile.thisType.toJava}#${method.name} failed.")),
                    INVOKESPECIAL(PredicateViolationExceptionType, isInterface = false, "<init>", JustTakes(ObjectType.String)),
                    ATHROW,
                    labelName
                )
            ).flatten
        }
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
                throw new IllegalStateException(s"Method ${weaver.classFile.thisType.fqn}#${specMethod.name}(${specMethod.descriptor.parameterTypes.mkString(", ")}) contains invokedynamic with " +
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

            currentIndex -= paramType.operandSize

            index -> (argIndex + 1, paramType)
        }.toMap
    }
}
