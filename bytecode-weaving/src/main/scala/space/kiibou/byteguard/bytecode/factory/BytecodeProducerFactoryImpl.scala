package space.kiibou.byteguard.bytecode.factory

import org.opalj.br.Method
import space.kiibou.byteguard.bytecode.BytecodeWeaver
import space.kiibou.byteguard.bytecode.producer._
import space.kiibou.byteguard.bytecode.producer.impl._
import space.kiibou.byteguard.specification.method.MethodSpecComponent
import space.kiibou.byteguard.specification.method.MethodSpecComponent.{RequiresGuardState, WhenComponent}

class BytecodeProducerFactoryImpl(private val weaver: BytecodeWeaver) extends BytecodeProducerFactory {

    def createGuardFieldProducer(): BytecodeProducer = {
        new GuardFieldProducer(weaver)
    }

    override def createMethodTransformer(method: Method): BytecodeProducer = {
        new MethodTransformer(weaver, this, method)
    }

    override def createGuardFieldInitProducer(): BytecodeProducer = {
        new GuardFieldInitProducer(weaver)
    }

    override def createRequiresGuardStateProducer(requires: RequiresGuardState): BytecodeProducer = {
        new RequiresGuardStateProducer(weaver, requires)
    }

    override def createWhenProducer(method: Method, when: WhenComponent): BytecodeProducer = {
        new WhenProducer(weaver, method, when)
    }

    override def createRequiresPredicateProducer(method: Method, predicates: Array[MethodSpecComponent.RequiresPredicate]): BytecodeProducer = {
        if (predicates.isEmpty) {
            new UnitProducer()
        } else {
            new RequiresPredicateProducer(weaver, method, predicates)
        }
    }

}
