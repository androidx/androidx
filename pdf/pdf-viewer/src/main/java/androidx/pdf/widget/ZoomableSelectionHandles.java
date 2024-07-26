/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.R;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.ObservableValue.ValueObserver;
import androidx.pdf.widget.ZoomView.ZoomScroll;

/**
 * Base class for selection handles on content inside a ZoomView.
 *
 * @param <S> The type of the selection that this class observes, updating the
 *            handles whenever it changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public abstract class ZoomableSelectionHandles<S> {
    private static final float SCALE_OFFSET = 0.5f;
    private static final float HANDLE_ALPHA = 1.0f;
    private static final float RIGHT_HANDLE_X_MARGIN = -0.25f;
    private static final float LEFT_HANDLE_X_MARGIN = -0.75f;
    protected final ZoomView mZoomView;
    protected final ObservableValue<S> mSelectionObservable;

    protected final ImageView mStartHandle;
    protected final ImageView mStopHandle;

    protected final OnTouchListener mOnTouchListener;

    /** Handle objects for removing observers at end of life. */
    protected final Object mSelectionObserverKey;
    protected final Object mZoomViewObserverKey;

    protected S mSelection;

    protected ZoomableSelectionHandles(@NonNull ZoomView zoomView, @NonNull ViewGroup handleParent,
            @NonNull ObservableValue<S> selectionObservable) {
        this.mZoomView = zoomView;
        this.mSelectionObservable = selectionObservable;

        this.mOnTouchListener = new HandleTouchListener();

        this.mStartHandle = createHandle(handleParent, false);
        this.mStopHandle = createHandle(handleParent, true);

        mSelectionObserverKey = createSelectionObserver();
        mZoomViewObserverKey = createZoomViewObserver();
    }

    @NonNull
    protected Object createSelectionObserver() {
        return mSelectionObservable.addObserver(new ValueObserver<S>() {
            @Override
            public void onChange(S oldSelection, S newSelection) {
                mSelection = newSelection;
                updateHandles();
            }

            @NonNull
            @Override
            public String toString() {
                return "ZoomableSelectionHandles#selectionObserver";
            }
        });
    }

    @NonNull
    protected Object createZoomViewObserver() {
        return mZoomView.zoomScroll().addObserver(new ValueObserver<ZoomScroll>() {
            @Override
            public void onChange(ZoomScroll oldValue, ZoomScroll newValue) {
                if (oldValue.zoom != newValue.zoom) {
                    updateHandles();
                }
            }

            @NonNull
            @Override
            public String toString() {
                return "ZoomableSelectionHandles#zoomViewObserver";
            }
        });
    }

    /** Destroy start and stop handles. */
    public void destroy() {
        mSelectionObservable.removeObserver(mSelectionObserverKey);
        mZoomView.zoomScroll().removeObserver(mZoomViewObserverKey);
        destroyHandle(mStartHandle);
        destroyHandle(mStopHandle);
    }

    /**
     * Show or hide both handles, according to the current selection. Should delegate
     * to {@link #hideHandles} or to {@link #showHandle} - showHandle will take the
     * zoom into account when displaying the handles.
     */
    protected abstract void updateHandles();

    protected void hideHandles() {
        mStartHandle.setVisibility(View.GONE);
        mStopHandle.setVisibility(View.GONE);
    }

    protected void showHandle(@NonNull ImageView handle, float rawX, float rawY, boolean isRight) {
        int resId = isRight
                ? R.drawable.selection_drag_handle_right
                : R.drawable.selection_drag_handle_left;
        handle.setImageResource(resId);

        // The sharp point of the handle is found at a particular point in the image -
        // (25%, 0%) for the right handle, and (75%, 0%) for a left handle. We apply these
        // as negative margins so that the handle's point is at the point specified.
        float xMargin = isRight ? RIGHT_HANDLE_X_MARGIN : LEFT_HANDLE_X_MARGIN;
        float yMargin = 0;
        float scale = 1.0f / mZoomView.getZoom();
        float x = calcTranslation(rawX, handle.getDrawable().getIntrinsicWidth(), scale, xMargin);
        float y = calcTranslation(rawY, handle.getDrawable().getIntrinsicHeight(), scale, yMargin);

        handle.setScaleX(scale);
        handle.setScaleY(scale);
        handle.setTranslationX(x);
        handle.setTranslationY(y);
        handle.setVisibility(View.VISIBLE);
    }

    protected float calcTranslation(float rawPos, float rawSize, float scale, float margin) {
        float result = rawPos;
        // Undo translation of top-left corner that is a side effect of scaling around the image
        // center:
        result += SCALE_OFFSET * rawSize * (scale - 1);
        // Apply margin to top-left corner.
        result += margin * rawSize * scale;
        return result;
    }

    /**
     * Creates a new text selection handle ImageView and adds it to the parent. Returns the handle.
     */
    @NonNull
    protected ImageView createHandle(@NonNull ViewGroup parent, boolean isStop) {
        ImageView handle = new ImageView(parent.getContext());
        handle.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        handle.setColorFilter(
                parent.getContext().getResources().getColor(R.color.selection_handles));
        handle.setAlpha(HANDLE_ALPHA);

        int descId = isStop ? R.string.desc_selection_stop : R.string.desc_selection_start;
        handle.setContentDescription(parent.getContext().getString(descId));
        handle.setVisibility(View.GONE);
        parent.addView(handle);
        handle.setOnTouchListener(mOnTouchListener);
        return handle;
    }

    protected void destroyHandle(@NonNull ImageView handle) {
        handle.setOnTouchListener(null);
        if (handle.getParent() != null) {
            ((ViewGroup) handle.getParent()).removeView(handle);
        }
    }

    /**
     * Handle drag begins. Implementation should note which handle is being dragged and store the
     * original positions of both handles.
     */
    protected abstract void onDragHandleDown(boolean isStopHandle);

    /**
     * Handle drag continues. Implementation should update the selection so that it spans between
     * the fixed handle, and the new position of the dragging handle, which is moved by
     * (deltaX, deltaY) from its original position.
     */
    protected abstract void onDragHandleMove(int deltaX, int deltaY);

    /** Handle drag stops. The implementation might not need to do anything here. */
    protected abstract void onDragHandleUp();

    /**
     * Touch listener for each handle that works out what the new boundary locations
     * should be by measuring where they are dragged to, and notifies the
     * onDragSelectionListener (if it is set).
     */
    private class HandleTouchListener implements OnTouchListener {
        private float mXDragDown;
        private float mYDragDown;

        @Override
        public boolean onTouch(View view, MotionEvent e) {
            boolean isStopHandle = (view == mStopHandle);

            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Starting a new drag: just record where we are starting:
                    onDragHandleDown(isStopHandle);
                    mXDragDown = e.getRawX();
                    mYDragDown = e.getRawY();
                    mZoomView.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Handle has been dragged: find out where it is now and update listener.
                    int xDelta = (int) ((e.getRawX() - mXDragDown) / mZoomView.getZoom());
                    int yDelta = (int) ((e.getRawY() - mYDragDown) / mZoomView.getZoom());
                    onDragHandleMove(xDelta, yDelta);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    onDragHandleUp();
                    mZoomView.requestDisallowInterceptTouchEvent(false);
            }

            return true;
        }
    }

}
