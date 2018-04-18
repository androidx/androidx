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

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;

import androidx.vectordrawable.graphics.drawable.Animatable2Compat.AnimationCallback;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@MediumTest
@RunWith(Parameterized.class)
public class AnimatedVectorDrawableParameterizedTest {
    @Rule public final ActivityTestRule<DrawableStubActivity> mActivityTestRule =
            new ActivityTestRule<>(DrawableStubActivity.class);;

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;
    private static final boolean DBG_DUMP_PNG = false;

    private Context mContext;
    private Resources mResources;
    private int mResId;
    private int mStartExpected;
    private int mEndExpected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { R.drawable.animation_path_morphing_rect, 0xffff0000, 0x0},
                { R.drawable.animation_path_motion_rect, 0xffff0000, 0x0},
        });
    }

    public AnimatedVectorDrawableParameterizedTest(final int resId, int startExpected,
            int endExpected) throws Throwable {
        mResId = resId;
        mStartExpected = startExpected;
        mEndExpected = endExpected;
    }

    /**
     * Render AVD with path morphing, make sure the bitmap is different when it render at the start
     * and the end.
     *
     * @throws Exception for time out or I/O problem while dumping debug images.
     */
    @Test
    public void testPathMorphing() throws Exception {
        final Object lock = new Object();
        final Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_WIDTH,
                Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bitmap);

        final AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mContext,
                mResId);
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        bitmap.eraseColor(0);
        avd.draw(c);
        int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2);
        assertTrue(centerColor == mStartExpected);

        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPNG(mResources, bitmap, -1, "start");
        }

        avd.registerAnimationCallback(new AnimationCallback() {
            @Override
            public void onAnimationStart(Drawable drawable) {
                // Nothing to do.
            }

            @Override
            public void onAnimationEnd(Drawable drawable) {
                bitmap.eraseColor(0);
                drawable.draw(c);
                int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2);
                assertTrue(centerColor == mEndExpected);

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
            saveVectorDrawableIntoPNG(mResources, bitmap, -1, "ended");
        }
    }

    @Before
    public void setup() throws Exception {
        mContext = mActivityTestRule.getActivity();
        mResources = mContext.getResources();
    }
}
