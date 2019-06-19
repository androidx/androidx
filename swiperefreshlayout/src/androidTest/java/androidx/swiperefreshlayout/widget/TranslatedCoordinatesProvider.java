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

package androidx.swiperefreshlayout.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.action.CoordinatesProvider;

public class TranslatedCoordinatesProvider implements CoordinatesProvider {
    private CoordinatesProvider mProvider;
    private float mDx;
    private float mDy;

    public TranslatedCoordinatesProvider(@NonNull CoordinatesProvider coordinatesProvider, float dx,
            float dy) {
        mProvider = coordinatesProvider;
        mDx = dx;
        mDy = dy;
    }

    @Override
    public float[] calculateCoordinates(View view) {
        float[] coords = mProvider.calculateCoordinates(view);
        coords[0] += mDx;
        coords[1] += mDy;
        return coords;
    }
}
