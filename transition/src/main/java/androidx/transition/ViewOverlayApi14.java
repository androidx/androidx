/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

class ViewOverlayApi14 implements ViewOverlayImpl {

    /**
     * The actual container for the drawables (and views, if it's a ViewGroupOverlay).
     * All of the management and rendering details for the overlay are handled in
     * OverlayViewGroup.
     */
    protected OverlayViewGroup mOverlayViewGroup;

    ViewOverlayApi14(Context context, ViewGroup hostView, View requestingView) {
        mOverlayViewGroup = new OverlayViewGroup(context, hostView, requestingView, this);
    }

    static ViewGroup getContentView(View view) {
        View parent = view;
        while (parent != null) {
            if (parent.getId() == android.R.id.content && parent instanceof ViewGroup) {
                return (ViewGroup) parent;
            }
            if (parent.getParent() instanceof ViewGroup) {
                parent = (ViewGroup) parent.getParent();
            }
        }
        return null;
    }

    static ViewOverlayApi14 createFrom(View view) {
        ViewGroup contentView = getContentView(view);
        if (contentView != null) {
            final int numChildren = contentView.getChildCount();
            for (int i = 0; i < numChildren; ++i) {
                View child = contentView.getChildAt(i);
                if (child instanceof OverlayViewGroup) {
                    return ((OverlayViewGroup) child).mViewOverlay;
                }
            }
            return new ViewGroupOverlayApi14(contentView.getContext(), contentView, view);
        }
        return null;
    }

    /**
     * Used internally by View and ViewGroup to handle drawing and invalidation
     * of the overlay
     */
    ViewGroup getOverlayView() {
        return mOverlayViewGroup;
    }

    @Override
    public void add(@NonNull Drawable drawable) {
        mOverlayViewGroup.add(drawable);
    }

    @Override
    public void clear() {
        mOverlayViewGroup.clear();
    }

    @Override
    public void remove(@NonNull Drawable drawable) {
        mOverlayViewGroup.remove(drawable);
    }

    boolean isEmpty() {
        return mOverlayViewGroup.isEmpty();
    }


    /**
     * OverlayViewGroup is a container that View and ViewGroup use to host
     * drawables and views added to their overlays  ({@code ViewOverlay} and
     * {@code ViewGroupOverlay}, respectively). Drawables are added to the overlay
     * via the add/remove methods in ViewOverlay, Views are added/removed via
     * ViewGroupOverlay. These drawable and view objects are
     * drawn whenever the view itself is drawn; first the view draws its own
     * content (and children, if it is a ViewGroup), then it draws its overlay
     * (if it has one).
     *
     * <p>Besides managing and drawing the list of drawables, this class serves
     * two purposes:
     * (1) it noops layout calls because children are absolutely positioned and
     * (2) it forwards all invalidation calls to its host view. The invalidation
     * redirect is necessary because the overlay is not a child of the host view
     * and invalidation cannot therefore follow the normal path up through the
     * parent hierarchy.</p>
     *
     * @see View#getOverlay()
     * @see ViewGroup#getOverlay()
     */
    static class OverlayViewGroup extends ViewGroup {

        static Method sInvalidateChildInParentFastMethod;

        static {
            try {
                sInvalidateChildInParentFastMethod = ViewGroup.class.getDeclaredMethod(
                        "invalidateChildInParentFast", int.class, int.class, Rect.class);
            } catch (NoSuchMethodException e) {
            }

        }

        /**
         * The View for which this is an overlay. Invalidations of the overlay are redirected to
         * this host view.
         */
        ViewGroup mHostView;
        View mRequestingView;
        /**
         * The set of drawables to draw when the overlay is rendered.
         */
        ArrayList<Drawable> mDrawables = null;
        /**
         * Reference to the hosting overlay object
         */
        ViewOverlayApi14 mViewOverlay;

        OverlayViewGroup(Context context, ViewGroup hostView, View requestingView,
                ViewOverlayApi14 viewOverlay) {
            super(context);
            mHostView = hostView;
            mRequestingView = requestingView;
            setRight(hostView.getWidth());
            setBottom(hostView.getHeight());
            hostView.addView(this);
            mViewOverlay = viewOverlay;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            // Intercept and noop all touch events - overlays do not allow touch events
            return false;
        }

        public void add(Drawable drawable) {
            if (mDrawables == null) {

                mDrawables = new ArrayList<>();
            }
            if (!mDrawables.contains(drawable)) {
                // Make each drawable unique in the overlay; can't add it more than once
                mDrawables.add(drawable);
                invalidate(drawable.getBounds());
                drawable.setCallback(this);
            }
        }

        public void remove(Drawable drawable) {
            if (mDrawables != null) {
                mDrawables.remove(drawable);
                invalidate(drawable.getBounds());
                drawable.setCallback(null);
            }
        }

        @Override
        protected boolean verifyDrawable(@NonNull Drawable who) {
            return super.verifyDrawable(who) || (mDrawables != null && mDrawables.contains(who));
        }

        public void add(View child) {
            if (child.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) child.getParent();
                if (parent != mHostView && parent.getParent() != null
                        && ViewCompat.isAttachedToWindow(parent)) {
                    // Moving to different container; figure out how to position child such that
                    // it is in the same location on the screen
                    int[] parentLocation = new int[2];
                    int[] hostViewLocation = new int[2];
                    parent.getLocationOnScreen(parentLocation);
                    mHostView.getLocationOnScreen(hostViewLocation);
                    ViewCompat.offsetLeftAndRight(child, parentLocation[0] - hostViewLocation[0]);
                    ViewCompat.offsetTopAndBottom(child, parentLocation[1] - hostViewLocation[1]);
                }
                parent.removeView(child);
//                if (parent.getLayoutTransition() != null) {
//                    // LayoutTransition will cause the child to delay removal - cancel it
//                    parent.getLayoutTransition().cancel(LayoutTransition.DISAPPEARING);
//                }
                // fail-safe if view is still attached for any reason
                if (child.getParent() != null) {
                    parent.removeView(child);
                }
            }
            super.addView(child, getChildCount() - 1);
        }

        public void remove(View view) {
            super.removeView(view);
            if (isEmpty()) {
                mHostView.removeView(this);
            }
        }

        public void clear() {
            removeAllViews();
            if (mDrawables != null) {
                mDrawables.clear();
            }
        }

        boolean isEmpty() {
            return getChildCount() == 0
                    && (mDrawables == null || mDrawables.size() == 0);
        }

        @Override
        public void invalidateDrawable(@NonNull Drawable drawable) {
            invalidate(drawable.getBounds());
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            int[] contentViewLocation = new int[2];
            int[] hostViewLocation = new int[2];
            mHostView.getLocationOnScreen(contentViewLocation);
            mRequestingView.getLocationOnScreen(hostViewLocation);
            canvas.translate(hostViewLocation[0] - contentViewLocation[0],
                    hostViewLocation[1] - contentViewLocation[1]);
            canvas.clipRect(
                    new Rect(0, 0, mRequestingView.getWidth(), mRequestingView.getHeight()));
            super.dispatchDraw(canvas);
            final int numDrawables = (mDrawables == null) ? 0 : mDrawables.size();
            for (int i = 0; i < numDrawables; ++i) {
                mDrawables.get(i).draw(canvas);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            // Noop: children are positioned absolutely
        }

        /*
         The following invalidation overrides exist for the purpose of redirecting invalidation to
         the host view. The overlay is not parented to the host view (since a View cannot be a
         parent), so the invalidation cannot proceed through the normal parent hierarchy.
         There is a built-in assumption that the overlay exactly covers the host view, therefore
         the invalidation rectangles received do not need to be adjusted when forwarded to
         the host view.
         */

        private void getOffset(int[] offset) {
            int[] contentViewLocation = new int[2];
            int[] hostViewLocation = new int[2];
            mHostView.getLocationOnScreen(contentViewLocation);
            mRequestingView.getLocationOnScreen(hostViewLocation);
            offset[0] = hostViewLocation[0] - contentViewLocation[0];
            offset[1] = hostViewLocation[1] - contentViewLocation[1];
        }

        public void invalidateChildFast(View child, final Rect dirty) {
            if (mHostView != null) {
                // Note: This is not a "fast" invalidation. Would be nice to instead invalidate
                // using DisplayList properties and a dirty rect instead of causing a real
                // invalidation of the host view
                int left = child.getLeft();
                int top = child.getTop();
                int[] offset = new int[2];
                getOffset(offset);
                // TODO: implement transforms
//                if (!child.getMatrix().isIdentity()) {
//                    child.transformRect(dirty);
//                }
                dirty.offset(left + offset[0], top + offset[1]);
                mHostView.invalidate(dirty);
            }
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        protected ViewParent invalidateChildInParentFast(int left, int top, Rect dirty) {
            if (mHostView instanceof ViewGroup && sInvalidateChildInParentFastMethod != null) {
                try {
                    int[] offset = new int[2];
                    getOffset(offset);
                    sInvalidateChildInParentFastMethod.invoke(mHostView, left, top, dirty);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @SuppressWarnings("deprecation")
        @Override
        public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
            if (mHostView != null) {
                dirty.offset(location[0], location[1]);
                if (mHostView instanceof ViewGroup) {
                    location[0] = 0;
                    location[1] = 0;
                    int[] offset = new int[2];
                    getOffset(offset);
                    dirty.offset(offset[0], offset[1]);
                    return super.invalidateChildInParent(location, dirty);
//                    return ((ViewGroup) mHostView).invalidateChildInParent(location, dirty);
                } else {
                    invalidate(dirty);
                }
            }
            return null;
        }

        static class TouchInterceptor extends View {
            TouchInterceptor(Context context) {
                super(context);
            }
        }
    }

    private ViewOverlayApi14() {
    }
}
