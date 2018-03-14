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

package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.ImageView;

import androidx.leanback.R;
import androidx.leanback.app.TestActivity;
import androidx.leanback.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.util.Random;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ImageCardViewTest {

    private static final String IMAGE_CARD_VIEW_ACTIVITY = "ImageCardViewActivity";
    private static final float FINAL_ALPHA_STATE = 1.0f;
    private static final float INITIAL_ALPHA_STATE = 0.0f;
    private static final float DELTA = 0.0f;
    private static final long ANIMATION_DURATION = 5000;
    private static final int RANDOM_COLOR_ONE = 0xffffffff;
    private static final int RANDOM_COLOR_TWO = 0x00000000;

    @Rule
    public TestName mUnitTestName = new TestName();

    // Enable lifecycle based testing
    private TestActivity.TestActivityTestRule mRule;

    // Only support alpha animation
    private static final String ALPHA = "alpha";

    // Flag to represent if the callback has been called or not
    private boolean mOnAnimationStartCalled;
    private boolean mOnAnimationPauseCalled;
    private boolean mOnAnimationResumeCalled;
    private boolean mOnAnimationCancelCalled;
    private boolean mOnAnimationEndCalled;
    private boolean mOnAnimationRepeatCalled;

    // ImageCardView for testing.
    private ImageCardView mImageCardView;

    // Animator for testing.
    private ObjectAnimator mFadeInAnimator;

    // ImageView on ImageCardView;
    private ImageView mImageView;

    // Sample Drawable which will be used as the parameter for some methods.
    private Drawable mSampleDrawable;

    // Another Sample Drawable.
    private Drawable mSampleDrawable2;

    // Generated Image View Id.
    private int mImageCardViewId;

    // Listener to capture animator's state
    private AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            mOnAnimationStartCalled = true;
        }

        @Override
        public void onAnimationPause(Animator animation) {
            super.onAnimationPause(animation);
            mOnAnimationPauseCalled = true;
        }

        @Override
        public void onAnimationResume(Animator animation) {
            super.onAnimationResume(animation);
            mOnAnimationResumeCalled = true; }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            mOnAnimationCancelCalled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            mOnAnimationEndCalled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            super.onAnimationRepeat(animation);
            mOnAnimationRepeatCalled = true;
        }
    };

    // Set up before executing test cases.
    @Before
    public void setUp() throws Exception {
        // The following provider will create an Activity which can inflate the ImageCardView
        // And the ImageCardView can be fetched through ID for future testing.
        TestActivity.Provider imageCardViewProvider = new TestActivity.Provider() {
            @Override
            public void onCreate(TestActivity activity, Bundle savedInstanceState) {
                super.onCreate(activity, savedInstanceState);

                // The theme must be set to make sure imageCardView can be populated correctly.
                activity.setTheme(R.style.Widget_Leanback_ImageCardView_BadgeStyle);

                // Create Drawable using random color for test purpose.
                mSampleDrawable = new ColorDrawable(RANDOM_COLOR_ONE);

                // Create Drawable using random color for test purpose.
                mSampleDrawable2 = new ColorDrawable(RANDOM_COLOR_TWO);

                // Create imageCardView and save system generated ID.
                ImageCardView imageCardView = new ImageCardView(activity);
                mImageCardViewId = imageCardView.generateViewId();
                imageCardView.setId(mImageCardViewId);

                // Set up imageCardView with activity programmatically.
                activity.setContentView(imageCardView);
            }
        };

        // Initialize testing rule and testing activity
        mRule = new TestActivity.TestActivityTestRule(imageCardViewProvider, generateProviderName(
                IMAGE_CARD_VIEW_ACTIVITY));
        final TestActivity imageCardViewTestActivity = mRule.launchActivity();

        // Create card view and image view
        mImageCardView = (ImageCardView) imageCardViewTestActivity.findViewById(mImageCardViewId);
        mImageView = mImageCardView.getMainImageView();

        // Create animator.
        mFadeInAnimator = mImageCardView.mFadeInAnimator;
        mFadeInAnimator.addListener(mAnimatorListener);

        // Set animation duration with longer period of time for robust testing.
        mFadeInAnimator.setDuration(ANIMATION_DURATION);
    }

    /**
     * Test SetMainImage method when the parameters are null and false
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageTest0() throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(null, false);

                // Currently, the animation hasn't started yet, the cancel event will not be
                // triggered
                assertFalse(mOnAnimationCancelCalled);

                // The animation will not be started, check status immediately.
                assertEquals(mImageCardView.getMainImage(), null);
                assertEquals(mImageView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
                assertEquals(mImageView.getVisibility(), View.INVISIBLE);
            }
        });
    }

    /**
     * Test SetMainImage method when the parameters are mSampleDrawable and false
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageTest1() throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(mSampleDrawable, false);

                // Currently, the animation hasn't started yet, the cancel event will not be
                // triggered
                assertFalse(mOnAnimationCancelCalled);

                // The animation will not be started, check status immediately.
                assertEquals(mImageCardView.getMainImage(), mSampleDrawable);
                assertEquals(mImageView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });
    }

    /**
     * Test SetMainImage method when the parameters are null and true
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageTest2() throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(null, true);

                // Currently, the animation hasn't started yet, the cancel event will not be
                // triggered
                assertFalse(mOnAnimationCancelCalled);

                // The animation will not be started, check status immediately.
                assertEquals(mImageCardView.getMainImage(), null);
                assertEquals(mImageView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
                assertEquals(mImageView.getVisibility(), View.INVISIBLE);
            }
        });
    }

    /**
     * Test SetMainImage method with sample drawable object and true parameter
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageTest3() throws Throwable {
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(mSampleDrawable, true);

                // The fadeIn method should be triggered in this scenario
                assertTrue(mOnAnimationStartCalled);

                assertEquals(mImageCardView.getMainImage(), mSampleDrawable);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });

        // Set time out limitation to be 2 * ANIMATION_DURATION.
        PollingCheck.waitFor(2 * ANIMATION_DURATION, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mOnAnimationEndCalled;
            }
        });

        // Test if animation ended successfully through alpha value.
        assertTrue(mOnAnimationEndCalled);
        assertEquals(mImageView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
    }

    /**
     * Test SetMainImage method's behavior when the animation is already started
     * In this test case, the parameters are set to null and false to interrupt existed animation
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageInTransitionTest0() throws Throwable {
        // The transition duration before the interruption happens.
        long durationBeforeInterruption = (long) (0.5 * ANIMATION_DURATION);

        // Perform an animation firstly
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(mSampleDrawable, true);

                // The fadeIn method should be triggered in this scenario
                assertTrue(mOnAnimationStartCalled);

                assertEquals(mImageCardView.getMainImage(), mSampleDrawable);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });

        // simulate the duration of animation
        SystemClock.sleep(durationBeforeInterruption);

        // Interrupt current animation using setMainImage(Drawable, boolean) method
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Interrupt existed animation
                mImageCardView.setMainImage(null, false);

                // Existed animation will be cancelled immediately.
                assertTrue(mOnAnimationCancelCalled);

                // New animation will not be triggered, check the status immediately
                assertEquals(mImageCardView.getMainImage(), null);
                assertEquals(mImageCardView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
                assertEquals(mImageView.getVisibility(), View.INVISIBLE);
            }
        });
    }

    /**
     * Test SetMainImage method's behavior when the animation is already started
     * In this test case, the parameters are set to mSampleDrawable2 and false to interrupt
     * existed animation
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageInTransitionTest1() throws Throwable {
        // The transition duration before the interruption happens.
        long durationBeforeInterruption = (long) (0.5 * ANIMATION_DURATION);

        // Perform an animation firstly
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(mSampleDrawable, true);

                // The fadeIn method should be triggered in this scenario
                assertTrue(mOnAnimationStartCalled);

                assertEquals(mImageCardView.getMainImage(), mSampleDrawable);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });

        // simulate the duration of animation
        SystemClock.sleep(durationBeforeInterruption);

        // Interrupt current animation using setMainImage(Drawable, boolean) method
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Interrupt existed animation
                mImageCardView.setMainImage(mSampleDrawable2, false);

                // Existed animation will be cancelled immediately.
                assertTrue(mOnAnimationCancelCalled);

                // New animation will not be triggered, check the status immediately
                assertEquals(mImageCardView.getMainImage(), mSampleDrawable2);
                assertEquals(mImageCardView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });
    }

    /**
     * Test SetMainImage method's behavior when the animation is already started
     * In this test case, the parameters are set to null and true to interrupt existed animation
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageInTransitionTest2() throws Throwable {
        // The transition duration before the interruption happens.
        long durationBeforeInterruption = (long) (0.5 * ANIMATION_DURATION);

        // Perform an animation firstly
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(mSampleDrawable, true);

                // The fadeIn method should be triggered in this scenario
                assertTrue(mOnAnimationStartCalled);

                assertEquals(mImageCardView.getMainImage(), mSampleDrawable);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });

        // simulate the duration of animation
        SystemClock.sleep(durationBeforeInterruption);

        // Interrupt current animation using setMainImage(Drawable, boolean) method
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Interrupt existed animation
                mImageCardView.setMainImage(null, true);

                // Existed animation will be cancelled immediately.
                assertTrue(mOnAnimationCancelCalled);

                // New animation will not be triggered, check the status immediately
                assertEquals(mImageCardView.getMainImage(), null);
                assertEquals(mImageCardView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
                assertEquals(mImageView.getVisibility(), View.INVISIBLE);
            }
        });
    }

    /**
     * Test SetMainImage method's behavior when the animation is already started
     * In this test case, the parameters are set to mSampleDrawable2 and true to interrupt
     * existed animation
     *
     * @throws Throwable
     */
    @Test
    public void testSetMainImageInTransitionTest3() throws Throwable {
        // The transition duration before the interruption happens.
        long durationBeforeInterruption = (long) (0.5 * ANIMATION_DURATION);

        // Perform an animation firstly
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // set random alpha value initially
                mImageView.setAlpha(generateInitialialAlphaValue());
                mImageCardView.setMainImage(mSampleDrawable, true);

                // The fadeIn method should be triggered in this scenario
                assertTrue(mOnAnimationStartCalled);

                assertEquals(mImageCardView.getMainImage(), mSampleDrawable);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });

        // Simulate the duration of animation
        SystemClock.sleep(durationBeforeInterruption);

        // Interrupt current animation using setMainImage(Drawable, boolean) method
        mRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Interrupt existed animation
                mImageCardView.setMainImage(mSampleDrawable2, true);

                // Existed animation will not be cancelled immediately.
                assertFalse(mOnAnimationCancelCalled);

                // New animation will not be triggered, check the status immediately
                assertEquals(mImageCardView.getMainImage(), mSampleDrawable2);
                assertEquals(mImageView.getVisibility(), View.VISIBLE);
            }
        });

        // Set time out limitation to be 2 * ANIMATION_DURATION.
        PollingCheck.waitFor(2 * ANIMATION_DURATION, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mOnAnimationEndCalled;
            }
        });

        // Test if animation ended successfully through alpha value.
        assertEquals(mImageView.getAlpha(), FINAL_ALPHA_STATE, DELTA);
    }


    // Helper function to register provider's name
    private String generateProviderName(String name) {
        return mUnitTestName.getMethodName() + "_" + name;
    }

    // generate random number as the initial alpha value
    private float generateInitialialAlphaValue() {
        Random generator = new Random();
        return generator.nextFloat();
    }
}


