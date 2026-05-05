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

package androidx.xr.scenecore.spatial.core

import android.annotation.SuppressLint
import android.extensions.xr.XrExtensions
import androidx.xr.runtime.SpatialApiVersionHelper
import androidx.xr.scenecore.runtime.XrExtensionsHolder
import androidx.xr.scenecore.runtime.extensions.XrExtensionsHolderProvider
import com.android.extensions.xr.XrExtensions as XrExtensionsLegacy
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Provides access to XR extensions for the current session.
 *
 * This class handles the initialization and caching of [XrExtensions] needed to support spatial
 * features on compatible devices.
 */
internal class SpatialCoreXrExtensionsHolderProvider : XrExtensionsHolderProvider {
    override val holder: XrExtensionsHolder<*>?
        get() = InstanceHolder.HOLDER

    override val holderLegacy: XrExtensionsHolder<*>
        get() = InstanceHolder.HOLDER_LEGACY

    companion object {
        val extensions: XrExtensions? = InstanceHolder.INSTANCE
        val extensionsLegacy: XrExtensionsLegacy = InstanceHolder.INSTANCE_LEGACY
    }

    // Lazy create the extensions instance.
    @Suppress("RestrictedApiAndroidX")
    @SuppressLint("BanUncheckedReflection")
    private object InstanceHolder {
        val INSTANCE_LEGACY = XrExtensionsLegacy()
        val INSTANCE: XrExtensions? =
            if (SpatialApiVersionHelper.spatialApiVersion > 1) {
                INSTANCE_LEGACY.underlyingObject
            } else {
                null
            }

        val HOLDER: XrExtensionsHolder<*>?
            get() {
                return if (SpatialApiVersionHelper.spatialApiVersion > 1) {
                    XrExtensionsHolder(INSTANCE_LEGACY.underlyingObject, XrExtensions::class.java)
                } else {
                    null
                }
            }

        val HOLDER_LEGACY: XrExtensionsHolder<*> =
            XrExtensionsHolder(INSTANCE_LEGACY, XrExtensionsLegacy::class.java)

        // Holds a reference to an exception that was safely caught during init, for the purposes
        // of inspecting during a debug session.
        var initializeException: Exception? = null

        init {
            // Try to call the @TestApi method XrExtensions.setCurrentExtensions to register
            // the current extensions instance. If this fails for various reasons (e.g. the
            // platform has removed this method or the app is not debuggable), ignore the
            // error.
            try {
                val setCurrentExtensionsMethod: Method =
                    XrExtensionsLegacy::class
                        .java
                        .getDeclaredMethod("setCurrentExtensions", XrExtensionsLegacy::class.java)
                setCurrentExtensionsMethod.isAccessible = true
                setCurrentExtensionsMethod.invoke(null, INSTANCE_LEGACY)
            } catch (e: Exception) {
                when (e) {
                    is NoSuchMethodException,
                    is InvocationTargetException,
                    is IllegalAccessException,
                    is SecurityException -> initializeException = e

                    else -> throw e
                }
            }
        }
    }
}
