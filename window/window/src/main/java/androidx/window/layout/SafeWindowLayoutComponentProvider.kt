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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.layout.WindowLayoutComponent
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Consumer
import kotlin.reflect.KClass

internal object SafeWindowLayoutComponentProvider {

    val windowLayoutComponent: WindowLayoutComponent? by lazy {
        val loader = SafeWindowLayoutComponentProvider::class.java.classLoader
        if (loader != null && canUseWindowLayoutComponent(loader)) {
            try {
                WindowExtensionsProvider.getWindowExtensions().windowLayoutComponent
            } catch (e: UnsupportedOperationException) {
                null
            }
        } else {
            null
        }
    }

    private fun canUseWindowLayoutComponent(classLoader: ClassLoader): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isWindowLayoutProviderValid(classLoader) &&
                isWindowExtensionsValid(classLoader) &&
                isWindowLayoutComponentValid(classLoader) &&
                isFoldingFeatureValid(classLoader)
        } else {
            false
        }
    }

    private fun isWindowLayoutProviderValid(classLoader: ClassLoader): Boolean {
        return validate {
            val providerClass = windowExtensionsProviderClass(classLoader)
            val getWindowExtensionsMethod = providerClass.getDeclaredMethod("getWindowExtensions")
            val windowExtensionsClass = windowExtensionsClass(classLoader)
            getWindowExtensionsMethod.doesReturn(windowExtensionsClass) &&
                getWindowExtensionsMethod.isPublic
        }
    }

    private fun isWindowExtensionsValid(classLoader: ClassLoader): Boolean {
        return validate {
            val extensionsClass = windowExtensionsClass(classLoader)
            val getWindowLayoutComponentMethod =
                extensionsClass.getMethod("getWindowLayoutComponent")
            val windowLayoutComponentClass = windowLayoutComponentClass(classLoader)
            getWindowLayoutComponentMethod.isPublic &&
                getWindowLayoutComponentMethod.doesReturn(windowLayoutComponentClass)
        }
    }

    private fun isFoldingFeatureValid(classLoader: ClassLoader): Boolean {
        return validate {
            val foldingFeatureClass = foldingFeatureClass(classLoader)
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

    @RequiresApi(24)
    private fun isWindowLayoutComponentValid(classLoader: ClassLoader): Boolean {
        return validate {
            val windowLayoutComponent = windowLayoutComponentClass(classLoader)
            val addListenerMethod = windowLayoutComponent
                .getMethod(
                    "addWindowLayoutInfoListener",
                    Activity::class.java,
                    Consumer::class.java
                )
            val removeListenerMethod = windowLayoutComponent
                .getMethod("removeWindowLayoutInfoListener", Consumer::class.java)
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

    private fun windowExtensionsProviderClass(classLoader: ClassLoader) =
        classLoader.loadClass("androidx.window.extensions.WindowExtensionsProvider")

    private fun windowExtensionsClass(classLoader: ClassLoader) =
        classLoader.loadClass("androidx.window.extensions.WindowExtensions")

    private fun foldingFeatureClass(classLoader: ClassLoader) =
        classLoader.loadClass("androidx.window.extensions.layout.FoldingFeature")

    private fun windowLayoutComponentClass(classLoader: ClassLoader) =
        classLoader.loadClass("androidx.window.extensions.layout.WindowLayoutComponent")
}
