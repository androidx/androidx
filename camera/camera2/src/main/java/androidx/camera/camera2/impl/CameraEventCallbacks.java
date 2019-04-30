/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl;


import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.MultiValueSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Different implementations of {@link CameraEventCallback}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CameraEventCallbacks extends MultiValueSet<CameraEventCallback> {

    public CameraEventCallbacks(CameraEventCallback ... callbacks) {
        addAll(Arrays.asList(callbacks));
    }

    /** Returns a camera event callback which calls a list of other callbacks. */
    public ComboCameraEventCallback createComboCallback() {
        return new ComboCameraEventCallback(getAllItems());
    }

    /** Returns a camera event callback which does nothing. */
    public static CameraEventCallbacks createEmptyCallback() {
        return new CameraEventCallbacks();
    }

    @Override
    public MultiValueSet<CameraEventCallback> clone() {
        CameraEventCallbacks ret = createEmptyCallback();
        ret.addAll(getAllItems());
        return ret;
    }

    /**
     * A CameraEventCallback which contains a list of CameraEventCallback and will
     * propagate received callback to the list.
     */
    public static final class ComboCameraEventCallback {
        private final List<CameraEventCallback> mCallbacks = new ArrayList<>();

        ComboCameraEventCallback(List<CameraEventCallback> callbacks) {
            for (CameraEventCallback callback : callbacks) {
                mCallbacks.add(callback);
            }
        }

        /**
         * To Invoke {@link CameraEventCallback#onPresetSession()} on the set of list and
         * aggregated the results to a set list.
         *
         * @return List<CaptureConfig> The request information to customize the session.
         */
        public List<CaptureConfig> onPresetSession() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                CaptureConfig presetCaptureStage = callback.onPresetSession();
                if (presetCaptureStage != null) {
                    ret.add(presetCaptureStage);
                }
            }
            return ret;
        }

        /**
         * To Invoke {@link CameraEventCallback#onEnableSession()} on the set of list and
         * aggregated the results to a set list.
         *
         * @return List<CaptureConfig> The request information to customize the session.
         */
        public List<CaptureConfig> onEnableSession() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                CaptureConfig enableCaptureStage = callback.onEnableSession();
                if (enableCaptureStage != null) {
                    ret.add(enableCaptureStage);
                }
            }
            return ret;
        }

        /**
         * To Invoke {@link CameraEventCallback#onRepeating()} on the set of list and
         * aggregated the results to a set list.
         *
         * @return List<CaptureConfig> The request information to customize the session.
         */
        public List<CaptureConfig> onRepeating() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                CaptureConfig repeatingCaptureStage = callback.onRepeating();
                if (repeatingCaptureStage != null) {
                    ret.add(repeatingCaptureStage);
                }
            }
            return ret;
        }

        /**
         * To Invoke {@link CameraEventCallback#onDisableSession()} on the set of list and
         * aggregated the results to a set list.
         *
         * @return List<CaptureConfig> The request information to customize the session.
         */
        public List<CaptureConfig> onDisableSession() {
            List<CaptureConfig> ret = new LinkedList<>();
            for (CameraEventCallback callback : mCallbacks) {
                CaptureConfig disableCaptureStage = callback.onDisableSession();
                if (disableCaptureStage != null) {
                    ret.add(disableCaptureStage);
                }
            }
            return ret;
        }

        @NonNull
        public List<CameraEventCallback> getCallbacks() {
            return mCallbacks;
        }
    }
}
