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
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class ReentrantFragmentTest(
    private val fromState: StrictFragment.State,
    private val toState: StrictFragment.State
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fromState={0}, toState={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf(StrictFragment.State.ATTACHED, StrictFragment.State.CREATED))
            add(arrayOf(StrictFragment.State.CREATED, StrictFragment.State.ACTIVITY_CREATED))
            add(arrayOf(StrictFragment.State.ACTIVITY_CREATED, StrictFragment.State.STARTED))
            add(arrayOf(StrictFragment.State.STARTED, StrictFragment.State.RESUMED))
            add(arrayOf(StrictFragment.State.RESUMED, StrictFragment.State.STARTED))
            add(arrayOf(StrictFragment.State.STARTED, StrictFragment.State.CREATED))
            add(arrayOf(StrictFragment.State.CREATED, StrictFragment.State.ATTACHED))
            add(arrayOf(StrictFragment.State.ATTACHED, StrictFragment.State.DETACHED))
        }
    }

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(EmptyFragmentTestActivity::class.java)

    // Make sure that executing transactions during activity lifecycle events
    // is properly prevented.
    @Test
    fun preventReentrantCalls() {
        activityRule.runOnUiThread(
            Runnable {
                val viewModelStore = ViewModelStore()
                val fc1 = activityRule.startupFragmentController(viewModelStore)

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
                    @Suppress("DEPRECATION")
                    savedState = fc1.saveAllState()
                    fc1.dispatchStop()
                    fc1.dispatchDestroy()
                    if (fromState > toState) {
                        fail(
                            "Expected IllegalStateException when moving from " +
                                "$fromState to $toState"
                        )
                    }
                } catch (e: IllegalStateException) {
                    if (fromState < toState) {
                        fail(
                            "Unexpected IllegalStateException when moving from " +
                                "$fromState to $toState"
                        )
                    }
                    assertThat(e)
                        .hasMessageThat()
                        .contains("FragmentManager is already executing transactions")
                    return@Runnable // test passed!
                }

                // now restore from saved state. This will be reached when
                // fromState < toState. We want to catch the fragment while it
                // is being restored as the fragment controller state is being brought up.

                try {
                    activityRule.startupFragmentController(
                        viewModelStore,
                        savedState
                    )
                    fail(
                        "Expected IllegalStateException when moving from " +
                            "$fromState to $toState"
                    )
                } catch (e: IllegalStateException) {
                    assertThat(e)
                        .hasMessageThat()
                        .contains("FragmentManager is already executing transactions")
                }
            }
        )
    }
}

class ReentrantFragment : StrictFragment() {
    companion object {
        private const val FROM_STATE = "fromState"
        private const val TO_STATE = "toState"

        fun create(fromState: State, toState: State): ReentrantFragment {
            val fragment = ReentrantFragment()
            fragment.fromState = fromState
            fragment.toState = toState
            fragment.isRestored = false
            return fragment
        }
    }

    private var fromState = State.DETACHED
    private var toState = State.DETACHED
    private var isRestored: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            fromState = savedInstanceState.getSerializable(FROM_STATE) as State
            toState = savedInstanceState.getSerializable(TO_STATE) as State
            isRestored = true
        }
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(FROM_STATE, fromState)
        outState.putSerializable(TO_STATE, toState)
    }

    override fun onStateChanged(fromState: State) {
        super.onStateChanged(fromState)
        // We execute the transaction when shutting down or after restoring
        if (fromState == this.fromState && currentState == toState &&
            (toState < this.fromState || isRestored)
        ) {
            executeTransaction()
        }
    }

    private fun executeTransaction() {
        parentFragmentManager.beginTransaction()
            .add(StrictFragment(), "should throw")
            .commitNow()
    }
}
