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

package androidx.navigation.fragment

import androidx.navigation.findNavController
import androidx.navigation.fragment.test.NavigationActivity
import androidx.navigation.fragment.test.NavigationActivityWithFragmentTag
import androidx.navigation.fragment.test.NavigationBaseActivity
import androidx.navigation.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class NavHostFragmentTest(
    private val activityClass: Class<NavigationBaseActivity>
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Class<out NavigationBaseActivity>> {
            return arrayOf(
                NavigationActivity::class.java,
                NavigationActivityWithFragmentTag::class.java
            )
        }
    }

    @Test
    fun testFindNavControllerXml() {
        with(ActivityScenario.launch(activityClass)) {
            val navController = withActivity {
                findNavController(R.id.nav_host)
            }
            assertWithMessage("NavController on the activity's view should be non-null")
                .that(navController)
                .isNotNull()
            val hostRootNavController = withActivity {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host)!!
                navHostFragment.requireView().findNavController()
            }
            assertWithMessage("NavController on the host's root view should be non-null")
                .that(hostRootNavController)
                .isNotNull()
        }
    }
}
