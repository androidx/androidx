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

package androidx.window.area.utils

import android.annotation.SuppressLint
import android.view.Window
import androidx.window.extensions.area.ExtensionWindowAreaPresentation
import java.lang.reflect.Method

internal object PresentationWindowCompatUtils {

    // We perform our own extensions vendor API level check at the call-site
    @SuppressLint("BanUncheckedReflection")
    fun getWindowBeforeVendorApiLevel4(
        extensionPresentation: ExtensionWindowAreaPresentation
    ): Window? {
        val getWindowMethod = getWindowMethod(extensionPresentation)
        return if (getWindowMethod == null) null
        else (getWindowMethod.invoke(extensionPresentation) as Window?)
    }

    private fun getWindowMethod(extensionPresentation: ExtensionWindowAreaPresentation): Method? {
        return extensionPresentation.javaClass.methods.firstOrNull { method: Method? ->
            method?.name == "getWindow" && method.returnType == android.view.Window::class.java
        }
    }
}
