/*
 * Copyright (C) 2014 The Android Open Source Project
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
package androidx.slice;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

 /**
 * A wrapper for Drawables that uses a path to add mask for corners around the drawable,
 * to match the radius of the underlying shape.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CornerDrawable extends InsetDrawable {
    private float mCornerRadius;
    private final Path mPath = new Path();

    public CornerDrawable(@Nullable Drawable drawable, float cornerRadius) {
        super(drawable, 0);
        mCornerRadius = cornerRadius;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int saveCount = canvas.save();
        canvas.clipPath(mPath);
        super.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onBoundsChange(@Nullable Rect r) {
        if (mPath != null) {
            mPath.reset();
            mPath.addRoundRect(new RectF(r), mCornerRadius, mCornerRadius, Path.Direction.CW);
        }
        super.onBoundsChange(r);
    }
}
