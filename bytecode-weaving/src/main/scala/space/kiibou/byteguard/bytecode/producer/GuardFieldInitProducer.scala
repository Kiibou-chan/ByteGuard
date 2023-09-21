package space.kiibou.byteguard.bytecode.producer
import org.opalj.ba.CodeElement
import org.opalj.br.instructions.{ALOAD_0, ICONST_0, ICONST_1, PUTFIELD}
import org.opalj.br.{BooleanType, Field, FieldTemplates, MethodTemplates}
import space.kiibou.byteguard.Guard
import space.kiibou.byteguard.bytecode.BytecodeWeaver

import scala.collection.immutable.ArraySeq

class GuardFieldInitProducer(private val weaver: BytecodeWeaver) extends BytecodeProducer {
    override def fields(): FieldTemplates = ArraySeq.empty

    override def methods(): MethodTemplates = ArraySeq.empty

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = {
        weaver.guardFields.map { field =>
            val code = Seq[CodeElement[AnyRef]](
                ALOAD_0,
                if (field.get(weaver.spec).asInstanceOf[Guard].initialState() == Guard.State.SET) ICONST_1 else ICONST_0,
                PUTFIELD(weaver.classFile.thisType, field.getName, BooleanType)
            )

            (PreCondition, code)
        }
    }
}
