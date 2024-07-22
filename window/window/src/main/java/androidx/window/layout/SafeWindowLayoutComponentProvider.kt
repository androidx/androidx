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

package androidx.window.layout

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import androidx.window.SafeWindowExtensionsProvider
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.ReflectionUtils.validateReflection
import androidx.window.reflection.WindowExtensionsConstants.DISPLAY_FOLD_FEATURE_CLASS
import androidx.window.reflection.WindowExtensionsConstants.FOLDING_FEATURE_CLASS
import androidx.window.reflection.WindowExtensionsConstants.JAVA_CONSUMER
import androidx.window.reflection.WindowExtensionsConstants.SUPPORTED_WINDOW_FEATURES_CLASS
import androidx.window.reflection.WindowExtensionsConstants.WINDOW_CONSUMER
import androidx.window.reflection.WindowExtensionsConstants.WINDOW_LAYOUT_COMPONENT_CLASS
import java.lang.reflect.ParameterizedType

/**
 * Reflection Guard for [WindowLayoutComponent]. This will go through the [WindowLayoutComponent]'s
 * method by reflection and check each method's name and signature to see if the interface is what
 * we required.
 */
internal class SafeWindowLayoutComponentProvider(
    private val loader: ClassLoader,
    private val consumerAdapter: ConsumerAdapter
) {
    private val safeWindowExtensionsProvider = SafeWindowExtensionsProvider(loader)

    val windowLayoutComponent: WindowLayoutComponent?
        get() {
            return if (canUseWindowLayoutComponent()) {
                try {
                    WindowExtensionsProvider.getWindowExtensions().windowLayoutComponent
                } catch (e: UnsupportedOperationException) {
                    null
                }
            } else {
                null
            }
        }

    private fun canUseWindowLayoutComponent(): Boolean {
        if (!isWindowLayoutComponentAccessible()) {
            return false
        }
        val vendorApiLevel = ExtensionsUtil.safeVendorApiLevel
        return when {
            vendorApiLevel < 1 -> false
            vendorApiLevel == 1 -> hasValidVendorApiLevel1()
            vendorApiLevel < 5 -> hasValidVendorApiLevel2()
            else -> hasValidVendorApiLevel6()
        }
    }

    @VisibleForTesting
    internal fun isWindowLayoutComponentAccessible(): Boolean =
        safeWindowExtensionsProvider.isWindowExtensionsValid() &&
            isWindowLayoutProviderValid() &&
            isFoldingFeatureValid()

    /**
     * [WindowExtensions.VENDOR_API_LEVEL_1] includes the following methods
     * - [WindowLayoutComponent.addWindowLayoutInfoListener] with [Activity] and
     *   [java.util.function.Consumer]
     * - [WindowLayoutComponent.removeWindowLayoutInfoListener] with [java.util.function.Consumer]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel1(): Boolean {
        return isMethodWindowLayoutInfoListenerJavaConsumerValid()
    }

    /**
     * [WindowExtensions.VENDOR_API_LEVEL_2] includes the following methods
     * - [WindowLayoutComponent.addWindowLayoutInfoListener] with [Context] and [Consumer]
     * - [WindowLayoutComponent.removeWindowLayoutInfoListener] with [Consumer]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel2(): Boolean {
        return hasValidVendorApiLevel1() && isMethodWindowLayoutInfoListenerWindowConsumerValid()
    }

    @VisibleForTesting
    internal fun hasValidVendorApiLevel6(): Boolean {
        return hasValidVendorApiLevel2() &&
            isDisplayFoldFeatureValid() &&
            isSupportedWindowFeaturesValid() &&
            isGetSupportedWindowFeaturesValid()
    }

    private fun isWindowLayoutProviderValid(): Boolean {
        return validateReflection("WindowExtensions#getWindowLayoutComponent is not valid") {
            val extensionsClass = safeWindowExtensionsProvider.windowExtensionsClass
            val getWindowLayoutComponentMethod =
                extensionsClass.getMethod("getWindowLayoutComponent")
            val windowLayoutComponentClass = windowLayoutComponentClass
            getWindowLayoutComponentMethod.isPublic &&
                getWindowLayoutComponentMethod.doesReturn(windowLayoutComponentClass)
        }
    }

    private fun isFoldingFeatureValid(): Boolean {
        return validateReflection("FoldingFeature class is not valid") {
            val foldingFeatureClass = foldingFeatureClass
            val getBoundsMethod = foldingFeatureClass.getMethod("getBounds")
            val getTypeMethod = foldingFeatureClass.getMethod("getType")
            val getStateMethod = foldingFeatureClass.getMethod("getState")
            getBoundsMethod.doesReturn(Rect::class) &&
                getBoundsMethod.isPublic &&
                getTypeMethod.doesReturn(Int::class) &&
                getTypeMethod.isPublic &&
                getStateMethod.doesReturn(Int::class) &&
                getStateMethod.isPublic
        }
    }

    private fun isMethodWindowLayoutInfoListenerJavaConsumerValid(): Boolean {
        return validateReflection(
            "WindowLayoutComponent#addWindowLayoutInfoListener(" +
                "${Activity::class.java.name}, $JAVA_CONSUMER) is not valid"
        ) {
            val consumerClass =
                consumerAdapter.consumerClassOrNull() ?: return@validateReflection false
            val windowLayoutComponent = windowLayoutComponentClass
            val addListenerMethod =
                windowLayoutComponent.getMethod(
                    "addWindowLayoutInfoListener",
                    Activity::class.java,
                    consumerClass
                )
            val removeListenerMethod =
                windowLayoutComponent.getMethod("removeWindowLayoutInfoListener", consumerClass)
            addListenerMethod.isPublic && removeListenerMethod.isPublic
        }
    }

    private fun isMethodWindowLayoutInfoListenerWindowConsumerValid(): Boolean {
        return validateReflection(
            "WindowLayoutComponent#addWindowLayoutInfoListener" +
                "(${Context::class.java.name}, $WINDOW_CONSUMER) is not valid"
        ) {
            val windowLayoutComponent = windowLayoutComponentClass
            val addListenerMethod =
                windowLayoutComponent.getMethod(
                    "addWindowLayoutInfoListener",
                    Context::class.java,
                    Consumer::class.java
                )
            val removeListenerMethod =
                windowLayoutComponent.getMethod(
                    "removeWindowLayoutInfoListener",
                    Consumer::class.java
                )
            addListenerMethod.isPublic && removeListenerMethod.isPublic
        }
    }

    private fun isDisplayFoldFeatureValid(): Boolean {
        return validateReflection("DisplayFoldFeature is not valid") {
            val displayFoldFeatureClass = displayFoldFeatureClass

            val getTypeMethod = displayFoldFeatureClass.getMethod("getType")
            val hasPropertyMethod =
                displayFoldFeatureClass.getMethod("hasProperty", Int::class.java)
            val hasPropertiesMethod =
                displayFoldFeatureClass.getMethod("hasProperties", IntArray::class.java)

            getTypeMethod.isPublic &&
                getTypeMethod.doesReturn(Int::class.java) &&
                hasPropertyMethod.isPublic &&
                hasPropertyMethod.doesReturn(Boolean::class.java) &&
                hasPropertiesMethod.isPublic &&
                hasPropertiesMethod.doesReturn(Boolean::class.java)
        }
    }

    private fun isSupportedWindowFeaturesValid(): Boolean {
        return validateReflection("SupportedWindowFeatures is not valid") {
            val supportedWindowFeaturesClass = supportedWindowFeaturesClass

            val getDisplayFoldFeaturesMethod =
                supportedWindowFeaturesClass.getMethod("getDisplayFoldFeatures")
            val returnTypeGeneric =
                (getDisplayFoldFeaturesMethod.genericReturnType as ParameterizedType)
                    .actualTypeArguments[0]
                    as Class<*>

            getDisplayFoldFeaturesMethod.isPublic &&
                getDisplayFoldFeaturesMethod.doesReturn(List::class.java) &&
                returnTypeGeneric == displayFoldFeatureClass
        }
    }

    private fun isGetSupportedWindowFeaturesValid(): Boolean {
        return validateReflection("WindowLayoutComponent#getSupportedWindowFeatures is not valid") {
            val windowLayoutComponent = windowLayoutComponentClass
            val getSupportedWindowFeaturesMethod =
                windowLayoutComponent.getMethod("getSupportedWindowFeatures")

            getSupportedWindowFeaturesMethod.isPublic &&
                getSupportedWindowFeaturesMethod.doesReturn(supportedWindowFeaturesClass)
        }
    }

    private val displayFoldFeatureClass: Class<*>
        get() {
            return loader.loadClass(DISPLAY_FOLD_FEATURE_CLASS)
        }

    private val supportedWindowFeaturesClass: Class<*>
        get() {
            return loader.loadClass(SUPPORTED_WINDOW_FEATURES_CLASS)
        }

    private val foldingFeatureClass: Class<*>
        get() {
            return loader.loadClass(FOLDING_FEATURE_CLASS)
        }

    private val windowLayoutComponentClass: Class<*>
        get() {
            return loader.loadClass(WINDOW_LAYOUT_COMPONENT_CLASS)
        }
}
