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

package androidx.transition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.AnimationDurationScaleRule;
import androidx.transition.test.R;

import org.junit.Before;
import org.junit.Rule;

import java.util.ArrayList;

public abstract class BaseTransitionTest extends BaseTest {

    ArrayList<View> mTransitionTargets = new ArrayList<>();
    LinearLayout mRoot;
    Transition mTransition;
    Transition.TransitionListener mListener;
    float mAnimatedValue;

    @Rule
    public final AnimationDurationScaleRule mAnimationDurationScaleRule =
            AnimationDurationScaleRule.createForAllTests(1f);

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
        mRoot = (LinearLayout) rule.getActivity().findViewById(R.id.root);
        mTransitionTargets.clear();
        mTransition = createTransition();
        mListener = spy(new TransitionListenerAdapter());
        mTransition.addListener(mListener);
    }

    Transition createTransition() {
        return new TestTransition();
    }

    void waitForStart() {
        verify(mListener, timeout(3000)).onTransitionStart(any(Transition.class));
    }

    void waitForEnd() {
        verify(mListener, timeout(3000)).onTransitionEnd(any(Transition.class));
    }

    Scene loadScene(final int layoutId) throws Throwable {
        final Scene[] scene = new Scene[1];
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scene[0] = Scene.getSceneForLayout(mRoot, layoutId, rule.getActivity());
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return scene[0];
    }

    void startTransition(final int layoutId) throws Throwable {
        startTransition(loadScene(layoutId));
    }

    void startTransition(final Scene scene) throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.go(scene, mTransition);
            }
        });
        waitForStart();
    }

    void enterScene(final int layoutId) throws Throwable {
        enterScene(loadScene(layoutId));
    }

    void enterScene(final Scene scene) throws Throwable {
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scene.enter();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    void resetListener() {
        mTransition.removeListener(mListener);
        mListener = spy(new TransitionListenerAdapter());
        mTransition.addListener(mListener);
    }

    void setAnimatedValue(float animatedValue) {
        mAnimatedValue = animatedValue;
    }

    public class TestTransition extends Visibility {

        @Override
        public Animator onAppear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                TransitionValues startValues, TransitionValues endValues) {
            mTransitionTargets.add(endValues.view);
            return ObjectAnimator.ofFloat(BaseTransitionTest.this, "animatedValue", 0, 1);
        }

        @Override
        public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull View view,
                TransitionValues startValues, TransitionValues endValues) {
            mTransitionTargets.add(startValues.view);
            return ObjectAnimator.ofFloat(BaseTransitionTest.this, "animatedValue", 1, 0);
        }

    }

}
