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

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/227469801, b/274738266
 *     Description: Quirk indicates Preview is stretched when VideoCapture is bound.
 *     Device(s): Samsung J3, Samsung J5, Samsung J7, Samsung J1 Ace neo and Oppo A37F
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class PreviewStretchWhenVideoCaptureIsBoundQuirk implements Quirk {

    static boolean load() {
        return isHuaweiP8Lite() || isSamsungJ3() || isSamsungJ7() || isSamsungJ1AceNeo()
                || isOppoA37F() || isSamsungJ5();
    }

    private static boolean isHuaweiP8Lite() {
        return "HUAWEI".equalsIgnoreCase(Build.MANUFACTURER)
                && "HUAWEI ALE-L04".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ3() {
        return "Samsung".equalsIgnoreCase(Build.MANUFACTURER)
                && "sm-j320f".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ5() {
        return "Samsung".equalsIgnoreCase(Build.MANUFACTURER)
                && "sm-j510fn".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ7() {
        return "Samsung".equalsIgnoreCase(Build.MANUFACTURER)
                && "sm-j700f".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ1AceNeo() {
        return "Samsung".equalsIgnoreCase(Build.MANUFACTURER)
                && "sm-j111f".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isOppoA37F() {
        return "OPPO".equalsIgnoreCase(Build.MANUFACTURER) && "A37F".equalsIgnoreCase(Build.MODEL);
    }
}
