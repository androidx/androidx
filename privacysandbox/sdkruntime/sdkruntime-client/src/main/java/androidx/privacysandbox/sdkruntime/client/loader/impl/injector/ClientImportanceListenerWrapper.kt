/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import android.annotation.SuppressLint
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import java.lang.reflect.Method

/**
 * Creates reflection wrapper for implementation of [SdkSandboxClientImportanceListenerCompat]
 * interface loaded by SDK classloader.
 */
internal class ClientImportanceListenerWrapper
private constructor(private val listenerOnForegroundImportanceChangedMethod: Method) {

    fun wrapSdkSandboxClientImportanceListenerCompat(
        listenerCompat: Any
    ): SdkSandboxClientImportanceListenerCompat =
        WrappedListener(listenerCompat, listenerOnForegroundImportanceChangedMethod)

    private class WrappedListener(
        private val originalListener: Any,
        private val listenerOnForegroundImportanceChangedMethod: Method,
    ) : SdkSandboxClientImportanceListenerCompat {
        @SuppressLint("BanUncheckedReflection") // using reflection on library classes
        override fun onForegroundImportanceChanged(isForeground: Boolean) {
            listenerOnForegroundImportanceChangedMethod.invoke(originalListener, isForeground)
        }
    }

    companion object {
        fun createFor(classLoader: ClassLoader): ClientImportanceListenerWrapper {
            val sdkSandboxActivityHandlerCompatClass =
                Class.forName(
                    "androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat",
                    /* initialize = */ false,
                    classLoader,
                )
            val listenerOnForegroundImportanceChangedMethod =
                sdkSandboxActivityHandlerCompatClass.getMethod(
                    "onForegroundImportanceChanged",
                    Boolean::class.javaPrimitiveType,
                )

            return ClientImportanceListenerWrapper(
                listenerOnForegroundImportanceChangedMethod =
                    listenerOnForegroundImportanceChangedMethod
            )
        }
    }
}
