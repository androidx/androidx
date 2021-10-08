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

package androidx.navigation.fragment

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.fragment.test.NavigationActivityMultiNavHost
import androidx.navigation.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class MultiNavHostFragmentTest {

    @Test
    fun testFragmentToNavHost() {
        with(ActivityScenario.launch(NavigationActivityMultiNavHost::class.java)) {
            val navController = withActivity {
                findNavController(R.id.nav_host_fragment)
            }

            withActivity {
                navController.setGraph(R.navigation.nav_nav_host)
                navController.navigate(R.id.nav_host_1)
            }

            val rootNavController = withActivity {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.requireView().findNavController()
            }
            assertWithMessage("Child should have changed the NavController")
                .that(rootNavController)
                .isNotEqualTo(navController)
        }
    }

    @Test
    fun testNavHostToFragment() {
        with(ActivityScenario.launch(NavigationActivityMultiNavHost::class.java)) {
            val navController = withActivity {
                findNavController(R.id.nav_host_fragment)
            }

            withActivity {
                navController.setGraph(R.navigation.nav_nav_host)
                navController.navigate(R.id.nav_host_1)
            }
            val childFragment = withActivity {
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager?.findFragmentById(R.id.nav_host_fragment) as
                    BasicNavHostFragment
            }

            withActivity {
                navController.popBackStack()
            }
            val returnNavController = withActivity {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.requireView().findNavController()
            }

            assertThat(childFragment.destroyViewCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            assertWithMessage("NavController should not have changed")
                .that(returnNavController)
                .isSameInstanceAs(navController)
        }
    }

    @Test
    fun testNavHostToNavHost() {
        with(ActivityScenario.launch(NavigationActivityMultiNavHost::class.java)) {
            val navController = withActivity {
                findNavController(R.id.nav_host_fragment)
            }

            withActivity {
                navController.setGraph(R.navigation.nav_nav_host)
                navController.navigate(R.id.nav_host_1)
            }

            val firstChildNavController = withActivity {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.requireView().findNavController()
            }
            assertWithMessage("child should have changed the NavController")
                .that(firstChildNavController)
                .isNotEqualTo(navController)

            withActivity {
                navController.navigate(R.id.nav_host_2)
            }
            val secondChildNavController = withActivity {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.requireView().findNavController()
            }

            assertWithMessage("Second child should have changed the NavController")
                .that(secondChildNavController)
                .isNotEqualTo(firstChildNavController)

            assertWithMessage("the second child navHost should be different from the parent")
                .that(secondChildNavController)
                .isNotEqualTo(navController)
        }
    }
}

class BasicNavHostFragment : NavHostFragment() {
    val destroyViewCountDownLatch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController.setGraph(R.navigation.nav_nav_host)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyViewCountDownLatch.countDown()
    }
}