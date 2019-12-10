/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.view;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;

import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for {@link TransformableSurfaceView}.
 */
@LargeTest
@Suppress
@RunWith(AndroidJUnit4.class)
public class TransformableSurfaceViewTest {

    private static final int ANY_WIDTH = 160;
    private static final int ANY_HEIGHT = 90;

    @Rule
    public ActivityTestRule<FakeActivity> mActivityTestRule =
            new ActivityTestRule<>(FakeActivity.class);

    private Context mContext;

    @Before
    public void setUp() {
        CoreAppTestUtil.assumeCompatibleDevice();
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    @UiThreadTest
    public void translateTransformation() throws Throwable {
        final int translateX = 50;
        final int translateY = 80;
        final Matrix matrix = new Matrix();
        matrix.setTranslate(translateX, translateY);

        transformSurfaceView(matrix, ANY_WIDTH, ANY_HEIGHT, translateX, translateY);
    }

    @Test
    @UiThreadTest
    public void scaleFromTopLeftTransformation() throws Throwable {
        final int scaleX = 2;
        final int scaleY = 5;
        final Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, 0, 0);

        final int expectedWidth = scaleX * ANY_WIDTH;
        final int expectedHeight = scaleY * ANY_HEIGHT;
        final int expectedX = 0;
        final int expectedY = 0;

        transformSurfaceView(matrix, expectedWidth, expectedHeight, expectedX, expectedY);
    }

    @Test
    @UiThreadTest
    public void scaleFromCenterTransformation() throws Throwable {
        final int scaleX = 2;
        final int scaleY = 5;
        final float centerX = ANY_WIDTH / 2f;
        final float centerY = ANY_HEIGHT / 2f;
        final Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, centerX, centerY);

        final int expectedWidth = scaleX * ANY_WIDTH;
        final int expectedHeight = scaleY * ANY_HEIGHT;
        final int expectedX = (int) (centerX - scaleX * ANY_WIDTH / 2f);
        final int expectedY = (int) (centerY - scaleY * ANY_HEIGHT / 2f);

        transformSurfaceView(matrix, expectedWidth, expectedHeight, expectedX, expectedY);
    }

    @Test
    @UiThreadTest
    public void scaleFromTopLeftAndTranslateTransformation() throws Throwable {
        final int scaleX = 2;
        final int scaleY = 5;
        final int translateX = 50;
        final int translateY = 80;
        final Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, 0, 0);
        matrix.postTranslate(translateX, translateY);

        final int expectedWidth = scaleX * ANY_WIDTH;
        final int expectedHeight = scaleY * ANY_HEIGHT;

        transformSurfaceView(matrix, expectedWidth, expectedHeight, translateX, translateY);
    }

    @Test
    @UiThreadTest
    public void scaleFromCenterAndTranslateTransformation() throws Throwable {
        final int scaleX = 2;
        final int scaleY = 5;
        final int translateX = 50;
        final int translateY = 80;
        final float centerX = ANY_WIDTH / 2f;
        final float centerY = ANY_HEIGHT / 2f;
        final Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, centerX, centerY);
        matrix.postTranslate(translateX, translateY);

        final int expectedWidth = scaleX * ANY_WIDTH;
        final int expectedHeight = scaleY * ANY_HEIGHT;
        final int expectedX = (int) (translateX + centerX - scaleX * ANY_WIDTH / 2f);
        final int expectedY = (int) (translateY + centerY - scaleY * ANY_HEIGHT / 2f);

        transformSurfaceView(matrix, expectedWidth, expectedHeight, expectedX, expectedY);
    }

    @Test(expected = IllegalArgumentException.class)
    @UiThreadTest
    public void ignoreRotationTransformation() throws Throwable {
        final Matrix matrix = new Matrix();
        matrix.setRotate(-45);

        transformSurfaceView(matrix, ANY_WIDTH, ANY_HEIGHT, 0, 0);
    }

    private void transformSurfaceView(final Matrix matrix, final int expectedWidth,
            final int expectedHeight, final int expectedX, final int expectedY) throws Throwable {
        final TransformableSurfaceView surfaceView = new TransformableSurfaceView(mContext);
        surfaceView.layout(0, 0, ANY_WIDTH, ANY_HEIGHT);

        final Activity activity = mActivityTestRule.getActivity();
        mActivityTestRule.runOnUiThread(() -> activity.setContentView(surfaceView));

        surfaceView.setTransform(matrix);

        surfaceView.post(() -> {
            assertEquals(expectedWidth, surfaceView.getWidth());
            assertEquals(expectedHeight, surfaceView.getHeight());
            assertEquals(expectedX, Math.round(surfaceView.getX()));
            assertEquals(expectedY, Math.round(surfaceView.getY()));
        });
    }
}
