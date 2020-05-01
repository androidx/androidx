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
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentTest {

    @Test
    fun setFragmentResult() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            val fragment1 = ResultFragment()

            val fm = withActivity {
                supportFragmentManager
            }

            withActivity {
                fm.commitNow {
                    add(fragment1, null)
                }
            }
            val expectedResult = "resultGood"
            val fragment2 = SetResultFragment(expectedResult)

            withActivity {
                fm.commitNow {
                    add(fragment2, null)
                }
            }

            assertWithMessage("The result is incorrect")
                .that(fragment1.actualResult)
                .isEqualTo(expectedResult)
        }
    }

    class ResultFragment : Fragment() {
        var actualResult: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setFragmentResultListener("requestKey") { _, bundle ->
                actualResult = bundle.getString("bundleKey")
            }
        }
    }

    class SetResultFragment(val resultString: String) : Fragment() {
        override fun onStart() {
            super.onStart()

            setFragmentResult("requestKey", bundleOf("bundleKey" to resultString))
        }
    }
}
