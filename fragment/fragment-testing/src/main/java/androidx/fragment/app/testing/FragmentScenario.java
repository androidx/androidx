/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.fragment.app.testing;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.core.util.Preconditions;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.testing.R;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;

/**
 * FragmentScenario provides API to start and drive a Fragment's lifecycle state for testing. It
 * works with arbitrary fragments and works consistently across different versions of the Android
 * framework.
 * <p>
 * FragmentScenario only supports {@link Fragment androidx.fragment.app.Fragment}. If you are using
 * a deprecated fragment class such as {@code android.support.v4.app.Fragment} or
 * {@link android.app.Fragment android.app.Fragment}, please update your code to
 * {@link Fragment androidx.fragment.app.Fragment}.
 * <p>
 * If your testing Fragment has a dependency to specific theme such as {@code Theme.AppCompat},
 * use the theme ID parameter in {@link #launch} method.
 *
 * @param <F> The Fragment class being tested
 *
 * @see ActivityScenario a scenario API for Activity
 */
public final class FragmentScenario<F extends Fragment> {

    private static final String FRAGMENT_TAG = "FragmentScenario_Fragment_Tag";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Class<F> mFragmentClass;
    private final ActivityScenario<EmptyFragmentActivity> mActivityScenario;

    /**
     * An empty activity inheriting FragmentActivity. This Activity is used to host Fragment in
     * FragmentScenario.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static class EmptyFragmentActivity extends FragmentActivity {

        @NonNull
        public static final String THEME_EXTRAS_BUNDLE_KEY =
                "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity"
                        + ".THEME_EXTRAS_BUNDLE_KEY";

        @Override
        @SuppressLint("RestrictedApi")
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            setTheme(getIntent().getIntExtra(THEME_EXTRAS_BUNDLE_KEY,
                    R.style.FragmentScenarioEmptyFragmentActivityTheme));

            // Checks if we have a custom FragmentFactory and set it.
            FragmentFactory factory = FragmentFactoryHolderViewModel.getInstance(this)
                    .getFragmentFactory();
            if (factory != null) {
                getSupportFragmentManager().setFragmentFactory(factory);
            }

            // FragmentFactory needs to be set before calling the super.onCreate, otherwise the
            // Activity crashes when it is recreating and there is a fragment which has no
            // default constructor.
            super.onCreate(savedInstanceState);
        }
    }

    /**
     * A view-model to hold a fragment factory.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static class FragmentFactoryHolderViewModel extends ViewModel {

        private static final ViewModelProvider.Factory FACTORY = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                FragmentFactoryHolderViewModel viewModel = new FragmentFactoryHolderViewModel();
                return (T) viewModel;
            }
        };

        @NonNull
        static FragmentFactoryHolderViewModel getInstance(@NonNull FragmentActivity activity) {
            ViewModelProvider viewModelProvider = new ViewModelProvider(activity, FACTORY);
            return viewModelProvider.get(FragmentFactoryHolderViewModel.class);
        }

        @Nullable private FragmentFactory mFragmentFactory;

        void setFragmentFactory(@Nullable FragmentFactory factory) {
            mFragmentFactory = factory;
        }

        @Nullable
        FragmentFactory getFragmentFactory() {
            return mFragmentFactory;
        }

        @Override
        protected void onCleared() {
            super.onCleared();
            mFragmentFactory = null;
        }
    }

    private FragmentScenario(
            @NonNull Class<F> fragmentClass,
            @NonNull ActivityScenario<EmptyFragmentActivity> activityScenario) {
        this.mFragmentClass = fragmentClass;
        this.mActivityScenario = activityScenario;
    }

    /**
     * Launches a Fragment hosted by an empty {@link FragmentActivity} and waits for it to reach
     * the resumed state.
     *
     * @param fragmentClass a fragment class to instantiate
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launch(
            @NonNull Class<F> fragmentClass) {
        return launch(fragmentClass, /*fragmentArgs=*/ null);
    }

    /**
     * Launches a Fragment with given arguments hosted by an empty {@link FragmentActivity} and
     * waits for it to reach the resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     * @param fragmentArgs a bundle to passed into fragment
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launch(
            @NonNull Class<F> fragmentClass, @Nullable Bundle fragmentArgs) {
        return launch(fragmentClass, fragmentArgs, /*factory=*/null);
    }

    /**
     * Launches a Fragment with given arguments hosted by an empty {@link FragmentActivity} using
     * the given {@link FragmentFactory} and waits for it to reach the resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     * @param fragmentArgs a bundle to passed into fragment
     * @param factory a fragment factory to use or null to use default factory
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launch(
            @NonNull Class<F> fragmentClass, @Nullable Bundle fragmentArgs,
            @Nullable FragmentFactory factory) {
        return launch(fragmentClass, fragmentArgs,
                R.style.FragmentScenarioEmptyFragmentActivityTheme, factory);
    }

    /**
     * Launches a Fragment with given arguments hosted by an empty {@link FragmentActivity} themed
     * by {@code themeResId}, using the given {@link FragmentFactory} and waits for it to reach the
     * resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     * @param fragmentArgs a bundle to passed into fragment
     * @param themeResId a style resource id to be set to the host activity's theme
     * @param factory a fragment factory to use or null to use default factory
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launch(
            @NonNull Class<F> fragmentClass, @Nullable Bundle fragmentArgs,
            @StyleRes int themeResId, @Nullable FragmentFactory factory) {
        return internalLaunch(fragmentClass, fragmentArgs, themeResId, factory,
                /*containerViewId=*/ 0);
    }

    /**
     * Launches a Fragment in the Activity's root view container {@code android.R.id.content},
     * hosted by an empty {@link FragmentActivity} and waits for it to reach the resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launchInContainer(
            @NonNull Class<F> fragmentClass) {
        return launchInContainer(fragmentClass, /*fragmentArgs=*/ null);
    }

    /**
     * Launches a Fragment in the Activity's root view container {@code android.R.id.content}, with
     * given arguments hosted by an empty {@link FragmentActivity} and waits for it to reach the
     * resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     * @param fragmentArgs a bundle to passed into fragment
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launchInContainer(
            @NonNull Class<F> fragmentClass, @Nullable Bundle fragmentArgs) {
        return launchInContainer(fragmentClass, fragmentArgs, /*factory=*/null);
    }

    /**
     * Launches a Fragment in the Activity's root view container {@code android.R.id.content}, with
     * given arguments hosted by an empty {@link FragmentActivity} using the given
     * {@link FragmentFactory} and waits for it to reach the resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     * @param fragmentArgs a bundle to passed into fragment
     * @param factory a fragment factory to use or null to use default factory
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launchInContainer(
            @NonNull Class<F> fragmentClass, @Nullable Bundle fragmentArgs,
            @Nullable FragmentFactory factory) {
        return launchInContainer(fragmentClass, fragmentArgs,
                R.style.FragmentScenarioEmptyFragmentActivityTheme, factory);
    }

    /**
     * Launches a Fragment in the Activity's root view container {@code android.R.id.content}, with
     * given arguments hosted by an empty {@link FragmentActivity} themed by {@code themeResId},
     * using the given {@link FragmentFactory} and waits for it to reach the resumed state.
     * <p>
     * This method cannot be called from the main thread.
     *
     * @param fragmentClass a fragment class to instantiate
     * @param fragmentArgs a bundle to passed into fragment
     * @param themeResId a style resource id to be set to the host activity's theme
     * @param factory a fragment factory to use or null to use default factory
     */
    @NonNull
    public static <F extends Fragment> FragmentScenario<F> launchInContainer(
            @NonNull Class<F> fragmentClass, @Nullable Bundle fragmentArgs,
            @StyleRes int themeResId, @Nullable FragmentFactory factory) {
        return internalLaunch(
                fragmentClass, fragmentArgs, themeResId, factory, android.R.id.content);
    }

    @NonNull
    @SuppressLint("RestrictedApi")
    private static <F extends Fragment> FragmentScenario<F> internalLaunch(
            @NonNull final Class<F> fragmentClass, final @Nullable Bundle fragmentArgs,
            @StyleRes int themeResId, @Nullable final FragmentFactory factory,
            @IdRes final int containerViewId) {
        Intent startActivityIntent =
                Intent.makeMainActivity(
                        new ComponentName(getApplicationContext(),
                                EmptyFragmentActivity.class))
                        .putExtra(EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY, themeResId);
        FragmentScenario<F> scenario = new FragmentScenario<>(
                fragmentClass,
                ActivityScenario.<EmptyFragmentActivity>launch(startActivityIntent));
        scenario.mActivityScenario.onActivity(
                new ActivityScenario.ActivityAction<EmptyFragmentActivity>() {
                    @Override
                    public void perform(EmptyFragmentActivity activity) {
                        if (factory != null) {
                            FragmentFactoryHolderViewModel.getInstance(activity)
                                    .setFragmentFactory(factory);
                            activity.getSupportFragmentManager().setFragmentFactory(factory);
                        }
                        Fragment fragment = activity.getSupportFragmentManager()
                                .getFragmentFactory().instantiate(
                                        Preconditions.checkNotNull(fragmentClass.getClassLoader()),
                                        fragmentClass.getName());
                        fragment.setArguments(fragmentArgs);
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .add(containerViewId, fragment, FRAGMENT_TAG)
                                .commitNow();
                    }
                });
        return scenario;
    }

    /**
     * Moves Fragment state to a new state.
     * <p> If a new state and current state are the same, this method does nothing. It accepts
     * {@link State#CREATED CREATED}, {@link State#STARTED STARTED}, {@link State#RESUMED RESUMED},
     * and {@link State#DESTROYED DESTROYED}. {@link State#DESTROYED DESTROYED} is a terminal state.
     * You cannot move to any other state after the Fragment reaches that state.
     * <p> This method cannot be called from the main thread.
     * <p><em>Note: Moving state to {@link State#STARTED STARTED} is not supported on Android API
     * level 23 and lower. {@link UnsupportedOperationException} will be thrown.</em>
     */
    @NonNull
    public FragmentScenario<F> moveToState(@NonNull State newState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && newState == State.STARTED) {
            throw new UnsupportedOperationException(
                    "Moving state to STARTED is not supported on Android API level 23 and lower."
                    + " This restriction comes from the combination of the Android framework bug"
                    + " around the timing of onSaveInstanceState invocation and its workaround code"
                    + " in FragmentActivity. See http://issuetracker.google.com/65665621#comment3"
                    + " for more information.");
        }
        if (newState == State.DESTROYED) {
            mActivityScenario.onActivity(
                    new ActivityScenario.ActivityAction<EmptyFragmentActivity>() {
                        @Override
                        public void perform(EmptyFragmentActivity activity) {
                            Fragment fragment =
                                    activity.getSupportFragmentManager().findFragmentByTag(
                                            FRAGMENT_TAG);
                            // Null means the fragment has been destroyed already.
                            if (fragment != null) {
                                activity
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .remove(fragment)
                                        .commitNowAllowingStateLoss();
                            }
                        }
                    });
        } else {
            mActivityScenario.onActivity(
                    new ActivityScenario.ActivityAction<EmptyFragmentActivity>() {
                        @Override
                        public void perform(EmptyFragmentActivity activity) {
                            Fragment fragment =
                                    activity.getSupportFragmentManager().findFragmentByTag(
                                            FRAGMENT_TAG);
                            checkNotNull(fragment,
                                    "The fragment has been removed from FragmentManager already.");
                        }
                    });
            mActivityScenario.moveToState(newState);
        }
        return this;
    }

    /**
     * Recreates the host Activity.
     * <p>
     * After this method call, it is ensured that the Fragment state goes back to the same state
     * as its previous state.
     * <p>
     * This method cannot be called from the main thread.
     */
    @NonNull
    public FragmentScenario<F> recreate() {
        mActivityScenario.recreate();
        return this;
    }

    /**
     * FragmentAction interface should be implemented by any class whose instances are intended to
     * be executed by the main thread. A Fragment that is instrumented by the FragmentScenario is
     * passed to {@link FragmentAction#perform} method.
     * <p>
     * You should never keep the Fragment reference as it will lead to unpredictable behaviour.
     * It should only be accessed in {@link FragmentAction#perform} scope.
     */
    public interface FragmentAction<F extends Fragment> {
        /**
         * This method is invoked on the main thread with the reference to the Fragment.
         *
         * @param fragment a Fragment instrumented by the FragmentScenario.
         */
        void perform(@NonNull F fragment);
    }

    /**
     * Runs a given {@code action} on the current Activity's main thread.
     * <p>
     * Note that you should never keep Fragment reference passed into your {@code action}
     * because it can be recreated at anytime during state transitions.
     * <p>
     * Throwing an exception from {@code action} makes the host Activity crash. You can
     * inspect the exception in logcat outputs.
     * <p>
     * This method cannot be called from the main thread.
     */
    @NonNull
    public FragmentScenario<F> onFragment(@NonNull final FragmentAction<F> action) {
        mActivityScenario.onActivity(
                new ActivityScenario.ActivityAction<EmptyFragmentActivity>() {
                    @Override
                    public void perform(EmptyFragmentActivity activity) {
                        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(
                                FRAGMENT_TAG);
                        checkNotNull(fragment,
                                "The fragment has been removed from FragmentManager already.");
                        checkState(mFragmentClass.isInstance(fragment));
                        action.perform(Preconditions.checkNotNull(mFragmentClass.cast(fragment)));
                    }
                });
        return this;
    }
}
