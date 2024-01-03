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

import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.dynamicfeatures.fragment.DynamicNavHostFragment
import androidx.navigation.dynamicfeatures.fragment.NavigationActivity
import androidx.navigation.dynamicfeatures.fragment.R as mainR
import androidx.navigation.dynamicfeatures.fragment.test.R as testR
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class DefaultProgressFragmentTest {
    @Test
    fun testInstallationFailure() {
        lateinit var fragment: DynamicNavHostFragment
        lateinit var defaultProgressFragment: DefaultProgressFragment
        val failureCountdownLatch = CountDownLatch(1)
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

            withActivity {
                defaultProgressFragment =
                    fragment.childFragmentManager.primaryNavigationFragment
                        as DefaultProgressFragment
                val viewModel = ViewModelProvider(
                    defaultProgressFragment, InstallViewModel.FACTORY
                )[InstallViewModel::class.java]
                // On devices that have play store installed, instead of the split install failing
                // synchronously without a connection, the connection succeeds and we end up failing
                // asynchronously since there are no play accounts to install. In this case, we need
                // the test to wait for the failure signal from the splitInstall session. To do that
                // we observe the livedata of the DefaultProgressFragment's viewModel, and wait for
                // it to fail before we check for test failure.
                val liveData = viewModel.installMonitor!!.status
                val observer = object : Observer<SplitInstallSessionState> {
                    override fun onChanged(value: SplitInstallSessionState) {
                        if (value.status() == SplitInstallSessionStatus.FAILED) {
                            liveData.removeObserver(this)
                            failureCountdownLatch.countDown()
                        }
                    }
                }
                liveData.observe(defaultProgressFragment, observer)
            }

            assertThat(failureCountdownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

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
