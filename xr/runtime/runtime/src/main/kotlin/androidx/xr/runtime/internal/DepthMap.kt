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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Contains depth related information corresponding to the latest frame from the perspective of a
 * particular view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface DepthMap {

    /** The width of the depth map. */
    public val width: Int

    /** The height of the depth map. */
    public val height: Int

    /** Raw depth values representing meters from the image plane. */
    public val rawDepthMap: FloatBuffer?

    /** Confidence values for the raw depth map. Higher values represent higher confidence. */
    public val rawConfidenceMap: ByteBuffer?

    /** Smooth depth values representing meters from the image plane. */
    public val smoothDepthMap: FloatBuffer?

    /** Confidence values for the smooth depth map. Higher values represent higher confidence. */
    public val smoothConfidenceMap: ByteBuffer?
}
