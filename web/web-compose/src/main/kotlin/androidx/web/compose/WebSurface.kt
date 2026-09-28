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

import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature

/**
 * A composable that renders [WebContent].
 *
 * @param content The [WebContent] instance to render.
 * @param modifier The modifier to be applied to the layout.
 */
@Suppress("MissingJvmstatic")
@Composable
@RequiresFeature(
    name = WebFeature.WEB_SURFACE,
    enforcement = "androidx.web.WebFeature#isFeatureSupported",
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WebSurface(
    @NonNull content: WebContent,
    modifier: Modifier = Modifier,
) {
    var currentView by remember(content) { mutableStateOf(content.currentView) }
    DisposableEffect(content) {
        val listener: (WebContentView?) -> Unit = { view -> currentView = view }
        content.currentViewListener = listener
        onDispose {
            if (content.currentViewListener === listener) {
                content.currentViewListener = null
            }
        }
    }

    val view = currentView
    if (view != null) {
        key(view) {
            AndroidView(
                factory = {
                    (view.parent as? ViewGroup)?.removeView(view)
                    view
                },
                modifier = modifier,
            )
        }
    } else {
        Spacer(modifier = modifier)
    }
}
