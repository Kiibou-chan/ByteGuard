package space.kiibou.byteguard.agent

import org.junit.jupiter.api.assertThrows
import space.kiibou.byteguard.exception.PredicateViolationException
import kotlin.test.Test
import kotlin.test.assertEquals

class PredicateGuardedMethodsTest {

    @Test
    fun testMethodThrowsException() {
        val obj = PredicateGuardedMethods()

        assertThrows<PredicateViolationException> {
            obj.method()
        }
    }

    @Test
    fun testMethodWithArgThrowsException() {
        val obj = PredicateGuardedMethods()

        assertThrows<PredicateViolationException> {
            obj.method("string not starting with a")
        }
    }

    @Test
    fun testMethodWithArgWorksWithAStartingString() {
        val obj = PredicateGuardedMethods()

        assertEquals("abc", obj.method("abc"))
    }

}
