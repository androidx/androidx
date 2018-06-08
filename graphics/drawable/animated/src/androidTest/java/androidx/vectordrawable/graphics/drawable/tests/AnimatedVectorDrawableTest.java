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

package androidx.vectordrawable.graphics.drawable.tests;

import static androidx.vectordrawable.graphics.drawable.tests.DrawableUtils
        .saveVectorDrawableIntoPNG;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.core.view.ViewCompat;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat.AnimationCallback;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AnimatedVectorDrawableTest {
    @Rule public final ActivityTestRule<DrawableStubActivity> mActivityTestRule;

    private static final float PIXEL_ERROR_THRESHOLD = 0.3f;
    private static final float PIXEL_DIFF_THRESHOLD = 0.03f;
    private static final float PIXEL_DIFF_COUNT_THRESHOLD = 0.1f;

    private static final String LOGTAG = AnimatedVectorDrawableTest.class.getSimpleName();

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    @DrawableRes
    private static final int DRAWABLE_RES_ID = R.drawable.animation_vector_drawable_grouping_1;

    private Context mContext;
    private Resources mResources;
    private AnimatedVectorDrawableCompat mAnimatedVectorDrawable;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private static final boolean DBG_DUMP_PNG = false;

    // States to check for animation callback tests.
    private boolean mAnimationStarted = false;
    private boolean mAnimationEnded = false;

    // Animation callback used for all callback related tests.
    private AnimationCallback mAnimationCallback =
            new AnimationCallback() {
                @Override
                public void onAnimationStart(
                        Drawable drawable) {
                    mAnimationStarted = true;
                }

                @Override
                public void onAnimationEnd(
                        Drawable drawable) {
                    mAnimationEnded = true;
                }
            };

    public AnimatedVectorDrawableTest() {
        mActivityTestRule = new ActivityTestRule<>(DrawableStubActivity.class);
    }

    @Before
    public void setup() throws Exception {
        mBitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mContext = mActivityTestRule.getActivity();
        mResources = mContext.getResources();

        mAnimatedVectorDrawable = AnimatedVectorDrawableCompat.create(mContext, DRAWABLE_RES_ID);
    }


    @Test
    public void testInflate() throws Exception {
        // Setup AnimatedVectorDrawableCompat from xml file
        XmlPullParser parser = mResources.getXml(DRAWABLE_RES_ID);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        mAnimatedVectorDrawable.inflate(mResources, parser, attrs);
        mAnimatedVectorDrawable.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        mBitmap.eraseColor(0);
        mAnimatedVectorDrawable.draw(mCanvas);
        int sunColor = mBitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
        int earthColor = mBitmap.getPixel(IMAGE_WIDTH * 3 / 4 + 2, IMAGE_HEIGHT / 2);
        assertTrue(sunColor == 0xFFFF8000);
        assertTrue(earthColor == 0xFF5656EA);

        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPNG(mResources, mBitmap, DRAWABLE_RES_ID, null);
        }
    }

    /**
     * Render AVD sequence in an bitmap for several frames with the same content, and make sure
     * there is no image corruptions.
     *
     * @throws IOException only if DBG_DUMP_PNG is true when dumping images for debugging purpose.
     */
    @Test
    public void testRenderCorrectness() throws IOException {
        final int numTests = 5;
        final Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_WIDTH,
                Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bitmap);

        final AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mContext,
                R.drawable.animation_vector_drawable_circle);
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                avd.start();
            }
        });

        // First make sure the content is drawn into the bitmap.
        // Then save the first frame as the golden images.
        bitmap.eraseColor(0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                avd.draw(c);
            }
        });
        int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2);
        assertTrue(centerColor != 0);
        Bitmap firstFrame = Bitmap.createBitmap(bitmap);
        if (DBG_DUMP_PNG) {
            saveVectorDrawableIntoPNG(mResources, firstFrame, -1, "firstframe");
        }

        // Now compare the following frames with the 1st frames. Expect some minor difference like
        // Anti-Aliased edges, so the compare is fuzzy.
        for (int i = 0; i < numTests; i++) {
            bitmap.eraseColor(0);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    avd.draw(c);
                }
            });
            if (DBG_DUMP_PNG) {
                saveVectorDrawableIntoPNG(mResources, bitmap, -1, "correctness_" + i);
            }
            compareImages(firstFrame, bitmap, "correctness_" + i);
        }
    }

    /**
     * Utility function for fuzzy image comparison b/t 2 bitmap. Failed if the difference is bigger
     * than a threshold.
     */
    private void compareImages(Bitmap ideal, Bitmap given, String filename) {
        int idealWidth = ideal.getWidth();
        int idealHeight = ideal.getHeight();

        assertTrue(idealWidth == given.getWidth());
        assertTrue(idealHeight == given.getHeight());

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

                if ((totalError / 1024.0f) >= PIXEL_ERROR_THRESHOLD) {
                    fail((filename + ": totalError is " + totalError));
                }

                if ((totalError / 1024.0f) >= PIXEL_DIFF_THRESHOLD) {
                    totalDiffPixelCount++;
                }
            }
        }
        if ((totalDiffPixelCount / totalPixelCount) >= PIXEL_DIFF_COUNT_THRESHOLD) {
            fail((filename + ": totalDiffPixelCount is " + totalDiffPixelCount));
        }

    }

    @Test
    public void testGetChangingConfigurations() {
        AnimatedVectorDrawableCompat d1 = AnimatedVectorDrawableCompat.create(mContext,
                R.drawable.animated_color_fill_copy);
        ConstantState constantState = d1.getConstantState();

        if (constantState != null) {
            // default
            assertEquals(0, constantState.getChangingConfigurations());
            assertEquals(0, d1.getChangingConfigurations());

            // change the drawable's configuration does not affect the state's configuration
            d1.setChangingConfigurations(0xff);
            assertEquals(0xff, d1.getChangingConfigurations());
            assertEquals(0, constantState.getChangingConfigurations());

            // the state's configuration get refreshed
            constantState = d1.getConstantState();
            assertEquals(0xff, constantState.getChangingConfigurations());

            // set a new configuration to drawable
            d1.setChangingConfigurations(0xff00);
            assertEquals(0xff, constantState.getChangingConfigurations());
            assertEquals(0xffff, d1.getChangingConfigurations());
        }
    }

    @Test
    public void testGetConstantState() {
        ConstantState constantState = mAnimatedVectorDrawable.getConstantState();
        if (constantState != null) {
            assertEquals(0, constantState.getChangingConfigurations());

            mAnimatedVectorDrawable.setChangingConfigurations(1);
            constantState = mAnimatedVectorDrawable.getConstantState();
            assertNotNull(constantState);
            assertEquals(1, constantState.getChangingConfigurations());
        }
    }

    @Test
    public void testAnimateColor() throws Throwable {
        final ImageButton imageButton =
                (ImageButton) mActivityTestRule.getActivity().findViewById(R.id.imageButton);
        final int viewW = imageButton.getWidth();
        final int viewH = imageButton.getHeight();
        int pixelX = viewW / 2;
        int pixelY = viewH / 2;
        final int numTests = 5;
        final Bitmap bitmap = Bitmap.createBitmap(imageButton.getWidth(), imageButton.getHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bitmap);
        CountDownLatch latch = new CountDownLatch(numTests);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mContext,
                        R.drawable.animated_color_fill);
                ViewCompat.setBackground(imageButton, avd);
                avd.start();
            }
        });
        // Check the view several times during the animation to verify that it only
        // has red color in it
        for (int i = 0; i < numTests; ++i) {
            sleep(100);
            // check fill
            verifyRedOnly(pixelX, pixelY, imageButton, bitmap, c, latch);
            // check stroke
            verifyRedOnly(1, 1, imageButton, bitmap, c, latch);
        }
        latch.await(1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Utility method to verify that the pixel at the given location has only red values.
     */
    private void verifyRedOnly(final int pixelX, final int pixelY, final View button,
            final Bitmap bitmap, final Canvas canvas, final CountDownLatch latch) throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.draw(canvas);
                int pixel = bitmap.getPixel(pixelX, pixelY);
                int blue = pixel & 0xff;
                int green = pixel & 0xff00 >> 8;
                assertEquals("Blue channel not zero", 0, blue);
                assertEquals("Green channel not zero", 0, green);
                latch.countDown();
            }
        });
    }

    @Test
    public void testMutate() {
        AnimatedVectorDrawableCompat d1 =
                AnimatedVectorDrawableCompat.create(mContext, DRAWABLE_RES_ID);
        AnimatedVectorDrawableCompat d2 =
                AnimatedVectorDrawableCompat.create(mContext, DRAWABLE_RES_ID);
        AnimatedVectorDrawableCompat d3 =
                AnimatedVectorDrawableCompat.create(mContext, DRAWABLE_RES_ID);

        if (d1.getConstantState() != null) {
            int originalAlpha = d2.getAlpha();
            int newAlpha = (originalAlpha + 1) % 255;

            // AVD is different than VectorDrawable. Every instance of it is a deep copy
            // of the VectorDrawable.
            // So every setAlpha operation will happen only to that specific object.
            d1.setAlpha(newAlpha);
            assertEquals(newAlpha, d1.getAlpha());
            assertEquals(originalAlpha, d2.getAlpha());
            assertEquals(originalAlpha, d3.getAlpha());

            d1.mutate();
            d1.setAlpha(0x40);
            assertEquals(0x40, d1.getAlpha());
            assertEquals(originalAlpha, d2.getAlpha());
            assertEquals(originalAlpha, d3.getAlpha());

            d2.setAlpha(0x20);
            assertEquals(0x40, d1.getAlpha());
            assertEquals(0x20, d2.getAlpha());
            assertEquals(originalAlpha, d3.getAlpha());
        } else {
            assertEquals(d1.mutate(), d1);
        }
    }

    /**
     * A helper function to setup the AVDC for callback tests.
     */
    private AnimatedVectorDrawableCompat setupAnimatedVectorDrawableCompat() {
        final ImageButton imageButton =
                (ImageButton) mActivityTestRule.getActivity().findViewById(R.id.imageButton);
        mAnimationStarted = false;
        mAnimationEnded = false;

        AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mContext,
                R.drawable.animation_vector_drawable_grouping_1); // Duration is 50 ms.
        ViewCompat.setBackground(imageButton, avd);
        return avd;
    }

    @Test
    /**
     * Test show that callback is successfully registered.
     * Note that this test requires screen is on.
     */
    public void testRegisterCallback() throws Throwable {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                AnimatedVectorDrawableCompat avd = setupAnimatedVectorDrawableCompat();
                avd.registerAnimationCallback(mAnimationCallback);
                avd.start();
            }
        });
        Thread.sleep(500);
        assertTrue(mAnimationStarted);
        assertTrue(mAnimationEnded);
    }

    @Test
    /**
     * Test show that callback is successfully removed.
     * Note that this test requires screen is on.
     */
    public void testClearCallback() throws Throwable {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                AnimatedVectorDrawableCompat avd =
                        setupAnimatedVectorDrawableCompat();
                avd.registerAnimationCallback(mAnimationCallback);
                avd.clearAnimationCallbacks();
                avd.start();
            }
        });
        Thread.sleep(500);
        assertFalse(mAnimationStarted);
        assertFalse(mAnimationEnded);
    }

    @Test
    /**
     * Test show that callback is successfully unregistered.
     * Note that this test requires screen is on.
     */
    public void testUnregisterCallback() throws Throwable {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                AnimatedVectorDrawableCompat avd =
                        setupAnimatedVectorDrawableCompat();

                avd.registerAnimationCallback(mAnimationCallback);
                avd.unregisterAnimationCallback(mAnimationCallback);
                avd.start();
            }
        });
        Thread.sleep(500);
        assertFalse(mAnimationStarted);
        assertFalse(mAnimationEnded);
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
                R.drawable.animation_path_morphing_rect2);
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        bitmap.eraseColor(0);
        avd.draw(c);
        int centerColor = bitmap.getPixel(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2);
        assertTrue(centerColor == 0xffff0000);

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
                assertTrue(centerColor == 0);

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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Make sure when path didn't match, we got an exception.
     */
    @Test
    @UiThreadTest
    public void testPathMorphingException() throws Exception {
        thrown.expect(RuntimeException.class);
        final AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mContext,
                    R.drawable.animation_path_morphing_rect_exception);
    }
}
