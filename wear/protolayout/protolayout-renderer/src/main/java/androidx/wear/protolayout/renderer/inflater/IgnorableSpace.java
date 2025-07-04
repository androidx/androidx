/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.wear.protolayout.renderer.inflater;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import org.jspecify.annotations.NonNull;

/**
 * Space is a lightweight View subclass that may be used to create gaps between components in
 * general purpose layouts.
 */
public final class IgnorableSpace extends View {
    /** {@inheritDoc} */
    public IgnorableSpace(@NonNull Context context) {
        super(context, /* attrs= */ null, /* defStyleAttr= */ 0, /* defStyleRes= */ 0);
        if (getVisibility() == VISIBLE) {
            setVisibility(INVISIBLE);
        }
    }

    /**
     * Draw nothing.
     *
     * @param canvas an unused parameter.
     */
    @SuppressWarnings("MissingSuperCall") // android.widget.Space doesn't call it
    @Override
    public void draw(@NonNull Canvas canvas) {}

    /**
     * Compare to: {@link View#getDefaultSize(int, int)} If mode is AT_MOST, return the child size
     * instead of the parent size (unless it is too big).
     */
    private static int getDefaultSize2(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getDefaultSize2(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize2(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
}
