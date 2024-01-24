package space.kiibou.byteguard.processor.info

import kotlinx.serialization.Serializable
import space.kiibou.byteguard.Guard
import space.kiibou.byteguard.specification.GuardSpec
import space.kiibou.byteguard.specification.method.MethodSpec
import java.lang.reflect.Field
import java.lang.reflect.Method

@Serializable
data class ClassSpecificationInfo(
    val specClassType: ClassifierType,
    val targetClassType: ClassifierType,
    val methodSpecs: List<MethodSpecificationInfo>
) {

    fun getSpecClass(classLoader: ClassLoader): Class<out GuardSpec> {
        @Suppress("UNCHECKED_CAST")
        val specClass: Class<out GuardSpec> = classLoader.loadClass(specClassType.javaName) as Class<out GuardSpec>

        return specClass
    }

    fun getGuardFields(classLoader: ClassLoader): List<Field> {
        val guardClass: Class<Guard> = Guard::class.java
        val specClass: Class<out GuardSpec> = getSpecClass(classLoader)

        val guardFields: List<Field> = specClass.fields
            .filter { it.declaringClass == specClass }
            .filter { it.type == guardClass }

        return guardFields
    }

    fun getSpecInstance(classLoader: ClassLoader): GuardSpec {
        val specClass = getSpecClass(classLoader)
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
data class MethodInfo(
    val specMethodName: String,
    val targetMethodName: String,
    val targetReturnType: Type,
    val argumentTypes: List<Type>
) {

    fun getSpecMethod(javaClass: Class<out GuardSpec>): Method {
        return javaClass.getMethod(specMethodName, *getArgumentTypeClasses())
    }

    fun getArgumentTypeClasses(): Array<Class<*>> {
        return argumentTypes.map(Type::toJavaClass).toTypedArray()
    }

}
