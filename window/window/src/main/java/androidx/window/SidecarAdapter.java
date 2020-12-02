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

import static androidx.window.DeviceState.POSTURE_MAX_KNOWN;
import static androidx.window.DeviceState.POSTURE_UNKNOWN;
import static androidx.window.ExtensionCompat.DEBUG;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for translating Sidecar data classes.
 */
final class SidecarAdapter {

    private static final String TAG = SidecarAdapter.class.getSimpleName();

    @NonNull
    List<DisplayFeature> translate(SidecarWindowLayoutInfo sidecarWindowLayoutInfo,
            Rect windowBounds) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        if (sidecarWindowLayoutInfo.displayFeatures == null) {
            return displayFeatures;
        }

        for (SidecarDisplayFeature sidecarFeature : sidecarWindowLayoutInfo.displayFeatures) {
            final DisplayFeature displayFeature = translate(sidecarFeature, windowBounds);
            if (displayFeature != null) {
                displayFeatures.add(displayFeature);
            }
        }
        return displayFeatures;
    }

    @NonNull
    WindowLayoutInfo translate(@NonNull Activity activity,
            @Nullable SidecarWindowLayoutInfo extensionInfo) {
        if (extensionInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        Rect windowBounds = WindowBoundsHelper.getInstance().computeCurrentWindowBounds(activity);
        List<DisplayFeature> displayFeatures = translate(extensionInfo, windowBounds);
        return new WindowLayoutInfo(displayFeatures);
    }

    @NonNull
    DeviceState translate(@Nullable SidecarDeviceState sidecarDeviceState) {
        if (sidecarDeviceState == null) {
            return new DeviceState(POSTURE_UNKNOWN);
        }

        int posture = postureFromSidecar(sidecarDeviceState);
        return new DeviceState(posture);
    }


    @DeviceState.Posture
    private static int postureFromSidecar(SidecarDeviceState sidecarDeviceState) {
        int sidecarPosture = sidecarDeviceState.posture;
        if (sidecarPosture > POSTURE_MAX_KNOWN) {
            if (DEBUG) {
                Log.d(TAG, "Unknown posture reported, WindowManager library should be updated");
            }
            return POSTURE_UNKNOWN;
        }
        return sidecarPosture;
    }

    /**
     * Converts the display feature from extension. Can return {@code null} if there is an issue
     * with the value passed from extension.
     */
    @Nullable
    private static DisplayFeature translate(SidecarDisplayFeature feature, Rect windowBounds) {
        Rect bounds = feature.getRect();
        if (bounds.width() == 0 && bounds.height() == 0) {
            if (DEBUG) {
                Log.d(TAG, "Passed a display feature with empty rect, skipping: " + feature);
            }
            return null;
        }

        if (feature.getType() == SidecarDisplayFeature.TYPE_FOLD) {
            if (bounds.width() != 0 && bounds.height() != 0) {
                // Bounds for fold types are expected to be zero-wide or zero-high.
                // See DisplayFeature#getBounds().
                if (DEBUG) {
                    Log.d(TAG, "Passed a non-zero area display feature expected to be zero-area, "
                            + "skipping: " + feature);
                }
                return null;
            }
        }
        if (feature.getType() == SidecarDisplayFeature.TYPE_HINGE
                || feature.getType() == SidecarDisplayFeature.TYPE_FOLD) {
            if (!((bounds.left == 0 && bounds.right == windowBounds.width())
                    || (bounds.top == 0 && bounds.bottom == windowBounds.height()))) {
                // Bounds for fold and hinge types are expected to span the entire window space.
                // See DisplayFeature#getBounds().
                if (DEBUG) {
                    Log.d(TAG, "Passed a display feature expected to span the entire window but "
                            + "does not, skipping: " + feature);
                }
                return null;
            }
        }

        return new DisplayFeature(feature.getRect(), feature.getType());
    }
}
