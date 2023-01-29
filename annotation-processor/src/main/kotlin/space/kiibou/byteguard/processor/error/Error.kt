package space.kiibou.byteguard.processor.error

import javax.annotation.processing.Messager
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.tools.Diagnostic

data class Error(
    val message: String,
    val element: Element?,
    val annotationMirror: AnnotationMirror?,
    val annotationValue: AnnotationValue?
) {

    constructor(message: String) : this(message, null, null, null)

    constructor(message: String, element: Element) : this(message, element, null, null)

    constructor(message: String, element: Element, annotationMirror: AnnotationMirror) : this(
        message,
        element,
        annotationMirror,
        null
    )

    fun print(messager: Messager) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element, annotationMirror, annotationValue)
    }

}
