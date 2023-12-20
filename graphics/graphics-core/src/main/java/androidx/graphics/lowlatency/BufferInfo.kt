/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.lowlatency

import android.opengl.GLES20

/**
 * Class that represents information about the current buffer that is target for rendered output
 *
 * @param width Current width of the buffer taking pre-rotation into account.
 * @param height Current height of the buffer taking pre-rotation into account
 * @param frameBufferId Frame buffer object identifier. This is useful for retargeting rendering
 * operations to the original destination after rendering to intermediate scratch buffers.
 */
class BufferInfo internal constructor(
    width: Int = 0,
    height: Int = 0,
    frameBufferId: Int = -1
) {

    /**
     * Width of the buffer that is being rendered into. This can be different than the corresponding
     * dimensions specified as pre-rotation can occasionally swap width and height parameters in
     * order to avoid GPU composition to rotate content. This should be used as input to
     * [GLES20.glViewport].
     */
    var width: Int = width
        internal set

    /**
     * Height of the buffer that is being rendered into. This can be different than the
     * corresponding dimensions specified as pre-rotation can occasionally swap width and height
     * parameters in order to avoid GPU composition to rotate content. This should be used as input
     * to [GLES20.glViewport].
     */
    var height: Int = height
        internal set

    /**
     * Identifier of the destination frame buffer object that is being rendered into. This is
     * useful for re-binding to the original target after rendering to intermediate frame buffer
     * objects.
     */
    var frameBufferId: Int = frameBufferId
        internal set
}
