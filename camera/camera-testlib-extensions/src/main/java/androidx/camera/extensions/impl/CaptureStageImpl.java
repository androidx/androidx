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

package androidx.camera.extensions.impl;

import android.hardware.camera2.CaptureRequest;
import android.util.Pair;

import java.util.List;

/**
 * The set of parameters that defines a single capture that will be sent to the camera.
 *
 * @since 1.0
 */
public interface CaptureStageImpl {
    /** Returns the identifier for the {@link CaptureStageImpl}. */
    int getId();

    /**
     * Returns the set of {@link CaptureRequest.Key} and the corresponding values that will be
     * set for a single {@link CaptureRequest}.
     */
    List<Pair<CaptureRequest.Key, Object>> getParameters();
}
