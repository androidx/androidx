/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.resolutionselector;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;

import java.util.List;

/**
 * Applications can filter out unsuitable sizes and sort the resolution list in the preferred
 * order by implementing the resolution filter interface. The preferred order is the order in
 * which the resolutions should be tried first.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ResolutionFilter {
    /**
     * Removes unsuitable sizes and sorts the resolution list in the preferred order.
     *
     * <p>OEMs might make the width or height of the supported output sizes be mod 16 aligned for
     * performance reasons. This means that the device might support 1920x1088 instead of
     * 1920x1080, even though a 16:9 aspect ratio size is 1920x1080. Therefore, the input
     * supported sizes list also contains these aspect ratio sizes when applications specify
     * an {@link AspectRatioStrategy} with {@link AspectRatio#RATIO_16_9} and then also specify a
     * ResolutionFilter to apply their own selection logic.
     *
     * @param supportedSizes  the supported output sizes which have been filtered and sorted
     *                        according to the other resolution selector settings.
     * @param rotationDegrees the rotation degrees to rotate the image to the desired
     *                        orientation, matching the {@link UseCase}’s target rotation setting
     *                        . For example, the target rotation set via
     *                        {@link ImageCapture.Builder#setTargetRotation(int)} or
     *                        {@link ImageCapture#setTargetRotation(int)}.
     * @return the desired ordered sizes list for resolution selection. The returned list should
     * only include sizes in the provided input supported sizes list.
     */
    @NonNull
    List<Size> filter(@NonNull List<Size> supportedSizes, int rotationDegrees);
}
