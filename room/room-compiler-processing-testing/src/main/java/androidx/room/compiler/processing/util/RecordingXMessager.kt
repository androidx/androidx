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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import javax.tools.Diagnostic

/**
 * An XMessager implementation that holds onto dispatched diagnostics.
 */
class RecordingXMessager : XMessager() {
    private val diagnostics = mutableMapOf<Diagnostic.Kind, MutableList<DiagnosticMessage>>()

    fun diagnostics(): Map<Diagnostic.Kind, List<DiagnosticMessage>> = diagnostics

    override fun onPrintMessage(kind: Diagnostic.Kind, msg: String, element: XElement?) {
        diagnostics.getOrPut(
            kind
        ) {
            mutableListOf()
        }.add(
            DiagnosticMessage(
                msg = msg,
                element = element
            )
        )
    }
}