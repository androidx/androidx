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

package androidx.camera.video.internal.encoder;

import android.media.MediaFormat;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * The output config of an encoder.
 *
 * <p>The output config will be the final configuration of an {@link Encoder} relative to the
 * input config {@link EncoderConfig}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface OutputConfig {

    /** Gets the media format. */
    @Nullable
    MediaFormat getMediaFormat();
}
