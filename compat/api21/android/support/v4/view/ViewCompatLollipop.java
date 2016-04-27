/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.view;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowInsets;

class ViewCompatLollipop {

    private static ThreadLocal<Rect> sThreadLocalRect;

    public static void setTransitionName(View view, String transitionName) {
        view.setTransitionName(transitionName);
    }

    public static String getTransitionName(View view) {
        return view.getTransitionName();
    }

    public static void requestApplyInsets(View view) {
        view.requestApplyInsets();
    }

    public static void setElevation(View view, float elevation) {
        view.setElevation(elevation);
    }

    public static float getElevation(View view) {
        return view.getElevation();
    }

    public static void setTranslationZ(View view, float translationZ) {
        view.setTranslationZ(translationZ);
    }

    public static float getTranslationZ(View view) {
        return view.getTranslationZ();
    }

    public static void setOnApplyWindowInsetsListener(View view,
            final OnApplyWindowInsetsListener listener) {
        if (listener == null) {
            view.setOnApplyWindowInsetsListener(null);
        } else {
            view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    // Wrap the framework insets in our wrapper
                    WindowInsetsCompatApi21 insets = new WindowInsetsCompatApi21(windowInsets);
                    // Give the listener a chance to use the wrapped insets
                    insets = (WindowInsetsCompatApi21) listener.onApplyWindowInsets(view, insets);
                    // Return the unwrapped insets
                    return insets.unwrap();
                }
            });
        }
    }

    public static boolean isImportantForAccessibility(View view) {
        return view.isImportantForAccessibility();
    }

    static ColorStateList getBackgroundTintList(View view) {
        return view.getBackgroundTintList();
    }

    static void setBackgroundTintList(View view, ColorStateList tintList) {
        view.setBackgroundTintList(tintList);

        if (Build.VERSION.SDK_INT == 21) {
            // Work around a bug in L that did not update the state of the background
            // after applying the tint
            Drawable background = view.getBackground();
            boolean hasTint = (view.getBackgroundTintList() != null)
                    && (view.getBackgroundTintMode() != null);
            if ((background != null) && hasTint) {
                if (background.isStateful()) {
                    background.setState(view.getDrawableState());
                }
                view.setBackground(background);
            }
        }
    }

    static PorterDuff.Mode getBackgroundTintMode(View view) {
        return view.getBackgroundTintMode();
    }

    static void setBackgroundTintMode(View view, PorterDuff.Mode mode) {
        view.setBackgroundTintMode(mode);

        if (Build.VERSION.SDK_INT == 21) {
            // Work around a bug in L that did not update the state of the background
            // after applying the tint
            Drawable background = view.getBackground();
            boolean hasTint = (view.getBackgroundTintList() != null)
                    && (view.getBackgroundTintMode() != null);
            if ((background != null) && hasTint) {
                if (background.isStateful()) {
                    background.setState(view.getDrawableState());
                }
                view.setBackground(background);
            }
        }
    }

    public static WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
        if (insets instanceof WindowInsetsCompatApi21) {
            // First unwrap the compat version so that we have the framework instance
            WindowInsets unwrapped = ((WindowInsetsCompatApi21) insets).unwrap();
            // Now call onApplyWindowInsets
            WindowInsets result = v.onApplyWindowInsets(unwrapped);

            if (result != unwrapped) {
                // ...and return a newly wrapped compat insets instance if different
                insets = new WindowInsetsCompatApi21(result);
            }
        }
        return insets;
    }

    public static WindowInsetsCompat dispatchApplyWindowInsets(View v, WindowInsetsCompat insets) {
        if (insets instanceof WindowInsetsCompatApi21) {
            // First unwrap the compat version so that we have the framework instance
            WindowInsets unwrapped = ((WindowInsetsCompatApi21) insets).unwrap();
            // Now call dispatchApplyWindowInsets
            WindowInsets result = v.dispatchApplyWindowInsets(unwrapped);

            if (result != unwrapped) {
                // ...and return a newly wrapped compat insets instance if different
                insets = new WindowInsetsCompatApi21(result);
            }
        }
        return insets;
    }

    public static void setNestedScrollingEnabled(View view, boolean enabled) {
        view.setNestedScrollingEnabled(enabled);
    }

    public static boolean isNestedScrollingEnabled(View view) {
        return view.isNestedScrollingEnabled();
    }

    public static boolean startNestedScroll(View view, int axes) {
        return view.startNestedScroll(axes);
    }

    public static void stopNestedScroll(View view) {
        view.stopNestedScroll();
    }

    public static boolean hasNestedScrollingParent(View view) {
        return view.hasNestedScrollingParent();
    }

    public static boolean dispatchNestedScroll(View view, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return view.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    public static boolean dispatchNestedPreScroll(View view, int dx, int dy, int[] consumed,
            int[] offsetInWindow) {
        return view.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    public static boolean dispatchNestedFling(View view, float velocityX, float velocityY,
            boolean consumed) {
        return view.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    public static boolean dispatchNestedPreFling(View view, float velocityX, float velocityY) {
        return view.dispatchNestedPreFling(velocityX, velocityY);
    }

    public static float getZ(View view) {
        return view.getZ();
    }

    public static void setZ(View view, float z) {
        view.setZ(z);
    }

    static void offsetTopAndBottom(final View view, final int offset) {
        final Rect parentRect = getEmptyTempRect();
        boolean needInvalidateWorkaround = false;

        final ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View p = (View) parent;
            parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
            // If the view currently does not currently intersect the parent (and is therefore
            // not displayed) we may need need to invalidate
            needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom());
        }

        // Now offset, invoking the API 11+ implementation (which contains it's own workarounds)
        ViewCompatHC.offsetTopAndBottom(view, offset);

        // The view has now been offset, so let's intersect the Rect and invalidate where
        // the View is now displayed
        if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(),
                view.getRight(), view.getBottom())) {
            ((View) parent).invalidate(parentRect);
        }
    }

    static void offsetLeftAndRight(final View view, final int offset) {
        final Rect parentRect = getEmptyTempRect();
        boolean needInvalidateWorkaround = false;

        final ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View p = (View) parent;
            parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
            // If the view currently does not currently intersect the parent (and is therefore
            // not displayed) we may need need to invalidate
            needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom());
        }

        // Now offset, invoking the API 11+ implementation (which contains it's own workarounds)
        ViewCompatHC.offsetLeftAndRight(view, offset);

        // The view has now been offset, so let's intersect the Rect and invalidate where
        // the View is now displayed
        if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(),
                view.getRight(), view.getBottom())) {
            ((View) parent).invalidate(parentRect);
        }
    }

    private static Rect getEmptyTempRect() {
        if (sThreadLocalRect == null) {
            sThreadLocalRect = new ThreadLocal<>();
        }
        Rect rect = sThreadLocalRect.get();
        if (rect == null) {
            rect = new Rect();
            sThreadLocalRect.set(rect);
        }
        rect.setEmpty();
        return rect;
    }
}
