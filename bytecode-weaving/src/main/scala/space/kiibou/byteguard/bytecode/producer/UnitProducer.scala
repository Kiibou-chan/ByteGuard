package space.kiibou.byteguard.bytecode.producer

import org.opalj.ba.CodeElement
import org.opalj.br.{FieldTemplates, MethodTemplates}

import scala.collection.immutable.ArraySeq

class UnitProducer extends BytecodeProducer {
    override def fields(): FieldTemplates = ArraySeq.empty

    override def methods(): MethodTemplates = ArraySeq.empty

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = Seq.empty
}
