/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.car.utils;

import android.view.View;

import androidx.car.R;

/**
 * A utility class that will set the current background for a View that represents an card entry
 * in a list. The class will set the background depending on the position of the card within the
 * list.
 */
public class ListItemBackgroundResolver {
    private ListItemBackgroundResolver() {}

    /**
     * Sets the background on the given view so that the combination of all items looks like a
     * rectangle with rounded corner. The view is assumed to have a non-rounded corner outline.
     *
     * <p>The view will be set with rounded backgrounds if it is the only card within the list.
     * Or if it is the first or last view, it will have the top or bottom corners rounded
     * respectively.
     *
     * @param view The view whose background to set.
     * @param currentPosition The current position of the View within the list. This value should
     *                        be 0-based.
     * @param totalItems The total items within the list.
     */
    public static void setBackground(View view, int currentPosition, int totalItems) {
        if (currentPosition < 0) {
            throw new IllegalArgumentException("currentPosition cannot be less than zero.");
        }

        if (currentPosition >= totalItems) {
            throw new IndexOutOfBoundsException("currentPosition: " + currentPosition + "; "
                    + "totalItems: " + totalItems);
        }

        // Set the background for each card. Only the top and last card should have rounded corners.
        if (totalItems == 1) {
            // One card - all corners are rounded.
            view.setBackgroundResource(R.drawable.car_card_rounded_background);
        } else if (currentPosition == 0) {
            // First card gets rounded top.
            view.setBackgroundResource(R.drawable.car_card_rounded_top_background);
        } else if (currentPosition == totalItems - 1) {
            // Last one has a rounded bottom.
            view.setBackgroundResource(R.drawable.car_card_rounded_bottom_background);
        } else {
            // Middle has no rounded corners.
            view.setBackgroundResource(R.drawable.car_card_background);
        }
    }
}

