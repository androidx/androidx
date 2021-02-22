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

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.NonExistLocation
import javax.tools.Diagnostic

internal class KspMessager(
    private val logger: KSPLogger
) : XMessager() {
    override fun onPrintMessage(kind: Diagnostic.Kind, msg: String, element: XElement?) {
        val ksNode = (element as? KspElement)?.declaration

        @Suppress("NAME_SHADOWING") // intentional to avoid reporting without location
        val msg = if ((ksNode == null || ksNode.location == NonExistLocation) && element != null) {
            "$msg - ${element.fallbackLocationText}"
        } else {
            msg
        }
        when (kind) {
            Diagnostic.Kind.ERROR -> logger.error(msg, ksNode)
            Diagnostic.Kind.WARNING -> logger.warn(msg, ksNode)
            else -> logger.info(msg, ksNode)
        }
    }
}
