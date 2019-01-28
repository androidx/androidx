/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.animation;

import static androidx.animation.AnimatorSetTest.AnimEvent.EventType.END;
import static androidx.animation.AnimatorSetTest.AnimEvent.EventType.START;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AnimatorSetTest {
    private AnimatorSet mAnimatorSet;
    private float mPreviousDurationScale = 1.0f;
    private ValueAnimator mAnim1;
    private ValueAnimator mAnim2;
    private static final float EPSILON = 0.001f;
    enum PlayOrder {
        SEQUENTIAL,
        TOGETHER
    }

    @Before
    public void setup() {
        mPreviousDurationScale = ValueAnimator.getDurationScale();
        ValueAnimator.setDurationScale(1.0f);
        mAnim1 = ValueAnimator.ofFloat(0f, 100f);
        mAnim2 = ValueAnimator.ofInt(100, 200);
    }

    @After
    public void tearDown() {
        ValueAnimator.setDurationScale(mPreviousDurationScale);
    }

    @ClassRule
    public static AnimationTestRule sTestAnimationHandler = new AnimationTestRule();

    @UiThreadTest
    @Test
    public void testPlaySequentially() {

        List<Animator> animators = new ArrayList<Animator>();
        animators.add(mAnim1);
        animators.add(mAnim2);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playSequentially(animators);
        verifyPlayOrder(mAnimatorSet, new Animator[] {mAnim1, mAnim2}, PlayOrder.SEQUENTIAL);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(mAnim1, mAnim2);
        verifyPlayOrder(set, new Animator[] {mAnim1, mAnim2}, PlayOrder.SEQUENTIAL);
    }

    @UiThreadTest
    @Test
    public void testPlayTogether() {

        List<Animator> animators = new ArrayList<Animator>();
        animators.add(mAnim1);
        animators.add(mAnim2);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
        verifyPlayOrder(mAnimatorSet, new Animator[] {mAnim1, mAnim2}, PlayOrder.TOGETHER);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(mAnim1, mAnim2);
        verifyPlayOrder(set, new Animator[] {mAnim1, mAnim2}, PlayOrder.TOGETHER);
    }

    /**
     * Start the animator, and verify the animators are played sequentially in the order that is
     * defined in the array.
     *
     * @param set AnimatorSet to be started and verified
     * @param animators animators that we put in the AnimatorSet, in the order that they'll play
     */
    private void verifyPlayOrder(final AnimatorSet set, Animator[] animators, PlayOrder playOrder) {
        ArrayList<AnimEvent> animEvents = registerAnimatorsForEvents(animators);

        set.start();

        sTestAnimationHandler.advanceTimeBy(set.getStartDelay());
        for (int i = 0; i < animators.length; i++) {
            sTestAnimationHandler.advanceTimeBy(animators[i].getTotalDuration());
        }

        // All animations should finish by now
        int animatorNum = animators.length;
        assertEquals(animatorNum * 2, animEvents.size());

        if (playOrder == PlayOrder.SEQUENTIAL) {
            for (int i = 0; i < animatorNum; i++) {
                assertEquals(START, animEvents.get(i * 2).mType);
                assertEquals(animators[i], animEvents.get(i * 2).mAnim);
                assertEquals(END, animEvents.get(i * 2 + 1).mType);
                assertEquals(animators[i], animEvents.get(i * 2 + 1).mAnim);
            }
        } else {
            for (int i = 0; i < animatorNum; i++) {
                assertEquals(START, animEvents.get(i).mType);
                assertEquals(animators[i], animEvents.get(i).mAnim);
            }
        }
    }

    private ArrayList<AnimEvent> registerAnimatorsForEvents(Animator[] animators) {
        final ArrayList<AnimEvent> animEvents = new ArrayList<>();
        Animator.AnimatorListener listener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                animEvents.add(new AnimEvent(START, animation));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animEvents.add(new AnimEvent(END, animation));
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        for (int i = 0; i < animators.length; i++) {
            animators[i].removeListener(listener);
            animators[i].addListener(listener);
        }
        return animEvents;
    }

    @UiThreadTest
    @Test
    public void testPlayBeforeAfter() {

        final ValueAnimator anim3 = ValueAnimator.ofFloat(200f, 300f);

        AnimatorSet set = new AnimatorSet();
        set.play(mAnim1).before(mAnim2).after(anim3);

        verifyPlayOrder(set, new Animator[] {anim3, mAnim1, mAnim2}, PlayOrder.SEQUENTIAL);
    }

    @UiThreadTest
    @Test
    public void testListenerCallbackOnEmptySet() {
        // Create an AnimatorSet that only contains one empty AnimatorSet, and checks the callback
        // sequence by checking the time stamps of the callbacks.
        final AnimatorSet emptySet = new AnimatorSet();
        final AnimatorSet set = new AnimatorSet();
        set.play(emptySet);


        ArrayList<AnimEvent> animEvents = registerAnimatorsForEvents(
                new Animator[] {emptySet, set});
        set.start();
        sTestAnimationHandler.advanceTimeBy(10);

        // Check callback sequence via Animator Events
        assertEquals(animEvents.get(0).mAnim, set);
        assertEquals(animEvents.get(0).mType, START);
        assertEquals(animEvents.get(1).mAnim, emptySet);
        assertEquals(animEvents.get(1).mType, START);

        assertEquals(animEvents.get(2).mAnim, emptySet);
        assertEquals(animEvents.get(2).mType, END);
        assertEquals(animEvents.get(3).mAnim, set);
        assertEquals(animEvents.get(3).mType, END);
    }

    @UiThreadTest
    @Test
    public void testPauseAndResume() {
        final AnimatorSet set = new AnimatorSet();
        set.playTogether(mAnim1, mAnim2);

        set.start();
        sTestAnimationHandler.advanceTimeBy(0);
        set.pause();
        assertTrue(set.isPaused());
        sTestAnimationHandler.advanceTimeBy(5);

        // After 10s, set is still not yet finished.
        sTestAnimationHandler.advanceTimeBy(10000);
        assertTrue(set.isStarted());
        assertTrue(set.isPaused());
        set.resume();

        sTestAnimationHandler.advanceTimeBy(5);
        assertTrue(set.isStarted());
        assertFalse(set.isPaused());

        sTestAnimationHandler.advanceTimeBy(10000);
        assertFalse(set.isStarted());
        assertFalse(set.isPaused());
    }

    @UiThreadTest
    @Test
    public void testPauseBeforeStart() {
        final AnimatorSet set = new AnimatorSet();
        set.playSequentially(mAnim1, mAnim2);

        set.pause();
        // Verify that pause should have no effect on a not-yet-started animator.
        assertFalse(set.isPaused());
        set.start();
        sTestAnimationHandler.advanceTimeBy(0);
        assertTrue(set.isStarted());

        sTestAnimationHandler.advanceTimeBy(set.getTotalDuration());
        assertFalse(set.isStarted());
    }

    @UiThreadTest
    @Test
    public void testSeekAfterPause() {
        final AnimatorSet set = new AnimatorSet();
        ValueAnimator a1 = ValueAnimator.ofFloat(0f, 50f);
        a1.setDuration(50);
        ValueAnimator a2 = ValueAnimator.ofFloat(50, 100f);
        a2.setDuration(50);
        set.playSequentially(a1, a2);
        set.setInterpolator(new LinearInterpolator());

        set.start();
        set.pause();
        set.setCurrentPlayTime(60);
        assertEquals((long) set.getCurrentPlayTime(), 60);
        assertEquals((float) a1.getAnimatedValue(), 50f, EPSILON);
        assertEquals((float) a2.getAnimatedValue(), 60f, EPSILON);

        set.setCurrentPlayTime(40);
        assertEquals((long) set.getCurrentPlayTime(), 40);
        assertEquals((float) a1.getAnimatedValue(), 40f, EPSILON);
        assertEquals((float) a2.getAnimatedValue(), 50f, EPSILON);

        set.cancel();
    }

    @UiThreadTest
    @Test
    public void testDuration() {
        mAnim1.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {mAnim1, mAnim2};

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setDuration(1000);

        assertEquals(mAnimatorSet.getDuration(), 1000);
    }

    @UiThreadTest
    @Test
    public void testStartDelay() {
        mAnim1.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {mAnim1, mAnim2};

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setStartDelay(10);

        assertEquals(mAnimatorSet.getStartDelay(), 10);
    }

    /**
     * This test sets up an AnimatorSet with start delay. One of the child animators also has
     * start delay. We then verify that start delay was handled correctly on both AnimatorSet
     * and individual animator level.
     */
    @UiThreadTest
    @Test
    public void testReverseWithStartDelay() {
        ValueAnimator a1 = ValueAnimator.ofFloat(0f, 1f);
        a1.setDuration(200);

        ValueAnimator a2 = ValueAnimator.ofFloat(1f, 2f);
        a2.setDuration(200);
        // Set start delay on a2 so that the delay is passed 100ms after a1 is finished.
        a2.setStartDelay(300);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(a1, a2);
        set.setStartDelay(1000);

        set.reverse();
        sTestAnimationHandler.advanceTimeBy(0);
        assertTrue(a2.isStarted());
        assertTrue(a2.isRunning());
        assertFalse(a1.isStarted());

        // a2 should finish 200ms after reverse started
        sTestAnimationHandler.advanceTimeBy(200);
        assertFalse(a2.isStarted());
        // By the time a2 finishes reversing, a1 should not have started.
        assertFalse(a1.isStarted());

        sTestAnimationHandler.advanceTimeBy(100);
        assertTrue(a1.isStarted());

        // a1 finishes within 200ms after starting
        sTestAnimationHandler.advanceTimeBy(200);
        assertFalse(a1.isStarted());
        assertFalse(set.isStarted());

        set.cancel();
    }

    /**
     * Test that duration scale is handled correctly in the AnimatorSet.
     */
    @UiThreadTest
    @Test
    public void testZeroDurationScale() {
        ValueAnimator.setDurationScale(0);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(mAnim1, mAnim2);
        set.setStartDelay(1000);

        set.start();
        assertFalse(set.isStarted());
    }

    /**
     * Test that non-zero duration scale is handled correctly in the AnimatorSet.
     */
    @UiThreadTest
    @Test
    public void testDurationScale() {
        // Change the duration scale to 3
        ValueAnimator.setDurationScale(3f);

        ValueAnimator a1 = ValueAnimator.ofFloat(0f, 1f);
        a1.setDuration(100);

        ValueAnimator a2 = ValueAnimator.ofFloat(1f, 2f);
        a2.setDuration(100);
        // Set start delay on a2 so that the delay is passed 100ms after a1 is finished.
        a2.setStartDelay(200);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(a1, a2);
        set.setStartDelay(200);

        // Sleep for part of the start delay and check that no child animator has started, to verify
        // that the duration scale has been properly scaled.
        set.start();
        sTestAnimationHandler.advanceTimeBy(0);
        assertFalse(set.isRunning());
        // start delay of the set should be scaled to 600ms
        sTestAnimationHandler.advanceTimeBy(550);
        assertFalse(set.isRunning());

        sTestAnimationHandler.advanceTimeBy(50);
        assertTrue(set.isRunning());
        assertTrue(a1.isStarted());
        assertFalse(a2.isStarted());

        // Verify that a1 finish in 300ms (3x its defined duration)
        sTestAnimationHandler.advanceTimeBy(300);
        assertFalse(a1.isStarted());
        assertTrue(a2.isStarted());
        assertFalse(a2.isRunning());

        // a2 should finish the delay stage now
        sTestAnimationHandler.advanceTimeBy(600);
        assertTrue(a2.isStarted());
        assertTrue(a2.isRunning());

        sTestAnimationHandler.advanceTimeBy(300);
        assertFalse(a2.isStarted());
        assertFalse(a2.isRunning());
        assertFalse(set.isStarted());
    }

    @UiThreadTest
    @Test
    public void testClone() {
        final AnimatorSet set1 = new AnimatorSet();
        final AnimatorListenerAdapter setListener = new AnimatorListenerAdapter() {};
        set1.addListener(setListener);

        ObjectAnimator animator1 = new ObjectAnimator();
        animator1.setDuration(100);
        animator1.setPropertyName("x");
        animator1.setIntValues(5);
        animator1.setInterpolator(new LinearInterpolator());
        AnimatorListenerAdapter listener1 = new AnimatorListenerAdapter(){};
        AnimatorListenerAdapter listener2 = new AnimatorListenerAdapter(){};
        animator1.addListener(listener1);

        ObjectAnimator animator2 = new ObjectAnimator();
        animator2.setDuration(100);
        animator2.setInterpolator(new LinearInterpolator());
        animator2.addListener(listener2);
        animator2.setPropertyName("y");
        animator2.setIntValues(10);

        set1.playTogether(animator1, animator2);

        class AnimateObject {
            public int x = 1;
            public int y = 2;
            public void setX(int val) {
                x = val;
            }

            public void setY(int val) {
                y = val;
            }
        }
        set1.setTarget(new AnimateObject());

        set1.start();
        assertTrue(set1.isStarted());

        animator1.getListeners();
        AnimatorSet set2 = set1.clone();
        assertFalse(set2.isStarted());

        assertEquals(2, set2.getChildAnimations().size());

        Animator clone1 = set2.getChildAnimations().get(0);
        Animator clone2 = set2.getChildAnimations().get(1);

        assertTrue(clone1.getListeners().contains(listener1));
        assertTrue(clone2.getListeners().contains(listener2));

        assertTrue(set2.getListeners().contains(setListener));

        for (Animator.AnimatorListener listener : set1.getListeners()) {
            assertTrue(set2.getListeners().contains(listener));
        }

        assertEquals(animator1.getDuration(), clone1.getDuration());
        assertEquals(animator2.getDuration(), clone2.getDuration());
        assertSame(animator1.getInterpolator(), clone1.getInterpolator());
        assertSame(animator2.getInterpolator(), clone2.getInterpolator());
        set1.cancel();
    }

    /**
     * Testing seeking in an AnimatorSet containing sequential animators.
     */
    @UiThreadTest
    @Test
    public void testSeeking() {
        final AnimatorSet set = new AnimatorSet();
        final ValueAnimator a1 = ValueAnimator.ofFloat(0f, 150f);
        a1.setDuration(150);
        final ValueAnimator a2 = ValueAnimator.ofFloat(150f, 250f);
        a2.setDuration(100);
        final ValueAnimator a3 = ValueAnimator.ofFloat(250f, 300f);
        a3.setDuration(50);

        a1.setInterpolator(null);
        a2.setInterpolator(null);
        a3.setInterpolator(null);

        set.playSequentially(a1, a2, a3);

        set.setCurrentPlayTime(100);
        assertEquals(100f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(150f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(250f, (Float) a3.getAnimatedValue(), EPSILON);

        set.setCurrentPlayTime(280);
        assertEquals(150f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(250f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(280f, (Float) a3.getAnimatedValue(), EPSILON);

        set.start();
        sTestAnimationHandler.advanceTimeBy(0);
        assertEquals(280, set.getCurrentPlayTime());
        assertTrue(set.isRunning());
        sTestAnimationHandler.advanceTimeBy(20);
        assertFalse(set.isStarted());

        // Seek after a run to the middle-ish, and verify the first animator is at the end
        // value and the 3rd at beginning value, and the 2nd animator is at the seeked value.
        set.setCurrentPlayTime(200);
        assertEquals(150f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(200f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(250f, (Float) a3.getAnimatedValue(), EPSILON);
    }

    /**
     * Testing seeking in an AnimatorSet containing infinite animators.
     */
    @Test
    public void testSeekingInfinite() {
        final AnimatorSet set = new AnimatorSet();
        final ValueAnimator a1 = ValueAnimator.ofFloat(0f, 100f);
        a1.setDuration(100);
        final ValueAnimator a2 = ValueAnimator.ofFloat(100f, 200f);
        a2.setDuration(100);
        a2.setRepeatCount(ValueAnimator.INFINITE);
        a2.setRepeatMode(ValueAnimator.RESTART);

        final ValueAnimator a3 = ValueAnimator.ofFloat(100f, 200f);
        a3.setDuration(100);
        a3.setRepeatCount(ValueAnimator.INFINITE);
        a3.setRepeatMode(ValueAnimator.REVERSE);

        a1.setInterpolator(null);
        a2.setInterpolator(null);
        a3.setInterpolator(null);
        set.play(a1).before(a2);
        set.play(a1).before(a3);

        set.setCurrentPlayTime(50);
        assertEquals(50f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(100f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(100f, (Float) a3.getAnimatedValue(), EPSILON);

        set.setCurrentPlayTime(100);
        assertEquals(100f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(100f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(100f, (Float) a3.getAnimatedValue(), EPSILON);

        // Seek to the 1st iteration of the infinite repeat animators, and they should have the
        // same value.
        set.setCurrentPlayTime(180);
        assertEquals(100f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(180f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(180f, (Float) a3.getAnimatedValue(), EPSILON);

        // Seek to the 2nd iteration of the infinite repeat animators, and they should have
        // different values as they have different repeat mode.
        set.setCurrentPlayTime(280);
        assertEquals(100f, (Float) a1.getAnimatedValue(), EPSILON);
        assertEquals(180f, (Float) a2.getAnimatedValue(), EPSILON);
        assertEquals(120f, (Float) a3.getAnimatedValue(), EPSILON);

    }

    @UiThreadTest
    @Test
    public void testNotifiesAfterEnd() {
        final ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        class TestListener extends AnimatorListenerAdapter {
            public boolean startIsCalled = false;
            public boolean endIsCalled = false;
            @Override
            public void onAnimationStart(Animator animation) {
                assertTrue(animation.isStarted());
                assertTrue(animation.isRunning());
                startIsCalled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                assertFalse(animation.isRunning());
                assertFalse(animation.isStarted());
                super.onAnimationEnd(animation);
                endIsCalled = true;
            }
        }
        TestListener listener = new TestListener();
        animator.addListener(listener);

        TestListener setListener = new TestListener();
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator);
        animatorSet.addListener(setListener);

        animatorSet.start();
        animator.end();
        assertFalse(animator.isStarted());

        assertTrue(listener.startIsCalled);
        assertTrue(listener.endIsCalled);
        assertTrue(setListener.startIsCalled);
    }

    /**
     *
     * This test verifies that custom ValueAnimators will be start()'ed in a set.
     */
    @UiThreadTest
    @Test
    public void testChildAnimatorStartCalled() {
        class StartListener extends AnimatorListenerAdapter {
            public boolean mStartCalled = false;
            @Override
            public void onAnimationStart(Animator anim) {
                mStartCalled = true;
            }
        }

        StartListener l1 = new StartListener();
        StartListener l2 = new StartListener();
        mAnim1.addListener(l1);
        mAnim2.addListener(l2);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(mAnim1, mAnim2);
        set.start();
        assertTrue(l1.mStartCalled);
        assertTrue(l2.mStartCalled);
    }

    /**
     * This test sets up an AnimatorSet that contains two sequential animations. The first animation
     * is infinite, the second animation therefore has an infinite start time. This test verifies
     * that the infinite start time is handled correctly.
     */
    @UiThreadTest
    @Test
    public void testInfiniteStartTime() {
        ValueAnimator a1 = ValueAnimator.ofFloat(0f, 1f);
        a1.setRepeatCount(ValueAnimator.INFINITE);
        ValueAnimator a2 = ValueAnimator.ofFloat(0f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(a1, a2);

        set.start();

        assertEquals(Animator.DURATION_INFINITE, set.getTotalDuration());

        set.end();
    }

    /**
     * This test sets up 10 animators playing together. We expect the start time for all animators
     * to be the same.
     */
    @UiThreadTest
    @Test
    public void testMultipleAnimatorsPlayTogether() {
        Animator[] animators = new Animator[10];
        for (int i = 0; i < 10; i++) {
            animators[i] = ValueAnimator.ofFloat(0f, 1f);
            animators[i].setDuration(100 + i * 100);
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.setStartDelay(80);

        set.start();
        sTestAnimationHandler.advanceTimeBy(0);
        assertTrue(set.isStarted());
        assertFalse(set.isRunning());
        sTestAnimationHandler.advanceTimeBy(80);
        for (int i = 0; i < 10; i++) {
            assertTrue(animators[i].isRunning());
            sTestAnimationHandler.advanceTimeBy(100);
            assertFalse(animators[i].isStarted());
        }

        // The set should finish by now.
        assertFalse(set.isStarted());
    }

    @UiThreadTest
    @Test
    public void testGetChildAnimations() {
        Animator[] animatorArray = {mAnim1, mAnim2};

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.getChildAnimations();
        assertEquals(0, mAnimatorSet.getChildAnimations().size());
        mAnimatorSet.playSequentially(animatorArray);
        assertEquals(2, mAnimatorSet.getChildAnimations().size());
    }

    /**
     *
     */
    @UiThreadTest
    @Test
    public void testSetInterpolator() {
        mAnim1.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {mAnim1, mAnim2};

        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setInterpolator(interpolator);

        assertFalse(mAnimatorSet.isRunning());
        mAnimatorSet.start();

        ArrayList<Animator> animatorList = mAnimatorSet.getChildAnimations();
        assertEquals(interpolator, animatorList.get(0).getInterpolator());
        assertEquals(interpolator, animatorList.get(1).getInterpolator());
        mAnimatorSet.cancel();
    }


    static class AnimEvent {
        enum EventType {
            START,
            END
        }
        final EventType mType;
        final Animator mAnim;
        AnimEvent(EventType type, Animator anim) {
            mType = type;
            mAnim = anim;
        }
    }
}
