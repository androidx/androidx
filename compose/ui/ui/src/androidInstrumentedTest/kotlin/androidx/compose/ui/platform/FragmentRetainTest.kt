/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.runtime.LocalRetainScope
import androidx.compose.runtime.RetainScope
import androidx.compose.runtime.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.CountingRetainObject
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager.widget.ViewPager
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentRetainTest {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private lateinit var fragmentScenario: FragmentScenario<*>

    @After
    fun teardown() {
        if (::fragmentScenario.isInitialized) {
            fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
        }
        TestRetainFragment.viewCreationCounter = 0
    }

    private inline fun <reified F : Fragment> launchScenario(): FragmentScenario<F> =
        launchFragmentInContainer<F>().also { fragmentScenario = it }

    @Test
    fun retainScopedToFragment_activityRecreated() {
        val fragmentScenario = launchScenario<TestRetainFragment>()
        waitForIdleSync()
        val retained = fragmentScenario.withFragment { it.rootRetainedObjects }.single()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
        composeTestRule
            .onNode(hasTestTag(fragmentScenario.withFragment { it.toString() }))
            .assert(hasTextExactly("TestRetainFragment#${0},0"))

        fragmentScenario.recreate()
        assertNull(fragmentScenario.withFragment { it.rootRetainedObjects }.singleOrNull())
        retained.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
        composeTestRule
            .onNode(hasTestTag(fragmentScenario.withFragment { it.toString() }))
            .assert(hasTextExactly("TestRetainFragment#${1},0"))
    }

    @Test
    fun retainScopedToFragment_viewRecreated() {
        val fragmentScenario = launchScenario<TestRetainFragment>()
        val retained = fragmentScenario.withFragment { it.rootRetainedObjects }.single()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
        composeTestRule
            .onNode(hasTestTag(fragmentScenario.withFragment { it.toString() }))
            .assert(hasTextExactly("TestRetainFragment#${0},0"))

        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        waitForIdleSync()
        assertNull(fragmentScenario.withFragment { it.view }, "Fragment View should be destroyed")
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)
        assertSame(retained, fragmentScenario.withFragment { it.rootRetainedObjects }.single())
        retained.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
        composeTestRule
            .onNode(hasTestTag(fragmentScenario.withFragment { it.toString() }))
            .assert(hasTextExactly("TestRetainFragment#${1},0"))
    }

    @Test
    fun retainScopedToFragment_pager() {
        val fragmentScenario = launchScenario<PagerTestRetainFragment>()
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        fragmentScenario.onFragment { fragment ->
            fragment.children.forEachIndexed { index, child ->
                when (index) {
                    0,
                    1 -> {
                        assertNotNull(child.view, "Fragment $index should have a view")
                        assertFalse(
                            child.retainScope.isKeepingExitedValues,
                            "Fragment $index should not be keeping exited values",
                        )
                    }
                    else -> assertNull(child.view, "Fragment $index's view shouldn't exist")
                }
            }

            fragment.view!!.setCurrentItem(3, false)
        }

        waitForIdleSync()
        fragmentScenario.onFragment { fragment ->
            fragment.children.forEachIndexed { index, child ->
                when (index) {
                    0,
                    1 -> {
                        assertNull(child.view, "Fragment $index's view should be destroyed")
                        assertTrue(child.isDetached, "Fragment $index should be detached")
                        assertTrue(
                            child.retainScope.isKeepingExitedValues,
                            "Fragment $index should be keeping exited values",
                        )
                    }
                    2,
                    3,
                    4 -> {
                        assertNotNull(child.view, "Fragment $index should have a view")
                        assertFalse(
                            child.retainScope.isKeepingExitedValues,
                            "Fragment $index should not be keeping exited values",
                        )
                    }
                    else -> assertNull(child.view, "Fragment $index's view shouldn't exist")
                }
            }

            fragment.view!!.setCurrentItem(0, false)
        }

        waitForIdleSync()
        fragmentScenario.onFragment { fragment ->
            fragment.children.forEachIndexed { index, child ->
                when (index) {
                    0,
                    1 -> {
                        assertNotNull(child.view, "Fragment $index should have a view")
                        assertFalse(child.isDetached, "Fragment $index should not be detached")
                        assertFalse(
                            child.retainScope.isKeepingExitedValues,
                            "Fragment $index should not be keeping exited values",
                        )
                    }
                    2,
                    3,
                    4 -> {
                        assertNull(child.view, "Fragment $index's view should be destroyed")
                        assertTrue(child.isDetached, "Fragment $index should be detached")
                        assertTrue(
                            child.retainScope.isKeepingExitedValues,
                            "Fragment $index should be keeping exited values",
                        )
                    }
                    else -> assertNull(child.view, "Fragment $index's view shouldn't exist")
                }
            }

            fragment.view!!.currentItem = 0
        }
    }

    @Test
    fun retainScopedToFragment_fragmentDestroyed() {
        val fragmentScenario = launchScenario<TestRetainFragment>()
        val retained = fragmentScenario.withFragment { it.rootRetainedObjects }.single()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
        composeTestRule
            .onNode(hasTestTag(fragmentScenario.withFragment { it.toString() }))
            .assert(hasTextExactly("TestRetainFragment#${0},0"))
        fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <F : Fragment, R> FragmentScenario<F>.withFragment(action: (F) -> R): R {
        var result: R? = null
        onFragment { result = action(it) }
        return result as R
    }

    private fun waitForIdleSync() {
        Espresso.onIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}

class PagerTestRetainFragment : Fragment() {

    val children = List(12) { TestRetainFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        require(savedInstanceState == null) {
            "PagerTestRetainFragment does not support recreation."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return ViewPager(requireContext()).apply {
            id = View.generateViewId()
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            background = ColorDrawable(Color.WHITE)
            offscreenPageLimit = 1
            // Intentionally use the deprecated flavor. We want to test scenarios where only the
            // view is recreated, which isn't behavior that the replacement APIs provide.
            @Suppress("DEPRECATION")
            adapter =
                object : androidx.fragment.app.FragmentPagerAdapter(childFragmentManager) {
                    override fun getItem(index: Int): Fragment {
                        return children[index]
                    }

                    override fun getCount(): Int {
                        return children.size
                    }
                }
        }
    }

    override fun getView() = super.getView() as ViewPager?
}

class TestRetainFragment : Fragment() {

    var view: ComposeView? = null
        private set

    lateinit var retainScope: RetainScope
    val rootRetainedObjects = mutableListOf<CountingRetainObject>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Log.d("TestRetainFragment", "onCreateView()")
        return ComposeView(requireContext()).apply {
            view = this
            setContent {
                retainScope = LocalRetainScope.current
                retain { CountingRetainObject().also { rootRetainedObjects += it } }
                val viewInstanceId = viewCreationCounter++
                val viewRetainedInstanceId = retain { viewInstanceId }
                Text(
                    text = "TestRetainFragment#$viewInstanceId,$viewRetainedInstanceId",
                    modifier = Modifier.testTag("${this@TestRetainFragment}"),
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view = null
    }

    companion object {
        var viewCreationCounter = 0
    }
}
