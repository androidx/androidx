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

package androidx.web

import androidx.web.WebGlueCommunicator.asProxy
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.function.BiConsumer
import java.util.function.Consumer
import net.bytebuddy.ByteBuddy
import org.chromium.support_lib_boundary.web.WebContentBoundaryInterface
import org.chromium.support_lib_boundary.web.WebProviderFactoryBoundaryInterface
import org.chromium.support_lib_boundary.web.WebSurfaceBoundaryInterface
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Verifying that [WebFeature] gracefully reports features as unsupported when no WebView APK is
 * available on the JVM, and that [WebGlueCommunicator] falls back to
 * `WebViewProviderFactoryBoundaryInterface` on older WebView APKs.
 */
@RunWith(JUnit4::class)
class WebFeatureTest {
    @Test
    fun testFeaturesUnsupportedInUnsupportedEnvironment() {
        assertFalse(WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT))
    }

    @Test
    fun testWebViewProviderFactoryFallback() {
        // Simulate a pre-split WebView ClassLoader that has
        // WebViewProviderFactoryBoundaryInterface,
        // WebContentBoundaryInterface, and WebSurfaceBoundaryInterface in
        // org.chromium.support_lib_boundary, but NOT any interfaces in
        // org.chromium.support_lib_boundary.web.
        val webViewFactoryInterfaceName =
            "org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface"
        val legacyWebContentInterfaceName =
            "org.chromium.support_lib_boundary.WebContentBoundaryInterface"
        val legacyWebSurfaceInterfaceName =
            "org.chromium.support_lib_boundary.WebSurfaceBoundaryInterface"
        val parentWithoutWebBoundary =
            object : ClassLoader(WebFeatureTest::class.java.classLoader) {
                override fun loadClass(name: String, resolve: Boolean): Class<*> {
                    if (name.startsWith("org.chromium.support_lib_boundary.web.")) {
                        throw ClassNotFoundException(name)
                    }
                    return super.loadClass(name, resolve)
                }
            }
        val factoryType =
            ByteBuddy()
                .makeInterface()
                .name(webViewFactoryInterfaceName)
                .defineMethod("getSupportedFeatures", Array<String>::class.java, Modifier.PUBLIC)
                .withoutCode()
                .defineMethod("buildWebContent", InvocationHandler::class.java, Modifier.PUBLIC)
                .withParameters(Consumer::class.java)
                .withoutCode()
                .defineMethod("createWebSurface", InvocationHandler::class.java, Modifier.PUBLIC)
                .withParameters(BiConsumer::class.java)
                .withoutCode()
                .make()
        val contentType =
            ByteBuddy()
                .makeInterface()
                .name(legacyWebContentInterfaceName)
                .defineMethod("destroy", Void.TYPE, Modifier.PUBLIC)
                .withoutCode()
                .make()
        val surfaceType =
            ByteBuddy()
                .makeInterface()
                .name(legacyWebSurfaceInterfaceName)
                .defineMethod("setWebContent", Void.TYPE, Modifier.PUBLIC)
                .withParameters(InvocationHandler::class.java)
                .withoutCode()
                .defineMethod("destroy", Void.TYPE, Modifier.PUBLIC)
                .withoutCode()
                .make()
        val webViewClassLoader =
            surfaceType
                .include(contentType, factoryType)
                .load(parentWithoutWebBoundary)
                .loaded
                .classLoader!!

        var contentDestroyed = false
        val apkContentHandler = InvocationHandler { _, method, _ ->
            val declaringClassInApk =
                Class.forName(method.declaringClass.name, false, webViewClassLoader)
            val apkMethod =
                declaringClassInApk.getDeclaredMethod(method.name, *method.parameterTypes)
            when (apkMethod.name) {
                "destroy" -> {
                    contentDestroyed = true
                    null
                }
                else -> throw UnsupportedOperationException(apkMethod.name)
            }
        }
        var receivedContentHandler: Any? = null
        var surfaceDestroyed = false
        val apkSurfaceHandler = InvocationHandler { _, method, args ->
            val declaringClassInApk =
                Class.forName(method.declaringClass.name, false, webViewClassLoader)
            val apkMethod =
                declaringClassInApk.getDeclaredMethod(method.name, *method.parameterTypes)
            when (apkMethod.name) {
                "setWebContent" -> {
                    receivedContentHandler = args?.get(0)
                    null
                }
                "destroy" -> {
                    surfaceDestroyed = true
                    null
                }
                else -> throw UnsupportedOperationException(apkMethod.name)
            }
        }
        // Simulate Chromium's InvocationHandlerWithDelegateGetter which checks that the invoked
        // Method's declaringClass name exists in the WebView APK's ClassLoader.
        val apkFactoryHandler = InvocationHandler { _, method, _ ->
            val declaringClassInApk =
                Class.forName(method.declaringClass.name, false, webViewClassLoader)
            val apkMethod =
                declaringClassInApk.getDeclaredMethod(method.name, *method.parameterTypes)
            when (apkMethod.name) {
                "getSupportedFeatures" -> arrayOf("WEB_CONTENT", "WEB_SURFACE")
                "buildWebContent" -> apkContentHandler
                "createWebSurface" -> apkSurfaceHandler
                else -> throw UnsupportedOperationException(apkMethod.name)
            }
        }

        val factory =
            WebGlueCommunicator.adaptFactoryInvocationHandlerIfNeeded(
                    apkFactoryHandler,
                    webViewClassLoader,
                )
                .asProxy<WebProviderFactoryBoundaryInterface>()

        assertThat(factory.supportedFeatures).asList().containsExactly("WEB_CONTENT", "WEB_SURFACE")
        val contentBoundary = factory.buildWebContent {}.asProxy<WebContentBoundaryInterface>()
        val surfaceBoundary =
            factory.createWebSurface { _, _ -> }.asProxy<WebSurfaceBoundaryInterface>()
        surfaceBoundary.setWebContent(Proxy.getInvocationHandler(contentBoundary))
        assertThat(receivedContentHandler).isSameInstanceAs(apkContentHandler)

        surfaceBoundary.destroy()
        assertThat(surfaceDestroyed).isTrue()

        contentBoundary.destroy()
        assertThat(contentDestroyed).isTrue()
    }
}
