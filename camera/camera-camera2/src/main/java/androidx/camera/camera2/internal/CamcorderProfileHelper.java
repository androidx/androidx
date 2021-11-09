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

package androidx.camera.camera2.internal;

import android.media.CamcorderProfile;

import androidx.annotation.RequiresApi;

/**
 * This is helper class to use {@link android.media.CamcorderProfile} that may be mocked.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
interface CamcorderProfileHelper {
    /** Returns true if the camcorder profile exists for the given camera and quality. */
    boolean hasProfile(int cameraId, int quality);

    /** Returns the camcorder profile for the given camera at the given quality level. */
    CamcorderProfile get(int cameraId, int quality);
}
