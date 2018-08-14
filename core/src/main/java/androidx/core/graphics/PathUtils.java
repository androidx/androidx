/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.graphics;

import android.graphics.Path;
import android.graphics.PointF;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A set of path-related utility methods. */
public final class PathUtils {
    /**
     * Flattens (or approximate) a {@link Path} with a series of line segments using a 0.5 pixel
     * error.
     *
     * <em>Note:</em> This method requires API 26 or newer.
     *
     * @see #flatten(Path, float)
     */
    @RequiresApi(26)
    @NonNull
    public static Collection<PathSegment> flatten(@NonNull Path path) {
        return flatten(path, 0.5f);
    }

    /**
     * Flattens (or approximate) a {@link Path} with a series of line segments.
     *
     * <em>Note:</em> This method requires API 26 or newer.
     *
     * @param error The acceptable error for a line on the Path. Typically this would be
     *              0.5 so that the error is less than half a pixel.
     *
     * @see Path#approximate
     */
    @RequiresApi(26)
    @NonNull
    public static Collection<PathSegment> flatten(@NonNull final Path path,
            @FloatRange(from = 0) final float error) {
        float[] pathData = path.approximate(error);
        int pointCount = pathData.length / 3;
        List<PathSegment> segments = new ArrayList<>(pointCount);
        for (int i = 1; i < pointCount; i++) {
            int index = i * 3;
            int prevIndex = (i - 1) * 3;

            float d = pathData[index];
            float x = pathData[index + 1];
            float y = pathData[index + 2];

            float pd = pathData[prevIndex];
            float px = pathData[prevIndex + 1];
            float py = pathData[prevIndex + 2];

            if (d != pd && (x != px || y != py)) {
                segments.add(new PathSegment(new PointF(px, py), pd, new PointF(x, y), d));
            }
        }
        return segments;
    }

    private PathUtils() {
    }
}
