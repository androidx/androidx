/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.fragment.app.strictmode;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * FragmentStrictMode is a tool which detects things you might be doing by accident and brings
 * them to your attention so you can fix them. Basically, it's a version of
 * {@link android.os.StrictMode} specifically for fragment-related issues.
 *
 * <p>You can decide what should happen when a violation is detected. For example, using {@link
 * Policy.Builder#penaltyLog} you can watch the output of <code>adb logcat</code> while you
 * use your application to see the violations as they happen.
 */
@SuppressLint("SyntheticAccessor")
public final class FragmentStrictMode {
    private static final String TAG = "FragmentStrictMode";
    private static Policy defaultPolicy = Policy.LAX;

    private enum Flag {
        PENALTY_LOG,
        PENALTY_DEATH,

        DETECT_FRAGMENT_REUSE,
        DETECT_FRAGMENT_TAG_USAGE,
        DETECT_RETAIN_INSTANCE_USAGE,
        DETECT_SET_USER_VISIBLE_HINT,
        DETECT_TARGET_FRAGMENT_USAGE,
        DETECT_WRONG_FRAGMENT_CONTAINER,
    }

    private FragmentStrictMode() {}

    /**
     * When #{@link Policy.Builder#penaltyListener} is enabled, the listener is called when a
     * violation occurs.
     */
    public interface OnViolationListener {

        /** Called on a policy violation. */
        void onViolation(@NonNull Violation violation);
    }

    /**
     * {@link FragmentStrictMode} policy applied to a certain {@link FragmentManager} (or globally).
     *
     * <p>This policy can either be enabled globally using {@link #setDefaultPolicy} or for a
     * specific {@link FragmentManager} using {@link FragmentManager#setStrictModePolicy(Policy)}.
     * The current policy can be retrieved using {@link #getDefaultPolicy} and
     * {@link FragmentManager#getStrictModePolicy} respectively.
     *
     * <p>Note that multiple penalties may be provided and they're run in order from least to most
     * severe (logging before process death, for example). There's currently no mechanism to choose
     * different penalties for different detected actions.
     */
    public static final class Policy {
        private final Set<Flag> mFlags;
        private final OnViolationListener mListener;
        private final Map<Class<? extends Fragment>,
                Set<Class<? extends Violation>>> mAllowedViolations;

        /** The default, lax policy which doesn't catch anything. */
        @NonNull
        public static final Policy LAX = new Policy(new HashSet<>(), null, new HashMap<>());

        private Policy(
                @NonNull Set<Flag> flags,
                @Nullable OnViolationListener listener,
                @NonNull Map<Class<? extends Fragment>,
                        Set<Class<? extends Violation>>> allowedViolations) {
            this.mFlags = new HashSet<>(flags);
            this.mListener = listener;

            Map<Class<? extends Fragment>, Set<Class<? extends Violation>>>
                    newAllowedViolationsMap = new HashMap<>();
            for (Map.Entry<Class<? extends Fragment>,
                    Set<Class<? extends Violation>>> entry : allowedViolations.entrySet()) {
                newAllowedViolationsMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            this.mAllowedViolations = newAllowedViolationsMap;
        }

        /**
         * Creates {@link Policy} instances. Methods whose names start with {@code detect} specify
         * what problems we should look for. Methods whose names start with {@code penalty} specify
         * what we should do when we detect a problem.
         *
         * <p>You can call as many {@code detect} and {@code penalty} methods as you like. Currently
         * order is insignificant: all penalties apply to all detected problems.
         */
        public static final class Builder {
            private final Set<Flag> mFlags;
            private OnViolationListener mListener;
            private final Map<Class<? extends Fragment>,
                    Set<Class<? extends Violation>>> mAllowedViolations;

            /** Create a Builder that detects nothing and has no violations. */
            public Builder() {
                mFlags = new HashSet<>();
                mAllowedViolations = new HashMap<>();
            }

            /** Log detected violations to the system log. */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder penaltyLog() {
                mFlags.add(Flag.PENALTY_LOG);
                return this;
            }

            /**
             * Throws an exception on violation. This penalty runs at the end of all enabled
             * penalties so you'll still get to see logging or other violations before the exception
             * is thrown.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder penaltyDeath() {
                mFlags.add(Flag.PENALTY_DEATH);
                return this;
            }

            /**
             * Call #{@link OnViolationListener#onViolation} for every violation. The listener will
             * be called on the main thread of the fragment host.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder penaltyListener(@NonNull OnViolationListener listener) {
                this.mListener = listener;
                return this;
            }

            /**
             * Detects cases, where a #{@link Fragment} instance is reused, after it was previously
             * removed from a #{@link FragmentManager}.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder detectFragmentReuse() {
                mFlags.add(Flag.DETECT_FRAGMENT_REUSE);
                return this;
            }

            /** Detects usage of the &lt;fragment&gt; tag inside XML layouts. */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder detectFragmentTagUsage() {
                mFlags.add(Flag.DETECT_FRAGMENT_TAG_USAGE);
                return this;
            }

            /**
             * Detects calls to #{@link Fragment#setRetainInstance} and
             * #{@link Fragment#getRetainInstance()}.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder detectRetainInstanceUsage() {
                mFlags.add(Flag.DETECT_RETAIN_INSTANCE_USAGE);
                return this;
            }

            /** Detects calls to #{@link Fragment#setUserVisibleHint}. */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder detectSetUserVisibleHint() {
                mFlags.add(Flag.DETECT_SET_USER_VISIBLE_HINT);
                return this;
            }

            /**
             * Detects calls to #{@link Fragment#setTargetFragment},
             * #{@link Fragment#getTargetFragment()} and #{@link Fragment#getTargetRequestCode()}.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder detectTargetFragmentUsage() {
                mFlags.add(Flag.DETECT_TARGET_FRAGMENT_USAGE);
                return this;
            }

            /**
             * Detects cases where a #{@link Fragment} is added to a container other than a
             * #{@link FragmentContainerView}.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder detectWrongFragmentContainer() {
                mFlags.add(Flag.DETECT_WRONG_FRAGMENT_CONTAINER);
                return this;
            }

            /**
             * Allow the specified {@link Fragment} class to bypass penalties for the
             * specified {@link Violation}, if detected.
             *
             * By default, all {@link Fragment} classes will incur penalties for any
             * detected {@link Violation}.
             */
            @NonNull
            @SuppressLint("BuilderSetStyle")
            public Builder allowViolation(
                    @NonNull Class<? extends Fragment> fragmentClass,
                    @NonNull Class<? extends Violation> violationClass) {
                Set<Class<? extends Violation>> violationsToBypass =
                        mAllowedViolations.get(fragmentClass);
                if (violationsToBypass == null) {
                    violationsToBypass = new HashSet<>();
                }
                violationsToBypass.add(violationClass);
                mAllowedViolations.put(fragmentClass, violationsToBypass);
                return this;
            }

            /**
             * Construct the Policy instance.
             *
             * <p>Note: if no penalties are enabled before calling <code>build</code>, {@link
             * #penaltyLog} is implicitly set.
             */
            @NonNull
            public Policy build() {
                if (mListener == null && !mFlags.contains(Flag.PENALTY_DEATH)) {
                    penaltyLog();
                }
                return new Policy(mFlags, mListener, mAllowedViolations);
            }
        }
    }

    /** Returns the current default policy. */
     @NonNull
    public static Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    /**
     * Sets the policy for what actions should be detected, as well as the penalty if such actions
     * occur.
     *
     * @param policy the policy to put into place
     */
    public static void setDefaultPolicy(@NonNull Policy policy) {
        defaultPolicy = policy;
    }

    private static Policy getNearestPolicy(@Nullable Fragment fragment) {
        while (fragment != null) {
            if (fragment.isAdded()) {
                FragmentManager fragmentManager = fragment.getParentFragmentManager();
                if (fragmentManager.getStrictModePolicy() != null) {
                    return fragmentManager.getStrictModePolicy();
                }
            }
            fragment = fragment.getParentFragment();
        }
        return defaultPolicy;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void onFragmentReuse(@NonNull Fragment fragment, @NonNull String previousWho) {
        Violation violation = new FragmentReuseViolation(fragment, previousWho);
        logIfDebuggingEnabled(violation);

        Policy policy = getNearestPolicy(fragment);
        if (policy.mFlags.contains(Flag.DETECT_FRAGMENT_REUSE)
                && shouldHandlePolicyViolation(
                policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void onFragmentTagUsage(@NonNull Fragment fragment) {
        Violation violation = new FragmentTagUsageViolation(fragment);
        logIfDebuggingEnabled(violation);

        Policy policy = getNearestPolicy(fragment);
        if (policy.mFlags.contains(Flag.DETECT_FRAGMENT_TAG_USAGE)
                && shouldHandlePolicyViolation(
                policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void onRetainInstanceUsage(@NonNull Fragment fragment) {
        Violation violation = new RetainInstanceUsageViolation(fragment);
        logIfDebuggingEnabled(violation);

        Policy policy = getNearestPolicy(fragment);
        if (policy.mFlags.contains(Flag.DETECT_RETAIN_INSTANCE_USAGE)
                && shouldHandlePolicyViolation(
                policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void onSetUserVisibleHint(@NonNull Fragment fragment) {
        Violation violation = new SetUserVisibleHintViolation(fragment);
        logIfDebuggingEnabled(violation);

        Policy policy = getNearestPolicy(fragment);
        if (policy.mFlags.contains(Flag.DETECT_SET_USER_VISIBLE_HINT)
                && shouldHandlePolicyViolation(
                policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void onTargetFragmentUsage(@NonNull Fragment fragment) {
        Violation violation = new TargetFragmentUsageViolation(fragment);
        logIfDebuggingEnabled(violation);

        Policy policy = getNearestPolicy(fragment);
        if (policy.mFlags.contains(Flag.DETECT_TARGET_FRAGMENT_USAGE)
                && shouldHandlePolicyViolation(
                policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void onWrongFragmentContainer(@NonNull Fragment fragment) {
        Violation violation = new WrongFragmentContainerViolation(fragment);
        logIfDebuggingEnabled(violation);

        Policy policy = getNearestPolicy(fragment);
        if (policy.mFlags.contains(Flag.DETECT_WRONG_FRAGMENT_CONTAINER)
                && shouldHandlePolicyViolation(
                policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    @VisibleForTesting
    static void onPolicyViolation(@NonNull Violation violation) {
        logIfDebuggingEnabled(violation);

        Fragment fragment = violation.getFragment();
        Policy policy = getNearestPolicy(fragment);
        if (shouldHandlePolicyViolation(policy, fragment.getClass(), violation.getClass())) {
            handlePolicyViolation(policy, violation);
        }
    }

    private static void logIfDebuggingEnabled(@NonNull final Violation violation) {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(FragmentManager.TAG,
                    "StrictMode violation in " + violation.getFragment().getClass().getName(),
                    violation);
        }
    }

    private static boolean shouldHandlePolicyViolation(
            @NonNull final Policy policy,
            @NonNull Class<? extends Fragment> fragmentClass,
            @NonNull Class<? extends Violation> violationClass) {
        Set<Class<? extends Violation>> violationsToBypass =
                policy.mAllowedViolations.get(fragmentClass);
        return violationsToBypass == null || !violationsToBypass.contains(violationClass);
    }

    private static void handlePolicyViolation(
            @NonNull final Policy policy,
            @NonNull final Violation violation
    ) {
        final Fragment fragment = violation.getFragment();
        final String fragmentName = fragment.getClass().getName();

        if (policy.mFlags.contains(Flag.PENALTY_LOG)) {
            Log.d(TAG, "Policy violation in " + fragmentName, violation);
        }

        if (policy.mListener != null) {
            runOnHostThread(fragment, new Runnable() {
                @Override
                public void run() {
                    policy.mListener.onViolation(violation);
                }
            });
        }

        if (policy.mFlags.contains(Flag.PENALTY_DEATH)) {
            runOnHostThread(fragment, new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "Policy violation with PENALTY_DEATH in " + fragmentName, violation);
                    throw violation;
                }
            });
        }
    }

    private static void runOnHostThread(@NonNull Fragment fragment, @NonNull Runnable runnable) {
        if (fragment.isAdded()) {
            Handler handler = fragment.getParentFragmentManager().getHost().getHandler();
            if (handler.getLooper() == Looper.myLooper()) {
                runnable.run(); // Already on correct thread -> run synchronously
            } else {
                handler.post(runnable); // Switch to correct thread
            }
        } else {
            runnable.run(); // Fragment is not attached to any host -> run synchronously
        }
    }
}
