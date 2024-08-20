/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
class PredictiveBackTest {

    @Test
    fun backOnLastRecordTest() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .commit()
            executePendingTransactions()

            val fragment2 = StrictViewFragment()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(fm.backStackEntryCount).isEqualTo(1)

            val dispatcher = withActivity { onBackPressedDispatcher }
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
            executePendingTransactions()

            // In the broken case, the Activity will end here before we get to call onBackPressed
            // and trigger a cancellation.

            assertThat(fm.backStackEntryCount).isEqualTo(1)

            withActivity { dispatcher.onBackPressed() }
            executePendingTransactions()

            assertThat(fm.backStackEntryCount).isEqualTo(0)
        }
    }

    @Test
    fun backOnNoRecordDuringTransactionTest() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val fragment2 = StrictViewFragment()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(fm.backStackEntryCount).isEqualTo(2)

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                dispatcher.onBackPressed()
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                dispatcher.onBackPressed()
            }
            executePendingTransactions()

            assertThat(fm.backStackEntryCount).isEqualTo(0)
        }
    }

    @Test
    fun backOnNoRecordTest() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment()

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .commit()
            executePendingTransactions()

            val dispatcher = withActivity { onBackPressedDispatcher }

            // We need a pending commit that doesn't include a fragment to mimic calling
            // system back while commit is pending.
            fm.beginTransaction().commit()

            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
            withActivity { dispatcher.onBackPressed() }

            assertThat(fm.backStackEntryCount).isEqualTo(0)
        }
    }
}
