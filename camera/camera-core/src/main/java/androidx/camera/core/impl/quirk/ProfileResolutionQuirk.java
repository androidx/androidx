/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.impl.quirk;

import android.media.EncoderProfiles;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.List;

/**
 * A Quirk interface which denotes that CameraX should validate video resolutions returned from
 * {@link EncoderProfiles} instead of using them directly.
 *
 * <p>Subclasses of this quirk should provide a list of supported resolutions for CameraX to
 * verify.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ProfileResolutionQuirk extends Quirk {

    /** Returns a list of supported resolutions. */
    @NonNull
    List<Size> getSupportedResolutions();
}
