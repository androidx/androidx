/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentManagerSavedStateRegistryTest {

    @Test
    @Throws(Throwable::class)
    fun savedState() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }
            fragmentManager.beginTransaction()
                .add(StateSaveFragment(TEST_FRAGMENT_STRING), FRAGMENT_TAG)
                .commit()
            executePendingTransactions()

            recreate()
            val fragment = withActivity { supportFragmentManager }
                .findFragmentByTag(FRAGMENT_TAG) as StateSaveFragment
            assertThat(fragment.savedState).isEqualTo(TEST_FRAGMENT_STRING)
        }
    }
}

private const val FRAGMENT_TAG = "TEST_TAG"
private const val TEST_FRAGMENT_STRING = "TEST_STRING"
