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

package androidx.fragment.app

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.ViewModelStore
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class ReentrantFragmentTest(
    private val fromState: Int,
    private val toState: Int
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fromState={0}, toState={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf(StrictFragment.ATTACHED, StrictFragment.CREATED))
            add(arrayOf(StrictFragment.CREATED, StrictFragment.ACTIVITY_CREATED))
            add(arrayOf(StrictFragment.ACTIVITY_CREATED, StrictFragment.STARTED))
            add(arrayOf(StrictFragment.STARTED, StrictFragment.RESUMED))
            add(arrayOf(StrictFragment.RESUMED, StrictFragment.STARTED))
            add(arrayOf(StrictFragment.STARTED, StrictFragment.CREATED))
            add(arrayOf(StrictFragment.CREATED, StrictFragment.ATTACHED))
            add(arrayOf(StrictFragment.ATTACHED, StrictFragment.DETACHED))
        }
    }

    @get:Rule
    val activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    // Make sure that executing transactions during activity lifecycle events
    // is properly prevented.
    @Test
    fun preventReentrantCalls() {
        activityRule.runOnUiThread(Runnable {
            val viewModelStore = ViewModelStore()
            val fc1 = FragmentTestUtil.startupFragmentController(
                activityRule.activity,
                null,
                viewModelStore
            )

            val fm1 = fc1.supportFragmentManager

            val reentrantFragment = ReentrantFragment.create(fromState, toState)

            fm1.beginTransaction().add(reentrantFragment, "reentrant").commit()
            try {
                fm1.executePendingTransactions()
            } catch (e: IllegalStateException) {
                fail("An exception shouldn't happen when initially adding the fragment")
            }

            // Now shut down the fragment controller. When fromState > toState, this should
            // result in an exception
            val savedState: Parcelable?
            try {
                fc1.dispatchPause()
                savedState = fc1.saveAllState()
                fc1.dispatchStop()
                fc1.dispatchDestroy()
                if (fromState > toState) {
                    fail(
                        "Expected IllegalStateException when moving from " +
                                StrictFragment.stateToString(fromState) + " to " +
                                StrictFragment.stateToString(toState)
                    )
                }
            } catch (e: IllegalStateException) {
                if (fromState < toState) {
                    fail(
                        "Unexpected IllegalStateException when moving from " +
                                StrictFragment.stateToString(fromState) + " to " +
                                StrictFragment.stateToString(toState)
                    )
                }
                assertThat(e)
                    .hasMessageThat().contains("FragmentManager is already executing transactions")
                return@Runnable // test passed!
            }

            // now restore from saved state. This will be reached when
            // fromState < toState. We want to catch the fragment while it
            // is being restored as the fragment controller state is being brought up.

            try {
                FragmentTestUtil.startupFragmentController(
                    activityRule.activity,
                    savedState,
                    viewModelStore
                )
                fail(
                    "Expected IllegalStateException when moving from " +
                            StrictFragment.stateToString(fromState) + " to " +
                            StrictFragment.stateToString(toState)
                )
            } catch (e: IllegalStateException) {
                assertThat(e)
                    .hasMessageThat()
                    .contains("FragmentManager is already executing transactions")
            }
        })
    }
}

class ReentrantFragment : StrictFragment() {
    companion object {
        private const val FROM_STATE = "fromState"
        private const val TO_STATE = "toState"

        fun create(fromState: Int, toState: Int): ReentrantFragment {
            val fragment = ReentrantFragment()
            fragment.fromState = fromState
            fragment.toState = toState
            fragment.isRestored = false
            return fragment
        }
    }

    private var fromState = 0
    private var toState = 0
    private var isRestored: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            fromState = savedInstanceState.getInt(FROM_STATE)
            toState = savedInstanceState.getInt(TO_STATE)
            isRestored = true
        }
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(FROM_STATE, fromState)
        outState.putInt(TO_STATE, toState)
    }

    override fun onStateChanged(fromState: Int) {
        super.onStateChanged(fromState)
        // We execute the transaction when shutting down or after restoring
        if (fromState == this.fromState && currentState == toState &&
            (toState < this.fromState || isRestored)
        ) {
            executeTransaction()
        }
    }

    private fun executeTransaction() {
        requireFragmentManager().beginTransaction()
            .add(StrictFragment(), "should throw")
            .commitNow()
    }
}
