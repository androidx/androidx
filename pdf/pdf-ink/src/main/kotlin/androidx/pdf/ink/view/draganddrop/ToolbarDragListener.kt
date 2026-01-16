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

package androidx.pdf.ink.view.draganddrop

import android.view.MotionEvent
import androidx.annotation.RestrictTo
import androidx.pdf.ink.view.AnnotationToolbar

/**
 * Interface definition for callbacks to be invoked when a drag gesture is performed on the
 * [AnnotationToolbar].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ToolbarDragListener {
    /**
     * Called when a long-press gesture is detected and a drag operation is initiated.
     *
     * @param event The raw [MotionEvent] containing the current pointer coordinates, which can be
     *   used to calculate initial touch offset.
     */
    public fun onDragStart(event: MotionEvent)

    /**
     * Called for each movement of the user's finger across the screen during a drag.
     *
     * @param event The raw [MotionEvent] containing the current pointer coordinates, which can be
     *   used to update the toolbar's position.
     */
    public fun onDragMove(event: MotionEvent)

    /**
     * Called when the user lifts their finger, completing the drag operation. This is typically
     * used to snap the toolbar to a final docked position.
     */
    public fun onDragEnd()
}
