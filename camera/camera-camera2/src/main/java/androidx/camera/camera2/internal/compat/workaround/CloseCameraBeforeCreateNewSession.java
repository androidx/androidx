/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.quirk.CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk;
import androidx.camera.camera2.internal.compat.quirk.LegacyCameraOutputConfigNullPointerQuirk;
import androidx.camera.core.impl.Quirks;

/**
 * A workaround to determine whether the camera device should be closed before creating a new
 * capture session when a capture session has been opened.
 *
 * @see LegacyCameraOutputConfigNullPointerQuirk
 * @see CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk
 */
public class CloseCameraBeforeCreateNewSession {
    /**
     * Determines whether the camera device should be closed before creating a new capture session.
     */
    public static boolean shouldCloseCamera(@NonNull Quirks quirks) {
        return quirks.contains(LegacyCameraOutputConfigNullPointerQuirk.class) || quirks.contains(
                CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk.class);
    }

    private CloseCameraBeforeCreateNewSession() {}
}
