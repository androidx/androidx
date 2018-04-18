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

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

/**
 * Backport of android.view.GhostView introduced in API level 21.
 * <p>
 * While the platform version uses ViewOverlay, this ghost view finds the closest FrameLayout in
 * the hierarchy and adds itself there.
 * <p>
 * Since we cannot use RenderNode to delegate drawing, we instead use {@link View#draw(Canvas)} to
 * draw the target view. We apply the same transformation matrix applied to the target view. For
 * that, this view is sized as large as the parent FrameLayout (except padding) while the platform
 * version becomes as large as the target view.
 */
@SuppressLint("ViewConstructor")
class GhostViewApi14 extends View implements GhostViewImpl {

    static GhostViewImpl addGhost(View view, ViewGroup viewGroup) {
        GhostViewApi14 ghostView = getGhostView(view);
        if (ghostView == null) {
            FrameLayout frameLayout = findFrameLayout(viewGroup);
            if (frameLayout == null) {
                return null;
            }
            ghostView = new GhostViewApi14(view);
            frameLayout.addView(ghostView);
        }
        ghostView.mReferences++;
        return ghostView;
    }

    static void removeGhost(View view) {
        GhostViewApi14 ghostView = getGhostView(view);
        if (ghostView != null) {
            ghostView.mReferences--;
            if (ghostView.mReferences <= 0) {
                ViewParent parent = ghostView.getParent();
                if (parent instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) parent;
                    group.endViewTransition(ghostView);
                    group.removeView(ghostView);
                }
            }
        }
    }

    /**
     * Find the closest FrameLayout in the ascendant hierarchy from the specified {@code
     * viewGroup}.
     */
    private static FrameLayout findFrameLayout(ViewGroup viewGroup) {
        while (!(viewGroup instanceof FrameLayout)) {
            ViewParent parent = viewGroup.getParent();
            if (!(parent instanceof ViewGroup)) {
                return null;
            }
            viewGroup = (ViewGroup) parent;
        }
        return (FrameLayout) viewGroup;
    }

    /** The target view */
    final View mView;

    /** The parent of the view that is disappearing at the beginning of the animation */
    ViewGroup mStartParent;

    /** The view that is disappearing at the beginning of the animation */
    View mStartView;

    /** The number of references to this ghost view */
    int mReferences;

    /** The horizontal distance from the ghost view to the target view */
    private int mDeltaX;

    /** The horizontal distance from the ghost view to the target view */
    private int mDeltaY;

    /** The current transformation matrix of the target view */
    Matrix mCurrentMatrix;

    /** The matrix applied to the ghost view canvas */
    private final Matrix mMatrix = new Matrix();

    private final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // The target view was invalidated; get the transformation.
                    mCurrentMatrix = mView.getMatrix();
                    // We draw the view.
                    ViewCompat.postInvalidateOnAnimation(GhostViewApi14.this);
                    if (mStartParent != null && mStartView != null) {
                        mStartParent.endViewTransition(mStartView);
                        ViewCompat.postInvalidateOnAnimation(mStartParent);
                        mStartParent = null;
                        mStartView = null;
                    }
                    return true;
                }
            };

    GhostViewApi14(View view) {
        super(view.getContext());
        mView = view;
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setGhostView(mView, this);
        // Calculate the deltas
        final int[] location = new int[2];
        final int[] viewLocation = new int[2];
        getLocationOnScreen(location);
        mView.getLocationOnScreen(viewLocation);
        viewLocation[0] = (int) (viewLocation[0] - mView.getTranslationX());
        viewLocation[1] = (int) (viewLocation[1] - mView.getTranslationY());
        mDeltaX = viewLocation[0] - location[0];
        mDeltaY = viewLocation[1] - location[1];
        // Monitor invalidation of the target view.
        mView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
        // Make the target view invisible because we draw it instead.
        mView.setVisibility(INVISIBLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        mView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        mView.setVisibility(VISIBLE);
        setGhostView(mView, null);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Apply the matrix while adjusting the coordinates
        mMatrix.set(mCurrentMatrix);
        mMatrix.postTranslate(mDeltaX, mDeltaY);
        canvas.setMatrix(mMatrix);
        // Draw the target
        mView.draw(canvas);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mView.setVisibility(visibility == VISIBLE ? INVISIBLE : VISIBLE);
    }

    @Override
    public void reserveEndViewTransition(ViewGroup viewGroup, View view) {
        mStartParent = viewGroup;
        mStartView = view;
    }

    private static void setGhostView(@NonNull View view, GhostViewApi14 ghostView) {
        view.setTag(R.id.ghost_view, ghostView);
    }

    static GhostViewApi14 getGhostView(@NonNull View view) {
        return (GhostViewApi14) view.getTag(R.id.ghost_view);
    }

}
