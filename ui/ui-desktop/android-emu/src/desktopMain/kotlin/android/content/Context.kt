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
package android.content

import android.content.res.Resources
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager

public abstract class Context() {
    companion object {
        const val ACCESSIBILITY_SERVICE = "accessibility"
        const val CLIPBOARD_SERVICE = "clipboard"
        const val INPUT_METHOD_SERVICE = "input_method"
    }

    private val accessibilityManager = AccessibilityManager()
    private val clipboardManager = ClipboardManager()
    private val inputMethodManager = InputMethodManager()

    open fun getSystemService(service: String): Any? {
        return when (service) {
            Context.ACCESSIBILITY_SERVICE -> accessibilityManager
            Context.CLIPBOARD_SERVICE -> clipboardManager
            Context.INPUT_METHOD_SERVICE -> inputMethodManager
            else -> null
        }
    }

    public val resources: Resources = Resources()

    public val applicationContext: Context by lazy { ContextImpl() }
}

internal class ContextImpl() : Context()