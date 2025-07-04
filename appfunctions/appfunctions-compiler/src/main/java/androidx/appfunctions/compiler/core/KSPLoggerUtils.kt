/*
 * Copyright 2025 The Android Open Source Project
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
@file:JvmName("KSPLoggerUtil")

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import java.io.File

/** Logs a [ProcessingException] with detail about the error location. */
fun KSPLogger.logException(exception: ProcessingException) {
    val location = exception.symbol?.location as? FileLocation
    if (location == null) {
        error("AppFunction compiler failed: ${exception.message}", exception.symbol)
        return
    }

    val filePath = location.filePath
    val errorLineNumber = location.lineNumber
    val fileContent = File(filePath).readLines()
    val errorLine = fileContent[errorLineNumber - 1]
    val errorLinePointer = " ^".padStart(errorLine.indexOfFirst { it != ' ' } + 1)
    error(
        "Error in ${location.filePath}:${location.lineNumber}: " +
            "${exception.message}\n${errorLine}\n$errorLinePointer\n",
        exception.symbol,
    )
}
