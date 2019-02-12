/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import android.graphics.Rect;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CaptureRequestConfiguration;

/**
 * A interface for executing camera2 capture requests which is required for {@link
 * Camera2CameraControl} to achieve its functionality.
 *
 * <p>{@link Camera} implements this interface so Camera2CameraControl can issue {@link
 * CaptureRequest} for manipulating the camera. Camera2CameraControl can use it to execute single
 * request and re-send the repeating request with updated Control SessionConfiguration. For example,
 * {@link CameraControl#focus(Rect, Rect)} needs to send a single request to trigger AF as well as
 * resend the repeating request with updated focus area.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface Camera2RequestRunner {

    /**
     * Executes a single capture request.
     *
     * <p>CameraControl methods like focus, trigger AF need to send single request.
     */
    void submitSingleRequest(CaptureRequestConfiguration singleRequestConfig);

    /**
     * Re-sends the repeating request which contains the latest settings specified by {@link
     * CameraControl}.
     *
     * <p>CameraControl methods like setCropRegion, zoom, focus need to update repeating request.
     */
    void updateRepeatingRequest();
}
