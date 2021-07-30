/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

import javax.tools.Diagnostic

/**
 * Logging interface for the processor
 */
abstract class XMessager {
    private val watchers = mutableListOf<XMessager>()

    /**
     * Prints the given [msg] to the logs while also associating it with the given [element],
     * [annotation], and [annotationValue].
     *
     * @param kind Kind of the message
     * @param msg The actual message to report to the compiler
     */
    fun printMessage(kind: Diagnostic.Kind, msg: String) {
        printMsg(kind, msg)
    }

    /**
     * Prints the given [msg] to the logs while also associating it with the given [element],
     * [annotation], and [annotationValue].
     *
     * @param kind Kind of the message
     * @param msg The actual message to report to the compiler
     * @param element The element with whom the message should be associated with
     */
    final fun printMessage(
        kind: Diagnostic.Kind,
        msg: String,
        element: XElement,
    ) {
        printMsg(kind, msg, element)
    }

    /**
     * Prints the given [msg] to the logs while also associating it with the given [element],
     * [annotation], and [annotationValue].
     *
     * @param kind Kind of the message
     * @param msg The actual message to report to the compiler
     * @param element The element with whom the message should be associated with
     * @param annotation The annotation with whom the msg should be associated with
     */
    final fun printMessage(
        kind: Diagnostic.Kind,
        msg: String,
        element: XElement,
        annotation: XAnnotation,
    ) {
        printMsg(kind, msg, element, annotation)
    }

    /**
     * Prints the given [msg] to the logs while also associating it with the given [element],
     * [annotation], and [annotationValue].
     *
     * @param kind Kind of the message
     * @param msg The actual message to report to the compiler
     * @param element The element with whom the message should be associated with
     * @param annotation The annotation with whom the msg should be associated with
     * @param annotationValue The annotation value with whom the msg should be associated with
     */
    final fun printMessage(
        kind: Diagnostic.Kind,
        msg: String,
        element: XElement,
        annotation: XAnnotation,
        annotationValue: XAnnotationValue
    ) {
        printMsg(kind, msg, element, annotation, annotationValue)
    }

    private fun printMsg(
        kind: Diagnostic.Kind,
        msg: String,
        element: XElement? = null,
        annotation: XAnnotation? = null,
        annotationValue: XAnnotationValue? = null
    ) {
        watchers.forEach {
            it.printMsg(kind, msg, element, annotation, annotationValue)
        }
        onPrintMessage(kind, msg, element, annotation, annotationValue)
    }

    protected abstract fun onPrintMessage(
        kind: Diagnostic.Kind,
        msg: String,
        element: XElement? = null,
        annotation: XAnnotation? = null,
        annotationValue: XAnnotationValue? = null
    )

    fun addMessageWatcher(watcher: XMessager) {
        watchers.add(watcher)
    }

    fun removeMessageWatcher(watcher: XMessager) {
        watchers.remove(watcher)
    }
}
