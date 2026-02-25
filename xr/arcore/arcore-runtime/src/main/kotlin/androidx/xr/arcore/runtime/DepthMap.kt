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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Contains depth related information corresponding to the latest frame from the perspective of a
 * particular view.
 *
 * @property width the width of the depth map
 * @property height the height of the depth map
 * @property rawDepthMap raw depth values representing meters from the image plane
 * @property rawConfidenceMap confidence values for the raw depth map
 * @property smoothDepthMap smooth depth values representing meters from the image plane
 * @property smoothConfidenceMap confidence values for the smooth depth map
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface DepthMap {
    public val width: Int
    public val height: Int
    public val rawDepthMap: FloatBuffer?
    public val rawConfidenceMap: ByteBuffer?
    public val smoothDepthMap: FloatBuffer?
    public val smoothConfidenceMap: ByteBuffer?
}
