package space.kiibou.byteguard.agent

import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

inline fun <reified T> assertGuard(isSet: Boolean, name: String, obj: T) {
    val field = T::class.java.getDeclaredField(name)
    if (field.canAccess(obj) || field.trySetAccessible()) {
        val guardValue = field.get(obj)
        assertEquals(
            isSet,
            guardValue,
            "Expected guard $name in ${T::class.java.simpleName} to be set to $isSet, but was set to $guardValue"
        )
    } else {
        fail { "Could not access field $name on class ${T::class.java.simpleName}" }
    }
}
