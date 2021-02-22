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
package androidx.leanback.widget;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import androidx.core.app.ActivityCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.ViewCompat;
import androidx.leanback.transition.TransitionHelper;
import androidx.leanback.transition.TransitionListener;
import androidx.leanback.widget.DetailsOverviewRowPresenter.ViewHolder;

import java.lang.ref.WeakReference;
import java.util.List;

final class DetailsOverviewSharedElementHelper extends SharedElementCallback {

    static final String TAG = "DetailsTransitionHelper";
    static final boolean DEBUG = false;

    static final class TransitionTimeOutRunnable implements Runnable {
        final WeakReference<DetailsOverviewSharedElementHelper> mHelperRef;

        TransitionTimeOutRunnable(DetailsOverviewSharedElementHelper helper) {
            mHelperRef = new WeakReference<DetailsOverviewSharedElementHelper>(helper);
        }

        @Override
        public void run() {
            DetailsOverviewSharedElementHelper helper = mHelperRef.get();
            if (helper == null) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "timeout " + helper.mActivityToRunTransition);
            }
            helper.startPostponedEnterTransition();
        }
    }

    WeakReference<ViewHolder> mViewHolder = new WeakReference<>(null);
    Activity mActivityToRunTransition;
    boolean mStartedPostpone;
    String mSharedElementName;
    int mRightPanelWidth;
    int mRightPanelHeight;

    private ScaleType mSavedScaleType;
    private Matrix mSavedMatrix;

    private boolean hasImageViewScaleChange(View snapshotView) {
        return snapshotView instanceof ImageView;
    }

    private void saveImageViewScale() {
        ViewHolder vh = mViewHolder.get();
        if (mSavedScaleType == null && vh != null) {
            // only save first time after initialize/restoreImageViewScale()
            ImageView imageView = vh.mImageView;
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

    private static void changeImageViewScale(ViewHolder vh, View snapshotView) {
        ImageView snapshotImageView = (ImageView) snapshotView;
        ImageView imageView = vh.mImageView;
        if (DEBUG) {
            Log.d(TAG, "changeImageViewScale to "+snapshotImageView.getScaleType());
        }
        imageView.setScaleType(snapshotImageView.getScaleType());
        if (snapshotImageView.getScaleType() == ScaleType.MATRIX) {
            imageView.setImageMatrix(snapshotImageView.getImageMatrix());
        }
        updateImageViewAfterScaleTypeChange(imageView);
    }

    private void restoreImageViewScale(ViewHolder vh) {
        if (mSavedScaleType != null) {
            if (DEBUG) {
                Log.d(TAG, "restoreImageViewScale to "+mSavedScaleType);
            }
            ImageView imageView = vh.mImageView;
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
        ViewHolder vh = mViewHolder.get();
        View overviewView = sharedElements.get(0);
        if (vh == null || vh.mOverviewFrame != overviewView) {
            return;
        }
        View snapshot = sharedElementSnapshots.get(0);
        if (hasImageViewScaleChange(snapshot)) {
            saveImageViewScale();
            changeImageViewScale(vh, snapshot);
        }
        View imageView = vh.mImageView;
        final int width = overviewView.getWidth();
        final int height = overviewView.getHeight();
        imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        imageView.layout(0, 0, width, height);
        final View rightPanel = vh.mRightPanel;
        if (mRightPanelWidth != 0 && mRightPanelHeight != 0) {
            rightPanel.measure(MeasureSpec.makeMeasureSpec(mRightPanelWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mRightPanelHeight, MeasureSpec.EXACTLY));
            rightPanel.layout(width, rightPanel.getTop(), width + mRightPanelWidth,
                    rightPanel.getTop() + mRightPanelHeight);
        } else {
            rightPanel.offsetLeftAndRight(width - rightPanel.getLeft());
        }
        vh.mActionsRow.setVisibility(View.INVISIBLE);
        vh.mDetailsDescriptionFrame.setVisibility(View.INVISIBLE);
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
        ViewHolder vh = mViewHolder.get();
        View overviewView = sharedElements.get(0);
        if (vh == null || vh.mOverviewFrame != overviewView) {
            return;
        }
        restoreImageViewScale(vh);
        // temporary let action row take focus so we defer button background animation
        vh.mActionsRow.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        vh.mActionsRow.setVisibility(View.VISIBLE);
        vh.mActionsRow.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // switch focusability to VISIBLE wont trigger focusableViewAvailable() on O because
        // shared element details_frame is still INVISIBLE. b/63544781
        vh.mActionsRow.requestFocus();
        vh.mDetailsDescriptionFrame.setVisibility(View.VISIBLE);
    }

    void setSharedElementEnterTransition(Activity activity, String sharedElementName,
            long timeoutMs) {
        if ((activity == null && !TextUtils.isEmpty(sharedElementName))
                || (activity != null && TextUtils.isEmpty(sharedElementName))) {
            throw new IllegalArgumentException();
        }
        if (activity == mActivityToRunTransition
                && TextUtils.equals(sharedElementName, mSharedElementName)) {
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
            new Handler().postDelayed(new TransitionTimeOutRunnable(this), timeoutMs);
        }
    }

    void onBindToDrawable(ViewHolder vh) {
        if (DEBUG) {
            Log.d(TAG, "onBindToDrawable, could start transition of " + mActivityToRunTransition);
        }
        ViewHolder currentVh = mViewHolder.get();
        if (currentVh != null) {
            if (DEBUG) {
                Log.d(TAG, "rebind? clear transitionName on current viewHolder "
                        + currentVh.mOverviewFrame);
            }
            ViewCompat.setTransitionName(currentVh.mOverviewFrame, null);
        }
        // After we got a image drawable,  we can determine size of right panel.
        // We want right panel to have fixed size so that the right panel don't change size
        // when the overview is layout as a small bounds in transition.
        mViewHolder = new WeakReference<>(vh);
        vh.mRightPanel.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                ViewHolder vh = mViewHolder.get();
                if (vh != null) {
                    mRightPanelWidth = vh.mRightPanel.getWidth();
                    mRightPanelHeight = vh.mRightPanel.getHeight();
                    if (DEBUG) {
                        Log.d(TAG, "onLayoutChange records size of right panel as "
                                + mRightPanelWidth + ", " + mRightPanelHeight);
                    }
                }
            }
        });
        vh.mRightPanel.postOnAnimation(new Runnable() {
            @Override
            public void run() {
                ViewHolder vh = mViewHolder.get();
                if (vh == null) {
                    return;
                }
                if (DEBUG) {
                    Log.d(TAG, "setTransitionName " + vh.mOverviewFrame);
                }
                ViewCompat.setTransitionName(vh.mOverviewFrame, mSharedElementName);
                Object transition = TransitionHelper.getSharedElementEnterTransition(
                        mActivityToRunTransition.getWindow());
                if (transition != null) {
                    TransitionHelper.addTransitionListener(transition, new TransitionListener() {
                        @Override
                        public void onTransitionEnd(Object transition) {
                            if (DEBUG) {
                                Log.d(TAG, "onTransitionEnd " + mActivityToRunTransition);
                            }
                            // after transition if the action row still focused, transfer
                            // focus to its children
                            ViewHolder vh = mViewHolder.get();
                            if (vh != null && vh.mActionsRow.isFocused()) {
                                vh.mActionsRow.requestFocus();
                            }
                            TransitionHelper.removeTransitionListener(transition, this);
                        }
                    });
                }
                startPostponedEnterTransition();
            }
        });
    }

    void startPostponedEnterTransition() {
        if (!mStartedPostpone) {
            if (DEBUG) {
                Log.d(TAG, "startPostponedEnterTransition " + mActivityToRunTransition);
            }
            ActivityCompat.startPostponedEnterTransition(mActivityToRunTransition);
            mStartedPostpone = true;
        }
    }
}
