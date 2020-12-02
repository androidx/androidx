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

package androidx.vectordrawable.graphics.drawable.tests;

import static androidx.vectordrawable.graphics.drawable.tests.DrawableUtils.saveVectorDrawableIntoPng;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.core.animation.AnimatorTestRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.vectordrawable.seekable.test.R;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.atomic.AtomicBoolean;

@MediumTest
@RunWith(Parameterized.class)
public class PathInterpolatorParameterizedTest {

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    private int mResId;

    private static final boolean DBG_DUMP_PNG = false;

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
                R.drawable.animation_path_interpolator_1,
                R.drawable.animation_path_interpolator_2,
        };
    }

    @ClassRule
    public static AnimatorTestRule sAnimatorTestRule = new AnimatorTestRule();

    public PathInterpolatorParameterizedTest(int resId) {
        mResId = resId;
    }

    @Test
    public void testPathMorphing() throws Exception {
        final Bitmap bitmap =
                Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bitmap);
        final Context context = ApplicationProvider.getApplicationContext();

        final SeekableAnimatedVectorDrawable avd =
                SeekableAnimatedVectorDrawable.create(context, mResId);
        assertThat(avd).isNotNull();

        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        bitmap.eraseColor(0);
        avd.draw(c);
        int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2);
        assertThat(centerColor).isEqualTo(0);

        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPng(context.getResources(), bitmap, mResId, "start");
        }

        final AtomicBoolean ended = new AtomicBoolean(false);

        avd.registerAnimationCallback(new SeekableAnimatedVectorDrawable.AnimationCallback() {
            @Override
            public void onAnimationStart(@NonNull SeekableAnimatedVectorDrawable drawable) {
                // Nothing to do.
            }

            @Override
            public void onAnimationEnd(@NonNull SeekableAnimatedVectorDrawable drawable) {
                bitmap.eraseColor(0);
                drawable.draw(c);
                int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2);
                assertThat(centerColor).isEqualTo(0xffff0000);

                ended.set(true);
            }
        });

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            avd.start();
            sAnimatorTestRule.advanceTimeBy(1000);
        });
        assertThat(ended.get()).isTrue();

        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPng(context.getResources(), bitmap, mResId, "end");
        }
    }
}
