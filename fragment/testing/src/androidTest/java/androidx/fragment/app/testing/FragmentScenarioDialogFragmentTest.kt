/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.fragment.app.testing

import androidx.core.os.BuildCompat.isAtLeastQ
import androidx.lifecycle.Lifecycle.State
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for FragmentScenario's implementation against DialogFragment.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentScenarioDialogFragmentTest {
    @Test
    fun launchFragment() {
        with(launchFragment<SimpleDialogFragment>()) {
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
            onView(withText("my button")).inRoot(isDialog()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun launchFragmentInContainer() {
        with(launchFragmentInContainer<SimpleDialogFragment>()) {
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                // We show SimpleDialogFragment in container so dialog is not created.
                assertThat(fragment.dialog).isNull()
            }
            onView(withText("my button")).inRoot(not(isDialog())).check(matches(isDisplayed()))
        }
    }

    @Test
    fun fromResumedToCreated() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.CREATED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.CREATED)
                assertThat(fragment.dialog).isNotNull()
                if (isAtLeastQ()) {
                    assertThat(fragment.requireDialog().isShowing).isFalse()
                } else {
                    // Dialog#isShowing should've returned false but it returns true due to a
                    // bug which was fixed by aosp/637304.
                    assertThat(fragment.requireDialog().isShowing).isTrue()
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromResumedToStarted() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.STARTED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.STARTED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @Test
    fun fromResumedToResumed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.RESUMED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @Test
    fun fromResumedToDestroyed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.DESTROYED)
        }
    }

    @Test
    fun fromCreatedToCreated() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.CREATED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.CREATED)
                assertThat(fragment.dialog).isNotNull()
                if (isAtLeastQ()) {
                    assertThat(fragment.requireDialog().isShowing).isFalse()
                } else {
                    // Dialog#isShowing should've returned false but it returns true due to a
                    // bug which was fixed by aosp/637304.
                    assertThat(fragment.requireDialog().isShowing).isTrue()
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromCreatedToStarted() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.STARTED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.STARTED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @Test
    fun fromCreatedToResumed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.RESUMED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @Test
    fun fromCreatedToDestroyed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.DESTROYED)
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToCreated() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.CREATED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.CREATED)
                assertThat(fragment.dialog).isNotNull()
                if (isAtLeastQ()) {
                    assertThat(fragment.requireDialog().isShowing).isFalse()
                } else {
                    // Dialog#isShowing should've returned false but it returns true due to a
                    // bug which was fixed by aosp/637304.
                    assertThat(fragment.requireDialog().isShowing).isTrue()
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToStarted() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.STARTED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.STARTED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToResumed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.RESUMED)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToDestroyed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.DESTROYED)
        }
    }

    @Test
    fun fromDestroyedToDestroyed() {
        with(launchFragment<SimpleDialogFragment>()) {
            moveToState(State.DESTROYED)
            moveToState(State.DESTROYED)
        }
    }

    @Test
    fun recreateCreatedFragment() {
        var numOfInstantiation = 0
        with(launchFragment {
            ++numOfInstantiation
            SimpleDialogFragment()
        }) {
            assertThat(numOfInstantiation).isEqualTo(1)
            moveToState(State.CREATED)
            recreate()
            assertThat(numOfInstantiation).isEqualTo(2)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.CREATED)
                assertThat(fragment.dialog).isNotNull()
                if (isAtLeastQ()) {
                    assertThat(fragment.requireDialog().isShowing).isFalse()
                } else {
                    // Dialog#isShowing should've returned false but it returns true due to a
                    // bug which was fixed by aosp/637304.
                    assertThat(fragment.requireDialog().isShowing).isTrue()
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun recreateStartedFragment() {
        var numOfInstantiation = 0
        with(launchFragment {
            ++numOfInstantiation
            SimpleDialogFragment()
        }) {
            assertThat(numOfInstantiation).isEqualTo(1)
            moveToState(State.STARTED)
            recreate()
            assertThat(numOfInstantiation).isEqualTo(2)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.STARTED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @Test
    fun recreateResumedFragment() {
        var numOfInstantiation = 0
        with(launchFragment {
            ++numOfInstantiation
            SimpleDialogFragment()
        }) {
            assertThat(numOfInstantiation).isEqualTo(1)
            recreate()
            assertThat(numOfInstantiation).isEqualTo(2)
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
            }
        }
    }

    @Test
    fun dismissDialog() {
        with(launchFragment<SimpleDialogFragment>()) {
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
                assertThat(fragment.dialog).isNotNull()
                assertThat(fragment.requireDialog().isShowing).isTrue()
                fragment.dismiss()
                fragment.requireFragmentManager().executePendingTransactions()
                assertThat(fragment.dialog).isNull()
            }
        }
    }
}