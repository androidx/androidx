/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link FitWidthBitmapDrawable}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FitWidthBitmapDrawableTest {
    private final static int SCREEN_WIDTH = 1600;
    private final static int SCREEN_HEIGHT = 1080;
    private final static int WIDTH = 300;
    private final static int HEIGHT = 600;
    private Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);

    @Test
    public void draw_withOffset() {
        int offset = 600;
        FitWidthBitmapDrawable drawable = new FitWidthBitmapDrawable();
        drawable.setBitmap(bitmap);
        drawable.setVerticalOffset(offset);
        Rect bounds = new Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        drawable.setBounds(bounds);

        Canvas canvas = Mockito.mock(Canvas.class);
        drawable.draw(canvas);

        Rect expectedBounds = bounds;
        verify(canvas).clipRect(expectedBounds);

        Rect bitmapBounds = new Rect(0, 0, WIDTH, HEIGHT);
        int nH = (int) (((float)SCREEN_WIDTH/WIDTH * HEIGHT) + offset);
        Rect expectedDest = new Rect(0, offset, SCREEN_WIDTH, nH);
        verify(canvas).drawBitmap(eq(bitmap), eq(bitmapBounds), eq(expectedDest), any(Paint.class));
    }
}
