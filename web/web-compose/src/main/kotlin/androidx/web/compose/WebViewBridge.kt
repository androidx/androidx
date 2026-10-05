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

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.compose.ui.Modifier
import androidx.web.WebContent
import androidx.web.WebContentView

/**
 * Exposes [android.webkit.WebView] APIs not yet available directly on [WebContent] when presenting
 * via [WebSurface].
 *
 * When used with [WebSurface], instances of [WebViewBridge] must be created within the
 * `bridgeFactory` lambda using the [Context] provided to that factory.
 *
 * Because presentation, layout, and input are managed by [WebSurface] in Compose,
 * [android.view.View] methods are deprecated on this class and should be replaced with Compose
 * [Modifier]s or [WebContent] APIs.
 *
 * @see WebSurface
 * @see WebContent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class WebViewBridge : WebContentView {
    public constructor(@NonNull context: Context) : super(context)
}
