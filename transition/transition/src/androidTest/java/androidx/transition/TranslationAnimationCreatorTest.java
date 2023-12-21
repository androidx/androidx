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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

@LargeTest
public class TranslationAnimationCreatorTest extends BaseTest {

    @Test
    @UiThreadTest
    public void testTranslation() {
        View view = new View(rule.getActivity());
        Transition transition = mock(Transition.class);
        TransitionValues values = new TransitionValues(view);

        ArgumentCaptor<Transition.TransitionListener> listenerCaptor =
                ArgumentCaptor.forClass(Transition.TransitionListener.class);

        Animator animator = TranslationAnimationCreator.createAnimation(view, values,
                0, 0, 10, 10, 20, 20,
                new LinearInterpolator(), transition);

        animator.start();
        animator.end();
        // verify that onAnimationEnd doesn't reset translation to the initial one
        assertEquals(20, view.getTranslationX(), 0.01);

        verify(transition).addListener(listenerCaptor.capture());
        listenerCaptor.getValue().onTransitionEnd(transition, false);
        // but onTransitionEnd does
        assertEquals(0, view.getTranslationX(), 0.01);
    }

}
