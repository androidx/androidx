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
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil
import org.chromium.support_lib_boundary.util.Features

/** Utility class for checking which Web features are supported on the device. */
public object WebFeature {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(value = [WebFeature.WEB_CONTENT])
    @Retention(AnnotationRetention.SOURCE)
    public annotation class WebFeatures

    /** Feature for [WebContent]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public const val WEB_CONTENT: String = Features.WEB_CONTENT

    private val supportedFeatures: Set<String> by lazy {
        WebGlueCommunicator.factory.supportedFeatures.toSet()
    }

    /**
     * Return whether a feature is supported at runtime. On devices where this is not supported, an
     * [UnsupportedOperationException] will be thrown.
     *
     * @param feature the feature to be checked
     * @return true if the feature is supported
     */
    @JvmStatic
    public fun isFeatureSupported(@WebFeatures feature: String): Boolean {
        return BoundaryInterfaceReflectionUtil.containsFeature(supportedFeatures, feature)
    }

    internal fun getUnsupportedOperationException() =
        UnsupportedOperationException("This method is not supported by the current WebView APK")
}
