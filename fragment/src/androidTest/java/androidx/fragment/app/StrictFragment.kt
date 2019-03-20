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

import android.content.Context
import android.os.Bundle
import com.google.common.truth.Truth.assertWithMessage

/**
 * This fragment watches its primary lifecycle events and throws IllegalStateException
 * if any of them are called out of order or from a bad/unexpected state.
 */
open class StrictFragment : Fragment() {
    var currentState: Int = 0

    var calledOnAttach: Boolean = false
    var calledOnCreate: Boolean = false
    var calledOnActivityCreated: Boolean = false
    var calledOnStart: Boolean = false
    var calledOnResume: Boolean = false
    var calledOnSaveInstanceState: Boolean = false
    var calledOnPause: Boolean = false
    var calledOnStop: Boolean = false
    var calledOnDestroy: Boolean = false
    var calledOnDetach: Boolean = false
    var calledOnAttachFragment: Boolean = false
    var lastSavedInstanceState: Bundle? = null

    open fun onStateChanged(fromState: Int) {
        checkGetActivity()
    }

    fun checkGetActivity() {
        assertWithMessage("getActivity() returned null at unexpected time")
            .that(activity)
            .isNotNull()
    }

    fun checkState(caller: String, vararg expected: Int) {
        if (expected.isEmpty()) {
            throw IllegalArgumentException("must supply at least one expected state")
        }
        for (expect in expected) {
            if (currentState == expect) {
                return
            }
        }
        val expectString = StringBuilder(stateToString(expected[0]))
        for (i in 1 until expected.size) {
            expectString.append(" or ").append(stateToString(expected[i]))
        }
        throw IllegalStateException(
            "$caller called while fragment was ${stateToString(currentState)}; " +
                    "expected $expectString"
        )
    }

    fun checkStateAtLeast(caller: String, minState: Int) {
        if (currentState < minState) {
            throw IllegalStateException(
                "$caller called while fragment was ${stateToString(currentState)}; " +
                        "expected at least ${stateToString(minState)}"
            )
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        calledOnAttachFragment = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        calledOnAttach = true
        checkState("onAttach", DETACHED)
        currentState = ATTACHED
        onStateChanged(DETACHED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (calledOnCreate && !calledOnDestroy) {
            throw IllegalStateException("onCreate called more than once with no onDestroy")
        }
        calledOnCreate = true
        lastSavedInstanceState = savedInstanceState
        checkState("onCreate", ATTACHED)
        currentState = CREATED
        onStateChanged(ATTACHED)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        calledOnActivityCreated = true
        checkState("onActivityCreated", ATTACHED, CREATED)
        val fromState = currentState
        currentState = ACTIVITY_CREATED
        onStateChanged(fromState)
    }

    override fun onStart() {
        super.onStart()
        calledOnStart = true
        checkState("onStart", CREATED, ACTIVITY_CREATED)
        currentState = STARTED
        onStateChanged(ACTIVITY_CREATED)
    }

    override fun onResume() {
        super.onResume()
        calledOnResume = true
        checkState("onResume", STARTED)
        currentState = RESUMED
        onStateChanged(STARTED)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        calledOnSaveInstanceState = true
        checkGetActivity()
        // FIXME: We should not allow onSaveInstanceState except when STARTED or greater.
        // But FragmentManager currently does it in saveAllState for fragments on the
        // back stack, so fragments may be in the CREATED state.
        checkStateAtLeast("onSaveInstanceState", CREATED)
    }

    override fun onPause() {
        super.onPause()
        calledOnPause = true
        checkState("onPause", RESUMED)
        currentState = STARTED
        onStateChanged(RESUMED)
    }

    override fun onStop() {
        super.onStop()
        calledOnStop = true
        checkState("onStop", STARTED)
        currentState = CREATED
        onStateChanged(STARTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        calledOnDestroy = true
        checkState("onDestroy", CREATED)
        currentState = ATTACHED
        onStateChanged(CREATED)
    }

    override fun onDetach() {
        super.onDetach()
        calledOnDetach = true
        checkState("onDestroy", CREATED, ATTACHED)
        val fromState = currentState
        currentState = DETACHED
        onStateChanged(fromState)
    }

    companion object {
        const val DETACHED = 0
        const val ATTACHED = 1
        const val CREATED = 2
        const val ACTIVITY_CREATED = 3
        const val STARTED = 4
        const val RESUMED = 5

        internal fun stateToString(state: Int): String {
            when (state) {
                DETACHED -> return "DETACHED"
                ATTACHED -> return "ATTACHED"
                CREATED -> return "CREATED"
                ACTIVITY_CREATED -> return "ACTIVITY_CREATED"
                STARTED -> return "STARTED"
                RESUMED -> return "RESUMED"
            }
            return "(unknown $state)"
        }
    }
}
