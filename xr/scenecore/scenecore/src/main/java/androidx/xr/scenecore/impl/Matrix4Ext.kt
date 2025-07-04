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

@file:JvmName("Matrix4Ext")

package androidx.xr.scenecore.impl

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3

// TODO: b/377781580 - Consider removing this when rotation() can be used for scaled matrices.
/** Returns the unscaled version of this matrix. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Matrix4.getUnscaled(): Matrix4 {
    // Matrix4.scale returns either a positive or negative scale based on the
    // determinant of the rotation matrix.
    val positiveScale = Vector3.abs(this.scale)
    val scaleX = positiveScale.x
    val scaleY = positiveScale.y
    val scaleZ = positiveScale.z
    return Matrix4(
        floatArrayOf(
            data[0] / scaleX,
            data[1] / scaleX,
            data[2] / scaleX,
            data[3],
            data[4] / scaleY,
            data[5] / scaleY,
            data[6] / scaleY,
            data[7],
            data[8] / scaleZ,
            data[9] / scaleZ,
            data[10] / scaleZ,
            data[11],
            data[12],
            data[13],
            data[14],
            data[15],
        )
    )
}
