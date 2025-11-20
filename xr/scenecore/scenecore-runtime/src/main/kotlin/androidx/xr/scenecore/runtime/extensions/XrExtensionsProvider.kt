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

package androidx.xr.scenecore.runtime.extensions

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RestrictTo
import com.android.extensions.xr.XrExtensions
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/** Provides the OEM implementation of [XrExtensions]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object XrExtensionsProvider {
    private const val TAG = "XrExtensionsProvider"

    /**
     * Returns the OEM implementation of [XrExtensions] or returns null if no implementation is
     * found.
     */
    @JvmStatic
    public fun getXrExtensions(): XrExtensions? {
        try {
            return XrExtensionsHolder.INSTANCE
        } catch (e: NoClassDefFoundError) {
            Log.w(TAG, "No XrExtensions implementation found.", e)
            return null
        }
    }

    @SuppressLint("BanUncheckedReflection")
    private object XrExtensionsHolder {
        val INSTANCE: XrExtensions = XrExtensions()

        init {
            // Try to call the @TestApi method XrExtensions.setCurrentExtensions to register
            // the current extensions instance. If this fails for various reasons (e.g. the
            // platform has removed this method or the app is not debuggable), ignore the
            // error.
            try {
                val setCurrentExtensionsMethod: Method =
                    XrExtensions::class
                        .java
                        .getDeclaredMethod("setCurrentExtensions", XrExtensions::class.java)
                setCurrentExtensionsMethod.isAccessible = true
                setCurrentExtensionsMethod.invoke(null, INSTANCE)
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "XrExtensions.setCurrentExtensions method could not be called: $e")
            } catch (e: InvocationTargetException) {
                Log.d(TAG, "XrExtensions.setCurrentExtensions method could not be called: $e")
            } catch (e: IllegalAccessException) {
                Log.d(TAG, "XrExtensions.setCurrentExtensions method could not be called: $e")
            } catch (e: SecurityException) {
                Log.d(TAG, "XrExtensions.setCurrentExtensions method could not be called: $e")
            }
        }
    }
}
