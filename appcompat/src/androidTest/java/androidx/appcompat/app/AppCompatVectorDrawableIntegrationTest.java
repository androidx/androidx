/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.appcompat.app;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatVectorDrawableIntegrationTest {
    @Rule
    public final ActivityTestRule<AppCompatVectorDrawableIntegrationActivity> mActivityTestRule;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static final int LEFT_CENTER_X = WIDTH / 4;
    private static final int RIGHT_CENTER_X = WIDTH * 3 / 4;
    private static final int CENTER_Y = HEIGHT / 2;

    public AppCompatVectorDrawableIntegrationTest() {
        mActivityTestRule =
                new ActivityTestRule<>(AppCompatVectorDrawableIntegrationActivity.class);
    }

    @Before
    public void setup() {
        mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Test
    @UiThreadTest
    public void testVectorDrawableAutoMirrored() {
        Activity activity = mActivityTestRule.getActivity();
        ImageView view1 = (ImageView) activity.findViewById(R.id.view_vector_1);
        Drawable vectorDrawable = view1.getDrawable();
        // We update the bounds here for writing into the bitmap correctly.
        vectorDrawable.setBounds(0, 0, WIDTH, HEIGHT);
        vectorDrawable.draw(mCanvas);

        int leftColor = mBitmap.getPixel(LEFT_CENTER_X, CENTER_Y);
        int rightColor = mBitmap.getPixel(RIGHT_CENTER_X, CENTER_Y);

        // Gingerbread seems treating the alpha differently, so checking red channel only here.
        // It is enough to tell the difference.
        assertEquals("Left side should be white", Color.red(leftColor), 255);
        assertEquals("Right side should be black", Color.red(rightColor), 0);

        if (Build.VERSION.SDK_INT >= 19) {
            // setLayoutDirection is only available after API 17. However, it correctly set its
            // drawable's layout direction until API 19.
            view1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            vectorDrawable.draw(mCanvas);

            leftColor = mBitmap.getPixel(LEFT_CENTER_X, CENTER_Y);
            rightColor = mBitmap.getPixel(RIGHT_CENTER_X, CENTER_Y);

            assertEquals("Left side should be black", Color.red(leftColor), 0);
            assertEquals("Right side should be white", Color.red(rightColor), 255);
        }
    }
}
