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

package androidx.fragment.app

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentContainerInflatedFragmentTest {

    @Test
    fun testContentViewWithInflatedFragment() {
        // The StrictViewFragment runs the appropriate checks to make sure
        // we're moving through the states appropriately
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.inflated_fragment_container_view)
                supportFragmentManager
            }
            val fragment = fm.findFragmentByTag("fragment1")
            assertThat(fragment).isNotNull()
        }
    }

    @Test
    fun testContentViewWithInflatedFragmentWithClass() {
        // The StrictViewFragment runs the appropriate checks to make sure
        // we're moving through the states appropriately
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.inflated_fragment_container_view_with_class)
                supportFragmentManager
            }
            val fragment = fm.findFragmentByTag("fragment1")
            assertThat(fragment).isNotNull()
        }
    }

    @Test
    fun testGetInflatedFragmentInActivityOnCreate() {
        with(ActivityScenario.launch(ContainerViewActivity::class.java)) {
            val foundFragment = withActivity { foundFragment }

            assertThat(foundFragment).isTrue()
        }
    }

    @Test
    fun testContentViewWithNoID() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                try {
                    setContentView(R.layout.fragment_container_view_no_id)
                } catch (e: Exception) {
                    assertThat(e)
                        .hasMessageThat()
                        .contains(
                            "Error inflating class androidx.fragment.app.FragmentContainerView"
                        )
                }
            }
        }
    }

    @Test
    fun addInflatedFragmentToParentChildFragmentManager() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val parent = InflatedParentFragment()

            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, parent)
                    .commitNow()
            }

            val child = parent.childFragmentManager.findFragmentByTag("fragment1")

            assertThat(child).isNotNull()
        }
    }

    @Test
    fun addInflatedFragmentToGrandParentChildFragmentManager() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val grandParent = InflatedParentFragment()
            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, grandParent)
                    .commitNow()
            }

            val parent = StrictViewFragment(R.layout.fragment_container_view)

            withActivity {
                grandParent.childFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container_view, parent)
                    .commitNow()
            }

            val grandChild = grandParent.childFragmentManager.findFragmentByTag("fragment1")

            assertThat(grandChild).isNotNull()
        }
    }

    @Test
    fun addInflatedFragmentContainerWithClassToGrandParentChildFragmentManager() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val grandParent = InflatedParentFragmentContainerWithClass()
            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, grandParent)
                    .commitNow()
            }

            val parent = StrictViewFragment(R.layout.fragment_container_view)

            withActivity {
                grandParent.childFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container_view, parent)
                    .commitNow()
            }

            val grandChild = grandParent.childFragmentManager.findFragmentByTag("fragment1")

            assertThat(grandChild).isNotNull()
        }
    }

    @Test
    fun addInflatedAfterRestore() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val parent = InflatedParentFragment()

            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, parent, "parent")
                    .commitNow()
            }

            val childFragmentManager = parent.childFragmentManager
            val child = childFragmentManager.findFragmentByTag("fragment1")

            assertThat(childFragmentManager.fragments.count()).isEqualTo(1)

            recreate()

            val recreatedParent = withActivity {
                supportFragmentManager.findFragmentByTag("parent")!!
            }
            val recreatedChildFragmentManager = recreatedParent.childFragmentManager
            val recreatedChild = recreatedChildFragmentManager.findFragmentByTag("fragment1")

            assertThat(recreatedChildFragmentManager.fragments.count()).isEqualTo(1)
            assertThat(recreatedChild).isNotSameInstanceAs(child)
            assertThat(recreatedChild).isNotNull()
        }
    }

    @Test
    fun inflatedChildFragmentHasAttributesOnInflate() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val parent = InflatedParentFragment()

            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, parent)
                    .commitNow()
            }

            val childFragmentManager = parent.childFragmentManager
            val child = childFragmentManager.findFragmentByTag("fragment1") as InflatedFragment

            assertThat(child.name).isEqualTo(
                "androidx.fragment.app.InflatedFragment"
            )
        }
    }
}

class SimpleContainerActivity : FragmentActivity(R.layout.simple_container)

class ContainerViewActivity : FragmentActivity(R.layout.inflated_fragment_container_view) {
    var foundFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        foundFragment = (supportFragmentManager.findFragmentById(R.id
            .fragment_container_view)) != null
    }
}

class InflatedParentFragment : StrictViewFragment(R.layout.inflated_fragment_container_view)

class InflatedParentFragmentContainerWithClass : StrictViewFragment(R.layout
    .inflated_fragment_container_view)

class InflatedFragment() : StrictViewFragment() {
    var name: String? = null

    override fun onInflate(
        context: Context,
        attrs: AttributeSet,
        savedInstanceState: Bundle?
    ) {
        val a = context.obtainStyledAttributes(attrs,
            androidx.fragment.R.styleable.FragmentContainerView)
        name = a.getString(androidx.fragment.R.styleable.FragmentContainerView_android_name)
        a.recycle()
    }
}
