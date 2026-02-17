/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import java.awt.Desktop
import java.net.URI

internal class DesktopUriHandler : UriHandler {
    override fun openUri(uri: String) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI(uri))
        } else when (DesktopPlatform.Current) {
            DesktopPlatform.Linux -> Runtime.getRuntime().exec(arrayOf("xdg-open", URI(uri).toString()))
            DesktopPlatform.Windows, DesktopPlatform.MacOS ->
                throw UnsupportedOperationException(
                    "AWT doesn't support the BROWSE action on ${DesktopPlatform.Current}"
                )
            DesktopPlatform.Unknown ->
                throw UnsupportedOperationException("AWT doesn't support ${DesktopPlatform.Current}")
        }
    }
}

internal actual fun createPlatformUriHandler(): UriHandler = DesktopUriHandler()
