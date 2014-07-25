/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.internal.widget;

import android.support.v4.view.ViewCompat;
import android.view.View;

/**
 * @hide
 */
public class ViewUtils {

    private ViewUtils() {}

    public static boolean isLayoutRtl(View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Merge two states as returned by {@link ViewCompat#getMeasuredState(android.view.View)} ()}.
     * @param curState The current state as returned from a view or the result
     * of combining multiple views.
     * @param newState The new view state to combine.
     * @return Returns a new integer reflecting the combination of the two
     * states.
     */
    public static int combineMeasuredStates(int curState, int newState) {
        return curState | newState;
    }
}
