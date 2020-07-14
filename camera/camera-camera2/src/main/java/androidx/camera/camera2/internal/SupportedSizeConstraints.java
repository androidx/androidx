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

package androidx.camera.camera2.internal;

import android.graphics.ImageFormat;
import android.os.Build;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Supported sizes constraints.
 *
 * <p>All supported sizes related constraints can be added in this class.
 */
final class SupportedSizeConstraints {
    private static final String ALL_MODELS = "allmodels";
    private static final Range<Integer> ALL_API_LEVELS = new Range<>(0, Integer.MAX_VALUE);

    // The map to store the sizes that need to be excluded from some camera device. The key is
    // CameraDeviceId composed by Brand, Device, Model, ApiLevel and CameraId. The Model and
    // ApiLevel are optional. It depends on whether the excluded sizes need to be applied for a
    // series of devices or for a specific model. And whether the excluded sizes need to be
    // applied for all API levels or a specific API level.
    private static final Map<CameraDeviceId, List<ExcludedSizeConstraint>> EXCLUDED_SIZES_MAP;

    // Adds the excluded sizes that need to be excluded from some camera device here.
    static {
        EXCLUDED_SIZES_MAP = new TreeMap<>(new Comparator<CameraDeviceId>() {
            @Override
            public int compare(CameraDeviceId lhs, CameraDeviceId rhs) {
                if (lhs.equals(rhs)) {
                    return 0;
                }

                int cmp = 0;

                // Compares brand
                if ((cmp = lhs.getBrand().compareTo(rhs.getBrand()))
                        != 0) {
                    return cmp;
                }

                // Compares device
                if ((cmp = lhs.getDevice().compareTo(rhs.getDevice()))
                        != 0) {
                    return cmp;
                }

                // Compares model
                if (!ALL_MODELS.equals(lhs.getModel()) && !ALL_MODELS.equals(rhs.getModel())
                        && (cmp = lhs.getModel().compareTo(rhs.getModel())) != 0) {
                    return cmp;
                }

                // Compares camera id
                if ((cmp = lhs.getCameraId().compareTo(rhs.getCameraId())) != 0) {
                    return cmp;
                }

                return 0;
            }
        });

        EXCLUDED_SIZES_MAP.put(CameraDeviceId.create("OnePlus", "OnePlus6T", ALL_MODELS, "0"),
                Collections.singletonList(
                        ExcludedSizeConstraint.create(Collections.singleton(ImageFormat.JPEG),
                                ALL_API_LEVELS,
                                Arrays.asList(new Size(4160, 3120), new Size(4000, 3000)))));
        EXCLUDED_SIZES_MAP.put(CameraDeviceId.create("OnePlus", "OnePlus6", ALL_MODELS, "0"),
                Collections.singletonList(
                        ExcludedSizeConstraint.create(Collections.singleton(ImageFormat.JPEG),
                                ALL_API_LEVELS,
                                Arrays.asList(new Size(4160, 3120), new Size(4000, 3000)))));

    }

    /**
     * Gets the sizes that need to be excluded for the camera device and image format.
     *
     * @param cameraId    The target camera id.
     * @param imageFormat The target image format.
     */
    @NonNull
    static List<Size> getExcludedSizes(@NonNull String cameraId, int imageFormat) {
        CameraDeviceId cameraDeviceId = CameraDeviceId.create(Build.BRAND, Build.DEVICE,
                Build.MODEL, cameraId);

        if (EXCLUDED_SIZES_MAP.containsKey(cameraDeviceId)) {
            List<Size> excludedSizes = new ArrayList<>();
            List<ExcludedSizeConstraint> excludedSizeConstraintList =
                    EXCLUDED_SIZES_MAP.get(cameraDeviceId);

            for (ExcludedSizeConstraint constraint : excludedSizeConstraintList) {
                if (constraint.getAffectedFormats().contains(imageFormat)
                        && constraint.getAffectedApiLevels().contains(Build.VERSION.SDK_INT)) {
                    excludedSizes.addAll(constraint.getExcludedSizes());
                }
            }

            return excludedSizes;
        }

        return Collections.emptyList();
    }

    private SupportedSizeConstraints() {
    }

    interface Constraint {
        Set<Integer> getAffectedFormats();
        Range<Integer> getAffectedApiLevels();
    }

    @AutoValue
    abstract static class ExcludedSizeConstraint implements Constraint {
        ExcludedSizeConstraint() {
        }

        @NonNull
        public static ExcludedSizeConstraint create(Set<Integer> affectedFormats,
                Range<Integer> affectedApiLevels, List<Size> exclusedSizes) {
            return new AutoValue_SupportedSizeConstraints_ExcludedSizeConstraint(affectedFormats,
                    affectedApiLevels, exclusedSizes);
        }

        @Override
        public abstract Set<Integer> getAffectedFormats();

        @Override
        public abstract Range<Integer> getAffectedApiLevels();

        public abstract List<Size> getExcludedSizes();
    }
}
