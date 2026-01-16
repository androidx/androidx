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

package androidx.webgpu

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@IntDef(
    value =
        [
            SurfaceGetCurrentTextureStatus.SuccessOptimal,
            SurfaceGetCurrentTextureStatus.SuccessSuboptimal,
            SurfaceGetCurrentTextureStatus.Timeout,
            SurfaceGetCurrentTextureStatus.Outdated,
            SurfaceGetCurrentTextureStatus.Lost,
            SurfaceGetCurrentTextureStatus.Error,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Status codes for retrieving the current texture from a surface. */
public annotation class SurfaceGetCurrentTextureStatus {
    public companion object {

        /** A texture was successfully acquired with optimal presentation timing. */
        public const val SuccessOptimal: Int = 0x00000001

        /** A texture was successfully acquired but presentation timing is suboptimal. */
        public const val SuccessSuboptimal: Int = 0x00000002

        /** Acquiring the next texture timed out. */
        public const val Timeout: Int = 0x00000003

        /** The surface configuration is outdated and must be reconfigured. */
        public const val Outdated: Int = 0x00000004

        /** The surface or underlying window is lost. */
        public const val Lost: Int = 0x00000005

        /** An unexpected error occurred when trying to get the texture. */
        public const val Error: Int = 0x00000006
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "SuccessOptimal",
                0x00000002 to "SuccessSuboptimal",
                0x00000003 to "Timeout",
                0x00000004 to "Outdated",
                0x00000005 to "Lost",
                0x00000006 to "Error",
            )

        public fun toString(@SurfaceGetCurrentTextureStatus value: Int): String =
            names[value] ?: value.toString()
    }
}
