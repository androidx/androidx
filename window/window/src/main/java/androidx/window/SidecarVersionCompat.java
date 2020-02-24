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

import static androidx.window.DeviceState.POSTURE_UNKNOWN;
import static androidx.window.SidecarHelper.DEBUG;

import android.graphics.Rect;

import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Compatibility wrapper for different sidecar versions. Should be used to invoke the correct
 * Sidecar methods depending on the current Sidecar version.
 * <p>To maintain compatibility with {@link Version#VERSION_0_1} that was shipped on some foldable
 * devices before the initial version of the support library API was frozen, some methods of the
 * class use reflection to use the right field and method names.
 */
final class SidecarVersionCompat {
    private Version mSidecarVersion;

    /**
     * @param sidecarVersion Current sidecar version.
     */
    SidecarVersionCompat(Version sidecarVersion) {
        mSidecarVersion = sidecarVersion;
    }

    Rect getFeatureBounds(SidecarDisplayFeature feature) {
        if (mSidecarVersion.equals(Version.VERSION_0_1)) {
            try {
                return (Rect) SidecarDisplayFeature.class
                        .getMethod("getRect").invoke(feature);
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return new Rect();
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return new Rect();
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return new Rect();
            }
        } else {
            return feature.getBounds();
        }
    }

    @SuppressWarnings("unchecked")
    List<SidecarDisplayFeature> getSidecarDisplayFeatures(
            SidecarWindowLayoutInfo sidecarWindowLayoutInfo) {
        if (mSidecarVersion.equals(Version.VERSION_0_1)) {
            try {
                return (List<SidecarDisplayFeature>) sidecarWindowLayoutInfo.getClass()
                        .getDeclaredField("displayFeatures")
                        .get(sidecarWindowLayoutInfo);
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (NoSuchFieldException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            }
        } else {
            return sidecarWindowLayoutInfo.getDisplayFeatures();
        }
    }

    int getSidecarDevicePosture(SidecarDeviceState sidecarDeviceState) {
        if (mSidecarVersion.equals(Version.VERSION_0_1)) {
            try {
                return sidecarDeviceState.getClass().getDeclaredField("posture")
                        .getInt(sidecarDeviceState);
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return POSTURE_UNKNOWN;
            } catch (NoSuchFieldException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return POSTURE_UNKNOWN;
            }
        } else {
            return sidecarDeviceState.getPosture();
        }
    }

    SidecarDisplayFeature newSidecarDisplayFeature(Rect rect, int type) {
        if (mSidecarVersion.equals(Version.VERSION_0_1)) {
            try {
                SidecarDisplayFeature displayFeature =
                        SidecarDisplayFeature.class.getDeclaredConstructor().newInstance();
                SidecarDisplayFeature.class.getMethod("setRect", Rect.class)
                        .invoke(displayFeature, rect);
                SidecarDisplayFeature.class.getMethod("setType", int.class)
                        .invoke(displayFeature, type);
                return displayFeature;
            } catch (InstantiationException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            }
        } else {
            return new SidecarDisplayFeature(rect, type);
        }
    }

    SidecarDeviceState newSidecarDeviceState(int posture) {
        if (mSidecarVersion.equals(Version.VERSION_0_1)) {
            try {
                SidecarDeviceState deviceState =
                        SidecarDeviceState.class.getDeclaredConstructor().newInstance();
                SidecarDeviceState.class.getDeclaredField("posture")
                        .set(deviceState, posture);
                return deviceState;
            } catch (InstantiationException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (NoSuchFieldException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            }
        } else {
            return new SidecarDeviceState(posture);
        }
    }

    SidecarWindowLayoutInfo newSidecarWindowLayoutInfo(
            List<SidecarDisplayFeature> displayFeatures) {
        if (mSidecarVersion.equals(Version.VERSION_0_1)) {
            try {
                SidecarWindowLayoutInfo windowLayoutInfo =
                        SidecarWindowLayoutInfo.class.getDeclaredConstructor().newInstance();
                SidecarWindowLayoutInfo.class.getDeclaredField("displayFeatures")
                        .set(windowLayoutInfo, displayFeatures);
                return windowLayoutInfo;
            } catch (InstantiationException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (NoSuchFieldException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (NoSuchMethodException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            } catch (InvocationTargetException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return null;
            }
        } else {
            return new SidecarWindowLayoutInfo(displayFeatures);
        }
    }
}
