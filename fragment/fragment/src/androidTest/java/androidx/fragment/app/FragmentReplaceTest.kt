/*
 * Copyright 2018 The Android Open Source Project
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
import android.view.View
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test to prevent regressions in SupportFragmentManager fragment replace method.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentReplaceTest {

    @Test
    fun testReplaceFragment() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            fm.beginTransaction()
                .add(R.id.content, StrictViewFragment(R.layout.fragment_a))
                .addToBackStack(null)
                .commit()
            executePendingTransactions()
            withActivity {
                assertThat(findViewById<View>(R.id.textA)).isNotNull()
                assertThat(findViewById<View>(R.id.textB)).isNull()
                assertThat(findViewById<View>(R.id.textC)).isNull()
            }

            fm.beginTransaction()
                .add(R.id.content, StrictViewFragment(R.layout.fragment_b))
                .addToBackStack(null)
                .commit()
            executePendingTransactions()
            withActivity {
                assertThat(findViewById<View>(R.id.textA)).isNotNull()
                assertThat(findViewById<View>(R.id.textB)).isNotNull()
                assertThat(findViewById<View>(R.id.textC)).isNull()
            }

            fm.beginTransaction()
                .replace(R.id.content, StrictViewFragment(R.layout.fragment_c))
                .addToBackStack(null)
                .commit()
            executePendingTransactions()
            withActivity {
                assertThat(findViewById<View>(R.id.textA)).isNull()
                assertThat(findViewById<View>(R.id.textB)).isNull()
                assertThat(findViewById<View>(R.id.textC)).isNotNull()
            }
        }
    }

    @Test
    fun testReplaceFragmentInOnCreate() {
        with(ActivityScenario.launch(ReplaceInCreateActivity::class.java)) {
            val replaceInCreateFragment = withActivity { this.replaceInCreateFragment }

            assertThat(replaceInCreateFragment.isAdded)
                .isFalse()
            withActivity {
                assertThat(findViewById<View>(R.id.textA)).isNull()
                assertThat(findViewById<View>(R.id.textB)).isNotNull()
            }
        }
    }
}

class ReplaceInCreateActivity : FragmentActivity(R.layout.activity_content) {
    private val parentFragment: ParentFragment
        get() = supportFragmentManager.findFragmentById(R.id.content) as ParentFragment
    val replaceInCreateFragment: ReplaceInCreateFragment
        get() = parentFragment.replaceInCreateFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // This issue only appears for child fragments
            // so add parent fragment that contains the ReplaceInCreateFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.content, ParentFragment())
                .setReorderingAllowed(true)
                .commit()
        }
    }

    class ParentFragment : StrictViewFragment(R.layout.simple_container) {
        lateinit var replaceInCreateFragment: ReplaceInCreateFragment

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (savedInstanceState == null) {
                replaceInCreateFragment = ReplaceInCreateFragment()
                childFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, replaceInCreateFragment)
                    .setReorderingAllowed(true)
                    .commit()
            }
        }
    }
}

class ReplaceInCreateFragment : StrictViewFragment(R.layout.fragment_a) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, StrictViewFragment(R.layout.fragment_b))
            .setReorderingAllowed(true)
            .commit()
    }
}
