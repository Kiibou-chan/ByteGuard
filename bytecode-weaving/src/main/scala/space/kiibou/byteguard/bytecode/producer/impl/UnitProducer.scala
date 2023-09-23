package space.kiibou.byteguard.bytecode.producer.impl

import org.opalj.ba.CodeElement
import org.opalj.br.{FieldTemplates, MethodTemplates}
import space.kiibou.byteguard.bytecode.Location
import space.kiibou.byteguard.bytecode.producer.BytecodeProducer

import scala.collection.immutable.ArraySeq

class UnitProducer extends BytecodeProducer {
    override def fields(): FieldTemplates = ArraySeq.empty

    override def methods(): MethodTemplates = ArraySeq.empty

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = Seq.empty
}
