/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf

/**
 * A remote-compatible drawing scope for RemoteCompose that provides access to the content of the
 * component being drawn.
 */
public class RemoteContentDrawScope
internal constructor(
    remoteCanvas: RemoteCanvas,
    private val content: RemoteDrawScope.() -> Unit = {
        remoteCanvas.drawComponentContent()
    },
    private val contentWidth: (() -> RemoteFloat)? = null,
    private val contentHeight: (() -> RemoteFloat)? = null,
    private val paddingLeft: RemoteFloat? = null,
    private val paddingTop: RemoteFloat? = null,
) : RemoteDrawScope(remoteCanvas) {

    // When padding precedes Modifier.drawWithContent, the drawing scope represents the inner
    // content area rather than the outer node dimensions. Overriding width and height ensures
    // that drawing operations using this scope (e.g. background shapes, border outlines) reflect
    // the inner content bounds.
    override val width: RemoteFloat
        get() = contentWidth?.invoke() ?: super.width

    override val height: RemoteFloat
        get() = contentHeight?.invoke() ?: super.height

    /** Draws the content of the component. */
    public fun drawContent() {
        val hasOffset = paddingLeft != null || paddingTop != null
        if (hasOffset) {
            val dx = paddingLeft ?: 0f.rf
            val dy = paddingTop ?: 0f.rf
            // When padding precedes drawWithContent, the canvas is pre-translated by
            // (paddingLeft, paddingTop) so that drawWithContent operates in inner content
            // coordinates. However, LayoutComponent in core already applies padding
            // translation when painting its child components. Translating back by (-dx, -dy)
            // here prevents child content from being double-offset.
            remoteCanvas.translate(-dx, -dy)
            content()
            remoteCanvas.translate(dx, dy)
        } else {
            content()
        }
    }
}
