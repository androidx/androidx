/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;

/**
 * This class is a container for showing inline suggestions for cases where you
 * want to ensure they appear only in a given area in your app. An example is
 * having a scrollable list of suggestions with an icon on the left and one on
 * the right (think of a suggestion strip with actions on left, right and a
 * scrollable suggestions list in the middle) where scrolling the suggestions
 * should not cover the icons on both sides. Note that without this container
 * the suggestions would cover parts of your app as they are surfaces owned
 * by another process and always appearing on top of your app.
 */
@RequiresApi(api = Build.VERSION_CODES.Q) // TODO(b/147116534): Update to R
public class InlineSuggestionsHostView extends FrameLayout {
    // The trick that we use here is to have a hidden SurfaceView to whose
    // surface we reparent the surfaces of remote suggestions which are
    // also SurfaceViews. Since surface locations are based off the window
    // top-left making, making one surface parent of another compounds the
    // offset from the child's point of view. To compensate for that we
    // add a FrameLayout wrapping each child and set the FrameLayout's scroll
    // to be that of the parent surface - compensating the compounding.

    private final @NonNull ArraySet<SurfaceView> mReparentedChildren = new ArraySet<>();

    private final @NonNull int[] mTempLocation = new int[2];
    private final @NonNull SurfaceView mSurfaceClipView;

    public InlineSuggestionsHostView(@NonNull Context context) {
        this(context, /*attrs*/ null);
    }

    public InlineSuggestionsHostView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, /*defStyleAttr*/0);
    }

    public InlineSuggestionsHostView(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mSurfaceClipView = new SurfaceView(context);
        mSurfaceClipView.setZOrderOnTop(true);
        mSurfaceClipView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        mSurfaceClipView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                /* do nothing */
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                    int height) {
                /* do nothing */
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                updateState(InlineSuggestionsHostView.this,
                        /*parentSurfaceProvider*/ null);
            }
        });

        super.addView(mSurfaceClipView);

        // This is needed to handle the surfaces of the suggestions being created later and
        // also to keep things in sync. The update method is optimized to be called often.
        getViewTreeObserver().addOnGlobalLayoutListener(
                () -> updateState(this, mSurfaceClipView));
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        final FrameLayout locationOffsetWrapper = new FrameLayout(getContext());
        locationOffsetWrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        locationOffsetWrapper.addView(child);
        super.addView(locationOffsetWrapper, index, params);
        updateState(InlineSuggestionsHostView.this, mSurfaceClipView);
    }

    @Override
    public void removeView(@Nullable View view) {
        if (view == null) {
            return;
        }
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child == view) {
                super.removeView(child);
                updateState(view, /*parentSurfaceProvider*/ null);
                return;
            }
            if (!(child instanceof ViewGroup)) {
                continue;
            }
            final ViewGroup childGroup = (ViewGroup) child;
            final int grandChildCount = childGroup.getChildCount();
            for (int j = 0; j < grandChildCount; j++) {
                final View grandChild = childGroup.getChildAt(j);
                if (grandChild == view) {
                    super.removeView(child);
                    updateState(view, /*parentSurfaceProvider*/ null);
                    return;
                }
            }
        }
    }

    void updateState(@NonNull View root,
            @Nullable SurfaceView parentSurfaceProvider) {
        mSurfaceClipView.getLocationInWindow(mTempLocation);
        reparentChildSurfaceViewSurfacesRecursive(root, parentSurfaceProvider,
                /*parentSurfaceLeft*/ mTempLocation[0],
                /*parentSurfaceTop*/ mTempLocation[1]);
    }

    private void reparentChildSurfaceViewSurfacesRecursive(@Nullable View root,
            @Nullable SurfaceView parentSurfaceProvider, int parentSurfaceLeft,
            int parentSurfaceTop) {
        if (mSurfaceClipView.getSurfaceControl() == null
                || !mSurfaceClipView.getSurfaceControl().isValid()) {
            return;
        }
        if (!(root instanceof ViewGroup)) {
            return;
        }
        final ViewGroup rootGroup = (ViewGroup) root;
        final int childCount = rootGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = rootGroup.getChildAt(i);
            if (child == mSurfaceClipView) {
                continue;
            }
            if (child instanceof SurfaceView) {
                final SurfaceView childSurfaceView = (SurfaceView) child;
                if (childSurfaceView.getSurfaceControl() == null
                        || !childSurfaceView.getSurfaceControl().isValid()) {
                    continue;
                }
                if (parentSurfaceProvider != null) {
                    if (mReparentedChildren.contains(childSurfaceView)) {
                        continue;
                    }
                    new SurfaceControl.Transaction()
                            .reparent(childSurfaceView.getSurfaceControl(),
                                    parentSurfaceProvider.getSurfaceControl())
                            .apply();
                    root.setScrollX(parentSurfaceLeft);
                    root.setScrollY(parentSurfaceTop);
                    mReparentedChildren.add(childSurfaceView);
                } else {
                    if (!mReparentedChildren.contains(childSurfaceView)) {
                        continue;
                    }
                    new SurfaceControl.Transaction()
                            .reparent(childSurfaceView.getSurfaceControl(),
                                    /*newParent*/null)
                            .apply();
                    root.setScrollX(0);
                    root.setScrollY(0);
                    mReparentedChildren.remove(childSurfaceView);
                }
            }

            reparentChildSurfaceViewSurfacesRecursive(child, parentSurfaceProvider,
                    parentSurfaceLeft, parentSurfaceTop);
        }
    }
}
