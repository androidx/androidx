/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.Version.Companion.parse
import androidx.window.extensions.ExtensionFoldingFeature
import androidx.window.extensions.ExtensionInterface
import androidx.window.extensions.ExtensionInterface.ExtensionCallback
import androidx.window.extensions.ExtensionProvider
import androidx.window.extensions.ExtensionWindowLayoutInfo
import java.util.ArrayList

/** Compatibility wrapper for extension versions v1.0+.  */
internal class ExtensionCompat @VisibleForTesting constructor(
    @field:VisibleForTesting val windowExtension: ExtensionInterface?,
    private val adapter: ExtensionAdapter
) : ExtensionInterfaceCompat {
    constructor(context: Context) : this(
        ExtensionProvider.getExtensionImpl(context),
        ExtensionAdapter(WindowMetricsCalculatorCompat)
    ) {
        requireNotNull(windowExtension) { "Extension provider returned null" }
    }

    override fun setExtensionCallback(extensionCallback: ExtensionCallbackInterface) {
        val translatingCallback = ExtensionTranslatingCallback(extensionCallback, adapter)
        windowExtension?.setExtensionCallback(translatingCallback)
    }

    override fun onWindowLayoutChangeListenerAdded(activity: Activity) {
        windowExtension?.onWindowLayoutChangeListenerAdded(activity)
    }

    override fun onWindowLayoutChangeListenerRemoved(activity: Activity) {
        windowExtension?.onWindowLayoutChangeListenerRemoved(activity)
    }

    /** Verifies that extension implementation corresponds to the interface of the version.  */
    override fun validateExtensionInterface(): Boolean {
        return try {
            // extension.setExtensionCallback(ExtensionInterface.ExtensionCallback);
            val methodSetExtensionCallback = windowExtension?.javaClass?.getMethod(
                "setExtensionCallback", ExtensionCallback::class.java
            )
            val rSetExtensionCallback = methodSetExtensionCallback?.returnType
            if (rSetExtensionCallback != Void.TYPE) {
                throw NoSuchMethodException(
                    "Illegal return type for 'setExtensionCallback': $rSetExtensionCallback"
                )
            }

            // extension.onWindowLayoutChangeListenerAdded(Activity);
            val methodRegisterWindowLayoutChangeListener = windowExtension?.javaClass
                ?.getMethod("onWindowLayoutChangeListenerAdded", Activity::class.java)
            val rtRegisterWindowLayoutChangeListener =
                methodRegisterWindowLayoutChangeListener?.returnType
            if (rtRegisterWindowLayoutChangeListener != Void.TYPE) {
                throw NoSuchMethodException(
                    "Illegal return type for 'onWindowLayoutChangeListenerAdded': " +
                        "$rtRegisterWindowLayoutChangeListener"
                )
            }

            // extension.onWindowLayoutChangeListenerRemoved(Activity);
            val methodUnregisterWindowLayoutChangeListener = windowExtension?.javaClass
                ?.getMethod("onWindowLayoutChangeListenerRemoved", Activity::class.java)
            val rtUnregisterWindowLayoutChangeListener =
                methodUnregisterWindowLayoutChangeListener?.returnType
            if (rtUnregisterWindowLayoutChangeListener != Void.TYPE) {
                throw NoSuchMethodException(
                    "Illegal return type for 'onWindowLayoutChangeListenerRemoved': " +
                        "$rtUnregisterWindowLayoutChangeListener"
                )
            }

            // {@link ExtensionFoldingFeature} constructor
            val displayFoldingFeature = ExtensionFoldingFeature(
                Rect(0, 0, 100, 0),
                ExtensionFoldingFeature.TYPE_FOLD,
                ExtensionFoldingFeature.STATE_FLAT
            )

            // displayFoldFeature.getBounds()
            @Suppress("UNUSED_VARIABLE") val tmpRect = displayFoldingFeature.bounds

            // displayFoldFeature.getState()
            @Suppress("UNUSED_VARIABLE") val tmpState = displayFoldingFeature.state

            // displayFoldFeature.getType()
            @Suppress("UNUSED_VARIABLE") val tmpType = displayFoldingFeature.type

            // ExtensionWindowLayoutInfo constructor
            val windowLayoutInfo = ExtensionWindowLayoutInfo(ArrayList())

            // windowLayoutInfo.getDisplayFeatures()
            @Suppress("UNUSED_VARIABLE") val tmpDisplayFeatures = windowLayoutInfo.displayFeatures
            true
        } catch (t: Throwable) {
            if (DEBUG) {
                Log.e(
                    TAG,
                    "Extension implementation doesn't conform to interface version " +
                        "$extensionVersion, error: $t"
                )
            }
            false
        }
    }

    companion object {
        const val DEBUG = false
        private const val TAG = "ExtensionVersionCompat"
        val extensionVersion: Version?
            get() = try {
                val vendorVersion = ExtensionProvider.getApiVersion()
                if (!TextUtils.isEmpty(vendorVersion)) parse(vendorVersion) else null
            } catch (e: NoClassDefFoundError) {
                if (DEBUG) {
                    Log.d(TAG, "Extension version not found")
                }
                null
            } catch (e: UnsupportedOperationException) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Extension")
                }
                null
            }
    }
}