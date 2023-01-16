package space.kiibou.jguard.processor.info

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Type {
    val jvmName: String

    val javaName: String

    fun toJavaClass(): Class<*>
}

@Serializable
sealed interface PrimitiveType : Type

@Serializable
object BooleanType : PrimitiveType {
    override val jvmName: String = "Z"
    override val javaName: String = "boolean"

    override fun toJavaClass(): Class<*> = Boolean::class.java
}

@Serializable
object ByteType : PrimitiveType {
    override val jvmName: String = "B"
    override val javaName: String = "byte"

    override fun toJavaClass(): Class<*> = Byte::class.java
}

@Serializable
object ShortType : PrimitiveType {
    override val jvmName: String = "S"
    override val javaName: String = "short"

    override fun toJavaClass(): Class<*> = Short::class.java
}

@Serializable
object IntType : PrimitiveType {
    override val jvmName: String = "I"
    override val javaName: String = "int"

    override fun toJavaClass(): Class<*> = Int::class.java
}

@Serializable
object LongType : PrimitiveType {
    override val jvmName: String = "J"
    override val javaName: String = "long"

    override fun toJavaClass(): Class<*> = Long::class.java
}

@Serializable
object CharType : PrimitiveType {
    override val jvmName: String = "C"
    override val javaName: String = "char"

    override fun toJavaClass(): Class<*> = Char::class.java
}

@Serializable
object FloatType : PrimitiveType {
    override val jvmName: String = "F"
    override val javaName: String = "float"

    override fun toJavaClass(): Class<*> = Float::class.java
}

@Serializable
object DoubleType : PrimitiveType {
    override val jvmName: String = "D"
    override val javaName: String = "double"

    override fun toJavaClass(): Class<*> = Double::class.java
}

@Serializable
object VoidType : PrimitiveType {
    override val jvmName: String = "V"
    override val javaName: String = "void"

    override fun toJavaClass(): Class<*> = Void::class.java
}

@Serializable
data class ArrayType(@SerialName("wrappedType") val type: Type) : Type {
    override val jvmName: String
        get() = "[${type.jvmName}"

    override val javaName: String
        get() = "${type.javaName}[]"

    override fun toJavaClass(): Class<*> = Array<Any>::class.java

    override fun toString(): String {
        return javaName
    }

}

@Serializable
data class ClassifierType(private val name: String) : Type {
    override val jvmName: String
        get() = "L${name.replace('.', '/')};"

    override val javaName: String
        get() = name.replace('/', '.')

    override fun toJavaClass(): Class<*> = Class.forName(javaName)

    override fun toString(): String {
        return javaName
    }

}
