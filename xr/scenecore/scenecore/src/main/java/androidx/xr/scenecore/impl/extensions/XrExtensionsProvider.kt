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
package androidx.xr.scenecore.impl.extensions

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Log
import com.android.extensions.xr.XrExtensions
import java.lang.reflect.InvocationTargetException

/** Provides the OEM implementation of [XrExtensions]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object XrExtensionsProvider {

    /**
     * Returns the OEM implementation of [XrExtensions] or returns null if no implementation is
     * found.
     */
    @JvmStatic
    public fun getXrExtensions(): XrExtensions? {
        return try {
            XrExtensionsInstance.instance
        } catch (e: NoClassDefFoundError) {
            Log.warn(e) { "XrExtensionsProvider: No XrExtensions implementation found." }
            null
        }
    }

    private object XrExtensionsInstance {
        val instance: XrExtensions = XrExtensions()

        init {
            initExtensions()
        }

        @SuppressLint("BanUncheckedReflection")
        private fun initExtensions() {
            // Try to call the @TestApi method XrExtensions.setCurrentExtensions to register
            // the current extensions instance. If this fails for various reasons (e.g. the
            // platform has removed this method or the app is not debuggable), ignore the
            // error.
            try {
                val setCurrentExtensionsMethod =
                    XrExtensions::class
                        .java
                        .getDeclaredMethod("setCurrentExtensions", XrExtensions::class.java)
                setCurrentExtensionsMethod.isAccessible = true
                setCurrentExtensionsMethod.invoke(null, instance)
            } catch (e: Exception) {
                when (e) {
                    is NoSuchMethodException,
                    is InvocationTargetException,
                    is IllegalAccessException,
                    is SecurityException ->
                        Log.debug(
                            "XrExtensions.setCurrentExtensions method could not be called: $e"
                        )
                    else -> throw e
                }
            }
        }
    }
}
