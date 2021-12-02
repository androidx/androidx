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

package androidx.car.app.sample.showcase.common.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Simple renderer for the surface templates. */
public final class DefaultRenderer implements Renderer {
    private static final String TAG = "showcase";

    private static final int HORIZONTAL_TEXT_MARGIN = 10;
    private static final int VERTICAL_TEXT_MARGIN_FROM_TOP = 20;
    private static final int VERTICAL_TEXT_MARGIN_FROM_BOTTOM = 10;

    private final Paint mLeftInsetPaint = new Paint();
    private final Paint mRightInsetPaint = new Paint();
    private final Paint mCenterPaint = new Paint();

    public DefaultRenderer() {
        mLeftInsetPaint.setColor(Color.RED);
        mLeftInsetPaint.setAntiAlias(true);
        mLeftInsetPaint.setStyle(Paint.Style.STROKE);

        mRightInsetPaint.setColor(Color.RED);
        mRightInsetPaint.setAntiAlias(true);
        mRightInsetPaint.setStyle(Paint.Style.STROKE);
        mRightInsetPaint.setTextAlign(Paint.Align.RIGHT);

        mCenterPaint.setColor(Color.BLUE);
        mCenterPaint.setAntiAlias(true);
        mCenterPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void enable(@NonNull Runnable onChangeListener) {
        // Don't need to do anything here since renderFrame doesn't require any setup.
    }

    @Override
    public void disable() {
        // Don't need to do anything here since renderFrame doesn't require any setup.
    }

    @Override
    public void renderFrame(@NonNull Canvas canvas, @Nullable Rect visibleArea,
            @Nullable Rect stableArea) {

        // Draw a rectangle showing the inset.
        if (visibleArea != null) {
            if (visibleArea.isEmpty()) {
                // No inset set. The entire area is considered safe to draw.
                visibleArea.set(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
            }

            canvas.drawRect(visibleArea, mLeftInsetPaint);
            canvas.drawLine(
                    visibleArea.left,
                    visibleArea.top,
                    visibleArea.right,
                    visibleArea.bottom,
                    mLeftInsetPaint);
            canvas.drawLine(
                    visibleArea.right,
                    visibleArea.top,
                    visibleArea.left,
                    visibleArea.bottom,
                    mLeftInsetPaint);
            canvas.drawText(
                    "(" + visibleArea.left + " , " + visibleArea.top + ")",
                    visibleArea.left + HORIZONTAL_TEXT_MARGIN,
                    visibleArea.top + VERTICAL_TEXT_MARGIN_FROM_TOP,
                    mLeftInsetPaint);
            canvas.drawText(
                    "(" + visibleArea.right + " , " + visibleArea.bottom + ")",
                    visibleArea.right - HORIZONTAL_TEXT_MARGIN,
                    visibleArea.bottom - VERTICAL_TEXT_MARGIN_FROM_BOTTOM,
                    mRightInsetPaint);
        } else {
            Log.d(TAG, "Visible area not available.");
        }

        if (stableArea != null) {
            // Draw a cross-hairs at the stable center.
            final int lengthPx = 15;
            int centerX = stableArea.centerX();
            int centerY = stableArea.centerY();
            canvas.drawLine(centerX - lengthPx, centerY, centerX + lengthPx, centerY, mCenterPaint);
            canvas.drawLine(centerX, centerY - lengthPx, centerX, centerY + lengthPx, mCenterPaint);
            canvas.drawText(
                    "(" + centerX + ", " + centerY + ")",
                    centerX + HORIZONTAL_TEXT_MARGIN,
                    centerY,
                    mCenterPaint);
        } else {
            Log.d(TAG, "Stable area not available.");
        }
    }
}
