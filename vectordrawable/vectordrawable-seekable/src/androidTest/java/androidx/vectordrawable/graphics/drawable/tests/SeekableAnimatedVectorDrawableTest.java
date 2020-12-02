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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.animation.AnimatorTestRule;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.vectordrawable.seekable.test.R;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SeekableAnimatedVectorDrawableTest {

    @ClassRule
    public static AnimatorTestRule sAnimatorTestRule = new AnimatorTestRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final float PIXEL_ERROR_THRESHOLD = 0.3f;
    private static final float PIXEL_DIFF_THRESHOLD = 0.03f;
    private static final float PIXEL_DIFF_COUNT_THRESHOLD = 0.1f;

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    @Test
    @UiThreadTest
    public void inflate() throws Throwable {
        final Context context = ApplicationProvider.getApplicationContext();
        final Resources resources = context.getResources();
        final Resources.Theme theme = context.getTheme();
        XmlPullParser parser = resources.getXml(R.drawable.animation_vector_drawable_grouping_1);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        do {
            type = parser.next();
        } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT);
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd =
                SeekableAnimatedVectorDrawable.createFromXmlInner(resources, parser, attrs, theme);
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        bitmap.eraseColor(0);
        avd.draw(canvas);
        int sunColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
        int earthColor = bitmap.getPixel(IMAGE_WIDTH * 3 / 4 + 2, IMAGE_HEIGHT / 2);
        assertThat(sunColor).isEqualTo(0xFFFF8000);
        assertThat(earthColor).isEqualTo(0xFF5656EA);
    }

    @Test
    @UiThreadTest
    public void registerCallback() {
        final SeekableAnimatedVectorDrawable avd = createAvd();

        final AtomicBoolean started = new AtomicBoolean(false);
        final AtomicBoolean ended = new AtomicBoolean(false);
        final AtomicBoolean paused = new AtomicBoolean(false);
        final AtomicBoolean resumed = new AtomicBoolean(false);
        final AtomicBoolean updated = new AtomicBoolean(false);
        avd.registerAnimationCallback(new SeekableAnimatedVectorDrawable.AnimationCallback() {
            @Override
            public void onAnimationStart(@NonNull SeekableAnimatedVectorDrawable drawable) {
                started.set(true);
            }

            @Override
            public void onAnimationEnd(@NonNull SeekableAnimatedVectorDrawable drawable) {
                ended.set(true);
            }

            @Override
            public void onAnimationPause(@NonNull SeekableAnimatedVectorDrawable drawable) {
                paused.set(true);
            }

            @Override
            public void onAnimationResume(@NonNull SeekableAnimatedVectorDrawable drawable) {
                resumed.set(true);
            }

            @Override
            public void onAnimationUpdate(@NonNull SeekableAnimatedVectorDrawable drawable) {
                updated.set(true);
            }
        });

        assertThat(updated.get()).isFalse();
        assertThat(started.get()).isFalse();
        avd.start();
        assertThat(started.get()).isTrue();
        sAnimatorTestRule.advanceTimeBy(40L);

        assertThat(paused.get()).isFalse();
        avd.pause();
        assertThat(paused.get()).isTrue();

        assertThat(resumed.get()).isFalse();
        avd.resume();
        assertThat(resumed.get()).isTrue();

        assertThat(ended.get()).isFalse();
        sAnimatorTestRule.advanceTimeBy(1000L);
        assertThat(ended.get()).isTrue();
        assertThat(updated.get()).isTrue();
    }

    @Test
    @UiThreadTest
    public void clearCallback() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        avd.registerAnimationCallback(createFailingCallback());
        avd.clearAnimationCallbacks();
        avd.start();
        sAnimatorTestRule.advanceTimeBy(1000L);
    }

    @Test
    @UiThreadTest
    public void unregisterCallback() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        final SeekableAnimatedVectorDrawable.AnimationCallback callback = createFailingCallback();
        avd.registerAnimationCallback(callback);
        final boolean removed = avd.unregisterAnimationCallback(callback);
        assertThat(removed).isTrue();
        avd.start();
        sAnimatorTestRule.advanceTimeBy(1000L);
    }

    @Test
    @UiThreadTest
    public void constantState() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        // SAVD does not support ConstantState.
        assertThat(avd.getConstantState()).isNull();
    }

    @Test
    @UiThreadTest
    public void mutate() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        final Drawable mutated = avd.mutate();
        // SAVD does not support mutate.
        assertThat(mutated).isSameInstanceAs(avd);
    }

    @Test
    @UiThreadTest
    public void renderCorrectness() {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animation_vector_drawable_circle
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        avd.start();

        // First make sure the content is drawn into the bitmap.
        // Then save the first frame as the golden images.
        bitmap.eraseColor(0);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)).isNotEqualTo(0);
        final Bitmap firstFrame = Bitmap.createBitmap(bitmap);

        // Now compare the following frames with the 1st frames. Expect some minor difference like
        // Anti-Aliased edges, so the compare is fuzzy.
        for (int i = 0; i < 5; i++) {
            sAnimatorTestRule.advanceTimeBy(16L);
            bitmap.eraseColor(0);
            avd.draw(canvas);
            compareImages(firstFrame, bitmap);
        }
    }

    @Test
    @UiThreadTest
    public void animateColor() {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animated_color_fill
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        avd.draw(canvas);
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.RED);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2)).isEqualTo(Color.RED);

        avd.start();

        final ArrayList<Integer> historicalRed = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sAnimatorTestRule.advanceTimeBy(100L);
            avd.draw(canvas);
            final int strokeColor = bitmap.getPixel(0, 0);
            assertThat(Color.blue(strokeColor)).isEqualTo(0);
            assertThat(Color.green(strokeColor)).isEqualTo(0);
            final int fillColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
            assertThat(Color.blue(fillColor)).isEqualTo(0);
            assertThat(Color.green(fillColor)).isEqualTo(0);
            historicalRed.add(Color.red(fillColor));
        }
        assertThat(historicalRed).isInOrder((Comparator<Integer>) (o1, o2) -> o2 - o1);
    }

    @Test
    @UiThreadTest
    public void pathMorphing() {
        testPathMorphing(R.drawable.animation_path_morphing_rect);
        testPathMorphing(R.drawable.animation_path_morphing_rect2);
        testPathMorphing(R.drawable.animation_path_motion_rect);
    }

    @Test
    @UiThreadTest
    public void pathMorphing_exception() {
        expectedException.expect(InflateException.class);
        SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animation_path_morphing_rect_exception
        );
    }

    @Test
    @UiThreadTest
    public void pauseAndResume() {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);
        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animated_color_fill
        );

        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.RED);

        avd.start();
        assertThat(avd.isRunning()).isTrue();
        assertThat(avd.isPaused()).isFalse();
        sAnimatorTestRule.advanceTimeBy(100L);
        avd.draw(canvas);
        final int pausedColor = bitmap.getPixel(0, 0);
        assertThat(Color.red(pausedColor)).isLessThan(0xff);

        avd.pause();
        assertThat(avd.isRunning()).isTrue();
        assertThat(avd.isPaused()).isTrue();
        sAnimatorTestRule.advanceTimeBy(1000L);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(pausedColor);

        avd.resume();
        assertThat(avd.isRunning()).isTrue();
        assertThat(avd.isPaused()).isFalse();
        sAnimatorTestRule.advanceTimeBy(100L);
        avd.draw(canvas);
        assertThat(Color.red(bitmap.getPixel(0, 0))).isLessThan(Color.red(pausedColor));
    }

    @Test
    @UiThreadTest
    public void setCurrentPlayTime() {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);
        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animated_color_fill
        );

        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.RED);

        int previousRed = Integer.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            avd.setCurrentPlayTime((i + 1) * 100L);
            avd.draw(canvas);
            final int fillColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
            assertThat(Color.blue(fillColor)).isEqualTo(0);
            assertThat(Color.green(fillColor)).isEqualTo(0);
            int red = Color.red(fillColor);
            assertThat(red).isLessThan(previousRed);
            previousRed = red;
        }
    }

    @Test
    @UiThreadTest
    public void getCurrentPlayTime() {
        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animated_color_fill
        );
        assertThat(avd).isNotNull();
        avd.setCurrentPlayTime(100L);
        assertThat(avd.getCurrentPlayTime()).isEqualTo(100L);
    }

    @Test
    @UiThreadTest
    public void getTotalDuration() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        assertThat(avd.getTotalDuration()).isEqualTo(150L);
    }

    private SeekableAnimatedVectorDrawable createAvd() {
        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animation_vector_drawable_grouping_1 // Duration: 50 * 3 ms
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        return avd;
    }

    private SeekableAnimatedVectorDrawable.AnimationCallback createFailingCallback() {
        return new SeekableAnimatedVectorDrawable.AnimationCallback() {
            @Override
            public void onAnimationStart(@NonNull SeekableAnimatedVectorDrawable drawable) {
                fail("This callback should not be invoked.");
            }

            @Override
            public void onAnimationEnd(@NonNull SeekableAnimatedVectorDrawable drawable) {
                fail("This callback should not be invoked.");
            }
        };
    }

    private void testPathMorphing(@DrawableRes int resId) {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                resId
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        avd.draw(canvas);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)).isEqualTo(Color.RED);

        final AtomicBoolean ended = new AtomicBoolean(false);
        avd.registerAnimationCallback(new SeekableAnimatedVectorDrawable.AnimationCallback() {
            @Override
            public void onAnimationEnd(@NonNull SeekableAnimatedVectorDrawable drawable) {
                ended.set(true);
            }
        });

        avd.start();
        sAnimatorTestRule.advanceTimeBy(1000L);
        assertThat(ended.get()).isTrue();
        bitmap.eraseColor(0);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)).isEqualTo(Color.TRANSPARENT);
    }

    /**
     * Utility function for fuzzy image comparison between 2 bitmap. Fails if the difference is
     * bigger than a threshold.
     */
    private void compareImages(Bitmap ideal, Bitmap given) {
        int idealWidth = ideal.getWidth();
        int idealHeight = ideal.getHeight();

        assertThat(idealWidth).isEqualTo(given.getWidth());
        assertThat(idealHeight).isEqualTo(given.getHeight());

        int totalDiffPixelCount = 0;
        float totalPixelCount = idealWidth * idealHeight;
        for (int x = 0; x < idealWidth; x++) {
            for (int y = 0; y < idealHeight; y++) {
                int idealColor = ideal.getPixel(x, y);
                int givenColor = given.getPixel(x, y);
                if (idealColor == givenColor) {
                    continue;
                }

                float totalError = 0;
                totalError += Math.abs(Color.red(idealColor) - Color.red(givenColor));
                totalError += Math.abs(Color.green(idealColor) - Color.green(givenColor));
                totalError += Math.abs(Color.blue(idealColor) - Color.blue(givenColor));
                totalError += Math.abs(Color.alpha(idealColor) - Color.alpha(givenColor));

                assertThat(totalError / 1024.f).isAtMost(PIXEL_ERROR_THRESHOLD);

                if ((totalError / 1024.0f) >= PIXEL_DIFF_THRESHOLD) {
                    totalDiffPixelCount++;
                }
            }
        }
        assertThat(totalDiffPixelCount / totalPixelCount).isAtMost(PIXEL_DIFF_COUNT_THRESHOLD);
    }
}
