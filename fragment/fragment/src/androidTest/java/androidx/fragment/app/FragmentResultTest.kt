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

import android.os.Bundle
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentResultTest {

    @Test
    fun testReplaceResult() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .commit()
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

            assertWithMessage("The result is not set")
                .that(fragment1.actualResult)
                .isNull()

            withActivity {
                fm.popBackStackImmediate()
            }

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testClearResult() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .commit()
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

            assertWithMessage("The result should not be set")
                .that(fragment1.actualResult)
                .isNull()

            withActivity {
                fm.popBackStackImmediate()
            }

            assertWithMessage("The result should not be set")
                .that(fragment1.actualResult)
                .isNull()
        }
    }

    @Test
    fun testClearResultListener() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.clearFragmentResultListener("requestKey")
            fm.setFragmentResult("requestKey", resultBundle)

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isNull()
        }
    }

    @Test
    fun testSetResultWhileResumed() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = ResultFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment2)
                .commit()
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
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val fragment1 = StrictFragment()
            var actualResult: String? = null

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .commit()
            executePendingTransactions()

            val resultBundle = Bundle()
            val expectedResult = "resultGood"
            resultBundle.putString("bundleKey", expectedResult)

            fm.setFragmentResult("requestKey", resultBundle)

            fm.setFragmentResultListener("requestKey", fragment1, FragmentResultListener
            { _, bundle -> actualResult = bundle.getString("bundleKey") })

            assertWithMessage("The result is incorrect")
                .that(actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testReplaceResultSavedRestore() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            var fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            var fragment1 = ResultFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "fragment1")
                .commit()
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

            assertWithMessage("The result is not set")
                .that(fragment1.actualResult)
                .isNull()

            recreate()

            fm = withActivity { supportFragmentManager }

            withActivity {
                fm.popBackStackImmediate()
            }

            fragment1 = fm.findFragmentByTag("fragment1") as ResultFragment

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun testChildFragmentResult() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val parent = ParentResultFragment()

            fm.beginTransaction()
                .add(R.id.fragmentContainer, parent)
                .commit()
            executePendingTransactions()

            val child = ChildResultFragment()

            parent.childFragmentManager.beginTransaction()
                .add(child, "child")
                .commit()
            executePendingTransactions()

            assertWithMessage("The result is incorrect")
                .that(parent.actualResult)
                .isEqualTo("resultGood")
        }
    }
}

class ResultFragment : StrictFragment() {
    var actualResult: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener("requestKey", this, FragmentResultListener
        { _, bundle -> actualResult = bundle.getString("bundleKey") })
    }
}

class ParentResultFragment : StrictFragment() {
    var actualResult: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener("requestKey", this, FragmentResultListener
        { _, bundle -> actualResult = bundle.getString("bundleKey") })
    }
}

class ChildResultFragment : StrictFragment() {
    override fun onStart() {
        super.onStart()
        val resultBundle = Bundle().apply {
            putString("bundleKey", "resultGood")
        }
        parentFragmentManager.setFragmentResult("requestKey", resultBundle)
    }
}