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

package androidx.pdf.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A {@link Drawable} overlay that highlights given list of {@link DrawSpec}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class HighlightOverlay extends Drawable {
    protected final DrawSpec[] mDrawSpecs;

    protected HighlightOverlay(@NonNull DrawSpec... drawSpecs) {
        this.mDrawSpecs = drawSpecs;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        for (DrawSpec drawSpec : mDrawSpecs) {
            drawSpec.draw(canvas);
        }
    }

    // These methods are required to be a Drawable, but we don't use them.
    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void setAlpha(int alpha) {
        throw new UnsupportedOperationException("setAlpha");
    }

    @Override
    public void setColorFilter(@NonNull ColorFilter cf) {
        throw new UnsupportedOperationException("setColorFilter");
    }
}
