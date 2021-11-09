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
package androidx.fragment.app.strictmode

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.strictmode.FragmentStrictMode.Policy

/**
 * FragmentStrictMode is a tool which detects things you might be doing by accident and brings
 * them to your attention so you can fix them. Basically, it's a version of
 * [android.os.StrictMode] specifically for fragment-related issues.
 *
 * You can decide what should happen when a violation is detected. For example, using
 * [Policy.Builder.penaltyLog] you can watch the output of `adb logcat` while you
 * use your application to see the violations as they happen.
 */
object FragmentStrictMode {
    private const val TAG = "FragmentStrictMode"
    /**
     * The current policy for what actions should be detected, as well as the penalty if such
     * actions occur.
     */
    var defaultPolicy = Policy.LAX
    private fun getNearestPolicy(fragment: Fragment?): Policy {
        var declaringFragment = fragment
        while (declaringFragment != null) {
            if (declaringFragment.isAdded) {
                val fragmentManager = declaringFragment.parentFragmentManager
                if (fragmentManager.strictModePolicy != null) {
                    return fragmentManager.strictModePolicy!!
                }
            }
            declaringFragment = declaringFragment.parentFragment
        }
        return defaultPolicy
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onFragmentReuse(fragment: Fragment, previousFragmentId: String) {
        val violation: Violation = FragmentReuseViolation(fragment, previousFragmentId)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_FRAGMENT_REUSE) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onFragmentTagUsage(
        fragment: Fragment,
        container: ViewGroup?
    ) {
        val violation: Violation = FragmentTagUsageViolation(fragment, container)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_FRAGMENT_TAG_USAGE) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onSetRetainInstanceUsage(fragment: Fragment) {
        val violation: Violation = SetRetainInstanceUsageViolation(fragment)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_RETAIN_INSTANCE_USAGE) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onGetRetainInstanceUsage(fragment: Fragment) {
        val violation: Violation = GetRetainInstanceUsageViolation(fragment)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_RETAIN_INSTANCE_USAGE) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onSetUserVisibleHint(fragment: Fragment, isVisibleToUser: Boolean) {
        val violation: Violation = SetUserVisibleHintViolation(fragment, isVisibleToUser)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_SET_USER_VISIBLE_HINT) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onSetTargetFragmentUsage(
        violatingFragment: Fragment,
        targetFragment: Fragment,
        requestCode: Int
    ) {
        val violation: Violation = SetTargetFragmentUsageViolation(
            violatingFragment, targetFragment, requestCode
        )
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(violatingFragment)
        if (policy.flags.contains(Flag.DETECT_TARGET_FRAGMENT_USAGE) &&
            shouldHandlePolicyViolation(policy, violatingFragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onGetTargetFragmentUsage(fragment: Fragment) {
        val violation: Violation = GetTargetFragmentUsageViolation(fragment)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_TARGET_FRAGMENT_USAGE) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onGetTargetFragmentRequestCodeUsage(fragment: Fragment) {
        val violation: Violation = GetTargetFragmentRequestCodeUsageViolation(fragment)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_TARGET_FRAGMENT_USAGE) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun onWrongFragmentContainer(
        fragment: Fragment,
        container: ViewGroup
    ) {
        val violation: Violation = WrongFragmentContainerViolation(fragment, container)
        logIfDebuggingEnabled(violation)
        val policy = getNearestPolicy(fragment)
        if (policy.flags.contains(Flag.DETECT_WRONG_FRAGMENT_CONTAINER) &&
            shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)
        ) {
            handlePolicyViolation(policy, violation)
        }
    }

    @VisibleForTesting
    fun onPolicyViolation(violation: Violation) {
        logIfDebuggingEnabled(violation)
        val fragment: Fragment = violation.fragment
        val policy = getNearestPolicy(fragment)
        if (shouldHandlePolicyViolation(policy, fragment.javaClass, violation.javaClass)) {
            handlePolicyViolation(policy, violation)
        }
    }

    private fun logIfDebuggingEnabled(violation: Violation) {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(
                FragmentManager.TAG,
                "StrictMode violation in ${violation.fragment.javaClass.name}",
                violation
            )
        }
    }

    private fun shouldHandlePolicyViolation(
        policy: Policy,
        fragmentClass: Class<out Fragment>,
        violationClass: Class<out Violation>
    ): Boolean {
        val violationsToBypass = policy.mAllowedViolations[fragmentClass] ?: return true
        if (violationClass.superclass != Violation::class.java) {
            if (violationsToBypass.contains(violationClass.superclass)) {
                return false
            }
        }
        return !violationsToBypass.contains(violationClass)
    }

    private fun handlePolicyViolation(
        policy: Policy,
        violation: Violation
    ) {
        val fragment: Fragment = violation.fragment
        val fragmentName = fragment.javaClass.name
        if (policy.flags.contains(Flag.PENALTY_LOG)) {
            Log.d(TAG, "Policy violation in $fragmentName", violation)
        }
        if (policy.listener != null) {
            runOnHostThread(fragment) { policy.listener.onViolation(violation) }
        }
        if (policy.flags.contains(Flag.PENALTY_DEATH)) {
            runOnHostThread(fragment) {
                Log.e(TAG, "Policy violation with PENALTY_DEATH in $fragmentName", violation)
                throw violation
            }
        }
    }

    private fun runOnHostThread(fragment: Fragment, runnable: Runnable) {
        if (fragment.isAdded) {
            val handler = fragment.parentFragmentManager.host.handler
            if (handler.looper == Looper.myLooper()) {
                runnable.run() // Already on correct thread -> run synchronously
            } else {
                handler.post(runnable) // Switch to correct thread
            }
        } else {
            runnable.run() // Fragment is not attached to any host -> run synchronously
        }
    }

    internal enum class Flag {
        PENALTY_LOG,
        PENALTY_DEATH,
        DETECT_FRAGMENT_REUSE,
        DETECT_FRAGMENT_TAG_USAGE,
        DETECT_RETAIN_INSTANCE_USAGE,
        DETECT_SET_USER_VISIBLE_HINT,
        DETECT_TARGET_FRAGMENT_USAGE,
        DETECT_WRONG_FRAGMENT_CONTAINER
    }

    /**
     * When [Policy.Builder.penaltyListener] is enabled, the listener is called when a
     * violation occurs.
     */
    fun interface OnViolationListener {
        /** Called on a policy violation.  */
        fun onViolation(violation: Violation)
    }

    /**
     * [FragmentStrictMode] policy applied to a certain [FragmentManager] (or globally).
     *
     * This policy can either be enabled globally using [defaultPolicy] or for a
     * specific [FragmentManager] using [FragmentManager.setStrictModePolicy].
     * The current policy can be retrieved using [defaultPolicy] and
     * [FragmentManager.getStrictModePolicy] respectively.
     *
     * Note that multiple penalties may be provided and they're run in order from least to most
     * severe (logging before process death, for example). There's currently no mechanism to choose
     * different penalties for different detected actions.
     */
    class Policy internal constructor(
        internal val flags: Set<Flag>,
        listener: OnViolationListener?,
        allowedViolations: Map<Class<out Fragment>, MutableSet<Class<out Violation>>>
    ) {
        internal val listener: OnViolationListener?
        internal val mAllowedViolations: Map<Class<out Fragment>, Set<Class<out Violation>>>

        /**
         * Creates [Policy] instances. Methods whose names start with `detect` specify
         * what problems we should look for. Methods whose names start with `penalty` specify
         * what we should do when we detect a problem.
         *
         * You can call as many `detect` and `penalty` methods as you like. Currently
         * order is insignificant: all penalties apply to all detected problems.
         */
        class Builder {
            private val flags: MutableSet<Flag> = mutableSetOf()
            private var listener: OnViolationListener? = null
            private val mAllowedViolations:
                MutableMap<Class<out Fragment>, MutableSet<Class<out Violation>>> = mutableMapOf()

            /** Log detected violations to the system log.  */
            @SuppressLint("BuilderSetStyle")
            fun penaltyLog(): Builder {
                flags.add(Flag.PENALTY_LOG)
                return this
            }

            /**
             * Throws an exception on violation. This penalty runs at the end of all enabled
             * penalties so you'll still get to see logging or other violations before the exception
             * is thrown.
             */
            @SuppressLint("BuilderSetStyle")
            fun penaltyDeath(): Builder {
                flags.add(Flag.PENALTY_DEATH)
                return this
            }

            /**
             * Call [OnViolationListener.onViolation] for every violation. The listener will
             * be called on the main thread of the fragment host.
             */
            @SuppressLint("BuilderSetStyle")
            fun penaltyListener(listener: OnViolationListener): Builder {
                this.listener = listener
                return this
            }

            /**
             * Detects cases, where a [Fragment] instance is reused, after it was previously
             * removed from a [FragmentManager].
             */
            @SuppressLint("BuilderSetStyle")
            fun detectFragmentReuse(): Builder {
                flags.add(Flag.DETECT_FRAGMENT_REUSE)
                return this
            }

            /** Detects usage of the <fragment> tag inside XML layouts.  */
            @SuppressLint("BuilderSetStyle")
            fun detectFragmentTagUsage(): Builder {
                flags.add(Flag.DETECT_FRAGMENT_TAG_USAGE)
                return this
            }

            /**
             * Detects calls to [Fragment.setRetainInstance] and [Fragment.getRetainInstance].
             */
            @SuppressLint("BuilderSetStyle")
            fun detectRetainInstanceUsage(): Builder {
                flags.add(Flag.DETECT_RETAIN_INSTANCE_USAGE)
                return this
            }

            /** Detects calls to [Fragment.setUserVisibleHint].  */
            @SuppressLint("BuilderSetStyle")
            fun detectSetUserVisibleHint(): Builder {
                flags.add(Flag.DETECT_SET_USER_VISIBLE_HINT)
                return this
            }

            /**
             * Detects calls to [Fragment.setTargetFragment], [Fragment.getTargetFragment] and
             * [Fragment.getTargetRequestCode].
             */
            @SuppressLint("BuilderSetStyle")
            fun detectTargetFragmentUsage(): Builder {
                flags.add(Flag.DETECT_TARGET_FRAGMENT_USAGE)
                return this
            }

            /**
             * Detects cases where a [Fragment] is added to a container other than a
             * [androidx.fragment.app.FragmentContainerView].
             */
            @SuppressLint("BuilderSetStyle")
            fun detectWrongFragmentContainer(): Builder {
                flags.add(Flag.DETECT_WRONG_FRAGMENT_CONTAINER)
                return this
            }

            /**
             * Allow the specified [Fragment] class to bypass penalties for the specified
             * [Violation], if detected.
             *
             * By default, all [Fragment] classes will incur penalties for any detected [Violation].
             */
            @SuppressLint("BuilderSetStyle")
            fun allowViolation(
                fragmentClass: Class<out Fragment>,
                violationClass: Class<out Violation>
            ): Builder {
                var violationsToBypass = mAllowedViolations[fragmentClass]
                if (violationsToBypass == null) {
                    violationsToBypass = mutableSetOf()
                }
                violationsToBypass.add(violationClass)
                mAllowedViolations[fragmentClass] = violationsToBypass
                return this
            }

            /**
             * Construct the Policy instance.
             *
             * Note: if no penalties are enabled before calling `build`, [penaltyLog] is implicitly
             * set.
             */
            fun build(): Policy {
                if (listener == null && !flags.contains(Flag.PENALTY_DEATH)) {
                    penaltyLog()
                }
                return Policy(flags, listener, mAllowedViolations)
            }
        }

        internal companion object {
            /** The default, lax policy which doesn't catch anything.  */
            @JvmField
            val LAX = Policy(emptySet(), null, emptyMap())
        }

        init {
            this.listener = listener
            val newAllowedViolationsMap:
                MutableMap<Class<out Fragment>, Set<Class<out Violation>>> = mutableMapOf()
            for ((key, value) in allowedViolations) {
                newAllowedViolationsMap[key] = value
            }
            mAllowedViolations = newAllowedViolationsMap
        }
    }
}
