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

package androidx.fragment.app

import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BackStackRecordTest {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    @UiThreadTest
    fun testExpandCollapseOpsPrimaryNav() {
        val initialFragment = StrictFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(android.R.id.content, initialFragment)
            .setPrimaryNavigationFragment(initialFragment)
            .commitNow()

        val replacementFragment = StrictFragment()
        val backStackRecord = BackStackRecord(fm)
        backStackRecord.setPrimaryNavigationFragment(replacementFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment)
        }

        backStackRecord.expandOps(ArrayList(fm.fragments), initialFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_UNSET_PRIMARY_NAV, initialFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment) {
                fromExpandedOp = true
            }
        }

        backStackRecord.collapseOps()

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment)
        }
    }

    @Test
    @UiThreadTest
    fun testExpandCollapseOpsReplace() {
        val initialFragment = StrictFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, initialFragment).commitNow()

        val replacementFragment = StrictFragment()
        val backStackRecord = BackStackRecord(fm)
        backStackRecord.replace(android.R.id.content, replacementFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
        }

        backStackRecord.expandOps(ArrayList(fm.fragments), null)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REMOVE, initialFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_ADD, replacementFragment) {
                fromExpandedOp = true
            }
        }

        backStackRecord.collapseOps()

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
        }
    }

    @Test
    @UiThreadTest
    fun testExpandCollapseOpsReplacePrimaryNav() {
        val initialFragment = StrictFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(android.R.id.content, initialFragment)
            .setPrimaryNavigationFragment(initialFragment)
            .commitNow()

        val replacementFragment = StrictFragment()
        val backStackRecord = BackStackRecord(fm)
        backStackRecord
            .replace(android.R.id.content, replacementFragment)
            .setPrimaryNavigationFragment(replacementFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment)
        }

        backStackRecord.expandOps(ArrayList(fm.fragments), initialFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_UNSET_PRIMARY_NAV, initialFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_REMOVE, initialFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_ADD, replacementFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_UNSET_PRIMARY_NAV) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment) {
                fromExpandedOp = true
            }
        }

        backStackRecord.collapseOps()

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment)
        }
    }

    @Test
    @UiThreadTest
    fun testExpandCollapseOpsReplaceMultiple() {
        val initialFragment = StrictFragment()
        val addedFragment = StrictFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(android.R.id.content, initialFragment)
            .add(android.R.id.content, addedFragment)
            .commitNow()

        val replacementFragment = StrictFragment()
        val backStackRecord = BackStackRecord(fm)
        backStackRecord.replace(android.R.id.content, replacementFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
        }

        backStackRecord.expandOps(ArrayList(fm.fragments), null)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REMOVE, addedFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_REMOVE, initialFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_ADD, replacementFragment) {
                fromExpandedOp = true
            }
        }

        backStackRecord.collapseOps()

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
        }
    }

    @Test
    @UiThreadTest
    fun testExpandCollapseOpsReplaceMultiplePrimaryNav() {
        val initialFragment = StrictFragment()
        val addedFragment = StrictFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(android.R.id.content, initialFragment)
            .add(android.R.id.content, addedFragment)
            .setPrimaryNavigationFragment(addedFragment)
            .commitNow()

        val replacementFragment = StrictFragment()
        val backStackRecord = BackStackRecord(fm)
        backStackRecord
            .replace(android.R.id.content, replacementFragment)
            .setPrimaryNavigationFragment(replacementFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment)
        }

        backStackRecord.expandOps(ArrayList(fm.fragments), addedFragment)

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_UNSET_PRIMARY_NAV, addedFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_REMOVE, addedFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_REMOVE, initialFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_ADD, replacementFragment) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_UNSET_PRIMARY_NAV) {
                fromExpandedOp = true
            }
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment) {
                fromExpandedOp = true
            }
        }

        backStackRecord.collapseOps()

        backStackRecord.verifyOps {
            verify(FragmentTransaction.OP_REPLACE, replacementFragment)
            verify(FragmentTransaction.OP_SET_PRIMARY_NAV, replacementFragment)
        }
    }

    @Test
    @UiThreadTest
    fun testHideOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore1)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = activityRule.startupFragmentController(viewModelStore2)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()

        fm1.beginTransaction().add(fragment1, "1").commitNow()
        try {
            fm2.beginTransaction().hide(fragment1).commitNow()
            fail(
                "Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Cannot hide Fragment attached to " +
                        "a different FragmentManager. Fragment " + fragment1.toString() +
                        " is already attached to a FragmentManager."
                )
        }

        // Bring the state back down to destroyed before we finish the test
        fc1.shutdown(viewModelStore1)
        fc2.shutdown(viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testShowOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore1)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = activityRule.startupFragmentController(viewModelStore2)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()

        fm1.beginTransaction().add(fragment1, "1").commitNow()
        try {
            fm2.beginTransaction().show(fragment1).commitNow()
            fail(
                "Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Cannot show Fragment attached to " +
                        "a different FragmentManager. Fragment " + fragment1.toString() +
                        " is already attached to a FragmentManager."
                )
        }

        // Bring the state back down to destroyed before we finish the test
        fc1.shutdown(viewModelStore1)
        fc2.shutdown(viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testSetPrimaryNavigationFragmentOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore1)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = activityRule.startupFragmentController(viewModelStore2)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()

        fm1.beginTransaction().add(fragment1, "1").commitNow()
        try {
            fm2.beginTransaction().setPrimaryNavigationFragment(fragment1).commitNow()
            fail(
                "Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Cannot setPrimaryNavigation for Fragment attached to " +
                        "a different FragmentManager. Fragment " + fragment1.toString() +
                        " is already attached to a FragmentManager."
                )
        }

        // Bring the state back down to destroyed before we finish the test
        fc1.shutdown(viewModelStore1)
        fc2.shutdown(viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testDetachFragmentWithManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore1)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = activityRule.startupFragmentController(viewModelStore2)
        val fm2 = fc2.supportFragmentManager

        // Add the initial state
        val fragment1 = StrictFragment()

        fm1.beginTransaction().add(fragment1, "1").commitNow()

        try {
            fm2.beginTransaction().detach(fragment1).commitNow()
            fail(
                "Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Cannot detach Fragment attached to " +
                        "a different FragmentManager. Fragment " + fragment1.toString() +
                        " is already attached to a FragmentManager."
                )
        }

        // Bring the state back down to destroyed before we finish the test
        fc1.shutdown(viewModelStore1)
        fc2.shutdown(viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testRemoveFragmentWithManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore1)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = activityRule.startupFragmentController(viewModelStore2)
        val fm2 = fc2.supportFragmentManager

        // Add the initial state
        val fragment1 = StrictFragment()

        fm1.beginTransaction().add(fragment1, "1").commitNow()

        try {
            fm2.beginTransaction().remove(fragment1).commitNow()
            fail(
                "Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Cannot remove Fragment attached to " +
                        "a different FragmentManager. Fragment " + fragment1.toString() +
                        " is already attached to a FragmentManager."
                )
        }

        // Bring the state back down to destroyed before we finish the test
        fc1.shutdown(viewModelStore1)
        fc2.shutdown(viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycleWrongFragmentManager() {
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)
        val fc2 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager
        val fm2 = fc2.supportFragmentManager

        val fragment = StrictViewFragment()
        fm1.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        try {
            fm2.beginTransaction()
                .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                .commitNow()
            fail(
                "setting maxLifecycle on fragment not attached to fragment manager should throw" +
                    " IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "Cannot setMaxLifecycle for Fragment not attached to" +
                        " FragmentManager $fm2"
                )
        }
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycleInitialized() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val fragment = StrictViewFragment()

        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .setReorderingAllowed(true)
            .setMaxLifecycle(fragment, Lifecycle.State.INITIALIZED)
            .commitNow()

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        assertThat(fragment.calledOnResume).isFalse()
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycleInitializedAfterCreated() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val fragment = StrictViewFragment()

        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.CREATED)
            .commitNow()

        try {
            fm.beginTransaction()
                .setMaxLifecycle(fragment, Lifecycle.State.INITIALIZED)
                .commitNow()
            fail(
                "setting maxLifecycle state to state lower than created should throw" +
                    " IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "Cannot set maximum Lifecycle to INITIALIZED after the Fragment has been " +
                        "created"
                )
        }
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycleDestroyed() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fragment = StrictViewFragment()

        try {
            fc.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .setMaxLifecycle(fragment, Lifecycle.State.DESTROYED)
                .commitNow()
            fail(
                "setting maxLifecycle state to DESTROYED should throw IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "Cannot set maximum Lifecycle to DESTROYED. Use remove() to remove the " +
                        "fragment from the FragmentManager and trigger its destruction."
                )
        }
    }
}

internal class BackStackRecordVerify(private val backStackRecord: BackStackRecord) {
    var currentOp = 0

    fun verify(
        command: Int,
        fragment: Fragment? = null,
        block: BackStackRecordOpInfo.() -> Unit = {}
    ) {
        assertWithMessage(
            "Cannot verify op $currentOp as there is only ${backStackRecord.mOps.size} operations"
        ).that(backStackRecord.mOps.size).isAtLeast(currentOp + 1)
        backStackRecord.mOps[currentOp].verify(currentOp, command, fragment, block)
        currentOp++
    }
}

internal fun BackStackRecord.verifyOps(block: BackStackRecordVerify.() -> Unit) {
    val verify = BackStackRecordVerify(this).apply(block)
    assertWithMessage("Not all operations were verified")
        .that(verify.currentOp)
        .isEqualTo(mOps.size)
}

internal data class BackStackRecordOpInfo(
    var fromExpandedOp: Boolean = false
)

private fun FragmentTransaction.Op.verify(
    opIndex: Int,
    command: Int,
    fragment: Fragment? = null,
    block: BackStackRecordOpInfo.() -> Unit = {}
) {
    val (fromExpandedOp) = BackStackRecordOpInfo().apply { block() }

    assertWithMessage("Operation $opIndex had the wrong command")
        .that(mCmd)
        .isEqualTo(command)
    assertWithMessage("Operation $opIndex had the wrong fragment")
        .that(mFragment)
        .isSameInstanceAs(fragment)
    assertWithMessage(
        "Operation $opIndex " + (if (fromExpandedOp) "should" else "shouldn't") +
            " be marked as from an expanded op"
    ).that(mFromExpandedOp).isEqualTo(fromExpandedOp)
    assertThat(mFromExpandedOp).isEqualTo(fromExpandedOp)
}
