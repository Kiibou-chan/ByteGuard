package space.kiibou.byteguard.util

import org.junit.jupiter.api.Assertions.{assertEquals, fail}
import space.kiibou.byteguard.exception.PredicateViolationException

import java.lang.reflect.InvocationTargetException
import java.nio.file.{Files, Path, StandardOpenOption}

object TestUtil {

    def loadClassBytes(name: String): Array[Byte] = {
        ClassLoader.getSystemClassLoader.getResourceAsStream(name).readAllBytes()
    }

    def dumpClasses(classes: Map[String, Array[Byte]]): Unit = {
        for ((name, bytes) <- classes) {
            val path = Path.of(s"./build/test/${name.replace('.', '/')}.class")

            path.getParent.toFile.mkdirs()

            Files.write(
                path,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
        }
    }

    def assertViolatesPredicate(f: => Unit): Unit = {
        try {
            f

            fail()
        } catch {
            case ex: PredicateViolationException =>
            case ex: InvocationTargetException =>
                assertEquals(ex.getCause.getClass, classOf[PredicateViolationException])
        }
    }

}
