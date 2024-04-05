/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.SystemTheme
import kotlinx.browser.window
import org.w3c.dom.MediaQueryList
import org.w3c.dom.MediaQueryListEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

internal class SystemThemeObserverImpl(window : Window) : SystemThemeObserver {

    override val currentSystemTheme: State<SystemTheme>
        get() = _currentSystemTheme

    private val media: MediaQueryList by lazy {
        window.matchMedia("(prefers-color-scheme: dark)")
    }

    private val _currentSystemTheme = mutableStateOf(
        when {
            !isMatchMediaSupported() -> SystemTheme.Unknown
            media.matches -> SystemTheme.Dark
            else -> SystemTheme.Light
        }
    )

    private val listener: (Event) -> Unit = { event ->
        _currentSystemTheme.value = if ((event as MediaQueryListEvent).matches)
            SystemTheme.Dark else SystemTheme.Light
    }

    override fun dispose() {
        if (isMatchMediaSupported()){
            try {
                media.removeEventListener("change", listener)
            } catch (t : Throwable){
                media.removeListener(listener)
            }
        }
    }

    init {
        if (isMatchMediaSupported()) {
            try {
                media.addEventListener("change", listener)
            } catch (t: Throwable) {
                media.addListener(listener)
            }
        }
    }
}

internal actual fun getSystemThemeObserver(): SystemThemeObserver =
    SystemThemeObserverImpl(window)

// supported by all browsers since 2015
// https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia
@JsFun("() => window.matchMedia != undefined")
private external fun isMatchMediaSupported(): Boolean
