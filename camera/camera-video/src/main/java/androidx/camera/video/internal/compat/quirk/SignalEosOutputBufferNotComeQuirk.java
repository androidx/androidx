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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/317366465
 *     Description: Quirk denotes that the MediaCodec doesn't send an end of stream buffer callback
 *                  after {@link MediaCodec#signalEndOfInputStream()} is called.
 *                  <p>On Nokia 1, it happens when the camera repeating is stopped while recording.
 *                  E.g. lifecycle is stopped or VideoCapture is unbound while recording. It is
 *                  not 100% reproducible.
 *     Device(s): Nokia 1
 */
@RequiresApi(21)
public class SignalEosOutputBufferNotComeQuirk implements Quirk {

    static boolean load() {
        return isNokia1();
    }

    private static boolean isNokia1() {
        return "Nokia".equalsIgnoreCase(Build.BRAND) && "Nokia 1".equalsIgnoreCase(Build.MODEL);
    }
}

