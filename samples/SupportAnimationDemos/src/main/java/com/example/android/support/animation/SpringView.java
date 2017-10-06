/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.support.animation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * The view that draws the spring as it reacts (i.e. expands/compresses) to the user touch.
 */
public class SpringView extends View {
    final Paint mPaint = new Paint();
    private float mLastHeight = 175;

    public SpringView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mPaint.setColor(context.getResources().getColor(R.color.springColor));
        mPaint.setStrokeWidth(10);
    }

    /**
     * Sets the other end of the spring.
     *
     * @param height height of the mass, which is used to derive how to draw the spring
     */
    public void setMassHeight(float height) {
        mLastHeight = height;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Draws the spring
        // 30px long, 15 sections
        int num = 20;
        float sectionLen = 150; // px
        final float x = canvas.getWidth() / 2;
        float y = 0;
        float sectionHeight = mLastHeight / num;
        float sectionWidth = (float) Math.sqrt(sectionLen * sectionLen
                - sectionHeight * sectionHeight);
        canvas.drawLine(x, 0, x + sectionWidth / 2, sectionHeight / 2, mPaint);
        float lastX = x + sectionWidth / 2;
        float lastY = sectionHeight / 2;
        for (int i = 1; i < num; i++) {
            canvas.drawLine(lastX, lastY, 2 * x - lastX, lastY + sectionHeight, mPaint);
            lastX = 2 * x - lastX;
            lastY = lastY + sectionHeight;
        }
        canvas.drawLine(lastX, lastY, x, mLastHeight, mPaint);
    }
}
