/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v17.leanback.widget;

import android.support.v17.leanback.transition.LeanbackTransitionHelper;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;

/**
 * Helper for managing {@link android.support.v17.leanback.widget.TitleView}, including
 * transitions and focus movement.
 * Assumes the TitleView is overlayed on the topmost portion of the scene root view.
 */
public class TitleHelper {

    private ViewGroup mSceneRoot;
    private TitleView mTitleView;
    private Object mTitleUpTransition;
    private Object mTitleDownTransition;
    private Object mSceneWithTitle;
    private Object mSceneWithoutTitle;

    static TransitionHelper sTransitionHelper = TransitionHelper.getInstance();

    // When moving focus off the TitleView, this focus search listener assumes that the view that
    // should take focus comes before the TitleView in a focus search starting at the scene root.
    private final BrowseFrameLayout.OnFocusSearchListener mOnFocusSearchListener =
            new BrowseFrameLayout.OnFocusSearchListener() {
        @Override
        public View onFocusSearch(View focused, int direction) {
            if (focused != mTitleView && direction == View.FOCUS_UP) {
                return mTitleView;
            }
            final boolean isRtl = ViewCompat.getLayoutDirection(focused) ==
                    View.LAYOUT_DIRECTION_RTL;
            final int forward = isRtl ? View.FOCUS_LEFT : View.FOCUS_RIGHT;
            if (mTitleView.hasFocus() && direction == View.FOCUS_DOWN || direction == forward) {
                return mSceneRoot;
            }
            return null;
        }
    };

    public TitleHelper(ViewGroup sceneRoot, TitleView titleView) {
        if (sceneRoot == null || titleView == null) {
            throw new IllegalArgumentException("Views may not be null");
        }
        mSceneRoot = sceneRoot;
        mTitleView = titleView;
        createTransitions();
    }

    private void createTransitions() {
        mTitleUpTransition = LeanbackTransitionHelper.loadTitleOutTransition(
                mSceneRoot.getContext(), sTransitionHelper);
        mTitleDownTransition = LeanbackTransitionHelper.loadTitleInTransition(
                mSceneRoot.getContext(), sTransitionHelper);
        mSceneWithTitle = sTransitionHelper.createScene(mSceneRoot, new Runnable() {
            @Override
            public void run() {
                mTitleView.setVisibility(View.VISIBLE);
            }
        });
        mSceneWithoutTitle = sTransitionHelper.createScene(mSceneRoot, new Runnable() {
            @Override
            public void run() {
                mTitleView.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * Shows the title.
     */
    public void showTitle(boolean show) {
        if (show) {
            sTransitionHelper.runTransition(mSceneWithTitle, mTitleDownTransition);
        } else {
            sTransitionHelper.runTransition(mSceneWithoutTitle, mTitleUpTransition);
        }
    }

    /**
     * Returns the scene root ViewGroup.
     */
    public ViewGroup getSceneRoot() {
        return mSceneRoot;
    }

    /**
     * Returns the {@link TitleView}
     */
    public TitleView getTitleView() {
        return mTitleView;
    }

    /**
     * Returns a
     * {@link android.support.v17.leanback.widget.BrowseFrameLayout.OnFocusSearchListener} which
     * may be used to manage focus switching between the title view and scene root.
     */
    public BrowseFrameLayout.OnFocusSearchListener getOnFocusSearchListener() {
        return mOnFocusSearchListener;
    }
}
