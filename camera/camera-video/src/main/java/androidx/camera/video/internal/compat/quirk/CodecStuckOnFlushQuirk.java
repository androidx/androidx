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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/306557279
 *     Description: Quirk denotes that the codec gets stuck when the {@link MediaCodec#flush()}
 *                  method is called. The issue only occurs when the mime type is "video/mp4v-es";
 *                  "video/avc" works fine. This could be a timing issue in the codec
 *                  implementation that arises after continuously calling the
 *                  {@link MediaCodec#signalEndOfInputStream()} and {@link MediaCodec#flush()}
 *                  methods. One workaround is to call the {@link MediaCodec#flush()} method
 *                  until the end-of-stream output buffer has been received. This can
 *                  defer the timing to call {@link MediaCodec#flush()} and can avoid
 *                  the issue by experimental results.
 *     Device(s): Nokia 1
 */
@RequiresApi(21)
public class CodecStuckOnFlushQuirk implements Quirk {

    static boolean load() {
        return isNokia1();
    }

    private static boolean isNokia1() {
        return "Nokia".equalsIgnoreCase(Build.BRAND) && "Nokia 1".equalsIgnoreCase(Build.MODEL);
    }

    /** Checks if the mime type is relevant for this quirk. */
    public boolean isProblematicMimeType(@Nullable String mimeType) {
        return MediaFormat.MIMETYPE_VIDEO_MPEG4.equals(mimeType);
    }
}
