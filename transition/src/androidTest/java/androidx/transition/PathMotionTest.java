/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.graphics.Path;
import android.graphics.PathMeasure;

public abstract class PathMotionTest {

    public static void assertPathMatches(Path expectedPath, Path path) {
        PathMeasure expectedMeasure = new PathMeasure(expectedPath, false);
        PathMeasure pathMeasure = new PathMeasure(path, false);

        boolean expectedNextContour;
        boolean pathNextContour;
        int contourIndex = 0;
        do {
            float expectedLength = expectedMeasure.getLength();
            assertEquals("Lengths differ", expectedLength, pathMeasure.getLength(), 0.01f);

            float minLength = Math.min(expectedLength, pathMeasure.getLength());

            float[] pos = new float[2];

            float increment = minLength / 5f;
            for (float along = 0; along <= minLength; along += increment) {
                expectedMeasure.getPosTan(along, pos, null);
                float expectedX = pos[0];
                float expectedY = pos[1];

                pathMeasure.getPosTan(along, pos, null);
                assertEquals("Failed at " + increment + " in contour " + contourIndex,
                        expectedX, pos[0], 0.01f);
                assertEquals("Failed at " + increment + " in contour " + contourIndex,
                        expectedY, pos[1], 0.01f);
            }
            expectedNextContour = expectedMeasure.nextContour();
            pathNextContour = pathMeasure.nextContour();
            contourIndex++;
        } while (expectedNextContour && pathNextContour);
        assertFalse(expectedNextContour);
        assertFalse(pathNextContour);
    }

}
