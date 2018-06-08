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

package androidx.vectordrawable.graphics.drawable.tests;

import static androidx.vectordrawable.graphics.drawable.tests.DrawableUtils
        .saveVectorDrawableIntoPNG;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;

import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.test.R;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@MediumTest
@RunWith(Parameterized.class)
public class PathInterpolatorParameterizedTest {
    @Rule
    public ActivityTestRule<DrawableStubActivity> mActivityRule =
            new ActivityTestRule<>(DrawableStubActivity.class);

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    private Activity mActivity = null;
    private int mResId;

    private static final boolean DBG_DUMP_PNG = false;

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[] {
                R.drawable.animation_path_interpolator_1,
                R.drawable.animation_path_interpolator_2,
        };
    }

    public PathInterpolatorParameterizedTest(final int resId) throws Throwable {
        mResId = resId;
    }

    @Before
    public void setup() {
        mActivity = mActivityRule.getActivity();
    }

    @Test
    public void testPathMorphing() throws Exception {
        final Object lock = new Object();
        final Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_WIDTH,
                Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bitmap);

        final AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mActivity,
                mResId);
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        bitmap.eraseColor(0);
        avd.draw(c);
        int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2);
        Assert.assertTrue(centerColor == 0);

        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPNG(mActivity.getResources(), bitmap, mResId, "start");
        }

        avd.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationStart(Drawable drawable) {
                // Nothing to do.
            }

            @Override
            public void onAnimationEnd(Drawable drawable) {
                bitmap.eraseColor(0);
                drawable.draw(c);
                int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2);
                Assert.assertTrue(centerColor == 0xffff0000);

                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                avd.start();
            }
        });

        synchronized (lock) {
            lock.wait(1000);
        }

        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPNG(mActivity.getResources(), bitmap, mResId, "end");
        }
    }
}
