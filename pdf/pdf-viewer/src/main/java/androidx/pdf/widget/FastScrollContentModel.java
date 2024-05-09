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

package androidx.pdf.widget;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Model for a {@link FastScrollView} to update the scrollbar when the content view is scrolled
 * and update the content scroll position when the scroll thumb is dragged.
 * <p>
 * All units used are arbitrary but must be consistent. For example they could be pixels or page
 * numbers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface FastScrollContentModel {

    /**
     * Estimate the full content height.
     */
    float estimateFullContentHeight();

    /**
     * The currently visible height in the {@link FastScrollView} in the same units as
     * {@link #estimateFullContentHeight}.
     */
    float visibleHeight();

    /**
     * Scroll the content view to the position indicated.
     *
     * @param position the latest/current scroll position
     * @param stable   whether the scroll gesture is finished (stable is false while the gesture
     *                 is in
     *                 progress, and is true when the gesture finishes).
     */
    void fastScrollTo(float position, boolean stable);

    /**
     * Allow the {@link FastScrollView} to update the scroll thumb when the content scrolls.
     */
    void setFastScrollListener(@NonNull FastScrollListener listener);

    /**
     * Listener for content scroll events in units consistent with the other methods in this class.
     */
    interface FastScrollListener {

        /** Update scrollbar based on the given position. */
        void updateFastScrollbar(float position);
    }
}
