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
package androidx.wear.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.wear.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests for {@link RoundedDrawable} */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class RoundedDrawableTest {

    @Rule
    public final ActivityTestRule<LayoutTestActivity> mActivityRule = new ActivityTestRule<>(
            LayoutTestActivity.class, true, false);
    private static final int BITMAP_WIDTH = 64;
    private static final int BITMAP_HEIGHT = 32;

    private RoundedDrawable mRoundedDrawable;
    private BitmapDrawable mBitmapDrawable;

    @Mock
    Canvas mMockCanvas;

    @Before
    public void setUp() {
        mMockCanvas = mock(Canvas.class);
        mActivityRule.launchActivity(new Intent().putExtra(LayoutTestActivity
                        .EXTRA_LAYOUT_RESOURCE_ID,
                androidx.wear.test.R.layout.rounded_drawable_layout));
        mRoundedDrawable = new RoundedDrawable();
        mBitmapDrawable =
                new BitmapDrawable(
                        mActivityRule.getActivity().getResources(),
                        Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888));
    }

    @Test
    public void colorFilterIsAppliedCorrectly() {
        ColorFilter cf = new ColorFilter();
        mRoundedDrawable.setColorFilter(cf);
        assertEquals(cf, mRoundedDrawable.mPaint.getColorFilter());
    }

    @Test
    public void alphaIsAppliedCorrectly() {
        int alpha = 128;
        mRoundedDrawable.setAlpha(alpha);
        assertEquals(alpha, mRoundedDrawable.mPaint.getAlpha());
    }

    @Test
    public void radiusIsAppliedCorrectly() {
        int radius = 10;
        Rect bounds = new Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT);
        mRoundedDrawable.setDrawable(mBitmapDrawable);
        mRoundedDrawable.setClipEnabled(true);
        mRoundedDrawable.setRadius(radius);
        mRoundedDrawable.setBounds(bounds);
        mRoundedDrawable.draw(mMockCanvas);
        // One for background and one for the actual drawable, this should be called two times.
        verify(mMockCanvas, times(2))
                .drawRoundRect(
                        eq(new RectF(0, 0, bounds.width(), bounds.height())),
                        eq((float) radius),
                        eq((float) radius),
                        any(Paint.class));
    }

    @Test
    public void scalingIsAppliedCorrectly() {
        int radius = 14;
        // 14 px radius should apply 5 px padding due to formula ceil(radius * 1 - 1 / sqrt(2))
        Rect bounds = new Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT);
        mRoundedDrawable.setDrawable(mBitmapDrawable);
        mRoundedDrawable.setClipEnabled(false);
        mRoundedDrawable.setRadius(radius);
        mRoundedDrawable.setBounds(bounds);
        mRoundedDrawable.draw(mMockCanvas);
        assertEquals(BITMAP_WIDTH - 10, mBitmapDrawable.getBounds().width());
        assertEquals(BITMAP_HEIGHT - 10, mBitmapDrawable.getBounds().height());
        assertEquals(bounds.centerX(), mBitmapDrawable.getBounds().centerX());
        assertEquals(bounds.centerY(), mBitmapDrawable.getBounds().centerY());
        // Background should also be drawn
        verify(mMockCanvas)
                .drawRoundRect(
                        eq(new RectF(0, 0, bounds.width(), bounds.height())),
                        eq((float) radius),
                        eq((float) radius),
                        any(Paint.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void inflate() {
        RoundedDrawable roundedDrawable =
                (RoundedDrawable) mActivityRule.getActivity().getDrawable(
                        R.drawable.rounded_drawable);
        assertEquals(
                mActivityRule.getActivity().getColor(R.color.rounded_drawable_background_color),
                roundedDrawable.getBackgroundColor());
        assertTrue(roundedDrawable.isClipEnabled());
        assertNotNull(roundedDrawable.getDrawable());
        assertEquals(mActivityRule.getActivity().getResources().getDimensionPixelSize(
                R.dimen.rounded_drawable_radius), roundedDrawable.getRadius());
    }
}
