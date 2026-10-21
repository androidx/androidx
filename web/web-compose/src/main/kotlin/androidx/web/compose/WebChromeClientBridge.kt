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

package androidx.web.compose

import android.view.View
import android.webkit.WebChromeClient
import androidx.annotation.RestrictTo
import androidx.web.WebContent

/**
 * Exposes [WebChromeClient] callbacks not yet available directly on [WebContent] when presenting
 * via [WebSurface].
 *
 * Because presentation is managed by [WebSurface] in Compose, custom [View] fullscreen callbacks
 * are not supported on this class.
 *
 * @see WebViewBridge.setWebChromeClient
 * @see WebSurface
 * @see WebContent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class WebChromeClientBridge : WebChromeClient() {
    @Deprecated(
        message = "Fullscreen custom Views are not supported by WebSurface.",
        level = DeprecationLevel.ERROR,
    )
    public final override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        throw UnsupportedOperationException(
            "Fullscreen custom Views are not supported by WebSurface."
        )
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Fullscreen custom Views are not supported by WebSurface.",
        level = DeprecationLevel.ERROR,
    )
    public final override fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        callback: CustomViewCallback?,
    ) {
        throw UnsupportedOperationException(
            "Fullscreen custom Views are not supported by WebSurface."
        )
    }

    @Deprecated(
        message = "Fullscreen custom Views are not supported by WebSurface.",
        level = DeprecationLevel.ERROR,
    )
    public final override fun onHideCustomView() {
        throw UnsupportedOperationException(
            "Fullscreen custom Views are not supported by WebSurface."
        )
    }

    @Deprecated(
        message = "Fullscreen custom Views are not supported by WebSurface.",
        level = DeprecationLevel.ERROR,
    )
    public final override fun getVideoLoadingProgressView(): View? {
        throw UnsupportedOperationException(
            "Fullscreen custom Views are not supported by WebSurface."
        )
    }
}
