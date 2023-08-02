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
import org.w3c.dom.MediaQueryList
import org.w3c.dom.Window
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

internal class SystemThemeObserver(window : Window) {

    val currentSystemTheme: State<SystemTheme>
        get() = _currentSystemTheme

    private val media: MediaQueryList by lazy {
        window.matchMedia("(prefers-color-scheme: dark)")
    }

    private val _currentSystemTheme = mutableStateOf(
        when {
            !isSupported -> SystemTheme.Unknown
            media.matches -> SystemTheme.Dark
            else -> SystemTheme.Light
        }
    )

    // supported by all browsers since 2015
    // https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia
    private val isSupported: Boolean
        get() = js("window.matchMedia != undefined").unsafeCast<Boolean>()

    private val listener = EventListener {
        _currentSystemTheme.value = if (it.unsafeCast<MediaQueryList>().matches)
            SystemTheme.Dark else SystemTheme.Light
    }

    fun dispose() {
        if (isSupported){
            try {
                media.removeEventListener("change", listener)
            } catch (t : Throwable){
                media.removeListener(listener)
            }
        }
    }

    init {
        if (isSupported) {
            try {
                media.addEventListener("change", listener)
            } catch (t: Throwable) {
                media.addListener(listener)
            }
        }
    }
}