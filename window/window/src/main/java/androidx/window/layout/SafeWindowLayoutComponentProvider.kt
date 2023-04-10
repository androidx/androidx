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
import android.graphics.Rect
import androidx.window.core.ConsumerAdapter
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.layout.WindowLayoutComponent
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

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
        return isWindowLayoutProviderValid() &&
            isWindowExtensionsValid() &&
            isWindowLayoutComponentValid() &&
            isFoldingFeatureValid()
    }

    private fun isWindowLayoutProviderValid(): Boolean {
        return validate {
            val providerClass = windowExtensionsProviderClass
            val getWindowExtensionsMethod = providerClass.getDeclaredMethod("getWindowExtensions")
            val windowExtensionsClass = windowExtensionsClass
            getWindowExtensionsMethod.doesReturn(windowExtensionsClass) &&
                getWindowExtensionsMethod.isPublic
        }
    }

    private fun isWindowExtensionsValid(): Boolean {
        return validate {
            val extensionsClass = windowExtensionsClass
            val getWindowLayoutComponentMethod =
                extensionsClass.getMethod("getWindowLayoutComponent")
            val windowLayoutComponentClass = windowLayoutComponentClass
            getWindowLayoutComponentMethod.isPublic &&
                getWindowLayoutComponentMethod.doesReturn(windowLayoutComponentClass)
        }
    }

    private fun isFoldingFeatureValid(): Boolean {
        return validate {
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

    private fun isWindowLayoutComponentValid(): Boolean {
        return validate {
            val consumerClass = consumerAdapter.consumerClassOrNull() ?: return@validate false
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

    private fun validate(block: () -> Boolean): Boolean {
        return try {
            block()
        } catch (noClass: ClassNotFoundException) {
            false
        } catch (noMethod: NoSuchMethodException) {
            false
        }
    }

    private val Method.isPublic: Boolean
        get() {
            return Modifier.isPublic(modifiers)
        }

    private fun Method.doesReturn(clazz: KClass<*>): Boolean {
        return doesReturn(clazz.java)
    }

    private fun Method.doesReturn(clazz: Class<*>): Boolean {
        return returnType.equals(clazz)
    }

    private val windowExtensionsProviderClass: Class<*>
        get() {
            return loader.loadClass("androidx.window.extensions.WindowExtensionsProvider")
        }

    private val windowExtensionsClass: Class<*>
        get() {
            return loader.loadClass("androidx.window.extensions.WindowExtensions")
        }

    private val foldingFeatureClass: Class<*>
        get() {
            return loader.loadClass("androidx.window.extensions.layout.FoldingFeature")
        }

    private val windowLayoutComponentClass: Class<*>
        get() {
            return loader.loadClass("androidx.window.extensions.layout.WindowLayoutComponent")
        }
}
