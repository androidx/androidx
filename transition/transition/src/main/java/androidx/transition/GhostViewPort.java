/*
 * Copyright 2019 The Android Open Source Project
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
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

/**
 * Backport of android.view.GhostView introduced in API level 21.
 */
@SuppressLint("ViewConstructor")
class GhostViewPort extends ViewGroup implements GhostView {

    /** The parent of the view that is disappearing at the beginning of the animation */
    ViewGroup mStartParent;

    /** The view that is disappearing at the beginning of the animation */
    View mStartView;

    /** The target view */
    final View mView;

    /** The number of references to this ghost view */
    int mReferences;

    @Nullable
    private Matrix mMatrix;

    private final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // We draw the view.
                    ViewCompat.postInvalidateOnAnimation(GhostViewPort.this);
                    if (mStartParent != null && mStartView != null) {
                        mStartParent.endViewTransition(mStartView);
                        ViewCompat.postInvalidateOnAnimation(mStartParent);
                        mStartParent = null;
                        mStartView = null;
                    }
                    return true;
                }
            };

    GhostViewPort(View view) {
        super(view.getContext());
        mView = view;
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (getGhostView(mView) == this) {
            int inverseVisibility = (visibility == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE;
            ViewUtils.setTransitionVisibility(mView, inverseVisibility);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    void setMatrix(@NonNull Matrix matrix) {
        mMatrix = matrix;
    }

    @Override
    public void reserveEndViewTransition(ViewGroup viewGroup, View view) {
        mStartParent = viewGroup;
        mStartView = view;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setGhostView(mView, this);
        // Monitor invalidation of the target view.
        mView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
        // Make the target view invisible because we draw it instead.
        ViewUtils.setTransitionVisibility(mView, INVISIBLE);
        if (mView.getParent() != null) {
            ((View) mView.getParent()).invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        ViewUtils.setTransitionVisibility(mView, VISIBLE);
        setGhostView(mView, null);
        if (mView.getParent() != null) {
            ((View) mView.getParent()).invalidate();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        CanvasUtils.enableZ(canvas, true); // enable shadows
        canvas.setMatrix(mMatrix);

        // We need to mark mView as invalidated so drawChild() will recreate a RenderNode.
        // As invalidate() will do nothing while mView is INVISIBLE we need to silently
        // change it to VISIBLE, call invalidate and then change it back to INVISIBLE.
        ViewUtils.setTransitionVisibility(mView, VISIBLE);
        mView.invalidate();
        ViewUtils.setTransitionVisibility(mView, INVISIBLE);

        drawChild(canvas, mView, getDrawingTime());
        CanvasUtils.enableZ(canvas, false); // re-disable reordering/shadows
    }

    static void copySize(View from, View to) {
        ViewUtils.setLeftTopRightBottom(to,
                to.getLeft(),
                to.getTop(),
                to.getLeft() + from.getWidth(),
                to.getTop() + from.getHeight());
    }

    static GhostViewPort getGhostView(View view) {
        return (GhostViewPort) view.getTag(R.id.ghost_view);
    }

    static void setGhostView(@NonNull View view, @Nullable GhostViewPort ghostView) {
        view.setTag(R.id.ghost_view, ghostView);
    }

    static void calculateMatrix(View view, ViewGroup host, Matrix matrix) {
        ViewGroup parent = (ViewGroup) view.getParent();
        matrix.reset();
        ViewUtils.transformMatrixToGlobal(parent, matrix);
        matrix.preTranslate(-parent.getScrollX(), -parent.getScrollY());
        ViewUtils.transformMatrixToLocal(host, matrix);
    }

    static GhostViewPort addGhost(View view, ViewGroup viewGroup, Matrix matrix) {
        if (!(view.getParent() instanceof ViewGroup)) {
            throw new IllegalArgumentException("Ghosted views must be parented by a ViewGroup");
        }
        GhostViewHolder holder = GhostViewHolder.getHolder(viewGroup);
        GhostViewPort ghostView = getGhostView(view);
        int previousRefCount = 0;
        if (ghostView != null) {
            GhostViewHolder oldHolder = (GhostViewHolder) ghostView.getParent();
            if (oldHolder != holder) {
                previousRefCount = ghostView.mReferences;
                oldHolder.removeView(ghostView);
                ghostView = null;
            }
        }
        if (ghostView == null) {
            if (matrix == null) {
                matrix = new Matrix();
                calculateMatrix(view, viewGroup, matrix);
            }
            ghostView = new GhostViewPort(view);
            ghostView.setMatrix(matrix);
            if (holder == null) {
                holder = new GhostViewHolder(viewGroup);
            } else {
                holder.popToOverlayTop();
            }
            copySize(viewGroup, holder);
            copySize(viewGroup, ghostView);
            holder.addGhostView(ghostView);
            ghostView.mReferences = previousRefCount;
        } else if (matrix != null) {
            ghostView.setMatrix(matrix);
        }
        ghostView.mReferences++;
        return ghostView;
    }

    static void removeGhost(View view) {
        GhostViewPort ghostView = getGhostView(view);
        if (ghostView != null) {
            ghostView.mReferences--;
            if (ghostView.mReferences <= 0) {
                GhostViewHolder holder = (GhostViewHolder) ghostView.getParent();
                holder.removeView(ghostView);
            }
        }
    }

}
