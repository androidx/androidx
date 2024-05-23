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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.lifecycle.LifecycleOwner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The resolution strategy defines the resolution selection sequence to select the best size.
 *
 * <p>Applications can create a {@link ResolutionSelector} with a proper ResolutionStrategy to
 * choose the preferred resolution.
 */
public final class ResolutionStrategy {
    /**
     * A resolution strategy chooses the highest available resolution. This strategy does not
     * have a bound size or fallback rule. When using this strategy, CameraX selects the
     * available resolutions to use in descending order, starting with the highest quality
     * resolution available.
     */
    @NonNull
    public static final ResolutionStrategy HIGHEST_AVAILABLE_STRATEGY = new ResolutionStrategy();

    /**
     * CameraX doesn't select an alternate size when the specified bound size is unavailable.
     *
     * <p>Applications will receive {@link IllegalArgumentException} when binding the
     * {@link UseCase}s with this fallback rule if the device doesn't support the specified bound
     * size.
     */
    public static final int FALLBACK_RULE_NONE = 0;
    /**
     * When the specified bound size is unavailable, CameraX falls back to select the closest
     * higher resolution size. If CameraX still cannot find any available resolution, it will
     * fallback to select other lower resolutions.
     */
    public static final int FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER = 1;
    /**
     * When the specified bound size is unavailable, CameraX falls back to the closest higher
     * resolution size.
     */
    public static final int FALLBACK_RULE_CLOSEST_HIGHER = 2;
    /**
     * When the specified bound size is unavailable, CameraX falls back to select the closest
     * lower resolution size. If CameraX still cannot find any available resolution, it will
     * fallback to select other higher resolutions.
     */
    public static final int FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER = 3;
    /**
     * When the specified bound size is unavailable, CameraX falls back to the closest lower
     * resolution size.
     */
    public static final int FALLBACK_RULE_CLOSEST_LOWER = 4;

    /**
     * Defines the available fallback rules for ResolutionStrategy.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FALLBACK_RULE_NONE,
            FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            FALLBACK_RULE_CLOSEST_HIGHER,
            FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
            FALLBACK_RULE_CLOSEST_LOWER
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ResolutionFallbackRule {
    }

    @Nullable
    private Size mBoundSize = null;
    private int mFallbackRule = ResolutionStrategy.FALLBACK_RULE_NONE;

    /**
     * Creates a default ResolutionStrategy instance to select the highest available resolution.
     */
    private ResolutionStrategy() {
    }

    /**
     * Creates a new ResolutionStrategy instance, configured with the specified bound size and
     * fallback rule.
     *
     * <p>If the resolution candidate list contains the bound size and the bound size can fulfill
     * all resolution selector settings, CameraX can also select the specified bound size as the
     * result for the {@link UseCase}.
     *
     * <p>Some devices may have issues using sizes of the preferred aspect ratios. CameraX
     * recommends that applications use the following fallback rule setting to avoid no
     * resolution being available, as an {@link IllegalArgumentException} may be thrown when
     * calling
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner, CameraSelector, UseCase...)}
     * to bind {@link UseCase}s with the ResolutionStrategy specified in the
     * {@link ResolutionSelector}.
     * <ul>
     *     <li> {@link #FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER}
     *     <li> {@link #FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER}
     * </ul>
     *
     * @param boundSize the bound size to select the best resolution with the fallback rule.
     * @param fallbackRule the rule to follow when the specified bound size is not available.
     */
    public ResolutionStrategy(@NonNull Size boundSize, @ResolutionFallbackRule int fallbackRule) {
        mBoundSize = boundSize;
        mFallbackRule = fallbackRule;
    }

    /**
     * Returns the specified bound size.
     *
     * @return the specified bound size or {@code null} if this is instance of
     * {@link #HIGHEST_AVAILABLE_STRATEGY}.
     */
    @Nullable
    public Size getBoundSize() {
        return mBoundSize;
    }

    /**
     * Returns the fallback rule for choosing an alternate size when the specified bound size is
     * unavailable.
     */
    @ResolutionFallbackRule
    public int getFallbackRule() {
        return mFallbackRule;
    }
}
