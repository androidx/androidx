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

package androidx.camera.core;

import android.graphics.PointF;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Factory to create a {@link MeteringPoint}.
 *
 * <p>MeteringPointFactory implementations must extends this class and implement
 * translatePoint(float x, float y). Users can call createPoint(float x, float y) to create a
 * {@link MeteringPoint} with default areaSize and weight. There is a variation of createPoint
 * that accepts areaSize and weight as well.
 */
public abstract class MeteringPointFactory {
    public static final float DEFAULT_AREASIZE = 0.15f;
    public static final float DEFAULT_WEIGHT = 1.0f;
    @Nullable
    protected Rational mFOVAspectRatio = null; // null for using Preview aspect ratio.

    /**
     * Translates a logical x/y into the normalized crop region x/y.
     *
     * <p>The logical x/y is with respect to related to the implementations. Implementations specify
     * the logical width/height and define the orientation of the area. Some are sensor-oriented and
     * some are display-oriented. The logical x/y is the position from the area defined by the width
     * , height and the orientation.
     *
     * Implementation must implement this method for coordinates translation.
     *
     * @param x the logical x to be translated.
     * @param y the logical y to be translated.
     * @return a {@link PointF} consisting of translated normalized crop region x/y,
     */
    @NonNull
    protected abstract PointF translatePoint(float x, float y);

    /**
     * Creates a {@link MeteringPoint} by x, y.
     *
     * <p>The x/y is the position from the area defined by the width, height and the orientation in
     * specific {@link MeteringPointFactory} implementation.
     */
    @NonNull
    public final MeteringPoint createPoint(float x, float y) {
        return createPoint(x, y, DEFAULT_AREASIZE, DEFAULT_WEIGHT);
    }

    /**
     * Creates a {@link MeteringPoint} by x , y , areaSize and weight.
     *
     * <p>The x/y is the position from the area defined by the width, height and the orientation in
     * specific {@link MeteringPointFactory} implementation.
     *
     * @param x          the logical x to be translated
     * @param y          the logical y to be translated
     * @param areaLength area width/height. The value is ranging from 0 to 1 meaning the
     *                   percentage of
     *                   crop region width/height.
     * @param weight     weight of metering region ranging from 0 to 1.
     * @return A {@link MeteringPoint} that is translated into normalized crop region x/y.
     */
    @NonNull
    public final MeteringPoint createPoint(float x, float y, float areaLength, float weight) {
        PointF translatedXY = translatePoint(x, y);
        return new MeteringPoint(translatedXY.x, translatedXY.y, areaLength, weight,
                mFOVAspectRatio);
    }
}
