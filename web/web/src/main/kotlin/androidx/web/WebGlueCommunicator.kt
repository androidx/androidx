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
import java.lang.reflect.InvocationHandler
import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil

/** Utility class for calling into the WebView APK. */
internal object WebGlueCommunicator {
    private const val GLUE_FACTORY_PROVIDER_FETCHER_CLASS =
        "org.chromium.support_lib_glue.SupportLibReflectionUtil"
    private const val GLUE_FACTORY_PROVIDER_FETCHER_METHOD = "createWebViewProviderFactory"

    val factory: WebViewProviderFactoryBoundaryInterface by lazy(::createGlueProviderFactory)

    @Suppress("BanUncheckedReflection")
    private fun createGlueProviderFactory(): WebViewProviderFactoryBoundaryInterface {
        val glueFactoryProviderFetcherClass =
            Class.forName(GLUE_FACTORY_PROVIDER_FETCHER_CLASS, false, getWebViewClassLoader())
        val createProviderFactoryMethod =
            glueFactoryProviderFetcherClass.getDeclaredMethod(GLUE_FACTORY_PROVIDER_FETCHER_METHOD)
        val invocationHandler = createProviderFactoryMethod.invoke(null) as InvocationHandler
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
            WebViewProviderFactoryBoundaryInterface::class.java,
            invocationHandler,
        )!!
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
