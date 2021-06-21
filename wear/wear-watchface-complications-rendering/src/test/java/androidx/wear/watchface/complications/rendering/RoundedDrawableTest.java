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

package androidx.wear.watchface.complications.rendering;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link RoundedDrawable} */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class RoundedDrawableTest {

    private static final int BITMAP_WIDTH = 64;
    private static final int BITMAP_HEIGHT = 64;

    private RoundedDrawable mRoundedDrawable;
    private BitmapDrawable mBitmapDrawable;

    @Mock Canvas mMockCanvas;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mRoundedDrawable = new RoundedDrawable();
        mBitmapDrawable =
                new BitmapDrawable(
                        ApplicationProvider.getApplicationContext().getResources(),
                        Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void colorFilterIsAppliedCorrectly() {
        ColorFilter cf = new ColorFilter();
        mRoundedDrawable.setColorFilter(cf);
        assertThat(mRoundedDrawable.mPaint.getColorFilter()).isEqualTo(cf);
    }

    @Test
    public void alphaIsAppliedCorrectly() {
        int alpha = 128;
        mRoundedDrawable.setAlpha(alpha);
        assertThat(mRoundedDrawable.mPaint.getAlpha()).isEqualTo(alpha);
    }

    @Test
    public void radiusIsAppliedCorrectly() {
        int radius = 10;
        Rect bounds = new Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT);
        mRoundedDrawable.setDrawable(mBitmapDrawable);
        mRoundedDrawable.setRadius(10);
        mRoundedDrawable.setBounds(bounds);
        mRoundedDrawable.draw(mMockCanvas);
        verify(mMockCanvas)
                .drawRoundRect(
                        eq(new RectF(0, 0, bounds.width(), bounds.height())),
                        eq((float) radius),
                        eq((float) radius),
                        any(Paint.class));
    }
}
