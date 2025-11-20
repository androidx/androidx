/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.appsearch.compiler

import androidx.annotation.RestrictTo
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import javax.tools.Diagnostic

/** An exception thrown from the appsearch annotation processor to indicate something went wrong. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class XProcessingException(override val message: String, private val culprit: XElement?) :
    Exception(message) {
    /**
     * Warnings associated with this error which should be reported alongside it at a lower level.
     */
    private val warnings = mutableListOf<XProcessingException>()

    fun addWarning(newWarning: XProcessingException) {
        warnings.add(newWarning)
    }

    fun addWarnings(newWarnings: Collection<XProcessingException>) {
        warnings.addAll(newWarnings)
    }

    fun printDiagnostic(messager: XMessager) {
        printDiagnostic(messager, Diagnostic.Kind.ERROR)
    }

    private fun printDiagnostic(messager: XMessager, level: Diagnostic.Kind) {
        if (culprit == null) {
            messager.printMessage(level, message)
        } else {
            messager.printMessage(level, message, culprit)
        }
        for (warning in warnings) {
            warning.printDiagnostic(messager, Diagnostic.Kind.WARNING)
        }
    }
}
