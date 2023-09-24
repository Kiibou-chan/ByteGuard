package space.kiibou.byteguard.agent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import space.kiibou.byteguard.exception.PredicateViolationException
import kotlin.test.assertEquals

class InstanceCapturingReqPredTest {

    @Test
    fun testObjectCreation() {
        InstanceCapturingReqPred("abc", "ab")
    }

    @Test
    fun testUnitMethodSuccess() {
        val obj = InstanceCapturingReqPred("abc", "ab")

        assertEquals(obj.unit(), "abc")
    }

    @Test
    fun testUnitMethodFailure() {
        val obj = InstanceCapturingReqPred("bbc", "ab")

        assertThrows<PredicateViolationException> { obj.unit() }
    }

    @Test
    fun testConcat1MethodSuccess() {
        val obj = InstanceCapturingReqPred("abc", "ab")

        assertEquals(obj.concat("abd"), "abcabd")
    }

    @Test
    fun testConcat1MethodFailure() {
        val obj = InstanceCapturingReqPred("abc", "ab")

        assertThrows<PredicateViolationException> { obj.concat("xyz") }
    }

    @Test
    fun testConcat3MethodSuccess() {
        val obj = InstanceCapturingReqPred("abc", "ab")

        assertEquals(obj.concat("ab1", "ab2", "ab3"), "abcab1ab2ab3")
    }

    @Test
    fun testConcat3MethodFailure() {
        val obj = InstanceCapturingReqPred("abc", "ab")

        assertThrows<PredicateViolationException> { obj.concat("xxx", "abc", "abc") }
        assertThrows<PredicateViolationException> { obj.concat("abc", "xxx", "abc") }
        assertThrows<PredicateViolationException> { obj.concat("abc", "abc", "xxx") }
    }

}
