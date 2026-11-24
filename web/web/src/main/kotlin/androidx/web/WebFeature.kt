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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import org.chromium.support_lib_boundary.web.util.WebFeatures as Features

/** Utility class for checking which Web features are supported on the device. */
public object WebFeature {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(value = [WebFeature.WEB_CONTENT, WebFeature.WEB_SURFACE])
    @Retention(AnnotationRetention.SOURCE)
    public annotation class WebFeatures

    /** Feature for [WebContent]. */
    public const val WEB_CONTENT: String = "WEB_CONTENT"

    /** Feature for WebSurface. */
    public const val WEB_SURFACE: String = "WEB_SURFACE"

    private val supportedFeatures: Set<String> by lazy {
        try {
            WebGlueCommunicator.factory.supportedFeatures.toSet()
        } catch (e: Throwable) {
            when (e) {
                is ReflectiveOperationException,
                is NoSuchMethodError -> emptySet()
                else -> throw e
            }
        }
    }

    /**
     * Returns whether `feature` is supported at runtime.
     *
     * APIs annotated with [androidx.annotation.RequiresFeature] throw
     * [UnsupportedOperationException] on devices where this returns `false`.
     *
     * @param feature the feature to check, either [WEB_CONTENT] or [WEB_SURFACE]
     * @return `true` if `feature` is supported by the current WebView, `false` otherwise
     */
    @JvmStatic
    public fun isFeatureSupported(@WebFeatures feature: String): Boolean {
        val internalFeature =
            when (feature) {
                WEB_CONTENT -> Features.WEB_CONTENT_V2
                WEB_SURFACE -> Features.WEB_SURFACE
                else -> feature
            }
        return Features.containsFeature(supportedFeatures, internalFeature)
    }

    internal fun checkSupported(@WebFeatures feature: String) {
        if (!isFeatureSupported(feature)) {
            throw UnsupportedOperationException(
                "This method is not supported by the current WebView APK"
            )
        }
    }
}
