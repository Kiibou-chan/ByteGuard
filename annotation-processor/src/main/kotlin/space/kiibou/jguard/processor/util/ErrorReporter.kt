package space.kiibou.jguard.processor.util

import space.kiibou.jguard.processor.error.Error
import javax.annotation.processing.Messager
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element

class ErrorReporter(private val messager: Messager) {

    private val errors: MutableList<Error> = mutableListOf()

    fun report(message: String) {
        val error = Error(message)

        errors += error
    }

    fun report(message: String, element: Element) {
        val error = Error(message, element)

        errors += error
    }

    fun report(message: String, element: Element, annotationMirror: AnnotationMirror) {
        val error = Error(message, element, annotationMirror)

        errors += error
    }

    fun report(
        message: String,
        element: Element,
        annotationMirror: AnnotationMirror,
        annotationValue: AnnotationValue
    ) {
        val error = Error(message, element, annotationMirror, annotationValue)

        errors += error
    }

    fun hasErrors(): Boolean = errors.isNotEmpty()

    fun printErrors() {
        errors.forEach { error ->
            error.print(messager)
        }
    }

}
