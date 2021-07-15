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
        messager.printMessage(NOTE, msg.safeFormat(args), element)
    }

    fun d(msg: String, vararg args: Any) {
        messager.printMessage(NOTE, msg.safeFormat(args))
    }

    fun e(element: XElement, msg: String, vararg args: Any) {
        messager.printMessage(ERROR, msg.safeFormat(args), element)
    }

    fun e(msg: String, vararg args: Any) {
        messager.printMessage(ERROR, msg.safeFormat(args), defaultElement)
    }

    fun w(warning: Warning, element: XElement? = null, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        messager.printMessage(
            WARNING, msg.safeFormat(args),
            element ?: defaultElement
        )
    }

    fun w(warning: Warning, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        messager.printMessage(WARNING, msg.safeFormat(args), defaultElement)
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
            messages.getOrPut(
                kind,
                {
                    arrayListOf()
                }
            ).add(DiagnosticMessage(msg, element, annotation, annotationValue))
        }

        fun hasErrors() = messages.containsKey(ERROR)

        fun writeTo(context: Context) {
            val printMessage = context.logger.messager::printMessage
            messages.forEach { pair ->
                val kind = pair.key
                pair.value.forEach { diagnosticMessage ->
                    printMessage(
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
}
