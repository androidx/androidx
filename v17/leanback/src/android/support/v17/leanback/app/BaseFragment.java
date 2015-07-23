/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.transition.TransitionListener;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * @hide
 */
class BaseFragment extends BrandedFragment {

    private boolean mEntranceTransitionEnabled = false;
    private boolean mStartEntranceTransitionPending = false;
    private boolean mEntranceTransitionPreparePending = false;
    private Object mEntranceTransition;

    static TransitionHelper sTransitionHelper = TransitionHelper.getInstance();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mEntranceTransitionPreparePending) {
            mEntranceTransitionPreparePending = false;
            onEntranceTransitionPrepare();
        }
        if (mStartEntranceTransitionPending) {
            mStartEntranceTransitionPending = false;
            startEntranceTransition();
        }
    }

    /**
     * Enables entrance transition.<p>
     * Entrance transition is the standard slide-in transition that shows rows of data in
     * browse screen and details screen.
     * <p>
     * The method is ignored before LOLLIPOP (API21).
     * <p>
     * This method must be called in or
     * before onCreate().  Typically entrance transition should be enabled when savedInstance is
     * null so that fragment restored from instanceState does not run an extra entrance transition.
     * When the entrance transition is enabled, the fragment will make headers and content
     * hidden initially.
     * When data of rows are ready, app must call {@link #startEntranceTransition()} to kick off
     * the transition, otherwise the rows will be invisible forever.
     * <p>
     * It is similar to android:windowsEnterTransition and can be considered a late-executed
     * android:windowsEnterTransition controlled by app.  There are two reasons that app needs it:
     * <li> Workaround the problem that activity transition is not available between launcher and
     * app.  Browse activity must programmatically start the slide-in transition.</li>
     * <li> Separates DetailsOverviewRow transition from other rows transition.  So that
     * the DetailsOverviewRow transition can be executed earlier without waiting for all rows
     * to be loaded.</li>
     * <p>
     * Transition object is returned by createEntranceTransition().  Typically the app does not need
     * override the default transition that browse and details provides.
     */
    public void prepareEntranceTransition() {
        if (TransitionHelper.systemSupportsEntranceTransitions()) {
            mEntranceTransitionEnabled = true;
            if (getView() == null) {
                mEntranceTransitionPreparePending = true;
                return;
            }
            onEntranceTransitionPrepare();
        }
    }

    /**
     * Return true if entrance transition is enabled and not started yet.
     * Entrance transition can only be executed once and isEntranceTransitionEnabled()
     * is reset to false after entrance transition is started.
     */
    boolean isEntranceTransitionEnabled() {
        return mEntranceTransitionEnabled;
    }

    /**
     * Create entrance transition.  Subclass can override to load transition from
     * resource or construct manually.  Typically app does not need to
     * override the default transition that browse and details provides.
     */
    protected Object createEntranceTransition() {
        return null;
    }

    /**
     * Run entrance transition.  Subclass may use TransitionManager to perform
     * go(Scene) or beginDelayedTransition().  App should not override the default
     * implementation of browse and details fragment.
     */
    protected void runEntranceTransition(Object entranceTransition) {
    }

    /**
     * Callback when entrance transition is prepared.  This is when fragment should
     * stop user input and animations.
     */
    protected void onEntranceTransitionPrepare() {
    }

    /**
     * Callback when entrance transition is started.  This is when fragment should
     * stop processing layout.
     */
    protected void onEntranceTransitionStart() {
    }

    /**
     * Callback when entrance transition is ended.
     */
    protected void onEntranceTransitionEnd() {
    }

    /**
     * When fragment finishes loading data, it should call startEntranceTransition()
     * to execute the entrance transition.
     * startEntranceTransition() will start transition only if both two conditions
     * are satisfied:
     * <li> prepareEntranceTransition() was called.</li>
     * <li> has not executed entrance transition yet.</li>
     * <p>
     * If startEntranceTransition() is called before onViewCreated(), it will be pending
     * and executed when view is created.
     */
    public void startEntranceTransition() {
        if (!mEntranceTransitionEnabled || mEntranceTransition != null) {
            return;
        }
        // if view is not created yet, delay until onViewCreated()
        if (getView() == null) {
            mStartEntranceTransitionPending = true;
            return;
        }
        if (mEntranceTransitionPreparePending) {
            mEntranceTransitionPreparePending = false;
            onEntranceTransitionPrepare();
        }
        // wait till views get their initial position before start transition
        final View view = getView();
        view.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                internalCreateEntranceTransition();
                mEntranceTransitionEnabled = false;
                if (mEntranceTransition != null) {
                    onEntranceTransitionStart();
                    runEntranceTransition(mEntranceTransition);
                }
                return false;
            }
        });
        view.invalidate();
    }

    void internalCreateEntranceTransition() {
        mEntranceTransition = createEntranceTransition();
        if (mEntranceTransition == null) {
            return;
        }
        sTransitionHelper.setTransitionListener(mEntranceTransition, new TransitionListener() {
            @Override
            public void onTransitionEnd(Object transition) {
                mEntranceTransition = null;
                onEntranceTransitionEnd();
            }
        });
    }
}
