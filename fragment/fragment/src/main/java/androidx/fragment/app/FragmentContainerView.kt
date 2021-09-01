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

package androidx.fragment.app;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.R;

import java.util.ArrayList;

/**
 * FragmentContainerView is a customized Layout designed specifically for Fragments. It extends
 * {@link FrameLayout}, so it can reliably handle Fragment Transactions, and it also has additional
 * features to coordinate with fragment behavior.
 *
 * <p>FragmentContainerView should be used as the container for Fragments, commonly set in the
 * xml layout of an activity, e.g.: <p>
 *
 * <pre class="prettyprint">
 * &lt;androidx.fragment.app.FragmentContainerView
 *        xmlns:android="http://schemas.android.com/apk/res/android"
 *        xmlns:app="http://schemas.android.com/apk/res-auto"
 *        android:id="@+id/fragment_container_view"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent"&gt;
 * &lt;/androidx.fragment.app.FragmentContainerView&gt;
 * </pre>
 *
 * <p> FragmentContainerView can also be used to add a Fragment by using the
 * <code>android:name</code> attribute. FragmentContainerView will perform a one time operation
 * that:
 *
 * <ul>
 * <li>Creates a new instance of the Fragment</li>
 * <li>Calls {@link Fragment#onInflate(Context, AttributeSet, Bundle)}</li>
 * <li>Executes a FragmentTransaction to add the Fragment to the appropriate FragmentManager</li>
 * </ul>
 *
 * <p> You can optionally include an <code>android:tag</code> which allows you to use
 * {@link FragmentManager#findFragmentByTag(String)} to retrieve the added Fragment.
 *
 * <pre class="prettyprint">
 * &lt;androidx.fragment.app.FragmentContainerView
 *        xmlns:android="http://schemas.android.com/apk/res/android"
 *        xmlns:app="http://schemas.android.com/apk/res-auto"
 *        android:id="@+id/fragment_container_view"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent"
 *        android:name="com.example.MyFragment"
 *        android:tag="my_tag"&gt;
 * &lt;/androidx.fragment.app.FragmentContainerView&gt;
 * </pre>
 *
 * <p>FragmentContainerView should not be used as a replacement for other ViewGroups (FrameLayout,
 * LinearLayout, etc) outside of Fragment use cases.
 *
 * <p>FragmentContainerView will only allow views returned by a Fragment's
 * {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Attempting to add any other
 * view will result in an {@link IllegalStateException}.
 *
 * <p>Layout animations and transitions are disabled for FragmentContainerView for APIs above 17.
 * Otherwise, Animations should be done through
 * {@link FragmentTransaction#setCustomAnimations(int, int, int, int)}. If animateLayoutChanges is
 * set to <code>true</code> or {@link #setLayoutTransition(LayoutTransition)} is called directly an
 * {@link UnsupportedOperationException} will be thrown.
 *
 * <p>Fragments using exit animations are drawn before all others for FragmentContainerView. This
 * ensures that exiting Fragments do not appear on top of the view.
 */
public final class FragmentContainerView extends FrameLayout {

    private ArrayList<View> mDisappearingFragmentChildren;

    private ArrayList<View> mTransitioningFragmentViews;

    private OnApplyWindowInsetsListener mApplyWindowInsetsListener;

    // Used to indicate whether the FragmentContainerView should override the default ViewGroup
    // drawing order.
    private boolean mDrawDisappearingViewsFirst = true;

    public FragmentContainerView(@NonNull Context context) {
        super(context);
    }

    /**
     * Do not call this constructor directly. Doing so will result in an
     * {@link UnsupportedOperationException}.
     */
    public FragmentContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Do not call this constructor directly. Doing so will result in an
     * {@link UnsupportedOperationException}.
     */
    public FragmentContainerView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            String name = attrs.getClassAttribute();
            String attribute = "class";
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FragmentContainerView);
            if (name == null) {
                name = a.getString(R.styleable.FragmentContainerView_android_name);
                attribute = "android:name";
            }
            a.recycle();
            if (name != null && !isInEditMode()) {
                throw new UnsupportedOperationException("FragmentContainerView must be within "
                        + "a FragmentActivity to use " + attribute + "=\"" + name + "\"");
            }
        }
    }

    FragmentContainerView(
            @NonNull Context context,
            @NonNull AttributeSet attrs,
            @NonNull FragmentManager fm) {
        super(context, attrs);

        String name = attrs.getClassAttribute();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FragmentContainerView);
        if (name == null) {
            name = a.getString(R.styleable.FragmentContainerView_android_name);
        }
        String tag = a.getString(R.styleable.FragmentContainerView_android_tag);
        a.recycle();

        int id = getId();
        Fragment existingFragment = fm.findFragmentById(id);
        // If there is a name and there is no existing fragment,
        // we should add an inflated Fragment to the view.
        if (name != null && existingFragment == null) {
            if (id <= 0) {
                final String tagMessage = tag != null
                        ? " with tag " + tag
                        : "";
                throw new IllegalStateException("FragmentContainerView must have an android:id to "
                        + "add Fragment " + name + tagMessage);
            }
            Fragment containerFragment =
                    fm.getFragmentFactory().instantiate(context.getClassLoader(), name);
            containerFragment.onInflate(context, attrs, null);
            fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(this, containerFragment, tag)
                    .commitNowAllowingStateLoss();
        }
        fm.onContainerAvailable(this);
    }

    /**
     * When called, this method throws a {@link UnsupportedOperationException} on APIs above 17.
     * On APIs 17 and below, it calls {@link FrameLayout#setLayoutTransition(LayoutTransition)}
     * This can be called either explicitly, or implicitly by setting animateLayoutChanges to
     * <code>true</code>.
     *
     * <p>View animations and transitions are disabled for FragmentContainerView for APIs above 17.
     * Use {@link FragmentTransaction#setCustomAnimations(int, int, int, int)} and
     * {@link FragmentTransaction#setTransition(int)}.
     *
     * @param transition The LayoutTransition object that will animated changes in layout. A value
     * of <code>null</code> means no transition will run on layout changes.
     * @attr ref android.R.styleable#ViewGroup_animateLayoutChanges
     */
    @Override
    public void setLayoutTransition(@Nullable LayoutTransition transition) {
        if (Build.VERSION.SDK_INT < 18) {
            // Transitions on APIs below 18 are using an empty LayoutTransition as a replacement
            // for suppressLayout(true) and null LayoutTransition to then unsuppress it. If the
            // API is below 18, we should allow FrameLayout to handle this call.
            super.setLayoutTransition(transition);
            return;
        }

        throw new UnsupportedOperationException(
                "FragmentContainerView does not support Layout Transitions or "
                        + "animateLayoutChanges=\"true\".");
    }

    @Override
    public void setOnApplyWindowInsetsListener(@NonNull OnApplyWindowInsetsListener listener) {
        mApplyWindowInsetsListener = listener;
    }

    @NonNull
    @RequiresApi(20)
    @Override
    public WindowInsets onApplyWindowInsets(@NonNull WindowInsets insets) {
        return insets;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The sys ui flags must be set to enable extending the layout into the window insets.
     */
    @NonNull
    @RequiresApi(20)
    @Override
    public WindowInsets dispatchApplyWindowInsets(@NonNull WindowInsets insets) {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets);
        WindowInsetsCompat dispatchInsets = mApplyWindowInsetsListener != null
                ? WindowInsetsCompat.toWindowInsetsCompat(
                Api20Impl.onApplyWindowInsets(mApplyWindowInsetsListener, this, insets)
        ) : ViewCompat.onApplyWindowInsets(this, insetsCompat);
        if (!dispatchInsets.isConsumed()) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                ViewCompat.dispatchApplyWindowInsets(getChildAt(i), dispatchInsets);
            }
        }
        return insets;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (mDrawDisappearingViewsFirst && mDisappearingFragmentChildren != null) {
            for (int i = 0; i < mDisappearingFragmentChildren.size(); i++) {
                super.drawChild(canvas, mDisappearingFragmentChildren.get(i), getDrawingTime());
            }
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
        if (mDrawDisappearingViewsFirst && mDisappearingFragmentChildren != null
                && mDisappearingFragmentChildren.size() > 0) {
            // If the child is disappearing, we have already drawn it so skip.
            if (mDisappearingFragmentChildren.contains(child)) {
                return false;
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void startViewTransition(@NonNull View view) {
        if (view.getParent() == this) {
            if (mTransitioningFragmentViews == null) {
                mTransitioningFragmentViews = new ArrayList<>();
            }
            mTransitioningFragmentViews.add(view);
        }
        super.startViewTransition(view);
    }

    @Override
    public void endViewTransition(@NonNull View view) {
        if (mTransitioningFragmentViews != null) {
            mTransitioningFragmentViews.remove(view);
            if (mDisappearingFragmentChildren != null
                    && mDisappearingFragmentChildren.remove(view)) {
                mDrawDisappearingViewsFirst = true;
            }
        }
        super.endViewTransition(view);
    }

    // Used to indicate the container should change the default drawing order.
    void setDrawDisappearingViewsLast(boolean drawDisappearingViewsFirst) {
        mDrawDisappearingViewsFirst = drawDisappearingViewsFirst;
    }

    /**
     * <p>FragmentContainerView will only allow views returned by a Fragment's
     * {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Attempting to add any
     *  other view will result in an {@link IllegalStateException}.
     *
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, int index, @Nullable ViewGroup.LayoutParams params) {
        if (FragmentManager.getViewFragment(child) == null) {
            throw new IllegalStateException("Views added to a FragmentContainerView must be"
                    + " associated with a Fragment. View " + child + " is not associated with a"
                    + " Fragment.");
        }
        super.addView(child, index, params);
    }

    /**
     * <p>FragmentContainerView will only allow views returned by a Fragment's
     * {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Attempting to add any
     *  other view will result in an {@link IllegalStateException}.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean addViewInLayout(@NonNull View child, int index,
            @Nullable ViewGroup.LayoutParams params, boolean preventRequestLayout) {
        if (FragmentManager.getViewFragment(child) == null) {
            throw new IllegalStateException("Views added to a FragmentContainerView must be"
                    + " associated with a Fragment. View " + child + " is not associated with a"
                    + " Fragment.");
        }
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    @Override
    public void removeViewAt(int index) {
        View view = getChildAt(index);
        addDisappearingFragmentView(view);
        super.removeViewAt(index);
    }

    @Override
    public void removeViewInLayout(@NonNull View view) {
        addDisappearingFragmentView(view);
        super.removeViewInLayout(view);
    }

    @Override
    public void removeView(@NonNull View view) {
        addDisappearingFragmentView(view);
        super.removeView(view);
    }

    @Override
    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            final View view = getChildAt(i);
            addDisappearingFragmentView(view);
        }
        super.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            final View view = getChildAt(i);
            addDisappearingFragmentView(view);
        }
        super.removeViewsInLayout(start, count);
    }

    @Override
    public void removeAllViewsInLayout() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            addDisappearingFragmentView(view);
        }
        super.removeAllViewsInLayout();
    }

    @Override
    protected void removeDetachedView(@NonNull View child, boolean animate) {
        if (animate) {
            addDisappearingFragmentView(child);
        }
        super.removeDetachedView(child, animate);
    }

    /**
     * This method adds a {@link View} to the list of disappearing views only if it meets the
     * proper conditions to be considered a disappearing view.
     *
     * @param v {@link View} that might be added to list of disappearing views
     */
    private void addDisappearingFragmentView(@NonNull View v) {
        if (mTransitioningFragmentViews != null && mTransitioningFragmentViews.contains(v)) {
            if (mDisappearingFragmentChildren == null) {
                mDisappearingFragmentChildren = new ArrayList<>();
            }
            mDisappearingFragmentChildren.add(v);
        }
    }

    /**
     * This method grabs the {@link Fragment} whose view was most recently
     * added to the container. This may used as an alternative to calling
     * {@link FragmentManager#findFragmentById(int)} and passing in the
     * {@link FragmentContainerView}'s id.
     *
     * @return The fragment if any exist, null otherwise.
     */
    @Nullable
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"}) // a ClassCastException is
    // automatically thrown if the given type of F is wrong
    public <F extends Fragment> F getFragment() {
        return (F) FragmentManager.findFragmentManager(this).findFragmentById(this.getId());
    }

    @RequiresApi(20)
    static class Api20Impl {
        private Api20Impl() { }

        static WindowInsets onApplyWindowInsets(
                @NonNull OnApplyWindowInsetsListener onApplyWindowInsetsListener,
                @NonNull View v,
                @NonNull WindowInsets insets
        ) {
            return onApplyWindowInsetsListener.onApplyWindowInsets(v, insets);
        }
    }
}
