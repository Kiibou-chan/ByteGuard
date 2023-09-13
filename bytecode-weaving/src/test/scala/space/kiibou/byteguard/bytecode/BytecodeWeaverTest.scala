package space.kiibou.byteguard.bytecode

import org.junit.jupiter.api.Test
import org.opalj.util.InMemoryClassLoader
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import space.kiibou.byteguard.processor.info._
import space.kiibou.byteguard.util.TestUtil.{assertViolatesPredicate, dumpClasses, loadClassBytes}

import scala.jdk.CollectionConverters.SeqHasAsJava

class BytecodeWeaverTest extends AnyFlatSpec with Matchers {

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

        val newBytecode = new BytecodeWeaver(iteratorWrapperBytes, specInfo).weave()

        val classesMap = Map(
            "space.kiibou.byteguard.bytecode.IteratorWrapper" -> newBytecode
        )

        dumpClasses(classesMap)

        val loader = new InMemoryClassLoader(classesMap)

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

        val newBytecode = new BytecodeWeaver(iteratorWrapperBytes, specInfo).weave()

        val classesMap = Map(
            "space.kiibou.byteguard.bytecode.GuardedMethod" -> newBytecode
        )

        dumpClasses(classesMap)

        val loader = new InMemoryClassLoader(classesMap)

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

        val newBytecode = new BytecodeWeaver(iteratorWrapperBytes, specInfo).weave()

        val classMap = Map(
            ("space.kiibou.byteguard.bytecode.PredicateGuardedMethods", newBytecode)
        )

        dumpClasses(classMap)

        val loader = new InMemoryClassLoader(classMap)

        val cls = loader.findClass("space.kiibou.byteguard.bytecode.PredicateGuardedMethods")
        val method = cls.getMethod("method", classOf[String])
        method.trySetAccessible()

        val instance = cls.getDeclaredConstructor().newInstance()

        assert(method.invoke(instance, "a") == "a")
        assertViolatesPredicate(method.invoke(instance, "b"))
    }

}
