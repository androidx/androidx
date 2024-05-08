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

import static androidx.camera.core.AspectRatio.RATIO_16_9;
import static androidx.camera.core.AspectRatio.RATIO_4_3;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.lifecycle.LifecycleOwner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The aspect ratio strategy defines the sequence of aspect ratios that are used to select the
 * best size for a particular image.
 *
 * <p>Applications can create a {@link ResolutionSelector} with a proper AspectRatioStrategy to
 * choose a resolution that matches the preferred aspect ratio.
 *
 * <p>By default, CameraX supports the common 4:3 and 16:9 aspect ratio settings. Some devices may
 * offer additional output sizes. To access these, you'll need to create a
 * {@link ResolutionSelector} with a {@link ResolutionFilter} to find and select those specific
 * sizes.
 */
public final class AspectRatioStrategy {
    /**
     * CameraX doesn't fall back to select sizes of any other aspect ratio when this fallback
     * rule is used.
     *
     * <p>Note that an AspectRatioStrategy with more restricted settings may result in that no
     * resolution can be selected to use. Applications will receive
     * {@link IllegalArgumentException} when binding the {@link UseCase}s with such kind of
     * AspectRatioStrategy.
     */
    public static final int FALLBACK_RULE_NONE = 0;
    /**
     * CameraX automatically chooses the next best aspect ratio which contains the closest field
     * of view (FOV) of the camera sensor, from the remaining options.
     */
    public static final int FALLBACK_RULE_AUTO = 1;

    /**
     * Defines the available fallback rules for AspectRatioStrategy.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FALLBACK_RULE_NONE,
            FALLBACK_RULE_AUTO
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface AspectRatioFallbackRule {
    }

    /**
     * The pre-defined default aspect ratio strategy that selects sizes with
     * {@link AspectRatio#RATIO_4_3} in priority. Then, selects sizes with other aspect ratios
     * according to which aspect ratio can contain the closest FOV of the camera sensor.
     *
     * <p>Please see the
     * <a href="https://source.android.com/docs/core/camera/camera3_crop_reprocess">Output streams,
     * Cropping, and Zoom</a> introduction to know more about the camera FOV.
     */
    @NonNull
    public static final AspectRatioStrategy RATIO_4_3_FALLBACK_AUTO_STRATEGY =
            new AspectRatioStrategy(RATIO_4_3, FALLBACK_RULE_AUTO);

    /**
     * The pre-defined aspect ratio strategy that selects sizes with
     * {@link AspectRatio#RATIO_16_9} in priority. Then, selects sizes with other aspect ratios
     * according to which aspect ratio can contain the closest FOV of the camera sensor.
     *
     * <p>Please see the
     * <a href="https://source.android.com/docs/core/camera/camera3_crop_reprocess">Output streams,
     * Cropping, and Zoom</a> introduction to know more about the camera FOV.
     */
    @NonNull
    public static final AspectRatioStrategy RATIO_16_9_FALLBACK_AUTO_STRATEGY =
            new AspectRatioStrategy(RATIO_16_9, FALLBACK_RULE_AUTO);

    @AspectRatio.Ratio
    private final int mPreferredAspectRatio;
    @AspectRatioFallbackRule
    private final int mFallbackRule;

    /**
     * Creates a new AspectRatioStrategy instance, configured with the specified preferred aspect
     * ratio and fallback rule.
     *
     * <p>OEMs might make the width or height of the supported output sizes be mod 16 aligned for
     * performance reasons. This means that the device might support 1920x1088 instead of
     * 1920x1080, even though a 16:9 aspect ratio size is 1920x1080. CameraX can select these mod
     * 16 aligned sizes when applications specify the preferred aspect ratio as
     * {@link AspectRatio#RATIO_16_9}.
     *
     * <p>Some devices may have issues using sizes of the preferred aspect ratios. CameraX
     * recommends that applications use the {@link #FALLBACK_RULE_AUTO} setting to avoid no
     * resolution being available, as an {@link IllegalArgumentException} may be thrown when
     * calling
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner, CameraSelector, UseCase...)}
     * to bind {@link UseCase}s with the AspectRatioStrategy specified in the
     * {@link ResolutionSelector}.
     *
     * @param preferredAspectRatio the preferred aspect ratio to select first.
     * @param fallbackRule the rule to follow when the preferred aspect ratio is not available.
     */
    public AspectRatioStrategy(@AspectRatio.Ratio int preferredAspectRatio,
            @AspectRatioFallbackRule int fallbackRule) {
        mPreferredAspectRatio = preferredAspectRatio;
        mFallbackRule = fallbackRule;
    }

    /**
     * Returns the specified preferred aspect ratio.
     */
    @AspectRatio.Ratio
    public int getPreferredAspectRatio() {
        return mPreferredAspectRatio;
    }

    /**
     * Returns the specified fallback rule for choosing the aspect ratio when the preferred aspect
     * ratio is not available.
     */
    @AspectRatioFallbackRule
    public int getFallbackRule() {
        return mFallbackRule;
    }
}
