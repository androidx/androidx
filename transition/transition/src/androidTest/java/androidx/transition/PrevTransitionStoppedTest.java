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

package androidx.transition;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.filters.MediumTest;
import androidx.transition.test.R;

import org.junit.Test;

@MediumTest
public class PrevTransitionStoppedTest extends BaseTransitionTest {

    private Animator mAnimator;

    @Override
    Transition createTransition() {
        return new Visibility() {
            @Override
            public Animator onDisappear(ViewGroup sceneRoot, View view,
                    TransitionValues startValues, TransitionValues endValues) {
                mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                return mAnimator;
            }
        };
    }

    @Test
    public void testPrevTransitionStopped() throws Throwable {
        final Scene[] scenes = new Scene[2];
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup root = rule.getActivity().getRoot();
                LayoutInflater inflater = LayoutInflater.from(rule.getActivity());
                scenes[0] = new Scene(root, inflater.inflate(R.layout.scene11, root, false));
                scenes[1] = new Scene(root, inflater.inflate(R.layout.scene13, root, false));
                TransitionManager.go(scenes[0], null);
                TransitionManager.go(scenes[1], mTransition);
            }
        });
        waitForStart();
        assertNotNull(mAnimator);
        assertTrue(mAnimator.isStarted());

        Animator.AnimatorListener animListener = mock(Animator.AnimatorListener.class);
        mAnimator.addListener(animListener);

        resetListener();
        startTransition(scenes[0]);
        verify(animListener).onAnimationCancel(mAnimator);
    }

}
