/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoOutput;

/**
 * Quirk denotes that the encoder surface needs to be removed from repeating request before stop
 * the encoder.
 *
 * <p>As described in b/196039619, the camera fails if {@link MediaCodec} is stopped while frames
 * are still being provided.
 *
 * <p>In {@link Recorder}, {@link Recorder#getStreamState()} has to returns
 * {@link VideoOutput.StreamState#INACTIVE} when the active recording is being stopped.
 * {@link VideoCapture} monitors {@link VideoOutput#getStreamState()} and
 * detach the surface from camera if it becomes {@link VideoOutput.StreamState#INACTIVE inactive}.
 * So making the {@link Recorder} inactive in stopping state will stop the camera from producing
 * frames to the {@link MediaCodec} before actually stopping it.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class DeactivateEncoderSurfaceBeforeStopEncoderQuirk implements Quirk {

    static boolean load() {
        return Build.VERSION.SDK_INT <= 22;
    }
}
