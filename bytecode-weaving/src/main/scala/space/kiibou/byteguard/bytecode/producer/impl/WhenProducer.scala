package space.kiibou.byteguard.bytecode.producer.impl

import org.opalj.ba.CodeElement
import org.opalj.br._
import org.opalj.br.instructions.{ALOAD_0, DCMPL, DUP, DUP2, FCMPL, GETFIELD, GOTO, ICONST_0, ICONST_1, IFEQ, IFNE, IF_ICMPNE, LCMP, LDC_W, PUTFIELD}
import space.kiibou.byteguard.Guard
import space.kiibou.byteguard.bytecode.{BytecodeWeaver, Location, PostCondition}
import space.kiibou.byteguard.bytecode.producer.BytecodeProducer
import space.kiibou.byteguard.specification.method.MethodSpecComponent.WhenComponent
import space.kiibou.byteguard.specification.method.{MethodSpecComponent, ReturnsPredicate, WhenCondition, WhenConsequence}

import scala.collection.immutable.ArraySeq

class WhenProducer(private val weaver: BytecodeWeaver, private val method: Method, private val whenComponent: WhenComponent) extends BytecodeProducer {
    override def fields(): FieldTemplates = ArraySeq.empty

    override def methods(): MethodTemplates = ArraySeq.empty

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = {
        val jumpTarget = weaver.getUniqueSymbol("predicate")
        val condition = whenComponent.condition()

        val conditionCode: Seq[CodeElement[AnyRef]] = getWhenConditionCode(method, jumpTarget, condition)

        val consequenceCode: Seq[CodeElement[AnyRef]] = whenComponent match {
            case consequence: MethodSpecComponent.WhenThenConsequence =>
                getWhenConsequenceCode(method, consequence.consequence()) :++
                    Seq[CodeElement[AnyRef]](jumpTarget)
            case consequence: MethodSpecComponent.WhenThenElseConsequence =>
                val endTarget = weaver.getUniqueSymbol("when_end")

                getWhenConsequenceCode(method, consequence.thenConsequence()) :++
                    Seq[CodeElement[AnyRef]](GOTO(endTarget), jumpTarget) :++
                    getWhenConsequenceCode(method, consequence.elseConsequence()) :++
                    Seq[CodeElement[AnyRef]](endTarget)
        }

        Seq((PostCondition, conditionCode :++ consequenceCode))
    }

    private def getWhenConditionCode(method: Method, jumpTarget: Symbol, condition: WhenCondition): Seq[CodeElement[AnyRef]] = {
        condition match {
            case condition: WhenCondition.GuardCondition => Seq(
                ALOAD_0,
                GETFIELD(method.classFile.thisType, weaver.indexGuardNameMap(condition.guardState().guard().index()), BooleanType),
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
                ALOAD_0, // ..., guard (1)
                ICONST_0, // ..., guard (1), val (1)
                PUTFIELD(method.classFile.thisType, weaver.indexGuardNameMap(resets.guard().index()), BooleanType) // ...
            )
            case sets: WhenConsequence.SetsGuard => Seq(
                ALOAD_0, // ..., guard (1)
                ICONST_1, // ..., guard (1), val (1)
                PUTFIELD(method.classFile.thisType, weaver.indexGuardNameMap(sets.guard().index()), BooleanType) // ...
            )
        }
    }

}
