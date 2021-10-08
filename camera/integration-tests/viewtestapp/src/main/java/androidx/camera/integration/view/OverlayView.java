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

package androidx.camera.integration.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A overlay view for drawing a tile with {@link Canvas}.
 */
public final class OverlayView extends FrameLayout {

    private RectF mTile;
    private final Paint mPaint = new Paint();

    public OverlayView(@NonNull Context context) {
        super(context);
    }


    public OverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    void setTileRect(RectF tile) {
        mTile = tile;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mTile == null) {
            return;
        }

        // The tile paint is black stroke with a white glow so it's always visible regardless of
        // the background.
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(10);
        canvas.drawRect(mTile, mPaint);

        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(5);
        canvas.drawRect(mTile, mPaint);
    }
}
