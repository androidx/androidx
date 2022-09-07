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

package androidx.fragment.app

import android.content.res.Configuration
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ProviderCallbackTest {

    @Test
    fun onConfigurationChanged() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragment = CallbackFragment()

            withActivity {
                fm.beginTransaction()
                    .replace(R.id.content, fragment)
                    .commitNow()
            }

            withActivity {
                val newConfig = Configuration(resources.configuration)
                onConfigurationChanged(newConfig)
            }
            assertThat(fragment.configChangedCount).isEqualTo(1)
        }
    }

    @Test
    fun onConfigurationChangedNestedFragments() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val parent = StrictViewFragment(R.layout.fragment_container_view)
            val child = CallbackFragment()

            withActivity {
                fm.beginTransaction()
                    .replace(R.id.content, parent)
                    .commitNow()

                parent.childFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, child)
                    .commitNow()
            }

            withActivity {
                val newConfig = Configuration(resources.configuration)
                onConfigurationChanged(newConfig)
            }
            assertThat(child.configChangedCount).isEqualTo(1)
        }
    }
}

class CallbackFragment : StrictViewFragment() {
    var configChangedCount = 0

    override fun onConfigurationChanged(newConfig: Configuration) {
        configChangedCount++
    }
}
