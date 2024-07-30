/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.native

import androidx.compose.runtime.Composable
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skiko.SkiaLayer

// TODO: Align with Container/Mediator architecture
internal class ComposeLayer(
    internal val layer: SkiaLayer,
    private val scene: ComposeScene
) {
    fun dispose() {
        layer.detach()
        _initContent = null
    }

    fun setSize(width: Int, height: Int) {
        scene.size = IntSize(width, height)

        layer.needRedraw()
    }

    fun setContent(content: @Composable () -> Unit) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(content)
        }

        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        // TODO: do we need isDisplayable on SkiaLyer?
        // if (layer.isDisplayable) {
        _initContent?.invoke()
        _initContent = null
        // }
    }
}
