/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AnimRes;
import androidx.annotation.AnimatorRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.strictmode.FragmentStrictMode;
import androidx.lifecycle.Lifecycle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Static library support version of the framework's {@link android.app.FragmentTransaction}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */
public abstract class FragmentTransaction {

    static final int OP_NULL = 0;
    static final int OP_ADD = 1;
    static final int OP_REPLACE = 2;
    static final int OP_REMOVE = 3;
    static final int OP_HIDE = 4;
    static final int OP_SHOW = 5;
    static final int OP_DETACH = 6;
    static final int OP_ATTACH = 7;
    static final int OP_SET_PRIMARY_NAV = 8;
    static final int OP_UNSET_PRIMARY_NAV = 9;
    static final int OP_SET_MAX_LIFECYCLE = 10;

    static final class Op {
        int mCmd;
        Fragment mFragment;
        boolean mFromExpandedOp;
        int mEnterAnim;
        int mExitAnim;
        int mPopEnterAnim;
        int mPopExitAnim;
        Lifecycle.State mOldMaxState;
        Lifecycle.State mCurrentMaxState;

        Op() {
        }

        Op(int cmd, Fragment fragment) {
            this.mCmd = cmd;
            this.mFragment = fragment;
            this.mFromExpandedOp = false;
            this.mOldMaxState = Lifecycle.State.RESUMED;
            this.mCurrentMaxState = Lifecycle.State.RESUMED;
        }

        Op(int cmd, Fragment fragment, boolean fromExpandedOp) {
            this.mCmd = cmd;
            this.mFragment = fragment;
            this.mFromExpandedOp = fromExpandedOp;
            this.mOldMaxState = Lifecycle.State.RESUMED;
            this.mCurrentMaxState = Lifecycle.State.RESUMED;
        }

        Op(int cmd, @NonNull Fragment fragment, Lifecycle.State state) {
            this.mCmd = cmd;
            this.mFragment = fragment;
            this.mFromExpandedOp = false;
            this.mOldMaxState = fragment.mMaxState;
            this.mCurrentMaxState = state;
        }

        Op(Op op) {
            this.mCmd = op.mCmd;
            this.mFragment = op.mFragment;
            this.mFromExpandedOp = op.mFromExpandedOp;
            this.mEnterAnim = op.mEnterAnim;
            this.mExitAnim = op.mExitAnim;
            this.mPopEnterAnim = op.mPopEnterAnim;
            this.mPopExitAnim = op.mPopExitAnim;
            this.mOldMaxState = op.mOldMaxState;
            this.mCurrentMaxState = op.mCurrentMaxState;
        }
    }

    private final FragmentFactory mFragmentFactory;
    private final ClassLoader mClassLoader;

    ArrayList<Op> mOps = new ArrayList<>();
    int mEnterAnim;
    int mExitAnim;
    int mPopEnterAnim;
    int mPopExitAnim;
    int mTransition;
    boolean mAddToBackStack;
    boolean mAllowAddToBackStack = true;
    @Nullable String mName;

    int mBreadCrumbTitleRes;
    CharSequence mBreadCrumbTitleText;
    int mBreadCrumbShortTitleRes;
    CharSequence mBreadCrumbShortTitleText;

    ArrayList<String> mSharedElementSourceNames;
    ArrayList<String> mSharedElementTargetNames;
    boolean mReorderingAllowed = false;

    ArrayList<Runnable> mCommitRunnables;

    /**
     * @deprecated You should not instantiate a FragmentTransaction except via
     * {@link FragmentManager#beginTransaction()}.
     */
    @Deprecated
    public FragmentTransaction() {
        mFragmentFactory = null;
        mClassLoader = null;
    }

    FragmentTransaction(@NonNull FragmentFactory fragmentFactory,
            @Nullable ClassLoader classLoader) {
        mFragmentFactory = fragmentFactory;
        mClassLoader = classLoader;
    }

    FragmentTransaction(@NonNull FragmentFactory fragmentFactory,
            @Nullable ClassLoader classLoader, @NonNull FragmentTransaction ft) {
        this(fragmentFactory, classLoader);
        for (Op op : ft.mOps) {
            mOps.add(new Op(op));
        }
        mEnterAnim = ft.mEnterAnim;
        mExitAnim = ft.mExitAnim;
        mPopEnterAnim = ft.mPopEnterAnim;
        mPopExitAnim = ft.mPopExitAnim;
        mTransition = ft.mTransition;
        mAddToBackStack = ft.mAddToBackStack;
        mAllowAddToBackStack = ft.mAllowAddToBackStack;
        mName = ft.mName;
        mBreadCrumbShortTitleRes = ft.mBreadCrumbShortTitleRes;
        mBreadCrumbShortTitleText = ft.mBreadCrumbShortTitleText;
        mBreadCrumbTitleRes = ft.mBreadCrumbTitleRes;
        mBreadCrumbTitleText = ft.mBreadCrumbTitleText;
        if (ft.mSharedElementSourceNames != null) {
            mSharedElementSourceNames = new ArrayList<>();
            mSharedElementSourceNames.addAll(ft.mSharedElementSourceNames);
        }
        if (ft.mSharedElementTargetNames != null) {
            mSharedElementTargetNames = new ArrayList<>();
            mSharedElementTargetNames.addAll(ft.mSharedElementTargetNames);
        }
        mReorderingAllowed = ft.mReorderingAllowed;
    }

    void addOp(Op op) {
        mOps.add(op);
        op.mEnterAnim = mEnterAnim;
        op.mExitAnim = mExitAnim;
        op.mPopEnterAnim = mPopEnterAnim;
        op.mPopExitAnim = mPopExitAnim;
    }

    private @NonNull Fragment createFragment(@NonNull Class<? extends Fragment> fragmentClass,
            @Nullable Bundle args) {
        if (mFragmentFactory == null) {
            throw new IllegalStateException("Creating a Fragment requires that this "
                    + "FragmentTransaction was built with FragmentManager.beginTransaction()");
        }
        if (mClassLoader == null) {
            throw new IllegalStateException("The FragmentManager must be attached to its"
                    + "host to create a Fragment");
        }
        Fragment fragment = mFragmentFactory.instantiate(mClassLoader, fragmentClass.getName());
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    /**
     * Calls {@link #add(int, Class, Bundle, String)} with a 0 containerViewId.
     */
    public final @NonNull FragmentTransaction add(@NonNull Class<? extends Fragment> fragmentClass,
            @Nullable Bundle args, @Nullable String tag)  {
        return add(createFragment(fragmentClass, args), tag);
    }

    /**
     * Calls {@link #add(int, Fragment, String)} with a 0 containerViewId.
     */
    public @NonNull FragmentTransaction add(@NonNull Fragment fragment, @Nullable String tag)  {
        doAddOp(0, fragment, tag, OP_ADD);
        return this;
    }

    /**
     * Calls {@link #add(int, Class, Bundle, String)} with a null tag.
     */
    public final @NonNull FragmentTransaction add(@IdRes int containerViewId,
            @NonNull Class<? extends Fragment> fragmentClass, @Nullable Bundle args)  {
        return add(containerViewId, createFragment(fragmentClass, args));
    }

    /**
     * Calls {@link #add(int, Fragment, String)} with a null tag.
     */
    public @NonNull FragmentTransaction add(@IdRes int containerViewId,
            @NonNull Fragment fragment) {
        doAddOp(containerViewId, fragment, null, OP_ADD);
        return this;
    }

    /**
     * Add a fragment to the activity state.  This fragment may optionally
     * also have its view (if {@link Fragment#onCreateView Fragment.onCreateView}
     * returns non-null) into a container view of the activity.
     *
     * @param containerViewId Optional identifier of the container this fragment is
     * to be placed in.  If 0, it will not be placed in a container.
     * @param fragmentClass The fragment to be added, created via the
     * {@link FragmentManager#getFragmentFactory() FragmentManager's FragmentFactory}.
     * @param args Optional arguments to be set on the fragment.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public final @NonNull FragmentTransaction add(@IdRes int containerViewId,
            @NonNull Class<? extends Fragment> fragmentClass,
            @Nullable Bundle args, @Nullable String tag) {
        return add(containerViewId, createFragment(fragmentClass, args), tag);
    }

    /**
     * Add a fragment to the activity state.  This fragment may optionally
     * also have its view (if {@link Fragment#onCreateView Fragment.onCreateView}
     * returns non-null) into a container view of the activity.
     *
     * @param containerViewId Optional identifier of the container this fragment is
     * to be placed in.  If 0, it will not be placed in a container.
     * @param fragment The fragment to be added.  This fragment must not already
     * be added to the activity.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction add(@IdRes int containerViewId, @NonNull Fragment fragment,
            @Nullable String tag) {
        doAddOp(containerViewId, fragment, tag, OP_ADD);
        return this;
    }

    /**
     * Add a fragment to the activity state.  This fragment may optionally
     * also have its view (if {@link Fragment#onCreateView Fragment.onCreateView}
     * returns non-null) into a container view of the activity.
     *
     * @param container The container this fragment is to be placed in. The id of the container
     *                  should be non-zero and unique.
     * @param fragment The fragment to be added.  This fragment must not already
     * be added to the activity.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public final @NonNull FragmentTransaction add(@NonNull ViewGroup container,
            @NonNull Fragment fragment, @Nullable String tag) {
        fragment.mContainer = container;
        fragment.mInDynamicContainer = true;
        return add(container.getId(), fragment, tag);
    }

    void doAddOp(int containerViewId, Fragment fragment, @Nullable String tag, int opcmd) {
        if (fragment.mPreviousWho != null) {
            FragmentStrictMode.onFragmentReuse(fragment, fragment.mPreviousWho);
        }
        final Class<?> fragmentClass = fragment.getClass();
        final int modifiers = fragmentClass.getModifiers();
        if (fragmentClass.isAnonymousClass() || !Modifier.isPublic(modifiers)
                || (fragmentClass.isMemberClass() && !Modifier.isStatic(modifiers))) {
            throw new IllegalStateException("Fragment " + fragmentClass.getCanonicalName()
                    + " must be a public static class to be  properly recreated from"
                    + " instance state.");
        }

        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment "
                        + fragment + ": was " + fragment.mTag
                        + " now " + tag);
            }
            fragment.mTag = tag;
        }

        if (containerViewId != 0) {
            if (containerViewId == View.NO_ID) {
                throw new IllegalArgumentException("Can't add fragment "
                        + fragment + " with tag " + tag + " to container view with no id");
            }
            if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment "
                        + fragment + ": was " + fragment.mFragmentId
                        + " now " + containerViewId);
            }
            fragment.mContainerId = fragment.mFragmentId = containerViewId;
        }

        addOp(new Op(opcmd, fragment));
    }

    /**
     * Calls {@link #replace(int, Class, Bundle, String)} with a null tag.
     */
    public final @NonNull FragmentTransaction replace(@IdRes int containerViewId,
            @NonNull Class<? extends Fragment> fragmentClass, @Nullable Bundle args) {
        return replace(containerViewId, fragmentClass, args, null);
    }

    /**
     * Calls {@link #replace(int, Fragment, String)} with a null tag.
     */
    public @NonNull FragmentTransaction replace(@IdRes int containerViewId,
            @NonNull Fragment fragment) {
        return replace(containerViewId, fragment, null);
    }

    /**
     * Replace an existing fragment that was added to a container.  This is
     * essentially the same as calling {@link #remove(Fragment)} for all
     * currently added fragments that were added with the same containerViewId
     * and then {@link #add(int, Fragment, String)} with the same arguments
     * given here.
     *
     * @param containerViewId Identifier of the container whose fragment(s) are
     * to be replaced.
     * @param fragmentClass The new fragment to place in the container, created via the
     * {@link FragmentManager#getFragmentFactory() FragmentManager's FragmentFactory}.
     * @param args Optional arguments to be set on the fragment.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public final @NonNull FragmentTransaction replace(@IdRes int containerViewId,
            @NonNull Class<? extends Fragment> fragmentClass,
            @Nullable Bundle args, @Nullable String tag) {
        return replace(containerViewId, createFragment(fragmentClass, args), tag);
    }

    /**
     * Replace an existing fragment that was added to a container.  This is
     * essentially the same as calling {@link #remove(Fragment)} for all
     * currently added fragments that were added with the same containerViewId
     * and then {@link #add(int, Fragment, String)} with the same arguments
     * given here.
     *
     * @param containerViewId Identifier of the container whose fragment(s) are
     * to be replaced.
     * @param fragment The new fragment to place in the container.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction replace(@IdRes int containerViewId,
            @NonNull Fragment fragment, @Nullable String tag)  {
        if (containerViewId == 0) {
            throw new IllegalArgumentException("Must use non-zero containerViewId");
        }
        doAddOp(containerViewId, fragment, tag, OP_REPLACE);
        return this;
    }

    /**
     * Remove an existing fragment.  If it was added to a container, its view
     * is also removed from that container.
     *
     * @param fragment The fragment to be removed.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction remove(@NonNull Fragment fragment) {
        addOp(new Op(OP_REMOVE, fragment));

        return this;
    }

    /**
     * Hides an existing fragment.  This is only relevant for fragments whose
     * views have been added to a container, as this will cause the view to
     * be hidden.
     *
     * @param fragment The fragment to be hidden.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction hide(@NonNull Fragment fragment) {
        addOp(new Op(OP_HIDE, fragment));

        return this;
    }

    /**
     * Shows a previously hidden fragment.  This is only relevant for fragments whose
     * views have been added to a container, as this will cause the view to
     * be shown.
     *
     * @param fragment The fragment to be shown.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction show(@NonNull Fragment fragment) {
        addOp(new Op(OP_SHOW, fragment));

        return this;
    }

    /**
     * Detach the given fragment from the UI.  This is the same state as
     * when it is put on the back stack: the fragment is removed from
     * the UI, however its state is still being actively managed by the
     * fragment manager.  When going into this state its view hierarchy
     * is destroyed.
     *
     * @param fragment The fragment to be detached.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction detach(@NonNull Fragment fragment) {
        addOp(new Op(OP_DETACH, fragment));

        return this;
    }

    /**
     * Re-attach a fragment after it had previously been detached from
     * the UI with {@link #detach(Fragment)}.  This
     * causes its view hierarchy to be re-created, attached to the UI,
     * and displayed.
     *
     * @param fragment The fragment to be attached.
     *
     * @return Returns the same FragmentTransaction instance.
     */
    public @NonNull FragmentTransaction attach(@NonNull Fragment fragment) {
        addOp(new Op(OP_ATTACH, fragment));

        return this;
    }

    /**
     * Set a currently active fragment in this FragmentManager as the primary navigation fragment.
     *
     * <p>The primary navigation fragment's
     * {@link Fragment#getChildFragmentManager() child FragmentManager} will be called first
     * to process delegated navigation actions such as {@link FragmentManager#popBackStack()}
     * if no ID or transaction name is provided to pop to. Navigation operations outside of the
     * fragment system may choose to delegate those actions to the primary navigation fragment
     * as returned by {@link FragmentManager#getPrimaryNavigationFragment()}.</p>
     *
     * <p>The fragment provided must currently be added to the FragmentManager to be set as
     * a primary navigation fragment, or previously added as part of this transaction.</p>
     *
     * @param fragment the fragment to set as the primary navigation fragment
     * @return the same FragmentTransaction instance
     */
    public @NonNull FragmentTransaction setPrimaryNavigationFragment(@Nullable Fragment fragment) {
        addOp(new Op(OP_SET_PRIMARY_NAV, fragment));

        return this;
    }

    /**
     * Set a ceiling for the state of an active fragment in this FragmentManager. If fragment is
     * already above the received state, it will be forced down to the correct state.
     *
     * <p>The fragment provided must currently be added to the FragmentManager to have it's
     * Lifecycle state capped, or previously added as part of this transaction. If the
     * {@link Lifecycle.State#INITIALIZED} is passed in as the {@link Lifecycle.State} and the
     * provided fragment has already moved beyond {@link Lifecycle.State#INITIALIZED}, an
     * {@link IllegalArgumentException} will be thrown.</p>
     *
     * <p>If the {@link Lifecycle.State#DESTROYED} is passed in as the {@link Lifecycle.State} an
     * {@link IllegalArgumentException} will be thrown.</p>
     *
     * @param fragment the fragment to have it's state capped.
     * @param state the ceiling state for the fragment.
     * @return the same FragmentTransaction instance
     */
    public @NonNull FragmentTransaction setMaxLifecycle(@NonNull Fragment fragment,
            Lifecycle.@NonNull State state) {
        addOp(new Op(OP_SET_MAX_LIFECYCLE, fragment, state));
        return this;
    }

    /**
     * @return <code>true</code> if this transaction contains no operations,
     * <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return mOps.isEmpty();
    }

    /**
     * Bit mask that is set for all enter transitions.
     */
    public static final int TRANSIT_ENTER_MASK = 0x1000;

    /**
     * Bit mask that is set for all exit transitions.
     */
    public static final int TRANSIT_EXIT_MASK = 0x2000;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({TRANSIT_NONE, TRANSIT_FRAGMENT_OPEN, TRANSIT_FRAGMENT_CLOSE, TRANSIT_FRAGMENT_FADE,
            TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN, TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Transit {}

    /** Not set up for a transition. */
    public static final int TRANSIT_UNSET = -1;
    /** No animation for transition. */
    public static final int TRANSIT_NONE = 0;
    /** Fragment is being added onto the stack */
    public static final int TRANSIT_FRAGMENT_OPEN = 1 | TRANSIT_ENTER_MASK;
    /** Fragment is being removed from the stack */
    public static final int TRANSIT_FRAGMENT_CLOSE = 2 | TRANSIT_EXIT_MASK;
    /** Fragment should simply fade in or out; that is, no strong navigation associated
     * with it except that it is appearing or disappearing for some reason. */
    public static final int TRANSIT_FRAGMENT_FADE = 3 | TRANSIT_ENTER_MASK;

    /**
     * Fragment is being added onto the stack with Activity open transition.
     *
     * @see android.R.attr#activityOpenEnterAnimation
     * @see android.R.attr#activityOpenExitAnimation
     */
    public static final int TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN = 4 | TRANSIT_ENTER_MASK;

    /**
     * Fragment is being removed from the stack with Activity close transition.
     *
     * @see android.R.attr#activityCloseEnterAnimation
     * @see android.R.attr#activityCloseExitAnimation
     */
    public static final int TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE = 5 | TRANSIT_EXIT_MASK;

    /**
     * Set specific animation resources to run for the fragments that are
     * entering and exiting in this transaction. These animations will not be
     * played when popping the back stack.
     *
     * <p>This method applies the custom animations to all future fragment operations; previous
     * operations are unaffected. Fragment operations in the same {@link FragmentTransaction} can
     * set different animations by calling this method prior to each operation, e.g:
     *
     * <pre class="prettyprint">
     *  fragmentManager.beginTransaction()
     *      .setCustomAnimations(enter1, exit1)
     *      .add(MyFragmentClass, args, tag1) // this fragment gets the first animations
     *      .setCustomAnimations(enter2, exit2)
     *      .add(MyFragmentClass, args, tag2) // this fragment gets the second animations
     *      .commit()
     * </pre>
     *
     * @param enter An animation or animator resource ID used for the enter animation on the
     *              view of the fragment being added or attached.
     * @param exit An animation or animator resource ID used for the exit animation on the
     *             view of the fragment being removed or detached.
     */
    public @NonNull FragmentTransaction setCustomAnimations(@AnimatorRes @AnimRes int enter,
            @AnimatorRes @AnimRes int exit) {
        return setCustomAnimations(enter, exit, 0, 0);
    }

    /**
     * Set specific animation resources to run for the fragments that are
     * entering and exiting in this transaction. The <code>popEnter</code>
     * and <code>popExit</code> animations will be played for enter/exit
     * operations specifically when popping the back stack.
     *
     * <p>This method applies the custom animations to all future fragment operations; previous
     * operations are unaffected. Fragment operations in the same {@link FragmentTransaction} can
     * set different animations by calling this method prior to each operation, e.g:
     *
     * <pre class="prettyprint">
     *  fragmentManager.beginTransaction()
     *      .setCustomAnimations(enter1, exit1, popEnter1, popExit1)
     *      .add(MyFragmentClass, args, tag1) // this fragment gets the first animations
     *      .setCustomAnimations(enter2, exit2, popEnter2, popExit2)
     *      .add(MyFragmentClass, args, tag2) // this fragment gets the second animations
     *      .commit()
     * </pre>
     *
     * @param enter An animation or animator resource ID used for the enter animation on the
     *              view of the fragment being added or attached.
     * @param exit An animation or animator resource ID used for the exit animation on the
     *             view of the fragment being removed or detached.
     * @param popEnter An animation or animator resource ID used for the enter animation on the
     *                 view of the fragment being readded or reattached caused by
     *                 {@link FragmentManager#popBackStack()} or similar methods.
     * @param popExit An animation or animator resource ID used for the enter animation on the
     *                view of the fragment being removed or detached caused by
     *                {@link FragmentManager#popBackStack()} or similar methods.
     */
    public @NonNull FragmentTransaction setCustomAnimations(@AnimatorRes @AnimRes int enter,
            @AnimatorRes @AnimRes int exit, @AnimatorRes @AnimRes int popEnter,
            @AnimatorRes @AnimRes int popExit) {
        mEnterAnim = enter;
        mExitAnim = exit;
        mPopEnterAnim = popEnter;
        mPopExitAnim = popExit;
        return this;
    }

    /**
     * Used with custom Transitions to map a View from a removed or hidden
     * Fragment to a View from a shown or added Fragment.
     * <var>sharedElement</var> must have a unique transitionName in the View hierarchy.
     *
     * @param sharedElement A View in a disappearing Fragment to match with a View in an
     *                      appearing Fragment.
     * @param name The transitionName for a View in an appearing Fragment to match to the shared
     *             element.
     * @see Fragment#setSharedElementReturnTransition(Object)
     * @see Fragment#setSharedElementEnterTransition(Object)
     */
    public @NonNull FragmentTransaction addSharedElement(@NonNull View sharedElement,
            @NonNull String name) {
        if (FragmentTransition.supportsTransition()) {
            String transitionName = ViewCompat.getTransitionName(sharedElement);
            if (transitionName == null) {
                throw new IllegalArgumentException("Unique transitionNames are required for all"
                        + " sharedElements");
            }
            if (mSharedElementSourceNames == null) {
                mSharedElementSourceNames = new ArrayList<>();
                mSharedElementTargetNames = new ArrayList<>();
            } else if (mSharedElementTargetNames.contains(name)) {
                throw new IllegalArgumentException("A shared element with the target name '"
                        + name + "' has already been added to the transaction.");
            } else if (mSharedElementSourceNames.contains(transitionName)) {
                throw new IllegalArgumentException("A shared element with the source name '"
                        + transitionName + "' has already been added to the transaction.");
            }

            mSharedElementSourceNames.add(transitionName);
            mSharedElementTargetNames.add(name);
        }
        return this;
    }

    /**
     * Select a standard transition animation for this transaction.  May be
     * one of {@link #TRANSIT_NONE}, {@link #TRANSIT_FRAGMENT_OPEN},
     * {@link #TRANSIT_FRAGMENT_CLOSE}, or {@link #TRANSIT_FRAGMENT_FADE}.
     */
    public @NonNull FragmentTransaction setTransition(@Transit int transition) {
        mTransition = transition;
        return this;
    }

    /**
     * Set a custom style resource that will be used for resolving transit
     * animations.
     *
     * @deprecated The desired functionality never worked correctly. This should not be used.
     */
    @Deprecated
    public @NonNull FragmentTransaction setTransitionStyle(@StyleRes int styleRes) {
        return this;
    }

    /**
     * Add this transaction to the back stack.  This means that the transaction
     * will be remembered after it is committed, and will reverse its operation
     * when later popped off the stack.
     * <p>
     * {@link #setReorderingAllowed(boolean)} must be set to <code>true</code>
     * in the same transaction as addToBackStack() to allow the pop of that
     * transaction to be reordered.
     *
     * @param name An optional name for this back stack state, or null.
     */
    public @NonNull FragmentTransaction addToBackStack(@Nullable String name) {
        if (!mAllowAddToBackStack) {
            throw new IllegalStateException(
                    "This FragmentTransaction is not allowed to be added to the back stack.");
        }
        mAddToBackStack = true;
        mName = name;
        return this;
    }

    /**
     * Returns true if this FragmentTransaction is allowed to be added to the back
     * stack. If this method would return false, {@link #addToBackStack(String)}
     * will throw {@link IllegalStateException}.
     *
     * @return True if {@link #addToBackStack(String)} is permitted on this transaction.
     */
    public boolean isAddToBackStackAllowed() {
        return mAllowAddToBackStack;
    }

    /**
     * Disallow calls to {@link #addToBackStack(String)}. Any future calls to
     * addToBackStack will throw {@link IllegalStateException}. If addToBackStack
     * has already been called, this method will throw IllegalStateException.
     */
    public @NonNull FragmentTransaction disallowAddToBackStack() {
        if (mAddToBackStack) {
            throw new IllegalStateException(
                    "This transaction is already being added to the back stack");
        }
        mAllowAddToBackStack = false;
        return this;
    }

    /**
     * Set the full title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     * @deprecated Store breadcrumb titles separately from fragment transactions. For
     * example, by using an <code>android:label</code> on a fragment in a navigation graph.
     */
    @Deprecated
    public @NonNull FragmentTransaction setBreadCrumbTitle(@StringRes int res) {
        mBreadCrumbTitleRes = res;
        mBreadCrumbTitleText = null;
        return this;
    }

    /**
     * Like {@link #setBreadCrumbTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     * @deprecated Store breadcrumb titles separately from fragment transactions. For
     * example, by using an <code>android:label</code> on a fragment in a navigation graph.
     */
    @Deprecated
    public @NonNull FragmentTransaction setBreadCrumbTitle(@Nullable CharSequence text) {
        mBreadCrumbTitleRes = 0;
        mBreadCrumbTitleText = text;
        return this;
    }

    /**
     * Set the short title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     * @deprecated Store breadcrumb short titles separately from fragment transactions. For
     * example, by using an <code>android:label</code> on a fragment in a navigation graph.
     */
    @Deprecated
    public @NonNull FragmentTransaction setBreadCrumbShortTitle(@StringRes int res) {
        mBreadCrumbShortTitleRes = res;
        mBreadCrumbShortTitleText = null;
        return this;
    }

    /**
     * Like {@link #setBreadCrumbShortTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     * @deprecated Store breadcrumb short titles separately from fragment transactions. For
     * example, by using an <code>android:label</code> on a fragment in a navigation graph.
     */
    @Deprecated
    public @NonNull FragmentTransaction setBreadCrumbShortTitle(@Nullable CharSequence text) {
        mBreadCrumbShortTitleRes = 0;
        mBreadCrumbShortTitleText = text;
        return this;
    }

    /**
     * Sets whether or not to allow optimizing operations within and across
     * transactions. This will remove redundant operations, eliminating
     * operations that cancel. For example, if two transactions are executed
     * together, one that adds a fragment A and the next replaces it with fragment B,
     * the operations will cancel and only fragment B will be added. That means that
     * fragment A may not go through the creation/destruction lifecycle.
     * <p>
     * The side effect of removing redundant operations is that fragments may have state changes
     * out of the expected order. For example, one transaction adds fragment A,
     * a second adds fragment B, then a third removes fragment A. Without removing the redundant
     * operations, fragment B could expect that while it is being created, fragment A will also
     * exist because fragment A will be removed after fragment B was added.
     * With removing redundant operations, fragment B cannot expect fragment A to exist when
     * it has been created because fragment A's add/remove will be optimized out.
     * <p>
     * It can also reorder the state changes of Fragments to allow for better Transitions.
     * Added Fragments may have {@link Fragment#onCreate(Bundle)} called before replaced
     * Fragments have {@link Fragment#onDestroy()} called.
     * <p>
     * {@link Fragment#postponeEnterTransition()} requires {@code setReorderingAllowed(true)}.
     * <p>
     * The default is {@code false}.
     *
     * @param reorderingAllowed {@code true} to enable optimizing out redundant operations
     *                          or {@code false} to disable optimizing out redundant
     *                          operations on this transaction.
     */
    public @NonNull FragmentTransaction setReorderingAllowed(boolean reorderingAllowed) {
        mReorderingAllowed = reorderingAllowed;
        return this;
    }

    /**
     * @deprecated This has been renamed {@link #setReorderingAllowed(boolean)}.
     */
    @Deprecated
    public @NonNull FragmentTransaction setAllowOptimization(boolean allowOptimization) {
        return setReorderingAllowed(allowOptimization);
    }

    /**
     * Add a Runnable to this transaction that will be run after this transaction has
     * been committed. If fragment transactions are {@link #setReorderingAllowed(boolean) optimized}
     * this may be after other subsequent fragment operations have also taken place, or operations
     * in this transaction may have been optimized out due to the presence of a subsequent
     * fragment transaction in the batch.
     *
     * <p>If a transaction is committed using {@link #commitAllowingStateLoss()} this runnable
     * may be executed when the FragmentManager is in a state where new transactions may not
     * be committed without allowing state loss.</p>
     *
     * <p><code>runOnCommit</code> may not be used with transactions
     * {@link #addToBackStack(String) added to the back stack} as Runnables cannot be persisted
     * with back stack state. {@link IllegalStateException} will be thrown if
     * {@link #addToBackStack(String)} has been previously called for this transaction
     * or if it is called after a call to <code>runOnCommit</code>.</p>
     *
     * @param runnable Runnable to add
     * @return this FragmentTransaction
     * @throws IllegalStateException if {@link #addToBackStack(String)} has been called
     */
    public @NonNull FragmentTransaction runOnCommit(@NonNull Runnable runnable) {
        return runOnCommitInternal(false, runnable);
    }

    @NonNull FragmentTransaction runOnCommitInternal(
            boolean allowAddToBackStack,
            @NonNull Runnable runnable
    ) {
        if (!allowAddToBackStack) {
            disallowAddToBackStack();
        }
        if (mCommitRunnables == null) {
            mCommitRunnables = new ArrayList<>();
        }
        mCommitRunnables.add(runnable);
        return this;
    }

    /**
     * Schedules a commit of this transaction.  The commit does
     * not happen immediately; it will be scheduled as work on the main thread
     * to be done the next time that thread is ready.
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     *
     * @return Returns the identifier of this transaction's back stack entry,
     * if {@link #addToBackStack(String)} had been called.  Otherwise, returns
     * a negative number.
     */
    public abstract int commit();

    /**
     * Like {@link #commit} but allows the commit to be executed after an
     * activity's state is saved.  This is dangerous because the commit can
     * be lost if the activity needs to later be restored from its state, so
     * this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    public abstract int commitAllowingStateLoss();

    /**
     * Commits this transaction synchronously. Any added fragments will be
     * initialized and brought completely to the lifecycle state of their host
     * and any removed fragments will be torn down accordingly before this
     * call returns. Committing a transaction in this way allows fragments
     * to be added as dedicated, encapsulated components that monitor the
     * lifecycle state of their host while providing firmer ordering guarantees
     * around when those fragments are fully initialized and ready. Fragments
     * that manage views will have those views created and attached.
     *
     * <p>Calling <code>commitNow</code> is preferable to calling
     * {@link #commit()} followed by {@link FragmentManager#executePendingTransactions()}
     * as the latter will have the side effect of attempting to commit <em>all</em>
     * currently pending transactions whether that is the desired behavior
     * or not.</p>
     *
     * <p>Transactions committed in this way may not be added to the
     * FragmentManager's back stack, as doing so would break other expected
     * ordering guarantees for other asynchronously committed transactions.
     * This method will throw {@link IllegalStateException} if the transaction
     * previously requested to be added to the back stack with
     * {@link #addToBackStack(String)}.</p>
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     */
    @MainThread
    public abstract void commitNow();

    /**
     * Like {@link #commitNow} but allows the commit to be executed after an
     * activity's state is saved.  This is dangerous because the commit can
     * be lost if the activity needs to later be restored from its state, so
     * this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    @MainThread
    public abstract void commitNowAllowingStateLoss();
}
