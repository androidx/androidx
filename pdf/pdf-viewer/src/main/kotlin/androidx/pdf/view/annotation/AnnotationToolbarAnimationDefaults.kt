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

package androidx.pdf.view.annotation

import androidx.annotation.RestrictTo

/**
 * Animation tunables shared by the View-based ([AnnotationToolbarView],
 * `AnnotationToolbarCoordinatorView`) and Compose-based annotation toolbar implementations, so both
 * stay in sync from a single source of truth.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AnnotationToolbarAnimationDefaults {
    /** Duration of the toolbar expand/collapse transition, in milliseconds. */
    public const val TRANSITION_DURATION_MS: Int = 300

    /** Duration of the snap-to-edge animation that follows a drag, in milliseconds. */
    public const val SNAP_DURATION_MS: Int = 250

    /**
     * Tension of the overshoot applied by the snap-to-edge animation (see
     * `android.view.animation.OvershootInterpolator`).
     */
    public const val SNAP_OVERSHOOT_TENSION: Float = 1f
}
