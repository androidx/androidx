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

package androidx.compose.mpp.demo

import androidx.compose.ui.platform.ClipEntry
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

expect suspend fun ClipEntry?.getPlainText(): String?

expect fun createClipEntryWithPlainText(text: String): ClipEntry

// Setting the colors to indicate the presence of the backing textarea or input, and its focus state
internal fun setupBackingTextAreaDebugHints() {
    val sr = document.getElementById("composeApplication")!!.shadowRoot!!

    val srStyle = document.createElement("style").apply {
        // language=css
        textContent = """
            :host {
                --my-variable: transparent;
            }
            :host:has(textarea) {
                --my-variable: aliceblue;
            }
            :host:has(input) {
                --my-variable: aliceblue;
            }
            :host:has(textarea:focus) {
                --my-variable: #eaffe3;
            }
            :host:has(input:focus) {
                --my-variable: #eaffe3;
            }
            #debugBackingInputIndicator {
                background-color: var(--my-variable);
                position: absolute;
                top: 0;
                left: 0;
                margin: 0;
                width: 100%;
                height: 100%;
                z-index: -10;
            }
        """.trimIndent()
    }

    val firstChild = sr.children.item(0) as HTMLElement
    sr.insertBefore(srStyle, firstChild)

    sr.appendChild(document.createElement("div").apply {
        id = "debugBackingInputIndicator"
    })
}