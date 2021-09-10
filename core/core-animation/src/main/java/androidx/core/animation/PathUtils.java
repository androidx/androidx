/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.core.animation;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

class PathUtils {
    private static final int NUM_COMPONENTS = 3;
    private static final int MAX_NUM_POINTS = 100;
    private static final float EPSILON = 0.0001f;

    private PathUtils() {}

    static float[] createKeyFrameData(Path path, float precision) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.approximate(path, precision);
        } else {
            // Measure the total length the whole path.
            final PathMeasure measureForTotalLength = new PathMeasure(path, false);
            float totalLength = 0;
            // The sum of the previous contour plus the current one. Using the sum here b/c we want
            // to directly subtract from it later.
            ArrayList<Float> contourLengths = new ArrayList<>();
            contourLengths.add(0f);
            do {
                final float pathLength = measureForTotalLength.getLength();
                totalLength += pathLength;
                contourLengths.add(totalLength);

            } while (measureForTotalLength.nextContour());

            // Now determine how many sample points we need, and the step for next sample.
            final PathMeasure pathMeasure = new PathMeasure(path, false);

            final int numPoints = Math.min(MAX_NUM_POINTS, (int) (totalLength / precision) + 1);

            ArrayList<Float> results = new ArrayList<>(numPoints * NUM_COMPONENTS);

            final float[] position = new float[2];

            int contourIndex = 0;
            float step = totalLength / (numPoints - 1 - contourLengths.size());
            float currentDistance = 0;

            float[] lastTangent = new float[2];
            float[] tangent = new float[2];
            boolean lastTwoPointsOnALine = false;

            // For each sample point, determine whether we need to move on to next contour.
            // After we find the right contour, then sample it using the current distance value
            // minus the previously sampled contours' total length.
            for (int i = 0; i < numPoints; ++i) {
                pathMeasure.getPosTan(currentDistance - contourLengths.get(contourIndex),
                        position, tangent);

                int lastIndex = results.size() - 1;
                if (i > 0 && twoPointsOnTheSameLinePath(tangent, lastTangent,
                        position[0], position[1], results.get(lastIndex - 1),
                        results.get(lastIndex))) {
                    // If the current point and the last two points have the same tangent, they are
                    // on the same line. Instead of adding new points, modify the last point entries
                    if (lastTwoPointsOnALine) {
                        // Modify the entries for the last point added.
                        results.set(lastIndex - 2, (currentDistance / totalLength));
                        results.set(lastIndex - 1, position[0]);
                        results.set(lastIndex, position[1]);

                    } else {
                        lastTwoPointsOnALine = true;
                        addDataEntry(results, currentDistance / totalLength,
                                position[0], position[1]);
                    }
                } else {
                    int skippedPoints = i - results.size() / 3;
                    if (skippedPoints > 0 && lastTwoPointsOnALine) {
                        float fineGrainedDistance = totalLength * results.get(results.size() - 3);
                        float samplePoints = Math.min(skippedPoints, 4);
                        float smallStep = step / samplePoints;

                        while (fineGrainedDistance + smallStep < currentDistance) {
                            fineGrainedDistance += smallStep;
                            pathMeasure.getPosTan(
                                    fineGrainedDistance - contourLengths.get(contourIndex),
                                    position, tangent);

                            addDataEntry(results, fineGrainedDistance / totalLength,
                                    position[0], position[1]);
                        }
                    } else {
                        addDataEntry(results, currentDistance / totalLength,
                                position[0], position[1]);
                    }
                    lastTwoPointsOnALine = false;
                }

                currentDistance += step;

                if ((contourIndex + 1) < contourLengths.size()
                        && currentDistance > contourLengths.get(contourIndex + 1)) {

                    float currentContourSum = contourLengths.get(contourIndex + 1);
                    // Add the point that defines the end of the contour, if it's not already added
                    pathMeasure.getPosTan(
                            currentContourSum - contourLengths.get(contourIndex),
                            position, tangent);
                    addDataEntry(results, currentContourSum / totalLength,
                            position[0], position[1]);

                    contourIndex++;
                    pathMeasure.nextContour();
                }

                lastTangent[0] = tangent[0];
                lastTangent[1] = tangent[1];

                if (currentDistance > totalLength) {
                    break;
                }
            }

            float[] optimizedResults = new float[results.size()];
            for (int i = 0; i < results.size(); i++) {
                optimizedResults[i] = results.get(i);
            }
            return optimizedResults;
        }
    }

    private static boolean twoPointsOnTheSameLinePath(float[] tan1, float[] tan2,
            float x1, float y1, float x2, float y2) {
        if (Math.abs(tan1[0] - tan2[0]) > EPSILON || Math.abs(tan1[1] - tan2[1]) > EPSILON) {
            return false;
        }
        float deltaX = x1 - x2;
        float deltaY = y1 - y2;
        // If deltaY / deltaX = tan1[1] / tan1[0], that means the two points are on the same line as
        // the path.
        return Math.abs(deltaX * tan1[1] - deltaY * tan1[0]) < EPSILON;
    }

    private static void addDataEntry(List<Float> data, float fraction, float x, float y) {
        data.add(fraction);
        data.add(x);
        data.add(y);
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static float[] approximate(Path path, float acceptableError) {
            return path.approximate(acceptableError);
        }
    }
}
