package space.kiibou.byteguard.bytecode

import org.junit.jupiter.api.Test
import org.opalj.util.InMemoryClassLoader
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import space.kiibou.byteguard.processor.info._

import java.io.File
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.jdk.CollectionConverters.SeqHasAsJava

// @RunWith(classOf[JUnitRunner])
class BytecodeWeaverTest extends AnyFlatSpec with Matchers {

    private def loadClassBytes(name: String): Array[Byte] = {
        ClassLoader.getSystemClassLoader.getResourceAsStream(name).readAllBytes()
    }

    @Test
    def generateIteratorWrapperCode(): Unit = {
        val iteratorWrapperBytes = loadClassBytes("space/kiibou/byteguard/bytecode/IteratorWrapper.class")

        val specInfo = new ClassSpecificationInfo(
            new ClassifierType("space.kiibou.byteguard.bytecode.IteratorGuardSpec"),
            new ClassifierType("space.kiibou.byteguard.bytecode.IteratorWrapper"),
            List(
                new MethodSpecificationInfo(
                    new MethodInfo(
                        "hasNext",
                        "hasNext",
                        new ClassifierType("java/lang/Object"),
                        List().asJava
                    )
                ),
                new MethodSpecificationInfo(
                    new MethodInfo(
                        "next",
                        "next",
                        BooleanType.INSTANCE,
                        List().asJava
                    )
                )
            ).asJava
        )

        val weaver = new BytecodeWeaver(iteratorWrapperBytes, specInfo)

        val newBytecode = weaver.weave()

        new File("./build/test/space/kiibou/byteguard/bytecode/").mkdirs()

        Files.write(
            Path.of("./build/test/space/kiibou/byteguard/bytecode/IteratorWrapper.class"),
            newBytecode,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )

        val loader = new InMemoryClassLoader(
            Map(
                "space.kiibou.byteguard.bytecode.IteratorWrapper" -> newBytecode
            )
        )

        loader.loadClass("space.kiibou.byteguard.bytecode.IteratorWrapper")
    }

    @Test
    def generateGuardedMethodCode(): Unit = {
        val iteratorWrapperBytes = loadClassBytes("space/kiibou/byteguard/bytecode/GuardedMethod.class")

        val specInfo = new ClassSpecificationInfo(
            new ClassifierType("space.kiibou.byteguard.bytecode.GuardedMethod$GuardedMethodSpec"),
            new ClassifierType("space.kiibou.byteguard.bytecode.GuardedMethod"),
            List(
                new MethodSpecificationInfo(
                    new MethodInfo(
                        "toggleMethod",
                        "toggleMethod",
                        VoidType.INSTANCE,
                        List().asJava
                    )
                ),
                new MethodSpecificationInfo(
                    new MethodInfo(
                        "method",
                        "method",
                        VoidType.INSTANCE,
                        List().asJava
                    )
                )
            ).asJava
        )

        val weaver = new BytecodeWeaver(iteratorWrapperBytes, specInfo)

        val newBytecode = weaver.weave()

        new File("./build/test/space/kiibou/byteguard/bytecode/").mkdirs()

        Files.write(
            Path.of("./build/test/space/kiibou/byteguard/bytecode/GuardedMethod.class"),
            newBytecode,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )

        val loader = new InMemoryClassLoader(
            Map(
                "space.kiibou.byteguard.bytecode.GuardedMethod" -> newBytecode
            )
        )

        loader.loadClass("space.kiibou.byteguard.bytecode.GuardedMethod")
    }

    @Test
    def generatePredicateGuardedMethodCode(): Unit = {
        val iteratorWrapperBytes = loadClassBytes("space/kiibou/byteguard/bytecode/PredicateGuardedMethods.class")

        val specInfo = new ClassSpecificationInfo(
            new ClassifierType("space.kiibou.byteguard.bytecode.PredicateGuardedMethods$PredicateGuardedMethodsSpec"),
            new ClassifierType("space.kiibou.byteguard.bytecode.PredicateGuardedMethods"),
            List(
                new MethodSpecificationInfo(
                    new MethodInfo(
                        "method",
                        "method",
                        VoidType.INSTANCE,
                        List().asJava
                    )
                ),
                new MethodSpecificationInfo(
                    new MethodInfo(
                        "method",
                        "method",
                        VoidType.INSTANCE,
                        List(new ClassifierType("java/lang/String")).asJava
                    )
                )
            ).asJava
        )

        val weaver = new BytecodeWeaver(iteratorWrapperBytes, specInfo)

        val newBytecode = weaver.weave()

        new File("./build/test/space/kiibou/byteguard/bytecode/").mkdirs()

        Files.write(
            Path.of("./build/test/space/kiibou/byteguard/bytecode/PredicateGuardedMethods.class"),
            newBytecode,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )

        val loader = new InMemoryClassLoader(
            Map(
                ("space.kiibou.byteguard.bytecode.PredicateGuardedMethods", newBytecode)
            )
        )

        val cls = loader.findClass("space.kiibou.byteguard.bytecode.PredicateGuardedMethods")
        val method = cls.getMethod("method", classOf[String])

        val instance = cls.getDeclaredConstructor().newInstance()

        method.invoke(instance, "a")
        method.invoke(instance, "b")
    }


}
