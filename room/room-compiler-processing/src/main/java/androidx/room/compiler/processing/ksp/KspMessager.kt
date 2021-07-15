/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import javax.tools.Diagnostic

internal class KspMessager(
    private val logger: KSPLogger
) : XMessager() {
    override fun onPrintMessage(
        kind: Diagnostic.Kind,
        msg: String,
        element: XElement?,
        annotation: XAnnotation?,
        annotationValue: XAnnotationValue?
    ) {
        if (element == null) {
            internalPrintMessage(kind, msg)
            return
        }

        // In Javac, the Messager requires all preceding parameters to report an error.
        // In KSP, the KspLogger only needs the last so ignore the preceding parameters.
        val ksNode = if (annotationValue != null) {
            (annotationValue as KspAnnotationValue).valueArgument
        } else if (annotation != null) {
            (annotation as KspAnnotation).ksAnnotated
        } else if (element != null) {
            (element as KspElement).declaration
        } else {
            null
        }

        if (ksNode == null || ksNode.location == NonExistLocation) {
            internalPrintMessage(kind, "$msg - ${element.fallbackLocationText}")
        } else {
            internalPrintMessage(kind, msg, ksNode)
        }
    }

    private fun internalPrintMessage(kind: Diagnostic.Kind, msg: String, ksNode: KSNode? = null) {
        when (kind) {
            Diagnostic.Kind.ERROR -> logger.error(msg, ksNode)
            Diagnostic.Kind.WARNING -> logger.warn(msg, ksNode)
            else -> logger.info(msg, ksNode)
        }
    }
}
