/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.ActivityResult
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentResultTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testReplaceResult() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)

            assertWithMessage("The result is not set").that(fragment1.actualResult).isNull()

            withActivity { fm.popBackStackImmediate() }

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testClearResult() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)
            fm.clearFragmentResult("requestKey")

            assertWithMessage("The result should not be set").that(fragment1.actualResult).isNull()

            withActivity { fm.popBackStackImmediate() }

            assertWithMessage("The result should not be set").that(fragment1.actualResult).isNull()
        }
    }

    @Test
    fun testClearResultListener() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)
            withActivity { fm.clearFragmentResultListener("requestKey") }

            fm.setFragmentResult("requestKey", resultBundle)

            assertWithMessage("The result is incorrect").that(fragment1.actualResult).isNull()
        }
    }

    @Test
    fun testClearResultListenerInCallback() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }

            val fragment1 = ClearResultFragment()

            // set a result while no listener is available so it is stored in the fragment manager
            fm.setFragmentResult("requestKey", Bundle())

            // adding the fragment is going to execute and clear its listener.
            withActivity {
                fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commitNow()
                // lets set another listener with the same key as the original
                fm.setFragmentResultListener(
                    "requestKey",
                    fragment1,
                    FragmentResultListener { _, _ -> }
                )
            }

            // do a replace to force the lifecycle back below STARTED
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, StrictFragment())
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            // store the result in the fragment manager since no listener is available
            fm.setFragmentResult("requestKey", Bundle())

            // pop the back stack to execute the new listener
            withActivity { fm.popBackStackImmediate() }

            assertWithMessage("the first listener should only be executed once")
                .that(fragment1.callbackCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun testClearResultListenerInCallbackWhenStarted() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }

            val fragment1 = ClearResultFragment(Lifecycle.State.RESUMED)

            // set a result while no listener is available so it is stored in the fragment manager
            fm.setFragmentResult("requestKey", Bundle())

            // adding the fragment is going to execute and clear its listener.
            withActivity {
                fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commitNow()
            }

            withActivity {
                // Send a second result, which should not be received by fragment1
                fm.setFragmentResult("requestKey", Bundle())
            }

            assertWithMessage("the listener should only be executed once")
                .that(fragment1.callbackCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun testResetResultListener() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }

            var firstListenerFired = false
            var secondListenerFired = false

            val fragment1 = StrictFragment()

            withActivity {
                // set a listener
                fm.setFragmentResultListener(
                    "requestKey",
                    fragment1,
                    FragmentResultListener { _, _ -> firstListenerFired = true }
                )

                // lets set another listener before the first is fired
                fm.setFragmentResultListener(
                    "requestKey",
                    fragment1,
                    FragmentResultListener { _, _ -> secondListenerFired = true }
                )
            }

            // set a result while no listener is available so it is stored in the fragment manager
            fm.setFragmentResult("requestKey", Bundle())

            // adding the fragment is going to execute the listener's callback
            withActivity {
                fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commitNow()
            }

            assertWithMessage("the first listener should never be executed")
                .that(firstListenerFired)
                .isFalse()
            assertWithMessage("the second listener should have be executed")
                .that(secondListenerFired)
                .isTrue()
        }
    }

    @Test
    fun testSetResultWhileResumed() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment2).commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testStoredSetResultWhileResumed() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = StrictFragment()
            var actualResult: String? = null

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)

            withActivity {
                fm.setFragmentResultListener(
                    "requestKey",
                    fragment1,
                    FragmentResultListener { _, bundle ->
                        actualResult = bundle.getString("bundleKey")
                    }
                )
            }

            assertWithMessage("The result is incorrect")
                .that(actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testReplaceResultSavedRestore() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            var fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            var fragment1 = ResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "fragment1").commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)

            assertWithMessage("The result is not set").that(fragment1.actualResult).isNull()

            recreate()

            fm = withActivity { supportFragmentManager }

            withActivity { fm.popBackStackImmediate() }

            fragment1 = fm.findFragmentByTag("fragment1") as ResultFragment

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testChildFragmentResult() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val parent = ParentResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, parent).commit()
            executePendingTransactions()

            val child = ChildResultFragment()

            parent.childFragmentManager.beginTransaction().add(child, "child").commit()
            executePendingTransactions()

            assertWithMessage("The result is incorrect")
                .that(parent.actualResult)
                .isEqualTo("resultGood")
        }
    }

    @Test
    fun testReplaceResultWithParcelableOnRecreation() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            var fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            var fragment1 = ParcelableResultFragment()

            fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "fragment1").commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = ActivityResult(Activity.RESULT_OK, null)
            resultBundle.putParcelable("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)

            assertWithMessage("The result is not set").that(fragment1.actualResult).isNull()

            recreate()

            fm = withActivity { supportFragmentManager }

            withActivity { fm.popBackStackImmediate() }

            fragment1 = fm.findFragmentByTag("fragment1") as ParcelableResultFragment

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }
}

class ResultFragment : StrictFragment() {
    var actualResult: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            "requestKey",
            this,
            FragmentResultListener { _, bundle -> actualResult = bundle.getString("bundleKey") }
        )
    }
}

class ClearResultFragment(
    private val setLifecycleInState: Lifecycle.State = Lifecycle.State.CREATED
) : StrictFragment() {
    var callbackCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (Lifecycle.Event.upTo(setLifecycleInState) == event) {
                    parentFragmentManager.setFragmentResultListener(
                        "requestKey",
                        this,
                        FragmentResultListener { _, _ ->
                            callbackCount++
                            parentFragmentManager.clearFragmentResultListener("requestKey")
                        }
                    )
                }
            }
        )
    }
}

class ParentResultFragment : StrictFragment() {
    var actualResult: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            "requestKey",
            this,
            FragmentResultListener { _, bundle -> actualResult = bundle.getString("bundleKey") }
        )
    }
}

class ChildResultFragment : StrictFragment() {
    override fun onStart() {
        super.onStart()
        val resultBundle = Bundle().apply { putString("bundleKey", "resultGood") }
        parentFragmentManager.setFragmentResult("requestKey", resultBundle)
    }
}

class ParcelableResultFragment : StrictFragment() {
    var actualResult: Parcelable? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            "requestKey",
            this,
            FragmentResultListener { _, bundle ->
                actualResult = bundle.getParcelable<ActivityResult>("bundleKey")
            }
        )
    }
}
