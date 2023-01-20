package space.kiibou.jguard.agent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import space.kiibou.jguard.exception.GuardViolationException
import space.kiibou.jguard.test.GuardedMethod

class GuardedMethodTest {

    @Test
    fun testCreateGuardedMethodObject() {
        val obj = GuardedMethod()

        assertGuard(false, "canCallMethod", obj)
    }

    @Test
    fun testThrowsGuardViolationException() {
        val obj = GuardedMethod()

        assertThrows<GuardViolationException> {
            obj.method()
        }
    }

    @Test
    fun testMethodAfterToggle() {
        val obj = GuardedMethod()

        obj.toggleMethod()

        assertGuard(true, "canCallMethod", obj)

        obj.method()

        assertGuard(true, "canCallMethod", obj)

        obj.toggleMethod()

        assertGuard(false, "canCallMethod", obj)
    }

}