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

package android.support.dynamicanimation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.os.SystemClock;
import android.support.animation.DynamicAnimation;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.dynamicanimation.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SpringTests {
    @Rule public final ActivityTestRule<AnimationActivity> mActivityTestRule;
    public View mView1;
    public View mView2;

    public SpringTests() {
        mActivityTestRule = new ActivityTestRule<>(AnimationActivity.class);
    }

    @Before
    public void setup() throws Exception {
        mView1 = mActivityTestRule.getActivity().findViewById(R.id.anim_view);
        mView2 = mActivityTestRule.getActivity().findViewById(R.id.anim_another_view);
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
                anim3.setStartValue(800).addUpdateListener(updateListener).addEndListener(l3)
                        .addEndListener(mockListener).start();
                anim1.setStartValue(800).addUpdateListener(updateListener).addEndListener(l1)
                        .start();

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
                anim2.setStartValue(360).addUpdateListener(updateListener).addEndListener(l2)
                        .addEndListener(mockListener).start();
                anim3.setStartValue(360).addEndListener(l3).start();
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
    @Test
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
                    }
                }
            });

            verify(mockListener, timeout(2000)).onAnimationEnd(springAnims[1], false, 0f, 0f);

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
            }
        });
        assertTrue(anim.isRunning());
        Thread.sleep(100);
        assertTrue(anim.isRunning());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.animateToFinalPosition(1.0f);
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
        final SpringAnimation anim = new SpringAnimation(mView1, DynamicAnimation.SCROLL_X, 0.0f);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.setStartValue(200).start();
            }
        });
        assertTrue(anim.isRunning());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                anim.skipToEnd();
            }
        });
        assertFalse(anim.isRunning());
        assertEquals(0, mView1.getScrollX());
    }

    static class MyEndListener implements DynamicAnimation.OnAnimationEndListener {
        public long endTime = -1;

        @Override
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value,
                float velocity) {
            endTime = SystemClock.uptimeMillis();
        }
    }
}
