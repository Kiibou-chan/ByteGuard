package space.kiibou.byteguard.bytecode

import org.opalj.br.Method
import space.kiibou.byteguard.bytecode.producer.BytecodeProducer
import space.kiibou.byteguard.specification.method.MethodSpecComponent
import space.kiibou.byteguard.specification.method.MethodSpecComponent.{RequiresGuardState, WhenComponent}

trait BytecodeProducerFactory {
    def createGuardFieldProducer(): BytecodeProducer

    def createMethodTransformer(method: Method): BytecodeProducer

    def createGuardFieldInitProducer(): BytecodeProducer

    def createRequiresGuardStateProducer(requires: RequiresGuardState): BytecodeProducer

    def createWhenProducer(method: Method, when: WhenComponent): BytecodeProducer

    def createRequiresPredicateProducer(method: Method, predicates: Array[MethodSpecComponent.RequiresPredicate]): BytecodeProducer

}
