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

import android.os.Looper
import androidx.fragment.app.StrictFragment
import androidx.fragment.app.executePendingTransactions
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class FragmentStrictModeTest {
    private val fragmentClass = StrictFragment::class.java

    private lateinit var originalPolicy: FragmentStrictMode.Policy

    @Before
    public fun setup() {
        originalPolicy = FragmentStrictMode.getDefaultPolicy()
    }

    @After
    public fun teardown() {
        FragmentStrictMode.setDefaultPolicy(originalPolicy)
    }

    @Test
    public fun penaltyDeath() {
        val policy = FragmentStrictMode.Policy.Builder()
            .penaltyDeath()
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        var violation: Violation? = null
        try {
            val fragment = StrictFragment()
            FragmentStrictMode.onPolicyViolation(object : Violation(fragment) {})
        } catch (thrown: Violation) {
            violation = thrown
        }
        assertWithMessage("No exception thrown on policy violation").that(violation).isNotNull()
    }

    @Test
    public fun policyHierarchy() {
        var lastTriggeredPolicy = ""

        fun policy(name: String) = FragmentStrictMode.Policy.Builder()
            .penaltyListener { lastTriggeredPolicy = name }
            .build()

        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val parentFragment = StrictFragment()
            fragmentManager.beginTransaction()
                .add(parentFragment, "parentFragment")
                .commit()
            executePendingTransactions()

            val childFragment = StrictFragment()
            parentFragment.childFragmentManager.beginTransaction()
                .add(childFragment, "childFragment")
                .commit()
            executePendingTransactions()

            val violation = object : Violation(childFragment) {}

            FragmentStrictMode.setDefaultPolicy(policy("Default policy"))
            FragmentStrictMode.onPolicyViolation(violation)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(lastTriggeredPolicy).isEqualTo("Default policy")

            fragmentManager.strictModePolicy = policy("Parent policy")
            FragmentStrictMode.onPolicyViolation(violation)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(lastTriggeredPolicy).isEqualTo("Parent policy")

            parentFragment.childFragmentManager.strictModePolicy = policy("Child policy")
            FragmentStrictMode.onPolicyViolation(violation)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(lastTriggeredPolicy).isEqualTo("Child policy")
        }
    }

    @Test
    public fun listenerCalledOnCorrectThread() {
        var thread: Thread? = null

        val policy = FragmentStrictMode.Policy.Builder()
            .penaltyListener { thread = Thread.currentThread() }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment = StrictFragment()
            fragmentManager.beginTransaction()
                .add(fragment, null)
                .commit()
            executePendingTransactions()

            FragmentStrictMode.onPolicyViolation(object : Violation(fragment) {})
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(thread).isEqualTo(Looper.getMainLooper().thread)
        }
    }

    @Test
    public fun detectFragmentReuse() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectFragmentReuse()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }
            val fragment = StrictFragment()

            fragmentManager.beginTransaction()
                .add(fragment, null)
                .commit()
            executePendingTransactions()

            fragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
            executePendingTransactions()

            fragmentManager.beginTransaction()
                .add(fragment, null)
                .commit()
            executePendingTransactions()

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(violation).isInstanceOf(FragmentReuseViolation::class.java)
        }
    }

    @Test
    public fun detectFragmentReuseInFlightTransaction() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectFragmentReuse()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }
            val fragment = StrictFragment()

            fragmentManager.beginTransaction()
                .add(fragment, null)
                .commit()
            executePendingTransactions()

            fragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
            // Don't execute transaction here, keep it in-flight

            fragmentManager.beginTransaction()
                .add(fragment, null)
                .commit()
            executePendingTransactions()

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(violation).isInstanceOf(FragmentReuseViolation::class.java)
        }
    }

    @Test
    public fun detectFragmentTagUsage() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectFragmentTagUsage()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity { setContentView(R.layout.activity_inflated_fragment) }
            assertThat(violation).isInstanceOf(FragmentTagUsageViolation::class.java)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun detectRetainInstanceUsage() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectRetainInstanceUsage()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        StrictFragment().retainInstance = true
        assertThat(violation).isInstanceOf(SetRetainInstanceUsageViolation::class.java)

        violation = null
        StrictFragment().retainInstance
        assertThat(violation).isInstanceOf(GetRetainInstanceUsageViolation::class.java)
    }

    @Suppress("DEPRECATION")
    @Test
    public fun detectSetUserVisibleHint() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectSetUserVisibleHint()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        val fragment = StrictFragment()
        fragment.userVisibleHint = true
        assertThat(violation).isInstanceOf(SetUserVisibleHintViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to set user visible hint to true for fragment $fragment"
        )
    }

    @Suppress("DEPRECATION")
    @Test
    public fun detectTargetFragmentUsage() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectTargetFragmentUsage()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        StrictFragment().setTargetFragment(StrictFragment(), 1)
        assertThat(violation).isInstanceOf(SetTargetFragmentUsageViolation::class.java)

        violation = null
        StrictFragment().targetFragment
        assertThat(violation).isInstanceOf(GetTargetFragmentUsageViolation::class.java)

        violation = null
        StrictFragment().targetRequestCode
        assertThat(violation).isInstanceOf(GetTargetFragmentRequestCodeUsageViolation::class.java)
    }

    @Test
    public fun detectWrongFragmentContainer() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectWrongFragmentContainer()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.setDefaultPolicy(policy)

        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.beginTransaction()
                .add(R.id.content, StrictFragment())
                .commit()
            executePendingTransactions()
            assertThat(violation).isInstanceOf(WrongFragmentContainerViolation::class.java)

            violation = null
            fragmentManager.beginTransaction()
                .replace(R.id.content, StrictFragment())
                .commit()
            executePendingTransactions()
            assertThat(violation).isInstanceOf(WrongFragmentContainerViolation::class.java)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun detectAllowedViolations() {
        val violationClass1 = RetainInstanceUsageViolation::class.java
        val violationClass2 = SetUserVisibleHintViolation::class.java
        val violationClass3 = GetTargetFragmentUsageViolation::class.java
        val violationClassList = listOf(violationClass1, violationClass2, violationClass3)

        var violation: Violation? = null
        var policyBuilder = FragmentStrictMode.Policy.Builder()
            .detectRetainInstanceUsage()
            .detectSetUserVisibleHint()
            .penaltyListener { violation = it }
        for (violationClass in violationClassList) {
            policyBuilder = policyBuilder.allowViolation(fragmentClass, violationClass)
        }
        FragmentStrictMode.setDefaultPolicy(policyBuilder.build())

        StrictFragment().retainInstance = true
        assertThat(violation).isNotInstanceOf(violationClass1)
        assertThat(violation).isNotInstanceOf(SetRetainInstanceUsageViolation::class.java)

        violation = null
        StrictFragment().retainInstance
        assertThat(violation).isNotInstanceOf(violationClass1)
        assertThat(violation).isNotInstanceOf(GetRetainInstanceUsageViolation::class.java)

        violation = null
        StrictFragment().userVisibleHint = true
        assertThat(violation).isNotInstanceOf(violationClass2)

        violation = null
        StrictFragment().targetFragment
        assertThat(violation).isNotInstanceOf(violationClass3)
    }
}
