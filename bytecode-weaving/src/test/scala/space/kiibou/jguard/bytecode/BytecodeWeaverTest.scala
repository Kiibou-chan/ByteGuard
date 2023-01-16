package space.kiibou.jguard.bytecode

import org.junit.runner.RunWith
import org.opalj.br.ClassFile
import org.opalj.br.reader.Java17Framework
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatest._
import flatspec._
import matchers._
import org.junit.jupiter.api.Test
import org.opalj.util.InMemoryClassLoader
import space.kiibou.jguard.processor.info.{BooleanType, ClassSpecificationInfo, ClassifierType, MethodInfo, MethodSpecificationInfo}

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

// @RunWith(classOf[JUnitRunner])
class BytecodeWeaverTest extends AnyFlatSpec with Matchers {

    private def loadClassBytes(name: String): Array[Byte] = {
        ClassLoader.getSystemClassLoader.getResourceAsStream(name).readAllBytes()
    }

    @Test
    def generateIteratorWrapperCode(): Unit = {
        val iteratorWrapperBytes = loadClassBytes("space/kiibou/jguard/bytecode/IteratorWrapper.class")

        val specInfo = new ClassSpecificationInfo(
            new ClassifierType("space.kiibou.jguard.bytecode.IteratorGuardSpec"),
            new ClassifierType("space.kiibou.jguard.bytecode.IteratorWrapper"),
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

        new File("./build/test/space/kiibou/jguard/bytecode/").mkdirs()

        Files.write(
            Path.of("./build/test/space/kiibou/jguard/bytecode/IteratorWrapper.class"),
            newBytecode,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )

        val classLoader = new InMemoryClassLoader(
            Map(
                "space.kiibou.jguard.bytecode.IteratorWrapper" -> newBytecode
            ),
            getClass.getClassLoader
        )

        val instrumentedClass = classLoader.loadClass("space.kiibou.jguard.bytecode.IteratorWrapper")

    }

    /*
    val constructor = instrumentedClass.getConstructor(classOf[util.Iterator[_]])

    val instance = constructor.newInstance(List("a", "b", "c").asJava)
     */

}
