package space.kiibou.jguard.processor.info

import kotlinx.serialization.Serializable
import space.kiibou.jguard.Guard
import space.kiibou.jguard.specification.GuardSpec
import space.kiibou.jguard.specification.method.MethodSpec
import java.lang.reflect.Field
import java.lang.reflect.Method

@Serializable
data class ClassSpecificationInfo(val specClassType: ClassifierType, val targetClassType: ClassifierType, val methodSpecs: List<MethodSpecificationInfo>) {

    fun getSpecClass(): Class<out GuardSpec> {
        @Suppress("UNCHECKED_CAST")
        val specClass: Class<out GuardSpec> = Class.forName(specClassType.javaName) as Class<out GuardSpec>

        return specClass
    }

    fun getGuardFields(): List<Field> {
        val guardClass: Class<Guard> = Guard::class.java
        val specClass: Class<out GuardSpec> = getSpecClass()

        val guardFields: List<Field> = specClass.fields
            .filter { it.declaringClass == specClass }
            .filter { it.type == guardClass }

        return guardFields
    }

    fun getSpecInstance(): GuardSpec {
        val specClass = getSpecClass()
        val constructor = specClass.getDeclaredConstructor()
        constructor.trySetAccessible()

        val returnVar = constructor.newInstance()

        return returnVar
    }

}

@Serializable
data class MethodSpecificationInfo(val methodInfo: MethodInfo) {

    val binaryMethodName: String
        get() = "(${methodInfo.argumentTypes.joinToString { it.jvmName }})${methodInfo.targetReturnType.jvmName}"

    fun getMethodSpec(javaClass: Class<out GuardSpec>, guardSpec: GuardSpec): MethodSpec {
        val specMethod = methodInfo.getSpecMethod(javaClass)
        specMethod.trySetAccessible()

        return specMethod.invoke(guardSpec, *Array<Any?>(specMethod.parameterCount) { null }) as MethodSpec
    }

}

@Serializable
data class MethodInfo(val specMethodName: String, val targetMethodName: String, val targetReturnType: Type, val argumentTypes: List<Type>) {

    fun getSpecMethod(javaClass: Class<out GuardSpec>): Method {
        return javaClass.getMethod(specMethodName, *getArgumentTypeClasses())
    }

    fun getArgumentTypeClasses(): Array<Class<*>> {
        return argumentTypes.map(Type::toJavaClass).toTypedArray()
    }

}
