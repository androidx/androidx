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
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.reflection.ReflectionUtils.checkIsPresent
import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.ReflectionUtils.validateReflection
import androidx.window.reflection.WindowExtensionsConstants.FOLDING_FEATURE_CLASS
import androidx.window.reflection.WindowExtensionsConstants.JAVA_CONSUMER
import androidx.window.reflection.WindowExtensionsConstants.WINDOW_CONSUMER
import androidx.window.reflection.WindowExtensionsConstants.WINDOW_EXTENSIONS_CLASS
import androidx.window.reflection.WindowExtensionsConstants.WINDOW_EXTENSIONS_PROVIDER_CLASS
import androidx.window.reflection.WindowExtensionsConstants.WINDOW_LAYOUT_COMPONENT_CLASS

/**
 * Reflection Guard for [WindowLayoutComponent].
 * This will go through the [WindowLayoutComponent]'s method by reflection and
 * check each method's name and signature to see if the interface is what we required.
 */
internal class SafeWindowLayoutComponentProvider(
    private val loader: ClassLoader,
    private val consumerAdapter: ConsumerAdapter
) {

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
        if (!isWindowExtensionsPresent() || !isWindowExtensionsValid() ||
            !isWindowLayoutProviderValid() ||
            !isFoldingFeatureValid()
        ) {
            return false
        }
        // TODO(b/267831038): can fallback to VendorApiLevel1 when level2 is not match
        //  but level 1 is matched
        return when (ExtensionsUtil.safeVendorApiLevel) {
            1 -> hasValidVendorApiLevel1()
            in 2..Int.MAX_VALUE -> hasValidVendorApiLevel2()
            // TODO(b/267956499): add hasValidVendorApiLevel3
            else -> false
        }
    }

    private fun isWindowExtensionsPresent(): Boolean {
        return checkIsPresent {
            loader.loadClass(WINDOW_EXTENSIONS_PROVIDER_CLASS)
        }
    }

    /**
     * [WindowExtensions.VENDOR_API_LEVEL_1] includes the following methods
     *  - [WindowLayoutComponent.addWindowLayoutInfoListener] with [Activity] and
     * [java.util.function.Consumer]
     *  - [WindowLayoutComponent.removeWindowLayoutInfoListener] with [java.util.function.Consumer]
     */
    private fun hasValidVendorApiLevel1(): Boolean {
        return isMethodWindowLayoutInfoListenerJavaConsumerValid()
    }

    /**
     * [WindowExtensions.VENDOR_API_LEVEL_2] includes the following methods
     *  - [WindowLayoutComponent.addWindowLayoutInfoListener] with [Context] and
     * [java.util.function.Consumer]
     *  - [WindowLayoutComponent.addWindowLayoutInfoListener] with [Context] and [Consumer]
     *  - [WindowLayoutComponent.removeWindowLayoutInfoListener] with [Consumer]
     */
    private fun hasValidVendorApiLevel2(): Boolean {
        return hasValidVendorApiLevel1() &&
            isMethodWindowLayoutInfoListenerWindowConsumerValid()
    }

    private fun isWindowExtensionsValid(): Boolean {
        return validateReflection("WindowExtensionsProvider#getWindowExtensions is not valid") {
            val providerClass = windowExtensionsProviderClass
            val getWindowExtensionsMethod = providerClass.getDeclaredMethod("getWindowExtensions")
            val windowExtensionsClass = windowExtensionsClass
            getWindowExtensionsMethod.doesReturn(windowExtensionsClass) &&
                getWindowExtensionsMethod.isPublic
        }
    }

    private fun isWindowLayoutProviderValid(): Boolean {
        return validateReflection("WindowExtensions#getWindowLayoutComponent is not valid") {
            val extensionsClass = windowExtensionsClass
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
            val addListenerMethod = windowLayoutComponent
                .getMethod(
                    "addWindowLayoutInfoListener",
                    Activity::class.java,
                    consumerClass
                )
            val removeListenerMethod = windowLayoutComponent
                .getMethod("removeWindowLayoutInfoListener", consumerClass)
            addListenerMethod.isPublic && removeListenerMethod.isPublic
        }
    }

    private fun isMethodWindowLayoutInfoListenerWindowConsumerValid(): Boolean {
        return validateReflection(
            "WindowLayoutComponent#addWindowLayoutInfoListener" +
                "(${Context::class.java.name}, $WINDOW_CONSUMER) is not valid"
        ) {
            val windowLayoutComponent = windowLayoutComponentClass
            val addListenerMethod = windowLayoutComponent
                .getMethod(
                    "addWindowLayoutInfoListener",
                    Context::class.java,
                    Consumer::class.java
                )
            val removeListenerMethod = windowLayoutComponent
                .getMethod("removeWindowLayoutInfoListener", Consumer::class.java)
            addListenerMethod.isPublic && removeListenerMethod.isPublic
        }
    }

    private val windowExtensionsProviderClass: Class<*>
        get() {
            return loader.loadClass(WINDOW_EXTENSIONS_PROVIDER_CLASS)
        }

    private val windowExtensionsClass: Class<*>
        get() {
            return loader.loadClass(WINDOW_EXTENSIONS_CLASS)
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
