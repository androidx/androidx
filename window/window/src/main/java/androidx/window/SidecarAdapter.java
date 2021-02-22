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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for translating Sidecar data classes.
 */
final class SidecarAdapter {

    private static final String TAG = SidecarAdapter.class.getSimpleName();

    @NonNull
    List<DisplayFeature> translate(SidecarWindowLayoutInfo sidecarWindowLayoutInfo,
            SidecarDeviceState deviceState, Rect windowBounds) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        List<SidecarDisplayFeature> sidecarDisplayFeatures =
                getSidecarDisplayFeatures(sidecarWindowLayoutInfo);
        if (sidecarDisplayFeatures == null) {
            return displayFeatures;
        }

        for (SidecarDisplayFeature sidecarFeature : sidecarDisplayFeatures) {
            final DisplayFeature displayFeature = translate(sidecarFeature, deviceState,
                    windowBounds);
            if (displayFeature != null) {
                displayFeatures.add(displayFeature);
            }
        }
        return displayFeatures;
    }

    // TODO(b/172620880): Workaround for Sidecar API implementation issue.
    @SuppressLint("BanUncheckedReflection")
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    @Nullable
    static List<SidecarDisplayFeature> getSidecarDisplayFeatures(SidecarWindowLayoutInfo info) {
        try {
            return info.displayFeatures;
        } catch (NoSuchFieldError error) {
            try {
                Method methodGetFeatures = SidecarWindowLayoutInfo.class.getMethod(
                        "getDisplayFeatures");
                return (List<SidecarDisplayFeature>) methodGetFeatures.invoke(info);
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // TODO(b/172620880): Workaround for Sidecar API implementation issue.
    @SuppressLint("BanUncheckedReflection")
    @VisibleForTesting
    static void setSidecarDisplayFeatures(SidecarWindowLayoutInfo info,
            List<SidecarDisplayFeature> displayFeatures) {
        try {
            info.displayFeatures = displayFeatures;
        } catch (NoSuchFieldError error) {
            try {
                Method methodSetFeatures = SidecarWindowLayoutInfo.class.getMethod(
                        "setDisplayFeatures", List.class);
                methodSetFeatures.invoke(info, displayFeatures);
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NonNull
    WindowLayoutInfo translate(@NonNull Activity activity,
            @Nullable SidecarWindowLayoutInfo extensionInfo, @NonNull SidecarDeviceState state) {
        if (extensionInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        SidecarDeviceState deviceState = new SidecarDeviceState();
        int devicePosture = getSidecarDevicePosture(state);
        setSidecarDevicePosture(deviceState, devicePosture);

        Rect windowBounds = WindowBoundsHelper.getInstance().computeCurrentWindowBounds(activity);
        List<DisplayFeature> displayFeatures = translate(extensionInfo, deviceState, windowBounds);
        return new WindowLayoutInfo(displayFeatures);
    }

    @NonNull
    DeviceState translate(@NonNull SidecarDeviceState sidecarDeviceState) {
        int posture = postureFromSidecar(sidecarDeviceState);
        return new DeviceState(posture);
    }

    @DeviceState.Posture
    private static int postureFromSidecar(SidecarDeviceState sidecarDeviceState) {
        int sidecarPosture = getSidecarDevicePosture(sidecarDeviceState);
        if (sidecarPosture > POSTURE_MAX_KNOWN) {
            if (DEBUG) {
                Log.d(TAG, "Unknown posture reported, WindowManager library should be updated");
            }
            return POSTURE_UNKNOWN;
        }
        return sidecarPosture;
    }

    // TODO(b/172620880): Workaround for Sidecar API implementation issue.
    @SuppressLint("BanUncheckedReflection")
    @VisibleForTesting
    static int getSidecarDevicePosture(SidecarDeviceState sidecarDeviceState) {
        try {
            return sidecarDeviceState.posture;
        } catch (NoSuchFieldError error) {
            try {
                Method methodGetPosture = SidecarDeviceState.class.getMethod("getPosture");
                return (int) methodGetPosture.invoke(sidecarDeviceState);
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        return SidecarDeviceState.POSTURE_UNKNOWN;
    }

    // TODO(b/172620880): Workaround for Sidecar API implementation issue.
    @SuppressLint("BanUncheckedReflection")
    @VisibleForTesting
    static void setSidecarDevicePosture(SidecarDeviceState sidecarDeviceState, int posture) {
        try {
            sidecarDeviceState.posture = posture;
        } catch (NoSuchFieldError error) {
            try {
                Method methodSetPosture = SidecarDeviceState.class.getMethod("setPosture",
                        int.class);
                methodSetPosture.invoke(sidecarDeviceState, posture);
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Converts the display feature from extension. Can return {@code null} if there is an issue
     * with the value passed from extension.
     */
    @Nullable
    // TODO(b/175507310): Remove after fix.
    @SuppressWarnings({"UnusedVariable", "ObjectToString"})
    private static DisplayFeature translate(SidecarDisplayFeature feature,
            SidecarDeviceState deviceState, Rect windowBounds) {
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
            // TODO(b/175507310): Reinstate after fix on the OEM side.
            if (!((bounds.left == 0/* && bounds.right == windowBounds.width()*/)
                    || (bounds.top == 0/* && bounds.bottom == windowBounds.height()*/))) {
                // Bounds for fold and hinge types are expected to span the entire window space.
                // See DisplayFeature#getBounds().
                if (DEBUG) {
                    Log.d(TAG, "Passed a display feature expected to span the entire window but "
                            + "does not, skipping: " + feature);
                }
                return null;
            }
        }

        final int type;
        switch (feature.getType()) {
            case SidecarDisplayFeature.TYPE_FOLD:
                type = FoldingFeature.TYPE_FOLD;
                break;
            case SidecarDisplayFeature.TYPE_HINGE:
                type = FoldingFeature.TYPE_HINGE;
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, "Unknown feature type: " + feature.getType()
                            + ", skipping feature.");
                }
                return null;
        }

        final int state;
        final int devicePosture = getSidecarDevicePosture(deviceState);
        switch (devicePosture) {
            case SidecarDeviceState.POSTURE_CLOSED:
            case SidecarDeviceState.POSTURE_UNKNOWN:
                return null;
            case SidecarDeviceState.POSTURE_FLIPPED:
                state = FoldingFeature.STATE_FLIPPED;
                break;
            case SidecarDeviceState.POSTURE_HALF_OPENED:
                state = FoldingFeature.STATE_HALF_OPENED;
                break;
            case SidecarDeviceState.POSTURE_OPENED:
            default:
                state = FoldingFeature.STATE_FLAT;
                break;
        }

        return new FoldingFeature(feature.getRect(), type, state);
    }

    boolean isEqualSidecarDeviceState(@Nullable SidecarDeviceState first,
            @Nullable SidecarDeviceState second) {
        if (first == second) {
            return true;
        }
        if (first == null) {
            return false;
        }
        if (second == null) {
            return false;
        }
        int firstPosture = getSidecarDevicePosture(first);
        int secondPosture = getSidecarDevicePosture(second);

        return firstPosture == secondPosture;
    }

    boolean isEqualSidecarWindowLayoutInfo(@Nullable SidecarWindowLayoutInfo first,
            @Nullable SidecarWindowLayoutInfo second) {
        if (first == second) {
            return true;
        }
        if (first == null) {
            return false;
        }
        if (second == null) {
            return false;
        }
        List<SidecarDisplayFeature> firstDisplayFeatures = getSidecarDisplayFeatures(first);
        List<SidecarDisplayFeature> secondDisplayFeatures = getSidecarDisplayFeatures(second);
        return isEqualSidecarDisplayFeatures(firstDisplayFeatures, secondDisplayFeatures);
    }

    private boolean isEqualSidecarDisplayFeatures(@Nullable List<SidecarDisplayFeature> first,
            @Nullable List<SidecarDisplayFeature> second) {
        if (first == second) {
            return true;
        }
        if (first == null) {
            return false;
        }
        if (second == null) {
            return false;
        }
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            SidecarDisplayFeature firstFeature = first.get(i);
            SidecarDisplayFeature secondFeature = second.get(i);
            if (!isEqualSidecarDisplayFeature(firstFeature, secondFeature)) {
                return false;
            }
        }
        return true;
    }

    private boolean isEqualSidecarDisplayFeature(@Nullable SidecarDisplayFeature first,
            @Nullable SidecarDisplayFeature second) {
        if (first == second) {
            return true;
        }
        if (first == null) {
            return false;
        }
        if (second == null) {
            return false;
        }
        if (first.getType() != second.getType()) {
            return false;
        }
        if (!first.getRect().equals(second.getRect())) {
            return false;
        }
        return true;
    }
}
