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

package androidx.wear.watchface.samples.minimal.complications;

import static androidx.wear.watchface.samples.minimal.complications.WatchFaceService.COMPLICATION_BOUNDS;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.RenderParameters.HighlightLayer;
import androidx.wear.watchface.RenderParameters.HighlightedElement;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

class WatchFaceRenderer extends Renderer.CanvasRenderer {

    private static final long UPDATE_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /** Turn this on to debug the highlight layer. */
    private static final boolean HIGHLIGHT_DEBUG = false;

    private final ComplicationSlotsManager mComplicationSlotsManager;
    private final Paint mPaint;
    private final Paint mHighlightPaint;
    private final Paint mHighlightFocusPaint;
    @Px
    private final float mHighlightExtraRadius;
    private final char[] mTime = new char[5];
    private Bitmap mHighlightBitmap;

    WatchFaceRenderer(
            @NonNull Resources resources,
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository userStyleRepository,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager) {
        super(surfaceHolder,
                userStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                UPDATE_DELAY_MILLIS);
        mComplicationSlotsManager = complicationSlotsManager;
        mPaint = new Paint();
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setTextSize(64f);
        mHighlightPaint = new Paint();
        mHighlightPaint.setTextAlign(Align.CENTER);
        mHighlightPaint.setStyle(Style.STROKE);
        mHighlightPaint.setStrokeWidth(resources.getDimension(R.dimen.highlight_stroke_width));
        mHighlightExtraRadius = resources.getDimension(R.dimen.highlight_extra_radius);
        mHighlightFocusPaint = new Paint();
        mHighlightFocusPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Rect rect,
            @NonNull ZonedDateTime zonedDateTime) {
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(rect, mPaint);

        for (ComplicationSlot complication :
                mComplicationSlotsManager.getComplicationSlots().values()) {
            if (complication.isEnabled()) {
                complication.render(canvas, zonedDateTime, getRenderParameters());
            }
        }

        mPaint.setColor(Color.WHITE);
        int hour = zonedDateTime.getHour() % 12;
        int minute = zonedDateTime.getMinute();
        int second = zonedDateTime.getSecond();
        mTime[0] = DIGITS[hour / 10];
        mTime[1] = DIGITS[hour % 10];
        mTime[2] = second % 2 == 0 ? ':' : ' ';
        mTime[3] = DIGITS[minute / 10];
        mTime[4] = DIGITS[minute % 10];
        canvas.drawText(mTime, 0, 5, rect.centerX(), rect.centerY(), mPaint);

        if (HIGHLIGHT_DEBUG) {
            HighlightLayer highlightLayerParams =
                    new HighlightLayer(
                            new HighlightedElement.ComplicationSlot(0), Color.MAGENTA, 0x10000000);
            if (mHighlightBitmap == null
                    || mHighlightBitmap.getWidth() != rect.width()
                    || mHighlightBitmap.getHeight() != rect.height()) {
                mHighlightBitmap =
                        Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
            }
            renderHighlightLayer(
                    new Canvas(mHighlightBitmap), rect, zonedDateTime, highlightLayerParams);
            canvas.drawBitmap(mHighlightBitmap, rect.left, rect.top, mPaint);
        }
    }

    @Override
    public void renderHighlightLayer(
            @NonNull Canvas canvas, @NonNull Rect rect, @NonNull ZonedDateTime zonedDateTime) {
        renderHighlightLayer(canvas, rect, zonedDateTime,
                getRenderParameters().getHighlightLayer());
    }

    private void renderHighlightLayer(
            @NonNull Canvas canvas,
            @NonNull Rect rect,
            @SuppressWarnings("UnusedVariable") @NonNull ZonedDateTime zonedDateTime,
            HighlightLayer params) {
        // There is no style defined, so the only options are rendering the highlight for the
        // only complication or for all complications, which is essentially the same.
        mHighlightPaint.setColor(params.getHighlightTint());
        mHighlightFocusPaint.setColor(params.getBackgroundTint());

        float complicationSlotRadius = COMPLICATION_BOUNDS.width() * rect.width() / 2f;
        float highlightRadius = complicationSlotRadius + mHighlightExtraRadius;
        float centerX = COMPLICATION_BOUNDS.centerX() * rect.width();
        float centerY = COMPLICATION_BOUNDS.centerY() * rect.height();

        canvas.drawColor(params.getBackgroundTint());
        canvas.drawCircle(centerX, centerY, highlightRadius, mHighlightFocusPaint);
        canvas.drawCircle(centerX, centerY, highlightRadius, mHighlightPaint);
    }
}
