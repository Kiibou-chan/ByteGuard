package space.kiibou.byteguard.bytecode.producer

import org.opalj.ba.CodeElement
import org.opalj.bi.ACC_PUBLIC
import org.opalj.br.{BooleanType, Field, FieldTemplates, MethodTemplates}
import space.kiibou.byteguard.bytecode.BytecodeWeaver

import scala.collection.immutable.ArraySeq

class GuardFieldProducer(protected val weaver: BytecodeWeaver) extends BytecodeProducer {

    override def fields(): FieldTemplates =
        weaver.indexGuardNameMap.map { case (_, name) =>
            Field(ACC_PUBLIC.mask, name, BooleanType)
        }.to(ArraySeq)

    override def methods(): MethodTemplates = ArraySeq.empty

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = Seq.empty

}
