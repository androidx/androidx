/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.action.CoordinatesProvider;

/**
 * Translates a {@link CoordinatesProvider} by the given x and y distances. The distances are given
 * in pixels. Common providers to start with can be found in
 * {@link androidx.test.espresso.action.GeneralLocation GeneralLocation}.
 */
public class TranslatedCoordinatesProvider implements CoordinatesProvider {
    private CoordinatesProvider mProvider;
    private float mDx;
    private float mDy;

    /**
     * Creates an instance of {@link TranslatedCoordinatesProvider}
     *
     * @param coordinatesProvider the {@link CoordinatesProvider} to translate
     * @param dx the distance in x direction
     * @param dy the distance in y direction
     */
    @SuppressLint("LambdaLast")
    public TranslatedCoordinatesProvider(@NonNull CoordinatesProvider coordinatesProvider, float dx,
            float dy) {
        mProvider = coordinatesProvider;
        mDx = dx;
        mDy = dy;
    }

    @NonNull
    @Override
    public float[] calculateCoordinates(@NonNull View view) {
        float[] coords = mProvider.calculateCoordinates(view);
        coords[0] += mDx;
        coords[1] += mDy;
        return coords;
    }
}
