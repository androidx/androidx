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

import android.os.Build
import android.webkit.WebView
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.chromium.support_lib_boundary.web.WebProviderFactoryBoundaryInterface

/** Utility class for calling into the WebView APK. */
internal object WebGlueCommunicator {
    private const val GLUE_FACTORY_PROVIDER_FETCHER_CLASS =
        "org.chromium.support_lib_glue.SupportLibReflectionUtil"
    private const val GLUE_FACTORY_PROVIDER_FETCHER_METHOD = "createWebViewProviderFactory"

    // Legacy boundary interface names in org.chromium.support_lib_boundary, used as a fallback to
    // support WebContent and WebSurface on WebView APKs shipped prior to the .web package split.
    private const val LEGACY_WEBVIEW_FACTORY_BOUNDARY_INTERFACE_CLASS =
        "org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface"
    private const val LEGACY_WEB_CONTENT_BOUNDARY_INTERFACE_CLASS =
        "org.chromium.support_lib_boundary.WebContentBoundaryInterface"
    private const val LEGACY_WEB_SURFACE_BOUNDARY_INTERFACE_CLASS =
        "org.chromium.support_lib_boundary.WebSurfaceBoundaryInterface"

    val factory: WebProviderFactoryBoundaryInterface by lazy(::createGlueProviderFactory)

    inline fun <reified T> InvocationHandler.asProxy(): T {
        return Proxy.newProxyInstance(
            WebGlueCommunicator::class.java.classLoader,
            arrayOf(T::class.java),
            this,
        ) as T
    }

    @Suppress("BanUncheckedReflection")
    private fun createGlueProviderFactory(): WebProviderFactoryBoundaryInterface {
        val webViewClassLoader = getWebViewClassLoader()
        val glueFactoryProviderFetcherClass =
            Class.forName(GLUE_FACTORY_PROVIDER_FETCHER_CLASS, false, webViewClassLoader)
        val createProviderFactoryMethod =
            glueFactoryProviderFetcherClass.getDeclaredMethod(GLUE_FACTORY_PROVIDER_FETCHER_METHOD)
        val invocationHandler = createProviderFactoryMethod.invoke(null) as InvocationHandler
        return adaptFactoryInvocationHandlerIfNeeded(invocationHandler, webViewClassLoader)
            .asProxy<WebProviderFactoryBoundaryInterface>()
    }

    /**
     * Adapts the factory [InvocationHandler] when running against an older WebView APK that
     * predates [WebProviderFactoryBoundaryInterface] and the
     * `org.chromium.support_lib_boundary.web` package split.
     *
     * This fallback exists for [WebContent] and [WebSurfaceChromium] (which existed in WebView
     * prior to the split) so that `getSupportedFeatures`, `buildWebContent`, and `createWebSurface`
     * continue to work on those WebView versions.
     */
    @VisibleForTesting
    @Suppress("BanUncheckedReflection")
    internal fun adaptFactoryInvocationHandlerIfNeeded(
        invocationHandler: InvocationHandler,
        webViewClassLoader: ClassLoader,
    ): InvocationHandler {
        return try {
            Class.forName(
                WebProviderFactoryBoundaryInterface::class.java.name,
                false,
                webViewClassLoader,
            )
            // This is the normal case, where the WebView APK contains the split-off
            // WebProviderFactoryBoundaryInterface. If we don't hit this, we are
            // running on an older WebView that predates the split.
            invocationHandler
        } catch (_: ClassNotFoundException) {
            // TODO(b/258722534): Remove this fallback in the future once older WebView versions
            // without WebProviderFactoryBoundaryInterface are no longer supported.
            // Prior to splitting WebProviderFactoryBoundaryInterface out into the .web package,
            // WebContentBoundaryInterface and WebSurfaceBoundaryInterface lived in
            // org.chromium.support_lib_boundary and their factory methods lived on
            // WebViewProviderFactoryBoundaryInterface.
            val webViewFactoryClass =
                Class.forName(
                    LEGACY_WEBVIEW_FACTORY_BOUNDARY_INTERFACE_CLASS,
                    false,
                    webViewClassLoader,
                )
            InvocationHandler { proxy, method, args ->
                fun invokeLegacyMethod(): Any? {
                    val webViewMethod =
                        webViewFactoryClass.getDeclaredMethod(method.name, *method.parameterTypes)
                    return invocationHandler.invoke(proxy, webViewMethod, args)
                }
                when (method.name) {
                    "getSupportedFeatures" -> invokeLegacyMethod()
                    "buildWebContent" ->
                        LegacyBoundaryHandler(
                            invokeLegacyMethod() as InvocationHandler,
                            LEGACY_WEB_CONTENT_BOUNDARY_INTERFACE_CLASS,
                            webViewClassLoader,
                        )
                    "createWebSurface" ->
                        LegacyBoundaryHandler(
                            invokeLegacyMethod() as InvocationHandler,
                            LEGACY_WEB_SURFACE_BOUNDARY_INTERFACE_CLASS,
                            webViewClassLoader,
                        )
                    else ->
                        throw IllegalStateException(
                            "Unexpected method call on legacy WebViewProviderFactory: ${method.name}"
                        )
                }
            }
        }
    }

    /**
     * Wraps a legacy `org.chromium.support_lib_boundary.*` [InvocationHandler] so calls made
     * through `org.chromium.support_lib_boundary.web.*` are forwarded to the legacy interface in
     * the WebView APK, unwrapping any adapted [InvocationHandler] arguments (such as passing a
     * `WebContent` handler to `WebSurface.setWebContent`).
     */
    @Suppress("BanUncheckedReflection")
    private class LegacyBoundaryHandler(
        val delegate: InvocationHandler,
        legacyClassName: String,
        webViewClassLoader: ClassLoader,
    ) : InvocationHandler {
        private val legacyClass: Class<*>

        init {
            legacyClass = Class.forName(legacyClassName, false, webViewClassLoader)
        }

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
            val legacyMethod = legacyClass.getDeclaredMethod(method.name, *method.parameterTypes)
            val unwrappedArgs =
                args
                    ?.map { arg -> if (arg is LegacyBoundaryHandler) arg.delegate else arg }
                    ?.toTypedArray()
            return delegate.invoke(proxy, legacyMethod, unwrappedArgs)
        }
    }

    private fun getWebViewClassLoader(): ClassLoader {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ApiHelperForP.getWebViewClassLoader()
        } else {
            getWebViewProviderFactory().javaClass.classLoader!!
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private object ApiHelperForP {
        @DoNotInline
        fun getWebViewClassLoader(): ClassLoader {
            return WebView.getWebViewClassLoader()
        }
    }

    @Suppress("BanUncheckedReflection")
    private fun getWebViewProviderFactory(): Any {
        return WebView::class
            .java
            .getDeclaredMethod("getFactory")
            .apply { isAccessible = true }
            .invoke(null)!!
    }
}
