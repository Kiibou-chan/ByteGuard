package space.kiibou.byteguard.agent

import org.junit.jupiter.api.assertThrows
import space.kiibou.byteguard.exception.GuardViolationException
import kotlin.test.Test
import kotlin.test.assertEquals

class IteratorWrapperTest {

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

}
