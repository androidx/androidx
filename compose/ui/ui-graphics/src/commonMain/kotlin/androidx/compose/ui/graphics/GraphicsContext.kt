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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.layer.GraphicsLayer

/**
 * Class responsible for providing graphics related dependencies. This includes the creation and
 * management of [GraphicsLayer] instances.
 */
interface GraphicsContext {

    /**
     * Create a [GraphicsLayer] instance. This may internally return a previously released
     * [GraphicsLayer] instance passed to [releaseGraphicsLayer]
     */
    fun createGraphicsLayer(): GraphicsLayer

    /**
     * Releases a [GraphicsLayer] instance so it can be re-used. After this method is invoked, it is
     * an error to use this [GraphicsLayer] instance again. The [GraphicsLayer] maybe reused
     * internally and obtained again through a subsequent call to [createGraphicsLayer]
     */
    fun releaseGraphicsLayer(layer: GraphicsLayer)
}
