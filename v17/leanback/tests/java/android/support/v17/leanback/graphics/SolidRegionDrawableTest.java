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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link SolidRegionDrawable}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SolidRegionDrawableTest {

    @Test
    public void draw_test() {
        int color = Color.RED;
        Rect bounds = new Rect(0, 0, 600, 1000);
        SolidRegionDrawable drawable = new SolidRegionDrawable(color);
        drawable.setBounds(bounds);
        Canvas canvas = Mockito.mock(Canvas.class);
        drawable.draw(canvas);
        ArgumentCaptor<Paint> paint = ArgumentCaptor.forClass(Paint.class);
        verify(canvas).drawRect(Mockito.eq(bounds), paint.capture());
        assertEquals(paint.getValue().getColor(), color);
    }
}
