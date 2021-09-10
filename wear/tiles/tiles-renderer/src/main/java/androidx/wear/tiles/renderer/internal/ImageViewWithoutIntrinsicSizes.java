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

package androidx.wear.tiles.renderer.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Fixed version of ImageView which doesn't ever use the intrinsic size of its drawables.
 *
 * <p>Tiles has a rule that the size of the layout should be statically resolvable. Because it can
 * asynchronously load the resources though, this is not possible if we ever use the intrinsic sizes
 * of images, as the layout may resize itself after an image is loaded. Take the following example:
 *
 * <p>Box (size = wrap()) { Text("Hello World") Image(size = expand()) }
 *
 * <p>The Box will size itself to wrap the contents, which it does by asking each child how large it
 * wishes to be. For Text, this is the size of the text run (ish, it gets a little more complex with
 * multiple lines), and for images, this is the intrinsic size of the drawable (even in the case
 * where the image is MATCH_PARENT; that gets applied later). This means that the layout can "jump"
 * after the image is loaded, if the image's intrinsic size is larger than the text.
 *
 * <p>This wrapper prevents that; if the image ever gets a MeasureSpec which allows it to pick its
 * own size, we clamp the max size to 0 to prevent it from ever doing that. This is safe within
 * Tiles; images only support absolute sizes (in which case, it has an exact measurespec), ratio
 * sizes (which is handled in RatioViewWrapper), and expand sizes, in which case this image gets
 * ignored for the first measure pass, and will receive an exact measurespec on the second measure
 * pass.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("AppCompatCustomView")
class ImageViewWithoutIntrinsicSizes extends ImageView {
    ImageViewWithoutIntrinsicSizes(Context context) {
        super(context);
    }

    ImageViewWithoutIntrinsicSizes(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    ImageViewWithoutIntrinsicSizes(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    ImageViewWithoutIntrinsicSizes(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Helpfully, half of ImageView that is needed in Measure (resolveUri) is private. We can
        // still hack this though. If we ever get an AT_MOST measurespec, then we _don't_ want to
        // use our intrinsic dimensions. Just measure that as AT_MOST = 0.

        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(1, MeasureSpec.AT_MOST);
        }

        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(1, MeasureSpec.AT_MOST);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
