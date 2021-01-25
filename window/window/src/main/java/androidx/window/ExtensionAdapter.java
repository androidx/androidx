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

package androidx.window;

import static androidx.window.ExtensionCompat.DEBUG;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionFoldingFeature;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for translating Extension data classes
 */
final class ExtensionAdapter {
    private static final String TAG = "ExtensionAdapter";

    @NonNull
    DeviceState translate(ExtensionDeviceState deviceState) {
        final int posture;
        switch (deviceState.getPosture()) {
            case ExtensionDeviceState.POSTURE_FLIPPED:
                posture = DeviceState.POSTURE_FLIPPED;
                break;
            case ExtensionDeviceState.POSTURE_HALF_OPENED:
                posture = DeviceState.POSTURE_HALF_OPENED;
                break;
            case ExtensionDeviceState.POSTURE_OPENED:
                posture = DeviceState.POSTURE_OPENED;
                break;
            default:
                posture = DeviceState.POSTURE_UNKNOWN;
        }
        return new DeviceState(posture);
    }

    /**
     * Perform the translation from {@link ExtensionWindowLayoutInfo} to {@link WindowLayoutInfo}.
     * Translates a valid {@link ExtensionDisplayFeature} into a valid {@link DisplayFeature}. If
     * a feature is not valid it is removed
     *
     * @param activity   An {@link android.app.Activity}.
     * @param layoutInfo The source {@link ExtensionWindowLayoutInfo} to be converted
     * @return {@link WindowLayoutInfo} containing the valid {@link DisplayFeature}
     */
    @NonNull
    WindowLayoutInfo translate(@NonNull Activity activity,
            @NonNull ExtensionWindowLayoutInfo layoutInfo) {
        List<DisplayFeature> featureList = new ArrayList<>();
        for (ExtensionDisplayFeature sourceFeature : layoutInfo.getDisplayFeatures()) {
            DisplayFeature targetFeature = translate(activity, sourceFeature);
            if (targetFeature != null) {
                featureList.add(targetFeature);
            }
        }
        return new WindowLayoutInfo(featureList);
    }

    @Nullable
    DisplayFeature translate(Activity activity, ExtensionDisplayFeature displayFeature) {
        if (!(displayFeature instanceof ExtensionFoldingFeature)) {
            return null;
        }
        ExtensionFoldingFeature feature = (ExtensionFoldingFeature) displayFeature;
        final Rect windowBounds = WindowBoundsHelper.getInstance()
                .computeCurrentWindowBounds(activity);
        return translateFoldFeature(windowBounds, feature);
    }

    @Nullable
    private static DisplayFeature translateFoldFeature(@NonNull Rect windowBounds,
            @NonNull ExtensionFoldingFeature feature) {
        if (!isValid(windowBounds, feature)) {
            return null;
        }
        int type;
        switch (feature.getType()) {
            case ExtensionFoldingFeature.TYPE_FOLD:
                type = FoldingFeature.TYPE_FOLD;
                break;
            case ExtensionFoldingFeature.TYPE_HINGE:
                type = FoldingFeature.TYPE_HINGE;
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, "Unknown feature type: " + feature.getType()
                            + ", skipping feature.");
                }
                return null;
        }
        int state;
        switch (feature.getState()) {
            case ExtensionFoldingFeature.STATE_FLAT:
                state = FoldingFeature.STATE_FLAT;
                break;
            case ExtensionFoldingFeature.STATE_FLIPPED:
                state = FoldingFeature.STATE_FLIPPED;
                break;
            case ExtensionFoldingFeature.STATE_HALF_OPENED:
                state = FoldingFeature.STATE_HALF_OPENED;
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, "Unknown feature state: " + feature.getState()
                            + ", skipping feature.");
                }
                return null;
        }
        return new FoldingFeature(feature.getBounds(), type, state);
    }

    private static boolean isValid(Rect windowBounds, ExtensionFoldingFeature feature) {
        if (feature.getBounds().width() == 0 && feature.getBounds().height() == 0) {
            return false;
        }
        if (feature.getType() == ExtensionFoldingFeature.TYPE_FOLD
                && !feature.getBounds().isEmpty()) {
            return false;
        }
        if (feature.getType() != ExtensionFoldingFeature.TYPE_FOLD
                && feature.getType() != ExtensionFoldingFeature.TYPE_HINGE) {
            return false;
        }
        return hasMatchingDimension(feature.getBounds(), windowBounds);
    }

    private static boolean hasMatchingDimension(Rect lhs, Rect rhs) {
        boolean matchesWidth = lhs.left == rhs.left && lhs.right == rhs.right;
        boolean matchesHeight = lhs.top == rhs.top && lhs.bottom == rhs.bottom;
        return matchesWidth || matchesHeight;
    }
}
