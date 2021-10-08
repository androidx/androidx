/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package androidx.room.log

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import androidx.room.processor.Context
import androidx.room.vo.Warning
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.Diagnostic.Kind.WARNING

class RLog(
    val messager: XMessager,
    val suppressedWarnings: Set<Warning>,
    val defaultElement: XElement?
) {
    private fun String.safeFormat(vararg args: Any): String {
        try {
            return format(args)
        } catch (ex: Throwable) {
            // the input string might be from random source in which case we rather print the
            // msg as is instead of crashing while reporting an error.
            return this
        }
    }

    fun d(element: XElement, msg: String, vararg args: Any) {
        printToMessager(messager, NOTE, msg.safeFormat(args), element)
    }

    fun d(msg: String, vararg args: Any) {
        printToMessager(messager, NOTE, msg.safeFormat(args))
    }

    fun e(element: XElement, msg: String, vararg args: Any) {
        printToMessager(messager, ERROR, msg.safeFormat(args), element)
    }

    fun e(msg: String, vararg args: Any) {
        printToMessager(messager, ERROR, msg.safeFormat(args), defaultElement)
    }

    fun w(warning: Warning, element: XElement? = null, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        printToMessager(messager, WARNING, msg.safeFormat(args), element ?: defaultElement)
    }

    fun w(warning: Warning, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        printToMessager(messager, WARNING, msg.safeFormat(args), defaultElement)
    }

    private data class DiagnosticMessage(
        val msg: String,
        val element: XElement?,
        val annotation: XAnnotation?,
        val annotationValue: XAnnotationValue?
    )

    class CollectingMessager : XMessager() {
        private val messages = mutableMapOf<Diagnostic.Kind, MutableList<DiagnosticMessage>>()
        override fun onPrintMessage(
            kind: Diagnostic.Kind,
            msg: String,
            element: XElement?,
            annotation: XAnnotation?,
            annotationValue: XAnnotationValue?
        ) {
            messages.getOrPut(kind) { arrayListOf() }
                .add(DiagnosticMessage(msg, element, annotation, annotationValue))
        }

        fun hasErrors() = messages.containsKey(ERROR)

        fun hasMissingTypeErrors() = messages.getOrElse(ERROR) { emptyList() }
            .any { it.msg.startsWith(MISSING_TYPE_PREFIX) }

        fun writeTo(
            context: Context,
            filterPredicate: (Diagnostic.Kind, String) -> Boolean = { _, _ -> true }
        ) {
            messages.forEach { (kind, diagnosticMessages) ->
                diagnosticMessages
                    .filter { filterPredicate(kind, it.msg) }
                    .forEach { diagnosticMessage ->
                        printToMessager(
                            context.logger.messager,
                            kind,
                            diagnosticMessage.msg,
                            diagnosticMessage.element,
                            diagnosticMessage.annotation,
                            diagnosticMessage.annotationValue
                        )
                    }
            }
        }
    }

    companion object {

        const val MISSING_TYPE_PREFIX = "[MissingType]"

        val MissingTypeErrorFilter: (Diagnostic.Kind, String) -> Boolean = { kind, msg ->
            kind == ERROR && msg.startsWith(MISSING_TYPE_PREFIX)
        }

        private fun printToMessager(
            messager: XMessager,
            kind: Diagnostic.Kind,
            msg: String,
            element: XElement? = null,
            annotation: XAnnotation? = null,
            annotationValue: XAnnotationValue? = null
        ) {
            if (element == null) {
                check(annotation == null && annotationValue == null) {
                    "If element is null, annotation and annotationValue must also be null."
                }
                messager.printMessage(kind, msg)
            } else if (annotation == null) {
                check(annotationValue == null) {
                    "If annotation is null, annotationValue must also be null."
                }
                messager.printMessage(kind, msg, element)
            } else if (annotationValue == null) {
                messager.printMessage(kind, msg, element, annotation)
            } else {
                messager.printMessage(kind, msg, element, annotation, annotationValue)
            }
        }
    }
}
