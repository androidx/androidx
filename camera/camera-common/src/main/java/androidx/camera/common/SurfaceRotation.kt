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

package androidx.camera.common

import android.view.Display
import android.view.Surface
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Annotation for integer values representing [Surface] rotation constants.
 *
 * These constants represent the rotation of the screen from its "natural" orientation. Note that
 * these are index values (0, 1, 2, 3) and not degrees.
 *
 * Valid values are:
 * - [Surface.ROTATION_0]
 * - [Surface.ROTATION_90]
 * - [Surface.ROTATION_180]
 * - [Surface.ROTATION_270]
 *
 * These values are typically returned by [Display.getRotation].
 *
 * Use [DiscreteRotation.fromSurfaceRotation] to convert these values to a [DiscreteRotation] which
 * represents the rotation in degrees.
 *
 * @see Display.getRotation
 * @see DiscreteRotation.fromSurfaceRotation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.SOURCE)
@IntDef(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)
public annotation class SurfaceRotation
