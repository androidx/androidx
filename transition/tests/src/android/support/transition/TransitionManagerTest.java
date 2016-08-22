/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.transition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.transition.test.R;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
public class TransitionManagerTest extends BaseTest {

    private Scene[] mScenes = new Scene[2];

    @Before
    public void prepareScenes() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        mScenes[0] = Scene.getSceneForLayout(root, R.layout.scene0, activity);
        mScenes[1] = Scene.getSceneForLayout(root, R.layout.scene1, activity);
    }

    @Test
    public void testSetup() {
        assertThat(mScenes[0], is(notNullValue()));
        assertThat(mScenes[1], is(notNullValue()));
    }

    @Test
    @UiThreadTest
    public void testGo_enterAction() {
        CheckCalledRunnable runnable = new CheckCalledRunnable();
        mScenes[0].setEnterAction(runnable);
        assertThat(runnable.wasCalled(), is(false));
        TransitionManager.go(mScenes[0]);
        assertThat(runnable.wasCalled(), is(true));
    }

    @Test
    public void testGo_exitAction() {
        final CheckCalledRunnable enter = new CheckCalledRunnable();
        final CheckCalledRunnable exit = new CheckCalledRunnable();
        mScenes[0].setEnterAction(enter);
        mScenes[0].setExitAction(exit);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                assertThat(enter.wasCalled(), is(false));
                assertThat(exit.wasCalled(), is(false));
                TransitionManager.go(mScenes[0]);
                assertThat(enter.wasCalled(), is(true));
                assertThat(exit.wasCalled(), is(false));
            }
        });
        // Let the main thread catch up with the scene change
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TransitionManager.go(mScenes[1]);
                assertThat(exit.wasCalled(), is(true));
            }
        });
    }

    @Test
    public void testGo_transitionListenerStart() {
        final SyncTransitionListener listener
                = new SyncTransitionListener(SyncTransitionListener.EVENT_START);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Transition transition = new AutoTransition();
                transition.setDuration(0);
                assertThat(transition.addListener(listener), is(sameInstance(transition)));
                TransitionManager.go(mScenes[0], transition);
            }
        });
        assertThat("Timed out waiting for the TransitionListener",
                listener.await(), is(true));
    }

    @Test
    public void testGo_transitionListenerEnd() {
        final SyncTransitionListener listener
                = new SyncTransitionListener(SyncTransitionListener.EVENT_END);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Transition transition = new AutoTransition();
                transition.setDuration(0);
                assertThat(transition.addListener(listener), is(sameInstance(transition)));
                TransitionManager.go(mScenes[0], transition);
            }
        });
        assertThat("Timed out waiting for the TransitionListener",
                listener.await(), is(true));
    }

    /**
     * This {@link Transition.TransitionListener} synchronously waits for the specified callback.
     */
    private static class SyncTransitionListener implements Transition.TransitionListener {

        static final int EVENT_START = 1;
        static final int EVENT_END = 2;
        static final int EVENT_CANCEL = 3;
        static final int EVENT_PAUSE = 4;
        static final int EVENT_RESUME = 5;

        private final int mTargetEvent;
        private final CountDownLatch mLatch = new CountDownLatch(1);

        SyncTransitionListener(int event) {
            mTargetEvent = event;
        }

        boolean await() {
            try {
                return mLatch.await(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        public void onTransitionStart(Transition transition) {
            if (mTargetEvent == EVENT_START) {
                mLatch.countDown();
            }
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            if (mTargetEvent == EVENT_END) {
                mLatch.countDown();
            }
        }

        @Override
        public void onTransitionCancel(Transition transition) {
            if (mTargetEvent == EVENT_CANCEL) {
                mLatch.countDown();
            }
        }

        @Override
        public void onTransitionPause(Transition transition) {
            if (mTargetEvent == EVENT_PAUSE) {
                mLatch.countDown();
            }
        }

        @Override
        public void onTransitionResume(Transition transition) {
            if (mTargetEvent == EVENT_RESUME) {
                mLatch.countDown();
            }
        }
    }

}
