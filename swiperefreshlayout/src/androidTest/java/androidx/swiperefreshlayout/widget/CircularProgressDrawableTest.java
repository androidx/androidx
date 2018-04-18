/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.swiperefreshlayout.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 * Tests for CircularProgressDrawable
 */
@RunWith(AndroidJUnit4.class)
public class CircularProgressDrawableTest {
    @Rule
    public final ActivityTestRule<CircularProgressDrawableActivity> mActivityTestRule =
            new ActivityTestRule<>(CircularProgressDrawableActivity.class);

    private CircularProgressDrawable mDrawableUnderTest;

    @Mock
    Canvas mMockCanvas;

    @Before
    public void setUp() {
        Context context = mActivityTestRule.getActivity().getApplicationContext();
        mMockCanvas = mock(Canvas.class);
        mDrawableUnderTest = new CircularProgressDrawable(context);
    }

    @Test
    @SmallTest
    public void sizeIsSquareBasedOnSmallerEdgeWithNoCenterRadius() {
        int width = 100;
        int height = 50;
        mDrawableUnderTest.setBounds(new Rect(0, 0, width, height));
        mDrawableUnderTest.draw(mMockCanvas);

        ArgumentCaptor<RectF> captor = ArgumentCaptor.forClass(RectF.class);
        verify(mMockCanvas).drawArc(captor.capture(), anyFloat(), anyFloat(), anyBoolean(),
                any(Paint.class));

        assertTrue(captor.getValue().width() == captor.getValue().height());
        assertTrue(captor.getValue().width() <= width);
        assertTrue(captor.getValue().width() <= height);
    }

    @Test
    @SmallTest
    public void setCenterRadiusFixesSize() {
        float radius = 10f;
        float strokeWidth = 4f;
        mDrawableUnderTest.setCenterRadius(radius);
        mDrawableUnderTest.setStrokeWidth(strokeWidth);
        mDrawableUnderTest.setBounds(new Rect(0, 0, 100, 50));
        mDrawableUnderTest.draw(mMockCanvas);

        ArgumentCaptor<RectF> boundsCaptor = ArgumentCaptor.forClass(RectF.class);
        verify(mMockCanvas).drawArc(boundsCaptor.capture(), anyFloat(), anyFloat(), anyBoolean(),
                any(Paint.class));

        assertEquals((radius + strokeWidth / 2f) * 2, boundsCaptor.getValue().width(), 0.5);
        assertEquals((radius + strokeWidth / 2f) * 2, boundsCaptor.getValue().height(), 0.5);
    }
}
