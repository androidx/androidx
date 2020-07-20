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

package androidx.camera.integration.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.NonNull;

/**
 * Allows to run a blur script on a bitmap using {@link RenderScript}. After testing on a
 * couple of devices (both low end and high end), it seems as though blurring an image typically
 * takes a couple of frames, about 3-4.
 */
class BlurBitmap {

    private static final int BLUR_RADIUS = 25;
    private final RenderScript mRenderScript;
    private final ScriptIntrinsicBlur mBlurScript;

    BlurBitmap(@NonNull Context context) {
        // Create renderScript
        mRenderScript = RenderScript.create(context);

        // Create blur script
        mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
        mBlurScript.setRadius(BLUR_RADIUS);
    }

    void blur(@NonNull Bitmap bitmap) {
        // Create input and output allocation
        final Allocation allocation = Allocation.createFromBitmap(mRenderScript, bitmap);

        // Perform blur effect, then copy script result to bitmap
        mBlurScript.setInput(allocation);
        mBlurScript.forEach(allocation);
        allocation.copyTo(bitmap);

        // clean up
        allocation.destroy();
    }

    void clear() {
        mBlurScript.destroy();
        mRenderScript.destroy();
    }
}
