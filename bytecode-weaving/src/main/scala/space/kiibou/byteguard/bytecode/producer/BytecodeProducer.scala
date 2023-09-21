package space.kiibou.byteguard.bytecode.producer

import org.opalj.ba.CodeElement
import org.opalj.br.{FieldTemplates, MethodTemplates}

trait Location

case object PreCondition extends Location
case object PostCondition extends Location

trait BytecodeProducer {

    def fields(): FieldTemplates

    def methods(): MethodTemplates

    def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])]

}
