package space.kiibou.byteguard.bytecode

import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.br._
import org.opalj.br.analyses.{Project, SomeProject}
import org.opalj.br.reader.Java17Framework
import space.kiibou.byteguard.Guard
import space.kiibou.byteguard.bytecode.BytecodeWeaver.{loadClassBytes, toClassFile}
import space.kiibou.byteguard.bytecode.factory.{BytecodeProducerFactory, BytecodeProducerFactoryImpl}
import space.kiibou.byteguard.bytecode.producer.BytecodeProducer
import space.kiibou.byteguard.processor.info
import space.kiibou.byteguard.processor.info.{ClassSpecificationInfo, ClassifierType}
import space.kiibou.byteguard.specification.GuardSpec
import space.kiibou.byteguard.specification.method._

import java.io.ByteArrayInputStream
import java.lang.reflect.{Field => JField}
import java.net.URL
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.ListHasAsScala

class BytecodeWeaver(private val bytecode: Array[Byte], private val classSpecInfo: ClassSpecificationInfo) {
    val classFile: ClassFile = toClassFile(bytecode)

    val specClassFile: ClassFile = toClassFile(loadClassBytes(classSpecInfo.getSpecClassType))
    val specClass: Class[_ <: GuardSpec] = classSpecInfo.getSpecClass
    val guardFields: List[JField] = classSpecInfo.getGuardFields.asScala.toList

    val spec: GuardSpec = classSpecInfo.getSpecInstance
    val indexGuardNameMap: Map[Int, String] = getIndexGuardNameMap(spec, guardFields)
    val methodSpecs: Map[(String, FieldTypes), MethodSpec] = getNameMethodSpecMap(specClass, spec)

    val project: SomeProject = Project[URL](
        List(
            specClassFile -> new URL(s"file://${specClassFile.thisType.fqn}"),
            classFile -> new URL(s"file://${classFile.thisType.fqn}")
        ),
        // org.opalj.bytecode.RTJar
    )

    private var additionalMethods: Seq[MethodTemplate] = Seq.empty[MethodTemplate]

    private var symbolIndex = 0

    private val factory: BytecodeProducerFactory = new BytecodeProducerFactoryImpl(this)

    def getUniqueSymbol(prefix: String): Symbol = {
        val index = symbolIndex
        symbolIndex += 1
        Symbol(prefix + s"_$index")
    }

    def weave(): Array[Byte] = {
        var producers = Seq.empty[BytecodeProducer]

        producers :+= factory.createGuardFieldProducer()
        producers :++= (for (method <- classFile.methodsWithBody) yield factory.createMethodTransformer(method))

        val newFields = classFile.fields.map(_.copy()) :++ producers.flatMap(_.fields())
        val newMethods = producers.flatMap(_.methods()).to(ArraySeq)

        val newBytecode = Assembler(toDA(classFile.copy(
            fields = newFields,
            methods = newMethods
        )))

        newBytecode
    }


    /**
     * Returns a map associating the index of a guard in a `GuardSpec` with it's field name
     */
    private def getIndexGuardNameMap(spec: GuardSpec, guardFields: List[JField]): Map[Int, String] = {
        guardFields.map { field =>
            if (!field.trySetAccessible()) {
                throw new IllegalAccessException(s"Class ${getClass.getSimpleName} could not obtain access to ${field.getDeclaringClass.getSimpleName}#${field.getName}")
            }

            val guard = field.get(spec).asInstanceOf[Guard]

            guard.index() -> field.getName
        }.toMap
    }

    /**
     * Returns a map associating the name of the target method with it's `MethodSpec`
     */
    private def getNameMethodSpecMap(specClass: Class[_ <: GuardSpec], spec: GuardSpec): Map[(String, FieldTypes), MethodSpec] = {
        val methodSpecInfo = classSpecInfo.getMethodSpecs.asScala.toList

        methodSpecInfo.map { info =>
            (info.getMethodInfo.getTargetMethodName, ArraySeq.unsafeWrapArray(info.getMethodInfo.getArgumentTypes.asScala.map(toFieldType).toArray)) -> info.getMethodSpec(specClass, spec)
        }.toMap
    }

    private def toFieldType(jType: info.Type): FieldType = {
        jType match {
            case arrayType: info.ArrayType => ArrayType(toFieldType(arrayType.getType))
            case classifierType: ClassifierType => ObjectType(classifierType.getJavaName.replace(".", "/"))
            case _: info.BooleanType => BooleanType
            case _: info.ShortType => ShortType
            case _: info.ByteType => ByteType
            case _: info.CharType => CharType
            case _: info.IntType => IntegerType
            case _: info.LongType => LongType
            case _: info.FloatType => FloatType
            case _: info.DoubleType => DoubleType
            case _: info.VoidType => throw new IllegalArgumentException("Can not convert void to FieldType")
        }
    }
}


object BytecodeWeaver {
    val GuardViolationExceptionType: ObjectType = ObjectType("space/kiibou/byteguard/exception/GuardViolationException")
    val PredicateViolationExceptionType: ObjectType = ObjectType("space/kiibou/byteguard/exception/PredicateViolationException")

    val requiresPredicateGuardMethodDescriptor: MethodDescriptor = SingleArgumentMethodDescriptor(
        ObjectType("java/util/function/BooleanSupplier"),
        ObjectType("space/kiibou/byteguard/specification/method/MethodSpecComponent")
    )

    private def toClassFile(bytecode: Array[Byte]): ClassFile = {
        val files: List[ClassFile] = Java17Framework.ClassFile(() => new ByteArrayInputStream(bytecode))

        if (files.size != 1) throw new IllegalArgumentException(
            s"Bytecode contains ${files.size} classes (${files.map(_.thisType.toJava).mkString(", ")}), but should contain exactly one. "
        )

        files.head
    }

    private def loadClassBytes(classType: ClassifierType): Array[Byte] = {
        ClassLoader.getSystemClassLoader.getResourceAsStream(classType.getJavaName.replace('.', '/') + ".class").readAllBytes()
    }
}
