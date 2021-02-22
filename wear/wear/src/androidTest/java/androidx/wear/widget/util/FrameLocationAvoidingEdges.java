/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.wear.widget.util;

import android.view.View;

import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralLocation;

/**
 * Calculates coordinate positions but adjusts them to allow for special areas of the screen that
 * are activated for "Gesture Navigation".
 *
 * <p>When "Gesture Navigation" is enabled (which appears to be the default for later API versions)
 * the bottom left and right corners of the screen and the lest and right edges are active areas for
 * initiative "Navigation gestures".</p>
 */
public enum FrameLocationAvoidingEdges implements CoordinatesProvider {
    CENTER_LEFT_AVOIDING_EDGE(GeneralLocation.CENTER_LEFT, Constants.OFFSET_FROM_EDGE, 0.0f),
    BOTTOM_CENTER_AVOIDING_EDGE(GeneralLocation.BOTTOM_CENTER, 0.0f,
            -(Constants.OFFSET_FROM_EDGE)),
    TOP_RIGHT_AVOIDING_CORNER(GeneralLocation.TOP_RIGHT, -(Constants.OFFSET_FROM_EDGE),
            (Constants.OFFSET_FROM_EDGE)),
    BOTTOM_RIGHT_AVOIDING_CORNER(GeneralLocation.BOTTOM_RIGHT, -(Constants.OFFSET_FROM_EDGE),
            -(Constants.OFFSET_FROM_EDGE));

    private final CoordinatesProvider mOriginalProvider;
    private final float mXAdjust;
    private final float mYAdjust;

    FrameLocationAvoidingEdges(CoordinatesProvider originalProvider, float xAdjust,
            float yAdjust) {
        mOriginalProvider = originalProvider;
        mXAdjust = xAdjust;
        mYAdjust = yAdjust;
    }

    @Override
    public float[] calculateCoordinates(View view) {
        float[] calculateCoordinates = mOriginalProvider.calculateCoordinates(view);
        calculateCoordinates[0] = calculateCoordinates[0] + mXAdjust;
        calculateCoordinates[1] = calculateCoordinates[1] + mYAdjust;
        return calculateCoordinates;
    }

    public static class Constants {
        /**
         * Distance from the edge of the screen in pixels that will ensure that we do not initiate
         * "Navigation gestures"
         */
        public static final float OFFSET_FROM_EDGE = 50.0f; //pixels
    }
}
