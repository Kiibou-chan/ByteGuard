package space.kiibou.jguard.processor

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.service.AutoService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.kiibou.jguard.annotation.SpecFor
import space.kiibou.jguard.processor.error.ReportException
import space.kiibou.jguard.processor.info.*
import space.kiibou.jguard.processor.util.ErrorReporter
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import javax.tools.FileObject
import javax.tools.StandardLocation
import kotlin.io.path.toPath
import space.kiibou.jguard.specification.method.MethodSpec as JGuardMethodSpec

@Suppress("UnstableApiUsage")
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("space.kiibou.jguard.annotation.SpecFor")
class SpecForProcessor : AbstractProcessor() {

    val typeUtils: Types by lazy { processingEnv.typeUtils }
    val elementUtils: Elements by lazy { processingEnv.elementUtils }
    val messager: Messager by lazy { processingEnv.messager }

    val errorReporter: ErrorReporter by lazy { ErrorReporter(messager) }

    private val allowedElementKinds = setOf(ElementKind.CLASS)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return try {
            val elements: Set<Element> = roundEnv.getElementsAnnotatedWith(SpecFor::class.java)

            if (elements.isEmpty()) return true

            verifyElements(elements)

            val specs = elements.map {
                processClassElement(it as TypeElement)
            }

            saveSpecs(specs)

            true
        } catch (ex: ReportException) {
            if (errorReporter.hasErrors()) {
                errorReporter.printErrors()
            }

            !errorReporter.hasErrors()
        } catch (ex: Throwable) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.stackTraceToString())

            false
        }
    }

    private fun verifyElements(elements: Set<Element>) {
        elements.forEach {
            if (it.kind !in allowedElementKinds) {
                wrongKindError<SpecFor>(ElementKind.CLASS, it.kind, it)
            }
        }

        checkForErrors()
    }

    private fun processClassElement(element: TypeElement): ClassSpecificationInfo {
        val specifiedClass = getSpecifiedClass(element)
        val methodSpecs = getMethodsReturning(JGuardMethodSpec::class.java, element)

        val methodMap = getMethodMap(specifiedClass, methodSpecs)

        return ClassSpecificationInfo(
            element.toClassifierType(),
            specifiedClass.toClassifierType(),
            methodMap.map { (spec, target) ->
                MethodSpecificationInfo(
                    MethodInfo(
                        spec.simpleName.toString(),
                        target.simpleName.toString(),
                        target.returnType.toJvmType(),
                        spec.parameters.map { it.asType().toJvmType() }
                    )
                )
            }
        )
    }

    fun TypeElement.toClassifierType(): ClassifierType {
        return if (this.nestingKind.isNested) {
            ClassifierType(
                "${MoreElements.asType(this.enclosingElement).toClassifierType().javaName}$${this.simpleName}"
            )
        } else {
            ClassifierType(this.qualifiedName.toString())
        }
    }

    private fun TypeMirror.toJvmType(): Type {
        return when (this.kind) {
            null -> TODO()
            TypeKind.BOOLEAN -> BooleanType
            TypeKind.BYTE -> ByteType
            TypeKind.SHORT -> ShortType
            TypeKind.INT -> IntType
            TypeKind.LONG -> LongType
            TypeKind.CHAR -> CharType
            TypeKind.FLOAT -> FloatType
            TypeKind.DOUBLE -> DoubleType
            TypeKind.VOID -> VoidType
            TypeKind.NONE -> TODO("NONE TYPE")
            TypeKind.NULL -> ClassifierType("java/lang/Object")
            TypeKind.ARRAY -> ArrayType(MoreTypes.asArray(this).componentType.toJvmType())
            TypeKind.DECLARED -> MoreTypes.asTypeElement(this).toClassifierType()
            TypeKind.ERROR -> TODO("ERROR TYPE")
            TypeKind.TYPEVAR -> MoreTypes.asTypeVariable(this).lowerBound.toJvmType()
            TypeKind.WILDCARD -> TODO("WILDCARD TYPE")
            TypeKind.PACKAGE -> TODO("PACKAGE TYPE")
            TypeKind.EXECUTABLE -> TODO("EXECUTABLE TYPE")
            TypeKind.OTHER -> TODO("OTHER TYPE")
            TypeKind.UNION -> TODO("UNION TYPE")
            TypeKind.INTERSECTION -> TODO("INTERSECTION TYPE")
            TypeKind.MODULE -> TODO("MODULE TYPE")
        }
    }

    private fun getSpecifiedClass(element: TypeElement): TypeElement {
        val className = element.getAnnotation(SpecFor::class.java).value

        // TODO (Svenja, 2023/07/01): Report error to user, if the class name can not be resolved (getTypeElement returns null)
        val typeElement = elementUtils.getTypeElement(className)

        if (typeElement == null) {
            specifiedClassNotFoundError(element, className)
            checkForErrors()
        }

        return typeElement
    }

    private fun specifiedClassNotFoundError(specClass: TypeElement, className: String) {
        errorReporter.report(
            "${specClass.qualifiedName} provides specification for class $className, but $className could not be found.",
            specClass
        )
    }

    /**
     * Returns a map associating a method ([ExecutableElement]) of the spec class with the method it is providing a
     * specification for.
     *
     * Assuming we have a class `@SpecFor("java.lang.Iterator") class IteratorSpec` with a method
     * `fun MethodSpec hasNext(): Unit`, the key is an element referencing `IteratorSpec#hasNext` and the corresponding
     * value is `Iterator#hasNext`.
     */
    private fun getMethodMap(
        specifiedClass: TypeElement,
        methodSpecs: List<ExecutableElement>
    ): Map<ExecutableElement, ExecutableElement> {
        val resultMap = methodSpecs.mapNotNull { element ->
            val specMethodName = element.simpleName.toString()
            val specParameters: List<VariableElement> = element.parameters

            val method = findMatchingMethod(specifiedClass, specMethodName, specParameters) ?: kotlin.run {
                noMatchingMethodError(specifiedClass, specMethodName, specParameters, element)

                return@mapNotNull null
            }

            element to method
        }.toMap()

        checkForErrors()

        return resultMap
    }

    private fun noMatchingMethodError(
        specifiedClass: TypeElement,
        specMethodName: String,
        specParameters: List<VariableElement>,
        element: ExecutableElement
    ) {
        errorReporter.report("Specification for class ${specifiedClass.qualifiedName} declared specification for method $specMethodName(${
            specParameters.joinToString(", ") {
                it.asType().toString()
            }
        }), but no matching method could be found!", element)
    }

    private fun findMatchingMethod(
        specifiedClass: TypeElement, specMethodName: String, specParameters: List<VariableElement>
    ): ExecutableElement? {
        val methods = MoreElements.getAllMethods(specifiedClass, typeUtils, elementUtils)

        return methods.firstOrNull {
            it.simpleName.toString() == specMethodName && parametersMatch(it.parameters, specParameters)
        }
    }

    private fun parametersMatch(parameterTypes: List<VariableElement>, specParameters: List<VariableElement>): Boolean {
        if (parameterTypes.size != specParameters.size) return false

        return parameterTypes.zip(specParameters).all { (classParameterType, specParameterType) ->
            classParameterType.asType() == specParameterType.asType()
        }
    }

    private fun getMethodsReturning(returnType: Class<*>, element: TypeElement): List<ExecutableElement> {
        val methods = MoreElements.getAllMethods(element, typeUtils, elementUtils).filter {
            it.enclosingElement == element
        }

        val methodSpecs = methods.filter {
            MoreTypes.isTypeOf(returnType, it.returnType)
        }

        return methodSpecs
    }

    private fun saveSpecs(specs: List<ClassSpecificationInfo>) {
        checkForErrors()

        val resourceFile: FileObject = processingEnv.filer.getResource(
            StandardLocation.CLASS_OUTPUT,
            "space.kiibou.jguard.spec",
            "ClassSpecs.json"
        )

        ensureExists(resourceFile)

        val serializedSpecs = Json.encodeToString(specs)

        Files.writeString(
            resourceFile.toUri().toPath(),
            serializedSpecs,
            Charsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )
    }

    private fun ensureExists(resourceFile: FileObject) {
        resourceFile.toUri().toPath().toFile().apply {
            if (!exists()) {
                parentFile.mkdirs()
                createNewFile()
            }
        }
    }

    private inline fun <reified T : Annotation> wrongKindError(
        expected: ElementKind, actual: ElementKind, element: Element
    ) {
        errorReporter.report(
            "Element $element is annotated with ${T::class.qualifiedName} and is expected to be of kind $expected but was of type $actual",
            element
        )
    }

    /**
     * Checks if the [errorReporter] has collected any errors.
     * If that is the case it throws a [ReportException] which triggers the [errorReporter] to print all errors
     * and causes the current execution to stop.
     */
    private fun checkForErrors() {
        if (errorReporter.hasErrors()) throw ReportException()
    }

    private fun info(message: String) {
        messager.printMessage(Diagnostic.Kind.NOTE, message)
    }

    private fun info(message: String, element: Element) {
        messager.printMessage(Diagnostic.Kind.NOTE, message, element)
    }

}
