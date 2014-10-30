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
package android.support.v17.leanback.widget;

import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewCompat;
import android.support.v17.leanback.R;
import android.support.v17.leanback.transition.TransitionListener;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter.ViewHolder;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.util.List;

final class DetailsOverviewSharedElementHelper extends SharedElementCallback {

    private static final String TAG = "DetailsOverviewSharedElementHelper";
    private static final boolean DEBUG = false;

    private ViewHolder mViewHolder;
    private Activity mActivityToRunTransition;
    private boolean mStartedPostpone;
    private String mSharedElementName;
    private int mRightPanelWidth;
    private int mRightPanelHeight;

    private ScaleType mSavedScaleType;
    private Matrix mSavedMatrix;

    private boolean hasImageViewScaleChange(View snapshotView) {
        return snapshotView instanceof ImageView;
    }

    private void saveImageViewScale() {
        if (mSavedScaleType == null) {
            // only save first time after initialize/restoreImageViewScale()
            ImageView imageView = mViewHolder.mImageView;
            mSavedScaleType = imageView.getScaleType();
            mSavedMatrix = mSavedScaleType == ScaleType.MATRIX ? imageView.getMatrix() : null;
            if (DEBUG) {
                Log.d(TAG, "saveImageViewScale: "+mSavedScaleType);
            }
        }
    }

    private static void updateImageViewAfterScaleTypeChange(ImageView imageView) {
        // enforcing imageView to update its internal bounds/matrix immediately
        imageView.measure(
                MeasureSpec.makeMeasureSpec(imageView.getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(imageView.getMeasuredHeight(), MeasureSpec.EXACTLY));
        imageView.layout(imageView.getLeft(), imageView.getTop(),
                imageView.getRight(), imageView.getBottom());
    }

    private void changeImageViewScale(View snapshotView) {
        ImageView snapshotImageView = (ImageView) snapshotView;
        ImageView imageView = mViewHolder.mImageView;
        if (DEBUG) {
            Log.d(TAG, "changeImageViewScale to "+snapshotImageView.getScaleType());
        }
        imageView.setScaleType(snapshotImageView.getScaleType());
        if (snapshotImageView.getScaleType() == ScaleType.MATRIX) {
            imageView.setImageMatrix(snapshotImageView.getImageMatrix());
        }
        updateImageViewAfterScaleTypeChange(imageView);
    }

    private void restoreImageViewScale() {
        if (mSavedScaleType != null) {
            if (DEBUG) {
                Log.d(TAG, "restoreImageViewScale to "+mSavedScaleType);
            }
            ImageView imageView = mViewHolder.mImageView;
            imageView.setScaleType(mSavedScaleType);
            if (mSavedScaleType == ScaleType.MATRIX) {
                imageView.setImageMatrix(mSavedMatrix);
            }
            // only restore once unless another save happens
            mSavedScaleType = null;
            updateImageViewAfterScaleTypeChange(imageView);
        }
    }

    @Override
    public void onSharedElementStart(List<String> sharedElementNames,
            List<View> sharedElements, List<View> sharedElementSnapshots) {
        if (DEBUG) {
            Log.d(TAG, "onSharedElementStart " + mActivityToRunTransition);
        }
        if (sharedElements.size() < 1) {
            return;
        }
        View overviewView = sharedElements.get(0);
        if (mViewHolder == null || mViewHolder.mOverviewFrame != overviewView) {
            return;
        }
        View snapshot = sharedElementSnapshots.get(0);
        if (hasImageViewScaleChange(snapshot)) {
            saveImageViewScale();
            changeImageViewScale(snapshot);
        }
        View imageView = mViewHolder.mImageView;
        final int width = overviewView.getWidth();
        final int height = overviewView.getHeight();
        imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        imageView.layout(0, 0, width, height);
        final View rightPanel = mViewHolder.mRightPanel;
        if (mRightPanelWidth != 0 && mRightPanelHeight != 0) {
            rightPanel.measure(MeasureSpec.makeMeasureSpec(mRightPanelWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mRightPanelHeight, MeasureSpec.EXACTLY));
            rightPanel.layout(width, rightPanel.getTop(), width + mRightPanelWidth,
                    rightPanel.getTop() + mRightPanelHeight);
        } else {
            rightPanel.offsetLeftAndRight(width - rightPanel.getLeft());
        }
        mViewHolder.mActionsRow.setVisibility(View.INVISIBLE);
        mViewHolder.mDetailsDescriptionFrame.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSharedElementEnd(List<String> sharedElementNames,
            List<View> sharedElements, List<View> sharedElementSnapshots) {
        if (DEBUG) {
            Log.d(TAG, "onSharedElementEnd " + mActivityToRunTransition);
        }
        if (sharedElements.size() < 1) {
            return;
        }
        View overviewView = sharedElements.get(0);
        if (mViewHolder == null || mViewHolder.mOverviewFrame != overviewView) {
            return;
        }
        restoreImageViewScale();
        // temporary let action row take focus so we defer button background animation
        mViewHolder.mActionsRow.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        mViewHolder.mActionsRow.setVisibility(View.VISIBLE);
        mViewHolder.mActionsRow.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        mViewHolder.mDetailsDescriptionFrame.setVisibility(View.VISIBLE);
    }

    void setSharedElementEnterTransition(Activity activity, String sharedElementName,
            long timeoutMs) {
        if (activity == null && !TextUtils.isEmpty(sharedElementName) ||
                activity != null && TextUtils.isEmpty(sharedElementName)) {
            throw new IllegalArgumentException();
        }
        if (activity == mActivityToRunTransition &&
                TextUtils.equals(sharedElementName, mSharedElementName)) {
            return;
        }
        if (mActivityToRunTransition != null) {
            ActivityCompat.setEnterSharedElementCallback(mActivityToRunTransition, null);
        }
        mActivityToRunTransition = activity;
        mSharedElementName = sharedElementName;
        if (DEBUG) {
            Log.d(TAG, "postponeEnterTransition " + mActivityToRunTransition);
        }
        ActivityCompat.setEnterSharedElementCallback(mActivityToRunTransition, this);
        ActivityCompat.postponeEnterTransition(mActivityToRunTransition);
        if (timeoutMs > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mStartedPostpone) {
                        return;
                    }
                    if (DEBUG) {
                        Log.d(TAG, "timeout " + mActivityToRunTransition);
                    }
                    startPostponedEnterTransition();
                }
            }, timeoutMs);
        }
    }

    void onBindToDrawable(ViewHolder vh) {
        if (DEBUG) {
            Log.d(TAG, "onBindToDrawable, could start transition of " + mActivityToRunTransition);
        }
        if (mViewHolder != null) {
            if (DEBUG) {
                Log.d(TAG, "rebind? clear transitionName on current viewHolder "
                        + mViewHolder.mOverviewFrame);
            }
            ViewCompat.setTransitionName(mViewHolder.mOverviewFrame, null);
        }
        // After we got a image drawable,  we can determine size of right panel.
        // We want right panel to have fixed size so that the right panel don't change size
        // when the overview is layout as a small bounds in transition.
        mViewHolder = vh;
        mViewHolder.mRightPanel.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mViewHolder.mRightPanel.removeOnLayoutChangeListener(this);
                mRightPanelWidth = mViewHolder.mRightPanel.getWidth();
                mRightPanelHeight = mViewHolder.mRightPanel.getHeight();
                if (DEBUG) {
                    Log.d(TAG, "onLayoutChange records size of right panel as "
                            + mRightPanelWidth + ", "+ mRightPanelHeight);
                }
            }
        });
        mViewHolder.mRightPanel.postOnAnimation(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    Log.d(TAG, "setTransitionName "+mViewHolder.mOverviewFrame);
                }
                ViewCompat.setTransitionName(mViewHolder.mOverviewFrame, mSharedElementName);
                final TransitionHelper transitionHelper = TransitionHelper.getInstance();
                Object transition = transitionHelper.getSharedElementEnterTransition(
                        mActivityToRunTransition.getWindow());
                if (transition != null) {
                    transitionHelper.setTransitionListener(transition, new TransitionListener() {
                        @Override
                        public void onTransitionEnd(Object transition) {
                            if (DEBUG) {
                                Log.d(TAG, "onTransitionEnd " + mActivityToRunTransition);
                            }
                            // after transition if the action row still focused, transfer
                            // focus to its children
                            if (mViewHolder.mActionsRow.isFocused()) {
                                mViewHolder.mActionsRow.requestFocus();
                            }
                            transitionHelper.setTransitionListener(transition, null);
                        }
                    });
                }
                startPostponedEnterTransition();
            }
        });
    }

    private void startPostponedEnterTransition() {
        if (!mStartedPostpone) {
            if (DEBUG) {
                Log.d(TAG, "startPostponedEnterTransition " + mActivityToRunTransition);
            }
            ActivityCompat.startPostponedEnterTransition(mActivityToRunTransition);
            mStartedPostpone = true;
        }
    }
}
