/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring

import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.ink.strokes.Stroke

/**
 * Notifies the client app when a [LegacyStroke] or [Stroke] (or more than one) has been completed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@UiThread
public interface InProgressStrokesFinishedListener {
    /**
     * Called when there are no longer any in-progress strokes. All strokes that were in progress
     * simultaneously will be delivered in the same callback. This callback will execute on the UI
     * thread. The implementer should prepare to start rendering the given strokes in their own
     * [android.view.View]. To do that, the strokes should be saved in a variable where they will be
     * picked up in a view's next call to [android.view.View.onDraw], and that view's
     * [android.view.View.invalidate] should be called. When that happens, in the same UI thread run
     * loop (HWUI frame), [InProgressStrokesView.removeFinishedStrokes] should be called with the
     * IDs of the strokes that are now being rendered in the other view. Failure to adhere to these
     * guidelines will result in brief rendering errors between this view and the client app's
     * view - either a gap where the stroke is not drawn during a frame, or a double draw where the
     * stroke is drawn twice and translucent strokes appear more opaque than they should.
     */
    public fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {}
}
