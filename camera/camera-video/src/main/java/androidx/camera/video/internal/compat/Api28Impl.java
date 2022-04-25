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

package androidx.camera.video.internal.compat;

import android.media.MediaCodecInfo;
import android.util.Range;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper class to avoid verification errors for methods introduced in Android 9 (API 28).
 */
@RequiresApi(28)
public final class Api28Impl {

    private Api28Impl() {
    }

    /**
     * Returns the supported range of quality values for
     * {@link MediaCodecInfo.EncoderCapabilities}.
     */
    @NonNull
    @DoNotInline
    public static Range<Integer> getQualityRange(@NonNull MediaCodecInfo.EncoderCapabilities caps) {
        return caps.getQualityRange();
    }
}
