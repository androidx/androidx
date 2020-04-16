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
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MenuVisibilityFragmentTest {

    @Test
    fun setMenuVisibility() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val fragment = MenuVisibilityFragment()

            assertWithMessage("Menu visibility should start out true")
                .that(fragment.isMenuVisible)
                .isTrue()

            withActivity {
                fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commitNow()
            }

            assertWithMessage("Menu visibility should be true")
                .that(fragment.isMenuVisible)
                .isTrue()

            withActivity {
                fm.beginTransaction()
                    .remove(fragment)
                    .commitNow()
            }

            assertWithMessage("Menu visibility should be false")
                .that(fragment.isMenuVisible)
                .isFalse()
        }
    }

    @Test
    fun setChildMenuVisibilityTrue() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val parentFragment = ParentMenuVisibilityFragment()
            val childFragment = parentFragment.childFragment

            withActivity {
                fm.beginTransaction()
                    .add(R.id.fragmentContainer, parentFragment)
                    .commitNow()
            }

            assertWithMessage("ChildFragment Menu Visibility should be true")
                .that(childFragment.isMenuVisible)
                .isTrue()

            withActivity {
                fm.beginTransaction()
                    .remove(parentFragment)
                    .commitNow()
            }

            assertWithMessage("ChildFragment Menu Visibility should be false")
                .that(childFragment.isMenuVisible)
                .isFalse()
        }
    }

    @Test
    fun setChildMenuVisibilityFalse() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val parentFragment = MenuVisibilityFragment()
            val childFragment = StrictFragment()

            childFragment.setMenuVisibility(false)

            withActivity {
                fm.beginTransaction()
                    .add(R.id.fragmentContainer, parentFragment)
                    .commitNow()

                parentFragment.childFragmentManager.beginTransaction()
                    .add(childFragment, "childFragment")
                    .commitNow()
            }

            assertWithMessage("ParentFragment Men Visibility should be true")
                .that(parentFragment.isMenuVisible)
                .isTrue()

            assertWithMessage("ChildFragment Menu Visibility should be false")
                .that(childFragment.isMenuVisible)
                .isFalse()

            withActivity {
                fm.beginTransaction()
                    .remove(parentFragment)
                    .commitNow()
            }

            assertWithMessage("ChildFragment Menu Visibility should be false")
                .that(childFragment.isMenuVisible)
                .isFalse()
        }
    }
}

class MenuVisibilityFragment : StrictFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMenuVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        setMenuVisibility(false)
    }
}

class ParentMenuVisibilityFragment : StrictFragment() {
    val childFragment = MenuVisibilityFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.beginTransaction()
            .add(childFragment, "childFragment")
            .commitNow()
    }
}
