package space.kiibou.jguard.agent

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import space.kiibou.jguard.exception.GuardViolationException
import space.kiibou.jguard.test.IteratorWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentTest {

    @Test
    fun testCreateIteratorWrapper() {
        val iter = IteratorWrapper(listOf("a", "b", "c").iterator())

        assertGuard(isSet = false, "canCallNext", iter)
    }

    @Test
    fun testCanCallNextGuardFieldExists() {
        val wrapperClass = IteratorWrapper::class.java

        wrapperClass.getDeclaredField("canCallNext")
    }

    @Test
    fun testNextWithoutHasNextThrowsException() {
        val iter = IteratorWrapper(listOf("a", "b", "c").iterator())

        assertThrows<GuardViolationException> { iter.next() }
    }

    @Test
    fun testNextWithHasNext() {
        val iter = IteratorWrapper(listOf("a", "b", "c").iterator())

        iter.hasNext()

        assertGuard(isSet = true, "canCallNext", iter)

        assertEquals("a", iter.next())

        assertGuard(isSet = false, "canCallNext", iter)
    }

    private inline fun <reified T> assertGuard(isSet: Boolean, name: String, obj: T) {
        val field = T::class.java.getDeclaredField(name)
        if (field.canAccess(obj) || field.trySetAccessible()) {
            val guardValue = field.get(obj)
            assertEquals(isSet, guardValue, "Expected $name in ${T::class.java.simpleName} to be set to $isSet, but was set to $guardValue")
        } else {
            fail { "Could not access field $name on class ${T::class.java.simpleName}" }
        }
    }

}
