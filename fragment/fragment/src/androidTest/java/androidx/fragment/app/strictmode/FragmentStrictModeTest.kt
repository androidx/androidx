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
import androidx.fragment.app.StrictViewFragment
import androidx.fragment.app.executePendingTransactions
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import leakcanary.SkipLeakDetection
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class FragmentStrictModeTest {
    private val fragmentClass = StrictFragment::class.java

    private lateinit var originalPolicy: FragmentStrictMode.Policy

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Before
    public fun setup() {
        originalPolicy = FragmentStrictMode.defaultPolicy
    }

    @After
    public fun teardown() {
        FragmentStrictMode.defaultPolicy = originalPolicy
    }

    @Test
    public fun penaltyDeath() {
        val policy = FragmentStrictMode.Policy.Builder()
            .penaltyDeath()
            .build()
        FragmentStrictMode.defaultPolicy = policy

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

       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
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

            FragmentStrictMode.defaultPolicy = policy("Default policy")
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
        FragmentStrictMode.defaultPolicy = policy

       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
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
        FragmentStrictMode.defaultPolicy = policy

       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
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
            assertThat(violation).hasMessageThat().contains(
                "Attempting to reuse fragment $fragment with previous ID ${fragment.mPreviousWho}"
            )
        }
    }

    @Test
    public fun detectFragmentReuseInFlightTransaction() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectFragmentReuse()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
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
            assertThat(violation).hasMessageThat().contains(
                "Attempting to reuse fragment $fragment with previous ID ${fragment.mPreviousWho}"
            )
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun detectFragmentTagUsage() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectFragmentTagUsage()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity { setContentView(R.layout.activity_inflated_fragment) }
            val fragment = withActivity {
                supportFragmentManager.findFragmentById(R.id.inflated_fragment)!!
            }
            val container = withActivity { findViewById(R.id.inflated_layout) }
            assertThat(violation).isInstanceOf(FragmentTagUsageViolation::class.java)
            assertThat(violation).hasMessageThat().contains(
                "Attempting to use <fragment> tag to add fragment $fragment to container $container"
            )
        }
    }

    @Test
    @Ignore("b/308684873")
    public fun detectWrongNestedHierarchyNoParent() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectWrongNestedHierarchy()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val outerFragment = StrictViewFragment(R.layout.scene1)
            val innerFragment = StrictViewFragment(R.layout.fragment_a)

            fm.beginTransaction()
                .add(R.id.fragmentContainer, outerFragment)
                .setReorderingAllowed(false)
                .commit()
            // Here we add childFragment to a layout within parentFragment, but we
            // specifically don't use parentFragment.childFragmentManager
            fm.beginTransaction()
                .add(R.id.squareContainer, innerFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions()

            assertThat(violation).isInstanceOf(WrongNestedHierarchyViolation::class.java)
            assertThat(violation).hasMessageThat().contains(
                "Attempting to nest fragment $innerFragment within the view " +
                    "of parent fragment $outerFragment via container with ID " +
                    "${R.id.squareContainer} without using parent's childFragmentManager"
            )
        }
    }

    @Test
    public fun detectWrongNestedHierarchyWrongParent() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectWrongNestedHierarchy()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val grandParent = StrictViewFragment(R.layout.scene1)
            val parentFragment = StrictViewFragment(R.layout.scene5)
            val childFragment = StrictViewFragment(R.layout.fragment_a)
            fm.beginTransaction()
                .add(R.id.fragmentContainer, grandParent)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions()
            grandParent.childFragmentManager.beginTransaction()
                .add(R.id.squareContainer, parentFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions()
            // Here we use the grandParent.childFragmentManager for the child
            // fragment, though we should actually be using parentFragment.childFragmentManager
            grandParent.childFragmentManager.beginTransaction()
                .add(R.id.sharedElementContainer, childFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions(parentFragment.childFragmentManager)
            assertThat(violation).isInstanceOf(WrongNestedHierarchyViolation::class.java)
            assertThat(violation).hasMessageThat().contains(
                "Attempting to nest fragment $childFragment within the view " +
                    "of parent fragment $parentFragment via container with ID " +
                    "${R.id.sharedElementContainer} without using parent's childFragmentManager"
            )
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
        FragmentStrictMode.defaultPolicy = policy

        val fragment = StrictFragment()
        fragment.retainInstance = true
        assertThat(violation).isInstanceOf(SetRetainInstanceUsageViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to set retain instance for fragment $fragment"
        )

        violation = null
        fragment.retainInstance
        assertThat(violation).isInstanceOf(GetRetainInstanceUsageViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to get retain instance for fragment $fragment"
        )
    }

    @Suppress("DEPRECATION")
    @Test
    public fun detectSetUserVisibleHint() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectSetUserVisibleHint()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

        val fragment = StrictFragment()
        fragment.userVisibleHint = true
        assertThat(violation).isInstanceOf(SetUserVisibleHintViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to set user visible hint to true for fragment $fragment"
        )
    }

    @SkipLeakDetection("This test throws an exception and can end in an invalid state")
    @Suppress("DEPRECATION")
    @Test
    public fun detectTargetFragmentUsage() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectTargetFragmentUsage()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

        val fragment = StrictFragment()
        val targetFragment = StrictFragment()
        val requestCode = 1
        fragment.setTargetFragment(targetFragment, requestCode)
        assertThat(violation).isInstanceOf(SetTargetFragmentUsageViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to set target fragment $targetFragment " +
                "with request code $requestCode for fragment $fragment"
        )

        violation = null
        fragment.targetFragment
        assertThat(violation).isInstanceOf(GetTargetFragmentUsageViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to get target fragment from fragment $fragment"
        )

        violation = null
        fragment.targetRequestCode
        assertThat(violation).isInstanceOf(GetTargetFragmentRequestCodeUsageViolation::class.java)
        assertThat(violation).hasMessageThat().contains(
            "Attempting to get target request code from fragment $fragment"
        )
    }

    @Test
    public fun detectWrongFragmentContainer() {
        var violation: Violation? = null
        val policy = FragmentStrictMode.Policy.Builder()
            .detectWrongFragmentContainer()
            .penaltyListener { violation = it }
            .build()
        FragmentStrictMode.defaultPolicy = policy

       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()
            fragmentManager.beginTransaction()
                .add(R.id.content, fragment1)
                .commit()
            executePendingTransactions()
            val container1 = withActivity { findViewById(R.id.content) }
            assertThat(violation).isInstanceOf(WrongFragmentContainerViolation::class.java)
            assertThat(violation).hasMessageThat().contains(
                "Attempting to add fragment $fragment1 to container " +
                    "$container1 which is not a FragmentContainerView"
            )

            violation = null
            val fragment2 = StrictFragment()
            fragmentManager.beginTransaction()
                .replace(R.id.content, fragment2)
                .commit()
            executePendingTransactions()
            val container2 = withActivity { findViewById(R.id.content) }
            assertThat(violation).isInstanceOf(WrongFragmentContainerViolation::class.java)
            assertThat(violation).hasMessageThat().contains(
                "Attempting to add fragment $fragment2 to container " +
                    "$container2 which is not a FragmentContainerView"
            )
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
        FragmentStrictMode.defaultPolicy = policyBuilder.build()

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

    @Suppress("DEPRECATION")
    @Test
    public fun detectAllowedViolationByClassString() {
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
            policyBuilder = policyBuilder.allowViolation(fragmentClass.name, violationClass)
        }
        FragmentStrictMode.defaultPolicy = policyBuilder.build()

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
