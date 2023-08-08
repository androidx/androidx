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

package androidx.dynamicanimation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.lt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AndroidRuntimeException;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.AnimationHandler;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.FrameCallbackScheduler;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.dynamicanimation.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@MediumTest
@RunWith(AndroidJUnit4.class)
public class SpringTests {
    @SuppressWarnings("deprecation")
    @Rule public final androidx.test.rule.ActivityTestRule<AnimationActivity> mActivityTestRule;
    public View mView1;
    public View mView2;

    @SuppressWarnings("deprecation")
    public SpringTests() {
        mActivityTestRule = new androidx.test.rule.ActivityTestRule<>(AnimationActivity.class);
    }

    @Before
    public void setup() {
        mView1 = mActivityTestRule.getActivity().findViewById(R.id.anim_view);
        mView2 = mActivityTestRule.getActivity().findViewById(R.id.anim_another_view);
    }

    /**
     * Test that custom properties are supported.
     */
    @Test
    public void testCustomProperties() {
        final Object animObj = new Object();
        FloatPropertyCompat property = new FloatPropertyCompat("") {
            private float mValue = 0f;
            @Override
            public float getValue(Object object) {
                assertEquals(animObj, object);
                return mValue;
            }

            @Override
            public void setValue(Object object, float value) {
                assertEquals(animObj, object);
                assertTrue(value >= mValue);
                mValue = value;
            }
        };
        final SpringAnimation anim = new SpringAnimation(animObj, property, 1f);
        DynamicAnimation.OnAnimationEndListener listener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        anim.addEndListener(listener);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.start();
            }
        });
        verify(listener, timeout(1000)).onAnimationEnd(anim, false, 1f, 0f);
        assertEquals(1f, property.getValue(animObj), 0f);
    }

    /**
     * Test that spring animation can work with a single property without an object.
     */
    @Test
    @Ignore("b/280665072")
    public void testFloatValueHolder() {
        final FloatValueHolder floatValueHolder = new FloatValueHolder(0f);
        DynamicAnimation.OnAnimationUpdateListener updateListener =
                new DynamicAnimation.OnAnimationUpdateListener() {
            private float mLastValue = 0f;
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                // New value >= value from last frame
                assertTrue(value >= mLastValue);
                mLastValue = value;
                assertEquals(value, floatValueHolder.getValue(), 0f);
            }
        };

        DynamicAnimation.OnAnimationUpdateListener mockListener =
                mock(DynamicAnimation.OnAnimationUpdateListener.class);

        final SpringAnimation anim = new SpringAnimation(floatValueHolder)
                .addUpdateListener(updateListener).addUpdateListener(mockListener);
        anim.setSpring(new SpringForce(1000).setDampingRatio(1.2f));

        DynamicAnimation.OnAnimationEndListener listener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        anim.addEndListener(listener);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.setStartValue(0).start();
                // Set the duration scale to 1 to avoid prematurely ending the animation.
                // ValueAnimator#getDurationScale is called in start().
                anim.getAnimationHandler().mDurationScale = 1.0f;
            }
        });

        verify(mockListener, timeout(1000).atLeast(10)).onAnimationUpdate(eq(anim), lt(1000f),
                any(float.class));
        verify(listener, timeout(1000)).onAnimationEnd(anim, false, 1000f, 0f);
    }

    /**
     * Cancel a spring animation right after an animateToFinalPosition() is called.
     */
    @Test
    public void testCancelAfterAnimateToFinalPosition() {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.TRANSLATION_X,
                0);
        final DynamicAnimation.OnAnimationEndListener listener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            anim.addEndListener(listener);
            anim.start();
            assertTrue(anim.isRunning());
            anim.animateToFinalPosition(200f);
            anim.cancel();
            anim.animateToFinalPosition(-200f);
            anim.skipToEnd();
        });
        verify(listener, timeout(1000)).onAnimationEnd(anim, true, 0f, 0f);
        verify(listener, timeout(1000)).onAnimationEnd(anim, false, -200f, 0f);
    }


    /**
     * Check the final position of the default spring against what's being set through the
     * constructor.
     */
    @Test
    public void testGetFinalPosition() {
        SpringAnimation animation = new SpringAnimation(mView1, DynamicAnimation.TRANSLATION_X, 20);
        assertEquals(20, animation.getSpring().getFinalPosition(), 0);

        SpringForce spring = new SpringForce();
        spring.setFinalPosition(25.0f);
        assertEquals(25.0f, spring.getFinalPosition(), 0.0f);
    }

    /**
     * Verify that for over-damped springs, the higher the damping ratio, the slower it is. Also
     * verify that critically damped springs finish faster than overdamped springs.
     */
    @Test
    public void testDampingRatioOverAndCriticallyDamped() {
        // Compare overdamped springs
        final SpringAnimation anim1 = new SpringAnimation(mView1, DynamicAnimation.X, 0);
        final SpringAnimation anim2 = new SpringAnimation(mView2, DynamicAnimation.Y, 0);
        final SpringAnimation anim3 = new SpringAnimation(mView2, DynamicAnimation.Z, 0);
        final DynamicAnimation.OnAnimationUpdateListener updateListener =
                new DynamicAnimation.OnAnimationUpdateListener() {
                    public float position1 = 1000;
                    public float position2 = 1000;
                    public float position3 = 1000;
                    @Override
                    public void onAnimationUpdate(DynamicAnimation animation, float value,
                            float velocity) {
                        if (animation == anim1) {
                            position1 = value;
                            if (position1 == 800) {
                                // first frame
                                assertEquals(position1, position2, 0);
                                assertEquals(position1, position3, 0);
                            } else {
                                assertTrue(position2 > position1);
                                assertTrue(position3 > position2);
                                assertTrue(800 > position3);
                            }
                        } else if (animation == anim2) {
                            position2 = value;
                        } else {
                            position3 = value;
                        }
                    }
                };
        final MyEndListener l1 = new MyEndListener();
        final MyEndListener l2 = new MyEndListener();
        final MyEndListener l3 = new MyEndListener();

        final DynamicAnimation.OnAnimationEndListener mockListener =
                mock(DynamicAnimation.OnAnimationEndListener.class);
        anim1.getSpring().setStiffness(SpringForce.STIFFNESS_HIGH).setDampingRatio(1f);
        anim2.getSpring().setStiffness(SpringForce.STIFFNESS_HIGH).setDampingRatio(1.5f);
        anim3.getSpring().setStiffness(SpringForce.STIFFNESS_HIGH).setDampingRatio(2.0f);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim2.setStartValue(800).addUpdateListener(updateListener).addEndListener(l2)
                        .start();
                // Set the duration scale to 1 to avoid prematurely ending the animation.
                anim2.getAnimationHandler().mDurationScale = 1.0f;
                anim3.setStartValue(800).addUpdateListener(updateListener).addEndListener(l3)
                        .addEndListener(mockListener).start();
                anim3.getAnimationHandler().mDurationScale = 1.0f;
                anim1.setStartValue(800).addUpdateListener(updateListener).addEndListener(l1)
                        .start();
                anim1.getAnimationHandler().mDurationScale = 1.0f;

            }
        });
        // The spring animation with critically-damped spring should return to rest position faster.
        verify(mockListener, timeout(2000)).onAnimationEnd(anim3, false, 0, 0);
        assertTrue(l1.endTime > 0);
        assertTrue(l2.endTime > l1.endTime);
        assertTrue(l3.endTime > l2.endTime);
    }

    /**
     * Verify that more underdamped springs are bouncier, and that critically damped springs finish
     * faster than underdamped springs.
     */
    @Test
    public void testDampingRatioUnderDamped() {
        final SpringAnimation anim1 = new SpringAnimation(mView1, DynamicAnimation.ROTATION, 0);
        final SpringAnimation anim2 = new SpringAnimation(mView2, DynamicAnimation.ROTATION_X, 0);
        final SpringAnimation anim3 = new SpringAnimation(mView2, DynamicAnimation.ROTATION_Y, 0);

        final DynamicAnimation.OnAnimationUpdateListener updateListener =
                new DynamicAnimation.OnAnimationUpdateListener() {
                    public float bounceCount1 = 0;
                    public float bounceCount2 = 0;

                    public float velocity1 = 0;
                    public float velocity2 = 0;

                    @Override
                    public void onAnimationUpdate(DynamicAnimation animation, float value,
                            float velocity) {
                        if (animation == anim1) {
                            if (velocity > 0 && velocity1 < 0) {
                                bounceCount1++;
                            }
                            velocity1 = velocity;
                        } else if (animation == anim2) {
                            velocity2 = velocity;
                            if (velocity > 0 && velocity2 < 0) {
                                bounceCount2++;
                                assertTrue(bounceCount1 > bounceCount2);
                            }
                        }
                    }
                };
        final MyEndListener l1 = new MyEndListener();
        final MyEndListener l2 = new MyEndListener();
        final MyEndListener l3 = new MyEndListener();

        final DynamicAnimation.OnAnimationEndListener mockListener =
                mock(DynamicAnimation.OnAnimationEndListener.class);
        anim1.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(0.3f);
        anim2.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(0.5f);
        anim3.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(1f);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim1.setStartValue(360).addUpdateListener(updateListener).addEndListener(l1)
                        .start();
                // Set the duration scale to 1 to avoid prematurely ending the animation.
                anim1.getAnimationHandler().mDurationScale = 1.0f;
                anim2.setStartValue(360).addUpdateListener(updateListener).addEndListener(l2)
                        .addEndListener(mockListener).start();
                anim2.getAnimationHandler().mDurationScale = 1.0f;
                anim3.setStartValue(360).addEndListener(l3).start();
                anim3.getAnimationHandler().mDurationScale = 1.0f;
            }
        });
        // The spring animation with critically-damped spring should return to rest position faster.
        verify(mockListener, timeout(2000)).onAnimationEnd(anim2, false, 0, 0);
        assertFalse(anim3.isRunning());
        assertTrue(l3.endTime > 0);
        assertTrue(l2.endTime > l3.endTime);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                if (anim1.isRunning()) {
                    anim1.cancel();
                } else {
                    assertTrue(l1.endTime > l2.endTime);
                }
            }
        });
    }

    /**
     * Verify that stiffer spring animations finish sooner than less stiff spring animations. Run
     * the same verification on different damping ratios.
     */
    @LargeTest
    @Test
    @Ignore("b/280665072")
    public void testStiffness() {
        float[] dampingRatios = {0.3f, 0.5f, 1f, 5f};
        final float[] stiffness = {50f, 500f, 1500f, 5000f};
        DynamicAnimation.ViewProperty[] viewProperties =
                {DynamicAnimation.SCROLL_X, DynamicAnimation.TRANSLATION_X,
                        DynamicAnimation.TRANSLATION_Y, DynamicAnimation.TRANSLATION_Z};
        assertEquals(viewProperties.length, stiffness.length);

        final SpringAnimation[] springAnims = new SpringAnimation[stiffness.length];
        SpringForce[] springs = new SpringForce[stiffness.length];
        MyEndListener[] listeners = new MyEndListener[stiffness.length];

        // Sets stiffness
        for (int i = 0; i < stiffness.length; i++) {
            springs[i] = new SpringForce(0).setStiffness(stiffness[i]);
            listeners[i] = new MyEndListener();
            springAnims[i] = new SpringAnimation(mView1, viewProperties[i]).setSpring(springs[i])
                    .addEndListener(listeners[i]);
        }

        for (int i = 0; i < dampingRatios.length; i++) {
            for (int j = 0; j < stiffness.length; j++) {
                springs[j].setDampingRatio(dampingRatios[i]);
                springAnims[j].setStartValue(0).setStartVelocity(500);
                listeners[j].endTime = -1;
            }
            DynamicAnimation.OnAnimationEndListener mockListener = mock(
                    DynamicAnimation.OnAnimationEndListener.class);
            springAnims[1].addEndListener(mockListener);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < stiffness.length; j++) {
                        springAnims[j].start();
                        // Set the duration scale to 1 to avoid prematurely ending the animation.
                        springAnims[j].getAnimationHandler().mDurationScale = 1.0f;
                    }
                }
            });

            verify(mockListener, timeout(4000)).onAnimationEnd(springAnims[1], false, 0f, 0f);

            if (springAnims[0].isRunning()) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                    @Override
                    public void run() {
                        springAnims[0].cancel();
                    }
                });
            }
            for (int j = 1; j < stiffness.length; j++) {
                // The stiffer spring should finish no later than the less stiff spring.
                assertTrue(listeners[j - 1].endTime > listeners[j].endTime);
            }
        }
    }

    /**
     * Test negative stiffness and expect exception.
     */
    @Test
    public void testInvalidStiffness() {
        SpringForce spring = new SpringForce();
        assertThrows(IllegalArgumentException.class, () -> spring.setStiffness(-5f));
    }

    /**
     * Test negative dampingRatio and expect exception.
     */
    @Test
    public void testInvalidDampingRatio() {
        SpringForce spring = new SpringForce();
        assertThrows(IllegalArgumentException.class, () -> spring.setDampingRatio(-5f));
    }

    /**
     * Remove an update listener and an end listener, and check that there are no interaction after
     * removal.
     */
    @Test
    public void testRemoveListeners() {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.ALPHA, 0.5f);
        DynamicAnimation.OnAnimationEndListener endListener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        DynamicAnimation.OnAnimationEndListener removedEndListener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        DynamicAnimation.OnAnimationUpdateListener updateListener = mock(
                DynamicAnimation.OnAnimationUpdateListener.class);
        DynamicAnimation.OnAnimationUpdateListener removedUpdateListener = mock(
                DynamicAnimation.OnAnimationUpdateListener.class);

        anim.addEndListener(removedEndListener);
        anim.addEndListener(endListener);
        anim.removeEndListener(removedEndListener);

        anim.addUpdateListener(removedUpdateListener);
        anim.addUpdateListener(updateListener);
        anim.removeUpdateListener(removedUpdateListener);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.start();
            }
        });

        verify(endListener, timeout(1000)).onAnimationEnd(anim, false, 0.5f, 0f);
        verify(updateListener, atLeast(2)).onAnimationUpdate(eq(anim), any(float.class),
                any(float.class));

        verifyZeroInteractions(removedEndListener);
        verifyZeroInteractions(removedUpdateListener);
    }

    /**
     * Verifies stiffness getter returns the right value.
     */
    @Test
    public void testGetStiffness() {
        SpringForce spring = new SpringForce();
        spring.setStiffness(1.0f);
        assertEquals(1.0f, spring.getStiffness(), 0.0f);
        spring.setStiffness(2.0f);
        assertEquals(2.0f, spring.getStiffness(), 0.0f);
    }

    /**
     * Verifies damping ratio getter returns the right value.
     */
    @Test
    public void testGetDampingRatio() {
        SpringForce spring = new SpringForce();
        spring.setDampingRatio(1.0f);
        assertEquals(1.0f, spring.getDampingRatio(), 0.0f);
        spring.setDampingRatio(2.0f);
        assertEquals(2.0f, spring.getDampingRatio(), 0.0f);
    }

    /**
     * Verifies that once min and max value threshold does apply to the values in animation.
     */
    @Test
    public void testSetMinMax() {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.SCALE_X, 0.0f);
        anim.setMinValue(0.0f);
        anim.setMaxValue(1.0f);
        anim.getSpring().setStiffness(SpringForce.STIFFNESS_HIGH).setDampingRatio(
                SpringForce.DAMPING_RATIO_HIGH_BOUNCY);

        final DynamicAnimation.OnAnimationUpdateListener mockUpdateListener = mock(
                DynamicAnimation.OnAnimationUpdateListener.class);
        final DynamicAnimation.OnAnimationEndListener mockEndListener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        final DynamicAnimation.OnAnimationUpdateListener updateListener =
                new DynamicAnimation.OnAnimationUpdateListener() {

            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                assertTrue(value >= 0.0f);
                assertTrue(value <= 1.0f);
            }
        };

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.setStartValue(1.0f).setStartVelocity(8000f)
                        .addEndListener(mockEndListener).addUpdateListener(mockUpdateListener)
                        .addUpdateListener(updateListener).start();
            }});

        verify(mockEndListener, timeout(2000)).onAnimationEnd(anim, false, 0f, 0f);
        verify(mockUpdateListener, atLeast(2)).onAnimationUpdate(eq(anim), any(float.class),
                any(float.class));
    }

    /**
     * Verifies animateToFinalPosition works both when the anim hasn't started and when it's
     * running.
     */
    @Test
    public void testAnimateToFinalPosition() throws InterruptedException {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.SCALE_Y, 0.0f);
        final DynamicAnimation.OnAnimationEndListener mockEndListener = mock(
                DynamicAnimation.OnAnimationEndListener.class);
        anim.addEndListener(mockEndListener);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.animateToFinalPosition(0.0f);
                // Set the duration scale to 1 to avoid prematurely ending the animation.
                anim.getAnimationHandler().mDurationScale = 1.0f;
            }
        });
        assertTrue(anim.isRunning());
        Thread.sleep(100);
        assertTrue(anim.isRunning());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.animateToFinalPosition(1.0f);
                // Set the duration scale to 1 to avoid prematurely ending the animation.
                anim.getAnimationHandler().mDurationScale = 1.0f;
            }
        });

        assertTrue(anim.isRunning());
        // Verify that it indeed ends at the value from the second animateToFinalPosition() call.
        verify(mockEndListener, timeout(1500)).onAnimationEnd(anim, false, 1.0f, 0.0f);
    }

    /**
     * Verifies that skip to end will stop the animation, and skips the value to the end value.
     */
    @Test
    public void testSkipToEnd() {
        final float finalPosition = 10f;
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.SCROLL_X,
                finalPosition);
        final DynamicAnimation.OnAnimationEndListener mockListener =
                mock(DynamicAnimation.OnAnimationEndListener.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.addEndListener(mockListener).setStartValue(200).start();
            }
        });
        assertTrue(anim.isRunning());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                int scrollX = mView1.getScrollX();
                anim.skipToEnd();
                // Expect no change in the animation values until next frame.
                assertEquals(scrollX, mView1.getScrollX());
                assertTrue(anim.isRunning());
            }
        });
        verify(mockListener, timeout(100).times(1)).onAnimationEnd(anim, false, finalPosition, 0);

        // Also make sure the skipToEnd() call doesn't affect next animation run.
        final DynamicAnimation.OnAnimationEndListener mockListener2 =
                mock(DynamicAnimation.OnAnimationEndListener.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.addEndListener(mockListener2);
                anim.animateToFinalPosition(finalPosition + 1000f);
            }
        });
        // Verify that the animation doesn't finish right away
        verify(mockListener2, timeout(300).times(0)).onAnimationEnd(any(DynamicAnimation.class),
                any(boolean.class), any(float.class), any(float.class));

        // But the animation should eventually finish.
        verify(mockListener, timeout(1000).times(1)).onAnimationEnd(anim, false,
                finalPosition + 1000f, 0);

    }

    /**
     * Check that the min visible change does affect how soon spring animations end.
     */
    public void testScaleMinChange() {
        FloatValueHolder valueHolder = new FloatValueHolder(0.5f);
        final SpringAnimation anim = new SpringAnimation(valueHolder);
        DynamicAnimation.OnAnimationUpdateListener mockListener =
                mock(DynamicAnimation.OnAnimationUpdateListener.class);
        anim.addUpdateListener(mockListener);

        final DynamicAnimation.OnAnimationEndListener endListener =
                mock(DynamicAnimation.OnAnimationEndListener.class);
        anim.addEndListener(endListener);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.animateToFinalPosition(1f);
            }
        });

        verify(endListener, timeout(500)).onAnimationEnd(anim, false, 0, 0);
        verify(mockListener, atMost(5)).onAnimationUpdate(eq(anim), anyFloat(), anyFloat());

        assertEquals(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS, anim.getMinimumVisibleChange(),
                0.01f);

        // Set the right threshold and start again.
        anim.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
        anim.setStartValue(0.5f);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.animateToFinalPosition(1f);
            }
        });

        verify(endListener, timeout(2000)).onAnimationEnd(anim, false, 0, 0);
        verify(mockListener, atLeast(10)).onAnimationUpdate(eq(anim), anyFloat(), anyFloat());
    }

    /**
     * Makes sure all the properties getter works.
     */
    @FlakyTest(bugId = 190540065)
    @Test
    public void testAllProperties() {
        final DynamicAnimation.ViewProperty[] properties = {
                DynamicAnimation.ALPHA, DynamicAnimation.TRANSLATION_X,
                DynamicAnimation.TRANSLATION_Y, DynamicAnimation.TRANSLATION_Z,
                DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y, DynamicAnimation.ROTATION,
                DynamicAnimation.ROTATION_X, DynamicAnimation.ROTATION_Y,
                DynamicAnimation.X, DynamicAnimation.Y, DynamicAnimation.Z,
                DynamicAnimation.SCROLL_X, DynamicAnimation.SCROLL_Y,
        };

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mView1.setAlpha(0f);
                mView1.setTranslationX(0f);
                mView1.setTranslationY(0f);
                ViewCompat.setTranslationZ(mView1, 0f);

                mView1.setScaleX(0f);
                mView1.setScaleY(0f);

                mView1.setRotation(0f);
                mView1.setRotationX(0f);
                mView1.setRotationY(0f);

                mView1.setX(0f);
                mView1.setY(0f);
                ViewCompat.setZ(mView1, 0f);

                mView1.setScrollX(0);
                mView1.setScrollY(0);
            }
        });

        final SpringAnimation[] anims = new SpringAnimation[properties.length];
        final DynamicAnimation.OnAnimationUpdateListener[] mockListeners =
                new DynamicAnimation.OnAnimationUpdateListener[properties.length];
        for (int i = 0; i < properties.length; i++) {
            anims[i] = new SpringAnimation(mView1, properties[i], 1);
            final int finalI = i;
            anims[i].addUpdateListener(
                    new DynamicAnimation.OnAnimationUpdateListener() {
                        boolean mIsFirstFrame = true;
                        @Override
                        public void onAnimationUpdate(DynamicAnimation animation, float value,
                                float velocity) {
                            if (mIsFirstFrame) {
                                assertEquals(value, 0f, 0f);
                            }
                            mIsFirstFrame = false;
                        }
                    });
            mockListeners[i] = mock(DynamicAnimation.OnAnimationUpdateListener.class);
            anims[i].addUpdateListener(mockListeners[i]);
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                for (int i = properties.length - 1; i >= 0; i--) {
                    anims[i].start();
                }
            }
        });

        for (int i = 0; i < properties.length; i++) {
            int timeout = i == 0 ? 100 : 0;
            verify(mockListeners[i], timeout(timeout).atLeast(1)).onAnimationUpdate(
                    any(SpringAnimation.class), any(float.class), any(float.class));
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < properties.length; i++) {
                    anims[i].cancel();
                }
            }
        });
    }

    /**
     * Test start() on a test thread.
     */
    @Test
    public void testStartOnNonAnimationHandlerThread() throws InterruptedException {
        assertThrows(AndroidRuntimeException.class, () -> {
            SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.ALPHA, 0f);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                anim.setScheduler(anim.getScheduler());
            });
            runRunnableOnNewThread(() -> {
                anim.start();
            });
        });
    }

    /**
     * Test cancel() on a test thread.
     */
    @Test
    public void testCancelOnNonAnimationHandlerThread() throws InterruptedException {
        assertThrows(AndroidRuntimeException.class, () -> {
            SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.ALPHA, 0f);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                anim.setScheduler(anim.getScheduler());
            });
            runRunnableOnNewThread(() -> {
                anim.cancel();
            });
        });
    }

    /**
     * Test skipToEnd() on a test thread.
     */
    @Test
    public void testSkipToEndOnNonAnimationHandlerThread() {
        assertThrows(AndroidRuntimeException.class, () -> {
            SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.ALPHA, 0f);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                anim.setScheduler(anim.getScheduler());
            });
            runRunnableOnNewThread(() -> {
                anim.skipToEnd();
            });
        });
    }

    /**
     * Runs {@param r} on a new looper thread, and propagates any runtime exceptions thrown while
     * {@param r} is running.
     */
    private void runRunnableOnNewThread(Runnable r) throws InterruptedException, RuntimeException {
        RuntimeException[] exceptions = new RuntimeException[1];
        CountDownLatch latch = new CountDownLatch(1);
        HandlerThread t = new HandlerThread("SpringTestsThread") {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    r.run();
                } catch (RuntimeException e) {
                    exceptions[0] = e;
                }
                latch.countDown();
            }
        };
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
    }

    /**
     * Test invalid start condition: no spring position specified, final position > max value,
     * and final position < min. Expect exception in all these cases.
     */
    @Test
    public void testInvalidStartingCondition() {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.X);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Expect exception from not setting spring final position before calling start.
                try {
                    anim.start();
                    fail("No exception is thrown when calling start() from non-main thread.");
                } catch (UnsupportedOperationException e) {
                }

                // Expect exception from having a final position < min value
                try {
                    anim.setMinValue(50);
                    // Final position < min value, expect exception.
                    anim.setStartValue(50).animateToFinalPosition(40);
                    fail("No exception is thrown when spring position is less than min value.");
                } catch (UnsupportedOperationException e) {
                }

                // Expect exception from not setting spring final position before calling start.
                try {
                    anim.setMaxValue(60);
                    // Final position < min value, expect exception.
                    anim.setStartValue(60).animateToFinalPosition(70);
                    fail("No exception is thrown when spring position is greater than max value.");
                } catch (UnsupportedOperationException e) {
                }
            }
        });
    }

    /**
     * Try skipToEnd() on an undamped spring, and expect exception.
     */
    @Test
    public void testUndampedSpring() {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.Y);
        anim.setSpring(new SpringForce(10).setDampingRatio(0));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Expect exception for ending an undamped spring.
                try {
                    anim.skipToEnd();
                    fail("No exception is thrown when calling skipToEnd() on an undamped spring");
                } catch (UnsupportedOperationException e) {
                }
            }
        });

    }

    @Ignore // b/268534501
    @Test
    @SdkSuppress(minSdkVersion = 33, maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void testDurationScaleChangeListener() throws InterruptedException {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.Y, 0f);
        final CountDownLatch registerUnregisterLatch = new CountDownLatch(2);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // getAnimationHandler and AnimationHandler.getInstance() requires a looper, so run
                // on the main thread.
                AnimationHandler animHandler = anim.getAnimationHandler();
                assertEquals(1, animHandler.getDurationScale(), 0);
                assertNull(animHandler.mDurationScaleChangeListener);
                animHandler.mDurationScaleChangeListener =
                        new MyDurationScaleChangeListener(animHandler, registerUnregisterLatch);

                anim.start();
                assertEquals(0, animHandler.getDurationScale(), 0);
            }
        });

        // Wait for the animation to end on the main thread.
        assertTrue(registerUnregisterLatch.await(1000, TimeUnit.MILLISECONDS));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                AnimationHandler animHandler = anim.getAnimationHandler();
                // Remove our custom listener at the end.
                animHandler.mDurationScaleChangeListener = null;
            }
        });
    }

    @Test
    public void testCustomHandler() {
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.Y, 0f);
        MyAnimationFrameCallbackScheduler scheduler =
                new MyAnimationFrameCallbackScheduler();

        anim.setScheduler(scheduler);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.start();
            }
        });

        assertTrue(scheduler.mCallback);
        assertEquals(scheduler, anim.getScheduler());
    }

    static class MyAnimationFrameCallbackScheduler implements FrameCallbackScheduler {

        boolean mCallback;

        @Override
        public void postFrameCallback(@NonNull Runnable frameCallback) {
            mCallback = true;
        }

        @Override
        public boolean isCurrentThread() {
            return true;
        }
    }

    static class MyEndListener implements DynamicAnimation.OnAnimationEndListener {
        public long endTime = -1;

        @Override
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value,
                float velocity) {
            endTime = SystemClock.uptimeMillis();
        }
    }

    @RequiresApi(api = 33)
    class MyDurationScaleChangeListener extends AnimationHandler.DurationScaleChangeListener33 {

        final CountDownLatch mRegisterUnregisterLatch;

        MyDurationScaleChangeListener(AnimationHandler handler, CountDownLatch countDownLatch) {
            // Call super to construct an inner class
            handler.super();
            mRegisterUnregisterLatch = countDownLatch;
        }

        @Override
        public boolean register() {
            mRegisterUnregisterLatch.countDown();
            assertEquals(1, mRegisterUnregisterLatch.getCount());
            return super.register();
        }

        @Override
        public boolean unregister() {
            mRegisterUnregisterLatch.countDown();
            return super.unregister();
        }
    }
}
