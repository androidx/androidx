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

import android.app.Activity;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for translating Extension data classes
 */
final class ExtensionAdapter {

    @NonNull
    DeviceState translate(ExtensionDeviceState deviceState) {
        final int posture;
        switch (deviceState.getPosture()) {
            case ExtensionDeviceState.POSTURE_CLOSED:
                posture = DeviceState.POSTURE_CLOSED;
                break;
            case ExtensionDeviceState.POSTURE_FLIPPED:
                posture = DeviceState.POSTURE_FLIPPED;
                break;
            case ExtensionDeviceState.POSTURE_HALF_OPENED:
                posture = DeviceState.POSTURE_HALF_OPENED;
                break;
            case ExtensionDeviceState.POSTURE_OPENED:
                posture = DeviceState.POSTURE_OPENED;
                break;
            case ExtensionDeviceState.POSTURE_UNKNOWN:
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
    DisplayFeature translate(Activity activity, ExtensionDisplayFeature feature) {
        final Rect windowBounds = WindowBoundsHelper.getInstance()
                .computeCurrentWindowBounds(activity);
        if (!isValid(feature, windowBounds)) {
            return null;
        }
        int type = DisplayFeature.TYPE_FOLD;
        switch (feature.getType()) {
            case ExtensionDisplayFeature.TYPE_FOLD:
                type = DisplayFeature.TYPE_FOLD;
                break;
            case ExtensionDisplayFeature.TYPE_HINGE:
                type = DisplayFeature.TYPE_HINGE;
                break;
        }
        return new DisplayFeature(feature.getBounds(), type);
    }

    boolean isValid(ExtensionDisplayFeature feature, Rect windowBounds) {
        if (feature.getBounds().width() == 0 && feature.getBounds().height() == 0) {
            return false;
        }
        if (feature.getType() == ExtensionDisplayFeature.TYPE_FOLD
                && !feature.getBounds().isEmpty()) {
            return false;
        }
        if (!hasMatchingDimension(feature.getBounds(), windowBounds)) {
            return false;
        }
        return true;
    }

    private boolean hasMatchingDimension(Rect lhs, Rect rhs) {
        boolean matchesWidth = lhs.left == rhs.left && lhs.right == rhs.right;
        boolean matchesHeight = lhs.top == rhs.top && lhs.bottom == rhs.bottom;
        return matchesWidth || matchesHeight;
    }
}
