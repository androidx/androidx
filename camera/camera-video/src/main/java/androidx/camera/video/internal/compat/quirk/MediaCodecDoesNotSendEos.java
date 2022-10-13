/*
 * Copyright 2022 The Android Open Source Project
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

/**
 * <p>QuirkSummary
 * Bug Id: b/248189542
 * Description: When recording video with effect pipeline enabled, calling
 *              {@link MediaCodec#signalEndOfInputStream()} doesn't trigger an EOS buffer to
 *              {@link MediaCodec.Callback}.
 * Device(s): twist 2 pro.
 */
@RequiresApi(21)
public class MediaCodecDoesNotSendEos implements Quirk {

    public static boolean isPositivoTwist2Pro() {
        return "positivo".equalsIgnoreCase(Build.BRAND) && "twist 2 pro".equalsIgnoreCase(
                Build.MODEL);
    }

    static boolean load() {
        return isPositivoTwist2Pro();
    }
}
