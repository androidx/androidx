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

package androidx.pdf.annotation

import androidx.annotation.RestrictTo

/**
 * Callback interface for gesture coordination events.
 *
 * These signals allow the host to coordinate the touch event stream between different simultaneous
 * interactions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface OnGestureClaimListener {
    /**
     * Called when the view 'claims' the current gesture stream.
     *
     * The host should typically use this signal to cancel any other 'shadow' or simultaneous
     * interactions that are currently tracking this gesture.
     */
    public fun onGestureClaimed()

    /**
     * Called when the view 'abandons' its interest in the current gesture.
     *
     * The host can use this signal to allow other interactions to continue exclusively.
     */
    public fun onGestureAbandoned()
}
