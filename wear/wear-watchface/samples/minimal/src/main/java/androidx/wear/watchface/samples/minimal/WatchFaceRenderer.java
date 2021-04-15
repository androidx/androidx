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

package androidx.wear.watchface.samples.minimal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.icu.util.Calendar;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Minimal rendered for the watch face, using canvas to render hours, minutes, and a blinking
 * separator.
 */
public class WatchFaceRenderer extends Renderer.CanvasRenderer {

    private static final long UPDATE_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    private final WatchState mWatchState;
    private final Paint mPaint;
    private final char[] mTimeText = new char[5];

    public WatchFaceRenderer(
            @NotNull SurfaceHolder surfaceHolder,
            @NotNull CurrentUserStyleRepository currentUserStyleRepository,
            @NotNull WatchState watchState) {
        super(surfaceHolder, currentUserStyleRepository, watchState, CanvasType.HARDWARE,
                UPDATE_DELAY_MILLIS);
        mWatchState = watchState;
        mPaint = new Paint();
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setTextSize(64f);
    }

    @Override
    public void render(@NotNull Canvas canvas, @NotNull Rect rect, @NotNull Calendar calendar) {
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(rect, mPaint);
        mPaint.setColor(Color.WHITE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        mTimeText[0] = DIGITS[hour / 10];
        mTimeText[1] = DIGITS[hour % 10];
        mTimeText[2] = second % 2 == 0 ? ':' : ' ';
        mTimeText[3] = DIGITS[minute / 10];
        mTimeText[4] = DIGITS[minute % 10];
        canvas.drawText(mTimeText,
                0,
                5,
                rect.centerX(),
                rect.centerY() - mWatchState.getChinHeight(),
                mPaint);
    }

    @Override
    public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect bounds,
            @NonNull Calendar calendar) {
        canvas.drawColor(getRenderParameters().getHighlightLayer().getBackgroundTint());
    }
}
