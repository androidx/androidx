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

package androidx.fragment.app

import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SaveRestoreBackStackTest {

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun saveBackStack() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(fragmentReplacement.viewModel.cleared)
                .isFalse()

            fm.saveBackStack("replacement")
            executePendingTransactions()

            assertWithMessage("Saved Fragments should have their state saved")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isTrue()

            // Saved Fragments should be destroyed
            assertWithMessage("Saved Fragments should be destroyed")
                .that(fragmentReplacement.calledOnDestroy)
                .isTrue()
            // But any ViewModels should not be cleared so that they're available
            // for later restoration
            assertWithMessage("ViewModel should not be cleared after saveBackStack()")
                .that(fragmentReplacement.viewModel.cleared)
                .isFalse()

            moveToState(Lifecycle.State.DESTROYED)

            assertWithMessage("ViewModel should be cleared after the activity is fully destroyed")
                .that(fragmentReplacement.viewModel.cleared)
                .isTrue()
        }
    }

    @Test
    fun saveBackStackWithoutExecutePendingTransactions() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            withActivity {
                fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content, fragmentReplacement)
                    .addToBackStack("replacement")
                    .commit()
                // Immediately save the back stack without calling executePendingTransactions
                fm.saveBackStack("replacement")
            }
            executePendingTransactions()

            // Now restore the back stack to ensure that the replacement fragment is restored
            fm.restoreBackStack("replacement")
            executePendingTransactions()

            val newFragmentReplacement = fm.findFragmentById(R.id.content)
            assertThat(newFragmentReplacement).isInstanceOf(StateSaveFragment::class.java)
            assertThat(fm.backStackEntryCount).isEqualTo(1)
        }
    }

    @Test
    fun saveBackStackAddedWithoutExecutePendingTransactions() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            withActivity {
                // Now we add the new transaction and immediately do a save+restore
                // without calling executePendingTransactions
                fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content, fragmentReplacement)
                    .addToBackStack("replacement")
                    .commit()
                fm.saveBackStack("replacement")
                fm.restoreBackStack("replacement")
            }
            executePendingTransactions()

            val newFragmentReplacement = fm.findFragmentById(R.id.content)
            assertThat(newFragmentReplacement).isInstanceOf(StateSaveFragment::class.java)
            assertThat(fm.backStackEntryCount).isEqualTo(1)
        }
    }

    @Test
    fun savePreviouslyReferencedFragment() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .remove(fragmentBase)
                .add(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            try {
                withActivity {
                    fm.saveBackStack("replacement")
                    fm.executePendingTransactions()
                }
                fail("executePendingTransactions() should fail with an IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e)
                    .hasMessageThat()
                    .startsWith(
                        "saveBackStack(\"replacement\") must be self contained and not " +
                            "reference fragments from non-saved FragmentTransactions."
                    )
            }
        }
    }

    @Test
    fun saveNonReorderingAllowedTransaction() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            try {
                withActivity {
                    fm.saveBackStack("replacement")
                    fm.executePendingTransactions()
                }
                fail("executePendingTransactions() should fail with an IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e)
                    .hasMessageThat()
                    .startsWith(
                        "saveBackStack(\"replacement\") included FragmentTransactions must use " +
                            "setReorderingAllowed(true) to ensure that the back stack can be " +
                            "restored as an atomic operation."
                    )
            }
        }
    }

    @Test
    fun saveNonReorderingAllowedSecondTransaction() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StrictViewFragment()
            val fragmentAboveReplacement = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .replace(R.id.content, fragmentAboveReplacement)
                .addToBackStack("above_replacement")
                .commit()
            executePendingTransactions()

            try {
                withActivity {
                    fm.saveBackStack("replacement")
                    fm.executePendingTransactions()
                }
                fail("executePendingTransactions() should fail with an IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e)
                    .hasMessageThat()
                    .startsWith(
                        "saveBackStack(\"replacement\") included FragmentTransactions must use " +
                            "setReorderingAllowed(true) to ensure that the back stack can be " +
                            "restored as an atomic operation."
                    )
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun saveRetainedFragment() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StrictViewFragment()
            fragmentReplacement.retainInstance = true

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            try {
                withActivity {
                    fm.saveBackStack("replacement")
                    fm.executePendingTransactions()
                }
                fail("executePendingTransactions() should fail with an IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e)
                    .hasMessageThat()
                    .startsWith(
                        "saveBackStack(\"replacement\") must not contain retained fragments. " +
                            "Found direct reference to retained fragment "
                    )
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun saveRetainedChildFragment() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StrictViewFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            fragmentReplacement.childFragmentManager.beginTransaction()
                .add(StrictFragment().apply { retainInstance = true }, "retained")
                .commit()
            executePendingTransactions(fragmentReplacement.childFragmentManager)

            try {
                withActivity {
                    fm.saveBackStack("replacement")
                    fm.executePendingTransactions()
                }
                fail("executePendingTransactions() should fail with an IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e)
                    .hasMessageThat()
                    .startsWith(
                        "saveBackStack(\"replacement\") must not contain retained fragments. " +
                            "Found retained child fragment "
                    )
            }
        }
    }

    @Test
    fun restoreBackStack() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment("saved", "unsaved")

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            val originalViewModel = fragmentReplacement.viewModel
            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(originalViewModel.cleared)
                .isFalse()
            assertThat(fm.backStackEntryCount).isEqualTo(1)

            fm.saveBackStack("replacement")
            executePendingTransactions()

            assertWithMessage("Saved Fragments should have their state saved")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isTrue()
            assertThat(fm.backStackEntryCount).isEqualTo(0)

            // Saved Fragments should be destroyed
            assertWithMessage("Saved Fragments should be destroyed")
                .that(fragmentReplacement.calledOnDestroy)
                .isTrue()
            // But any ViewModels should not be cleared so that they're available
            // for later restoration
            assertWithMessage("ViewModel should not be cleared after saveBackStack()")
                .that(originalViewModel.cleared)
                .isFalse()

            fm.restoreBackStack("replacement")
            executePendingTransactions()

            val newFragmentReplacement = fm.findFragmentById(R.id.content)

            assertThat(newFragmentReplacement).isInstanceOf(StateSaveFragment::class.java)

            val stateSavedReplacement = newFragmentReplacement as StateSaveFragment

            // Assert that restored fragment has its saved state restored
            assertThat(stateSavedReplacement.savedState).isEqualTo("saved")
            assertThat(stateSavedReplacement.unsavedState).isNull()
            assertThat(stateSavedReplacement.viewModel).isSameInstanceAs(originalViewModel)
            assertThat(fm.backStackEntryCount).isEqualTo(1)
        }
    }

    @Test
    fun restoreBackStackTwice() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment("saved", "unsaved")

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            fm.saveBackStack("replacement")
            executePendingTransactions()

            withActivity {
                // Only the first restoreBackStack() should have state to restore
                fm.restoreBackStack("replacement")
                fm.restoreBackStack("replacement")
            }
            executePendingTransactions()

            val newFragmentReplacement = fm.findFragmentById(R.id.content)
            assertThat(newFragmentReplacement).isInstanceOf(StateSaveFragment::class.java)
            assertThat(fm.backStackEntryCount).isEqualTo(1)
        }
    }

    @Test
    fun restoreBackStackTwoTransactions() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment("saved", "unsaved")
            val fragmentAboveReplacement = StateSaveFragment("savedAbove", "unsavedAbove")

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentAboveReplacement)
                .addToBackStack("above_replacement")
                .commit()
            executePendingTransactions()

            val originalViewModel = fragmentAboveReplacement.viewModel
            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(originalViewModel.cleared)
                .isFalse()
            assertThat(fm.backStackEntryCount).isEqualTo(2)

            fm.saveBackStack("replacement")
            executePendingTransactions()

            assertWithMessage("Saved Fragments should have their state saved")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isTrue()
            assertWithMessage("All saved Fragments should have their state saved")
                .that(fragmentAboveReplacement.calledOnSaveInstanceState)
                .isTrue()
            assertThat(fm.backStackEntryCount).isEqualTo(0)

            // Saved Fragments should be destroyed
            assertWithMessage("Saved Fragments should be destroyed")
                .that(fragmentReplacement.calledOnDestroy)
                .isTrue()
            assertWithMessage("All saved Fragments should be destroyed")
                .that(fragmentAboveReplacement.calledOnDestroy)
                .isTrue()
            // But any ViewModels should not be cleared so that they're available
            // for later restoration
            assertWithMessage("ViewModel should not be cleared after saveBackStack()")
                .that(originalViewModel.cleared)
                .isFalse()

            fm.restoreBackStack("replacement")
            executePendingTransactions()

            val newFragmentReplacement = fm.findFragmentById(R.id.content)

            assertThat(newFragmentReplacement).isInstanceOf(StateSaveFragment::class.java)

            val stateSavedReplacement = newFragmentReplacement as StateSaveFragment

            // Assert that restored fragment has its saved state restored
            assertThat(stateSavedReplacement.savedState).isEqualTo("savedAbove")
            assertThat(stateSavedReplacement.unsavedState).isNull()
            assertThat(stateSavedReplacement.viewModel).isSameInstanceAs(originalViewModel)
            assertThat(fm.backStackEntryCount).isEqualTo(2)
        }
    }

    @Test
    fun restoreBackStackAfterRecreate() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            var fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment("saved", "unsaved")

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            val originalViewModel = fragmentReplacement.viewModel
            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(originalViewModel.cleared)
                .isFalse()
            assertThat(fm.backStackEntryCount).isEqualTo(1)

            fm.saveBackStack("replacement")
            executePendingTransactions()

            assertWithMessage("Saved Fragments should have their state saved")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isTrue()
            assertThat(fm.backStackEntryCount).isEqualTo(0)

            // Saved Fragments should be destroyed
            assertWithMessage("Saved Fragments should be destroyed")
                .that(fragmentReplacement.calledOnDestroy)
                .isTrue()
            // But any ViewModels should not be cleared so that they're available
            // for later restoration
            assertWithMessage("ViewModel should not be cleared after saveBackStack()")
                .that(originalViewModel.cleared)
                .isFalse()

            // Now recreate the whole activity while the state of the back stack is saved
            recreate()

            fm = withActivity {
                supportFragmentManager
            }

            fm.restoreBackStack("replacement")
            executePendingTransactions()

            val newFragmentReplacement = fm.findFragmentById(R.id.content)

            assertThat(newFragmentReplacement).isInstanceOf(StateSaveFragment::class.java)

            val stateSavedReplacement = newFragmentReplacement as StateSaveFragment

            // Assert that restored fragment has its saved state restored
            assertThat(stateSavedReplacement.savedState).isEqualTo("saved")
            assertThat(stateSavedReplacement.unsavedState).isNull()
            assertThat(stateSavedReplacement.viewModel).isSameInstanceAs(originalViewModel)
            assertThat(fm.backStackEntryCount).isEqualTo(1)
        }
    }

    @Test
    fun restoreBackStackWithoutExecutePendingTransactions() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment("saved", "unsaved")

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            val originalViewModel = fragmentReplacement.viewModel
            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(originalViewModel.cleared)
                .isFalse()
            assertThat(fm.backStackEntryCount).isEqualTo(1)

            withActivity {
                fm.saveBackStack("replacement")
                // Immediately restore the back stack without calling executePendingTransactions
                fm.restoreBackStack("replacement")
            }
            executePendingTransactions()

            assertWithMessage("Saved Fragments should not go through onSaveInstanceState")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isFalse()
            assertWithMessage("Saved Fragments should not have been destroyed")
                .that(fragmentReplacement.calledOnDestroy)
                .isFalse()
            assertWithMessage("ViewModel should not be cleared after saveBackStack()")
                .that(originalViewModel.cleared)
                .isFalse()

            assertWithMessage("Fragment should still be returned by FragmentManager")
                .that(fm.findFragmentById(R.id.content))
                .isSameInstanceAs(fragmentReplacement)

            // Assert that restored fragment has its saved state restored
            assertThat(fragmentReplacement.savedState).isEqualTo("saved")
            assertThat(fragmentReplacement.unsavedState).isEqualTo("unsaved")
            assertThat(fragmentReplacement.viewModel).isSameInstanceAs(originalViewModel)
            assertThat(fm.backStackEntryCount).isEqualTo(1)
        }
    }

    @Test
    fun clearBackStack() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            val originalViewModel = fragmentReplacement.viewModel
            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(originalViewModel.cleared)
                .isFalse()

            fm.saveBackStack("replacement")
            executePendingTransactions()

            assertWithMessage("Saved Fragments should have their state saved")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isTrue()

            // Saved Fragments should be destroyed
            assertWithMessage("Saved Fragments should be destroyed")
                .that(fragmentReplacement.calledOnDestroy)
                .isTrue()
            // But any ViewModels should not be cleared so that they're available
            // for later restoration
            assertWithMessage("ViewModel should not be cleared after saveBackStack()")
                .that(originalViewModel.cleared)
                .isFalse()

            fm.clearBackStack("replacement")
            executePendingTransactions()

            assertWithMessage("ViewModel should be cleared after the back stack is cleared")
                .that(originalViewModel.cleared)
                .isTrue()
        }
    }

    @Test
    fun clearBackStackWithoutExecutePendingTransactions() {
       withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment("saved", "unsaved")

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            val originalViewModel = fragmentReplacement.viewModel
            assertWithMessage("ViewModel should not be cleared after commit()")
                .that(originalViewModel.cleared)
                .isFalse()
            assertThat(fm.backStackEntryCount).isEqualTo(1)

            withActivity {
                fm.saveBackStack("replacement")
                // Immediately clear the back stack without calling executePendingTransactions
                fm.clearBackStack("replacement")
            }
            executePendingTransactions()

            assertWithMessage("Saved Fragments should not go through onSaveInstanceState")
                .that(fragmentReplacement.calledOnSaveInstanceState)
                .isFalse()
            assertWithMessage("Saved Fragments should have been destroyed when cleared")
                .that(fragmentReplacement.calledOnDestroy)
                .isTrue()
            assertWithMessage("ViewModel should be cleared after the back stack is cleared")
                .that(fragmentReplacement.viewModel.cleared)
                .isTrue()

            // Assert that cleared fragment has been removed
            assertThat(fm.backStackEntryCount).isEqualTo(0)
        }
    }

    @Test
    fun resumeClearsFragmentStoreSavedState() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                supportFragmentManager
            }
            val fragmentBase = StrictViewFragment()
            val fragmentReplacement = StateSaveFragment()
            val fragmentReplacementChild = StateSaveFragment()

            fm.beginTransaction()
                .add(R.id.content, fragmentBase)
                .commit()
            executePendingTransactions()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragmentReplacement)
                .addToBackStack("replacement")
                .commit()
            executePendingTransactions()

            fragmentReplacement.childFragmentManager.beginTransaction()
                .add(fragmentReplacementChild, "replacementChild")
                .commit()
            executePendingTransactions(fragmentReplacement.childFragmentManager)

            // stop activity and save fragments
            moveToState(Lifecycle.State.CREATED)
            executePendingTransactions()

            // states should be stored in fragmentStore
            assertThat(fm.fragmentStore.getSavedState(fragmentReplacement.mWho))
                .isNotNull()
            assertThat(fragmentReplacement.childFragmentManager.fragmentStore
                .getSavedState(fragmentReplacementChild.mWho)
            ).isNotNull()

            // resume activity and restore fragments
            moveToState(Lifecycle.State.RESUMED)
            executePendingTransactions()

            // states should be cleared from fragmentStore
            assertThat(fm.fragmentStore.getSavedState(fragmentReplacement.mWho))
                .isNull()
            assertThat(fragmentReplacement.childFragmentManager.fragmentStore
                .getSavedState(fragmentReplacementChild.mWho)
            ).isNull()
        }
    }
}
