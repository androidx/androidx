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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentRetainTest {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private lateinit var fragmentScenario: FragmentScenario<TestRetainFragment>

    @Before
    fun setup() {
        fragmentScenario = launchFragmentInContainer<TestRetainFragment>()
    }

    @After
    fun teardown() {
        fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
        TestRetainFragment.viewCreationCounter = 0
    }

    @Test
    fun retainScopedToFragment_activityRecreated() {
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
    fun retainScopedToFragment_fragmentDestroyed() {
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

    private fun waitForIdleSync() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

class TestRetainFragment : Fragment() {

    var view: ComposeView? = null
        private set

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
