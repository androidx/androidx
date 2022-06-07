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

package androidx.navigation.dynamicfeatures.fragment.ui

import androidx.navigation.dynamicfeatures.fragment.R as mainR
import androidx.navigation.dynamicfeatures.fragment.test.R as testR
import android.widget.TextView
import androidx.navigation.dynamicfeatures.fragment.DynamicNavHostFragment
import androidx.navigation.dynamicfeatures.fragment.NavigationActivity
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class DefaultProgressFragmentTest {
    @Test
    fun testInstallationFailure() {
        lateinit var fragment: DynamicNavHostFragment
        with(ActivityScenario.launch(NavigationActivity::class.java)) {
            withActivity {
                fragment = DynamicNavHostFragment()
                supportFragmentManager.beginTransaction()
                    .add(testR.id.nav_host, fragment)
                    .commitNow()

                // setting the graph causes an attempted install of a dynamic-include,
                // which should fail
                val navController = fragment.findNavController()
                navController.setGraph(testR.navigation.include_dynamic_nav_graph)
            }

            // check that we are now on the installation failed screen
            withActivity {
                val title = findViewById<TextView>(mainR.id.progress_title)
                val installationFailedText = fragment.requireContext().resources
                    .getText(mainR.string.installation_failed)
                assertThat(title.text).isEqualTo(installationFailedText)
            }
        }
    }
}