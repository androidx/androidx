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
package androidx.fragment.app;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.os.Bundle;
import android.os.SystemClock;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment that has transitions that can be tracked.
 */
public class TransitionFragment extends StrictViewFragment {
    public final TrackingVisibility enterTransition = new TrackingVisibility();
    public final TrackingVisibility reenterTransition = new TrackingVisibility();
    public final TrackingVisibility exitTransition = new TrackingVisibility();
    public final TrackingVisibility returnTransition = new TrackingVisibility();
    public final TrackingTransition sharedElementEnter = new TrackingTransition();
    public final TrackingTransition sharedElementReturn = new TrackingTransition();

    private Transition.TransitionListener mListener = mock(Transition.TransitionListener.class);

    public TransitionFragment() {
        setEnterTransition(enterTransition);
        setReenterTransition(reenterTransition);
        setExitTransition(exitTransition);
        setReturnTransition(returnTransition);
        setSharedElementEnterTransition(sharedElementEnter);
        setSharedElementReturnTransition(sharedElementReturn);
        enterTransition.addListener(mListener);
        sharedElementEnter.addListener(mListener);
        reenterTransition.addListener(mListener);
        exitTransition.addListener(mListener);
        returnTransition.addListener(mListener);
        sharedElementReturn.addListener(mListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        checkGetActivity();
        checkState("onCreateView", CREATED);
        mOnCreateViewCalled = true;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    void waitForTransition() throws InterruptedException {
        verify(mListener, CtsMockitoUtils.within(300)).onTransitionEnd((Transition) any());
        reset(mListener);
    }

    void waitForNoTransition() throws InterruptedException {
        SystemClock.sleep(250);
        verify(mListener, never()).onTransitionStart((Transition) any());
    }
}
