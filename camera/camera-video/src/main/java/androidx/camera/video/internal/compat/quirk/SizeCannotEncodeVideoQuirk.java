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

import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.camera.core.impl.utils.TransformUtils.rotateSize;

import static java.util.Collections.singletonList;

import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.Quirk;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 318036483
 *     Description: On MotoC, resolution 720x1280 will crash the media server while encoding. The
 *                  workaround is to adjust the resolution according to encoder's alignment.
 *                  E.g. the workaround will adjust 720x1280 to 720x1264 when the height
 *                  alignment is 16.
 *     Device(s): MotoC
 */
public class SizeCannotEncodeVideoQuirk implements Quirk {

    static boolean load() {
        return isMotoC();
    }

    private static boolean isMotoC() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto c".equalsIgnoreCase(Build.MODEL);
    }

    @NonNull
    private static Set<Size> getProblematicSizes() {
        if (isMotoC()) {
            return new HashSet<>(singletonList(new Size(720, 1280)));
        }
        return Collections.emptySet();
    }

    /** Checks if the size is the problematic size for encoding. */
    public boolean isProblematicEncodeSize(@NonNull Size size) {
        return getProblematicSizes().contains(size);
    }

    /**
     * Adjusts the input crop rect if its size is the problematic size for encoding. Otherwise
     * returns the original crop rect.
     *
     * @param cropRectWithoutRotation the input crop rect without rotation involved.
     * @param rotationDegrees         the rotation degrees that should apply to the input crop rect.
     * @param videoEncoderInfo        the video encoder info.
     * @return the adjusted crop rect.
     */
    @NonNull
    public Rect adjustCropRectForProblematicEncodeSize(@NonNull Rect cropRectWithoutRotation,
            int rotationDegrees, @Nullable VideoEncoderInfo videoEncoderInfo) {
        Size sizeToEncode = rotateSize(rectToSize(cropRectWithoutRotation), rotationDegrees);
        if (!isProblematicEncodeSize(sizeToEncode)) {
            return cropRectWithoutRotation;
        }
        int halfAlignment =
                videoEncoderInfo != null ? videoEncoderInfo.getHeightAlignment() / 2 : 8;
        Rect rectToAdjust = new Rect(cropRectWithoutRotation);
        // Adjust the rect from the center of the height side and keep the same orientation.
        // E.g. On MotoC, l:0,t:0,r:1280,b:720 -> l:8,t:0,r:1272,b:720
        if (cropRectWithoutRotation.width() == sizeToEncode.getHeight()) {
            rectToAdjust.left += halfAlignment;
            rectToAdjust.right -= halfAlignment;
        } else {
            rectToAdjust.top += halfAlignment;
            rectToAdjust.bottom -= halfAlignment;
        }
        return rectToAdjust;
    }
}
