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

expect suspend fun ClipEntry?.getPlainText(): String?

expect fun createClipEntryWithPlainText(text: String): ClipEntry

// Setting the colors to indicate the presence of the backing textarea or input, and its focus state
internal fun setupBackingTextAreaDebugHints() {
    val shadowRootStyle = document.createElement("style")
    // language=css
    shadowRootStyle.textContent = """
        :host {
            --input-mode-indicator: transparent;
        }
        
        :host:has(input, textarea) {
            --input-mode-indicator: aliceblue;
        }
        
        :host:has(input:focus, textarea:focus) {
            --input-mode-indicator: #eaffe3;
        }
        
        #debugBackingInputIndicator {
            background-color: var(--input-mode-indicator);
            position: absolute;
            top: 0;
            left: 0;
            margin: 0;
            width: 100%;
            height: 100%;
            z-index: -10;
        }
""".trimIndent()

    val shadowRoot = document.getElementById("composeApplication")?.shadowRoot!!

    shadowRoot.prepend(shadowRootStyle)
    shadowRoot.appendChild(document.createElement("div").apply {
        id = "debugBackingInputIndicator"
    })
}