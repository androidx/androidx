/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.ComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.IntSize

/**
 * The marker interface to be implemented by the desktop root backing the composition.
 * To be used in tests.
 */
@InternalComposeUiApi
interface SkiaRootForTest : RootForTest {
    /**
     * See [WindowInfo.containerSize]
     */
    val containerSize: IntSize

    /**
     * The [ComposeScene] which contains this root.
     * Required only for dispatching input events.
     *
     * TODO: Extract separate interface only for pointer input.
     */
    val scene: ComposeScene get() = throw UnsupportedOperationException("SkiaRootForTest.scene is not implemented")

    /**
     * Whether the Owner has pending layout work.
     */
    val hasPendingMeasureOrLayout: Boolean

    companion object {
        /**
         * Called after an owner implementing [SkiaRootForTest] is created. Used by
         * SkikoComposeUiTest to keep track of all attached roots. Not to be
         * set or used by any other component.
         */
        // TODO: Move to "Shared Context" (aka Platform now)
        @InternalComposeUiApi
        var onRootCreatedCallback: ((SkiaRootForTest) -> Unit)? = null

        // TODO: Move to "Shared Context" (aka Platform now)
        @InternalComposeUiApi
        var onRootDisposedCallback: ((SkiaRootForTest) -> Unit)? = null
    }
}