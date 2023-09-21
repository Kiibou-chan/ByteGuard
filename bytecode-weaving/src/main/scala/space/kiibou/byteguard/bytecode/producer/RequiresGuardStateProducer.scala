package space.kiibou.byteguard.bytecode.producer

import org.opalj.ba.{CodeElement, InsertionPosition}
import org.opalj.br.MethodDescriptor.JustTakes
import org.opalj.br.instructions.{ALOAD_0, ATHROW, DUP, GETFIELD, IFEQ, IFNE, INVOKESPECIAL, LDC_W, NEW}
import org.opalj.br._
import space.kiibou.byteguard.Guard
import space.kiibou.byteguard.bytecode.BytecodeWeaver
import space.kiibou.byteguard.bytecode.BytecodeWeaver.GuardViolationExceptionType
import space.kiibou.byteguard.specification.method.MethodSpecComponent.RequiresGuardState

import scala.collection.immutable.ArraySeq

class RequiresGuardStateProducer(private val weaver: BytecodeWeaver,
                                 private val requires: RequiresGuardState) extends BytecodeProducer {
    override def fields(): FieldTemplates = ArraySeq.empty

    override def methods(): MethodTemplates = ArraySeq.empty

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = {
        val name = weaver.indexGuardNameMap(requires.guardState().guard().index())

        val labelName = weaver.getUniqueSymbol(s"${name}_correctly_set")

        Seq((PreCondition, Seq[CodeElement[AnyRef]](
            ALOAD_0,
            GETFIELD(weaver.classFile.thisType, name, BooleanType), // push state of the guard
            if (requires.guardState().state() == Guard.State.SET) IFNE(labelName) else IFEQ(labelName),
            NEW(GuardViolationExceptionType),
            DUP,
            LDC_W(ConstantString(s"Guard Violation: Guard $name should be ${requires.guardState().state()} but was ${requires.guardState().state().negate()}")),
            INVOKESPECIAL(GuardViolationExceptionType, isInterface = false, "<init>", JustTakes(ObjectType.String)),
            ATHROW,
            labelName
        )))
    }
}
