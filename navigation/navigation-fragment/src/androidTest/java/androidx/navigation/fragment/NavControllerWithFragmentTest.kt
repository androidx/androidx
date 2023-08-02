/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.navigation.fragment

import androidx.navigation.NavOptions
import androidx.navigation.fragment.test.NavigationActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import org.junit.Test
import org.junit.runner.RunWith
import androidx.navigation.fragment.test.R
import com.google.common.truth.Truth.assertWithMessage

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavControllerWithFragmentTest {

    @Test
    fun navigateWithSingleTop() {
        with(ActivityScenario.launch(NavigationActivity::class.java)) {
            withActivity {
                navController.navigate(R.id.empty_fragment)

                val fm =
                    supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
                fm?.executePendingTransactions()
                val fragment = fm?.findFragmentById(R.id.nav_host)

                navController.navigate(
                    R.id.empty_fragment,
                    null,
                    NavOptions.Builder().setLaunchSingleTop(true).build()
                )

                fm?.executePendingTransactions()

                val replacementFragment = fm?.findFragmentById(R.id.nav_host)

                assertWithMessage("Replacement should be a new instance")
                    .that(replacementFragment)
                    .isNotSameInstanceAs(fragment)
            }
        }
    }
}