package space.kiibou.byteguard.agent

import org.junit.jupiter.api.assertThrows
import space.kiibou.byteguard.exception.GuardViolationException
import kotlin.test.assertEquals

class MultipleReqPredicatesTest {

    fun testCreateObject() {
        MultipleReqPredicates()
    }

    fun testConcatWorks() {
        val obj = MultipleReqPredicates()

        assertEquals(obj.concat("a", 1.0, 10), "a1.010")
    }

    fun testThrowsExceptions() {
        val obj = MultipleReqPredicates()

        assertThrows<GuardViolationException> { obj.concat(null, 1.0, 10) }
        assertThrows<GuardViolationException> { obj.concat("a", null, 10) }
        assertThrows<GuardViolationException> { obj.concat("a", 1.0, null) }
    }

}
