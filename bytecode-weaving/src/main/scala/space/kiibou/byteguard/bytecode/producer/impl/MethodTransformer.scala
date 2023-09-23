package space.kiibou.byteguard.bytecode.producer.impl

import org.opalj.ba.{CodeElement, InsertionPosition, LabeledCode}
import org.opalj.br.instructions.{GOTO, ReturnInstruction}
import org.opalj.br.{FieldTemplates, Method, MethodTemplates, PCAndInstruction}
import space.kiibou.byteguard.bytecode.factory.BytecodeProducerFactory
import space.kiibou.byteguard.bytecode.producer.BytecodeProducer
import space.kiibou.byteguard.bytecode.{BytecodeWeaver, Location, PostCondition, PreCondition}
import space.kiibou.byteguard.specification.method.MethodSpecComponent
import space.kiibou.byteguard.specification.method.MethodSpecComponent.{RequiresGuardState, RequiresPredicate, WhenComponent}

import scala.collection.immutable.ArraySeq

class MethodTransformer(private val weaver: BytecodeWeaver,
                        private val factory: BytecodeProducerFactory,
                        private val method: Method) extends BytecodeProducer {

    private val components: Array[MethodSpecComponent] = {
        if (weaver.methodSpecs.contains(method.name -> method.parameterTypes)) {
            weaver.methodSpecs(method.name -> method.parameterTypes).components()
        } else {
            Array.empty
        }
    }

    private var genFields: FieldTemplates = ArraySeq.empty

    private var genMethods: MethodTemplates = ArraySeq.empty

    transform()

    private def transform(): Unit = {
        if (components.isEmpty) {
            genMethods :+= method.copy()
        } else {
            val producers: Seq[BytecodeProducer] = getProducers

            genFields :++= producers.flatMap(_.fields())
            genMethods :++= producers.flatMap(_.methods())

            val codes = producers.flatMap(_.bytecode())

            val labeledCode = LabeledCode(method.body.get)

            insertPreConditions(codes, labeledCode)
            insertPostConditions(codes, labeledCode)

            val (newCode, _) = labeledCode.result(weaver.classFile.version, method)

            genMethods :+= method.copy(body = Some(newCode))
        }
    }

    private def getProducers: Seq[BytecodeProducer] = {
        var producers: Seq[BytecodeProducer] = Seq.empty

        if (method.name == "<init>") {
            producers :+= factory.createGuardFieldInitProducer()
        }

        producers :++= components.collect {
            case requires: RequiresGuardState => factory.createRequiresGuardStateProducer(requires)
            case when: WhenComponent => factory.createWhenProducer(method, when)
        }

        producers :+= factory.createRequiresPredicateProducer(method,
            components.collect {
                case requires: RequiresPredicate => requires
            }
        )

        producers
    }

    private def insertPreConditions(codes: Seq[(Location, Seq[CodeElement[AnyRef]])], labeledCode: LabeledCode): Unit = {
        for ((PreCondition, bytecode) <- codes) {
            labeledCode.insert(0, InsertionPosition.Before, bytecode)
        }
    }

    private def insertPostConditions(codes: Seq[(Location, Seq[CodeElement[AnyRef]])], labeledCode: LabeledCode): Unit = {
        if (codes.exists { case (loc, _) => loc == PostCondition }) {
            val returnTarget = weaver.getUniqueSymbol("return_jump_target")

            val returnPCs = for (PCAndInstruction(pc, _: ReturnInstruction) <- labeledCode.originalCode) yield pc

            labeledCode.insert(
                returnPCs.last,
                InsertionPosition.Before,
                Seq[CodeElement[AnyRef]](returnTarget) :++ (for ((PostCondition, bytecode) <- codes) yield bytecode).flatten
            )

            for (pc <- returnPCs.toList.reverse.tail) {
                labeledCode.replace(pc, Seq(GOTO(returnTarget)))
            }
        }
    }

    override def fields(): FieldTemplates = genFields

    override def methods(): MethodTemplates = genMethods

    override def bytecode(): Seq[(Location, Seq[CodeElement[AnyRef]])] = Seq.empty

}
