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
 * A quirk to denote the devices cannot use the {@link android.provider.MediaStore.Video} to
 * create {@link androidx.camera.video.MediaStoreOutputOptions} for video recording.
 *
 * <p>QuirkSummary
 *     Bug Id: 223576109
 *     Description: Devices cannot successfully open the output stream and file descriptor for
 *                  {@link android.provider.MediaStore.Video}. Using
 *                  {@link androidx.camera.video.FileOutputOptions} or storing the file to a
 *                  different position can workaround this issue.
 *     Device(s): Twist 2 Pro and Itel w6004
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MediaStoreVideoCannotWrite implements Quirk {

    public static boolean isPositivoTwist2Pro() {
        return "positivo".equalsIgnoreCase(Build.BRAND) && "twist 2 pro".equalsIgnoreCase(
                Build.MODEL);
    }

    public static boolean isItelW6004() {
        return "itel".equalsIgnoreCase(Build.BRAND) && "itel w6004".equalsIgnoreCase(Build.MODEL);
    }

    static boolean load() {
        return isPositivoTwist2Pro() || isItelW6004();
    }

}
