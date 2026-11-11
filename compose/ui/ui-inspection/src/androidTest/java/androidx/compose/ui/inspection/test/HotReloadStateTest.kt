/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.inspection.test

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.R
import androidx.compose.ui.inspection.test.rules.JvmtiRule
import androidx.compose.ui.inspection.test.rules.createInspectorTester
import androidx.compose.ui.inspection.test.rules.sendCommand
import androidx.compose.ui.inspection.test.testdata.TestActivity
import androidx.compose.ui.inspection.test.util.GetComposablesCommand
import androidx.compose.ui.inspection.test.util.GetParametersByIdCommand
import androidx.compose.ui.inspection.test.util.flatten
import androidx.compose.ui.inspection.test.util.toMap
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

private const val PROVIDER_KEY = "HotReloadStateTest"

@LargeTest
class HotReloadStateTest {
    private val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private var inspectorTester: InspectorTester? = null
    private val debugInspectorInfoEnabled = isDebugInspectorInfoEnabled

    // An object remembered without rememberSaveable, which is recreated by a hot reload:
    private var remembered: Any? = null

    @Before
    fun before() {
        // Start with debug inspector info disabled, like an app before the inspector attaches:
        isDebugInspectorInfoEnabled = false
    }

    @After
    fun after() {
        inspectorTester?.dispose()
        isDebugInspectorInfoEnabled = debugInspectorInfoEnabled
    }

    @Test
    fun savedStatePreservedOnHotReload(): Unit = runBlocking {
        var navigateToDetails: (() -> Unit)? = null
        var currentScreen = ""

        rule.runOnUiThread {
            rule.activity.setContent {
                var screen by rememberSaveable { mutableStateOf("home") }
                navigateToDetails = { screen = "details" }
                currentScreen = screen
                Remembered()
                Screen(screen)
            }
        }
        rule.waitForIdle()
        assertThat(currentScreen).isEqualTo("home")

        // Navigate before the inspector connects (when the view has no inspection slot table yet):
        rule.runOnUiThread { navigateToDetails!!.invoke() }
        rule.waitForIdle()
        assertThat(currentScreen).isEqualTo("details")

        // Connecting the inspector adds a slot table and triggers hotReload():
        val tester = connectInspectorWithHotReload()

        // The rememberSaveable state must be preserved across the hot reload:
        assertThat(currentScreen).isEqualTo("details")

        val rootId = rule.runOnIdle {
            WindowInspector.getGlobalWindowViews().single().uniqueDrawingId
        }
        val response =
            tester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        val strings = response.stringsList.toMap()
        val screenNode =
            response.rootsList
                .flatMap { it.nodesList }
                .flatMap { it.flatten() }
                .single { strings[it.name] == "Screen" }
        val params =
            tester
                .sendCommand(
                    GetParametersByIdCommand(
                        rootId,
                        skipSystemComposables = false,
                        composableId = screenNode.id,
                    )
                )
                .getParametersResponse
        val paramStrings = params.stringsList.toMap()
        val screenParam =
            params.parameterGroup.parameterList.single { paramStrings[it.name] == "name" }
        assertThat(paramStrings[screenParam.int32Value]).isEqualTo("details")
    }

    @Test
    fun otherSavedStateProvidersNotTouchedOnHotReload(): Unit = runBlocking {
        var saveCount = 0
        rule.runOnUiThread {
            rule.activity.savedStateRegistry.registerSavedStateProvider(PROVIDER_KEY) {
                saveCount++
                Bundle()
            }
            rule.activity.setContent { Remembered() }
        }
        rule.waitForIdle()

        connectInspectorWithHotReload()

        rule.runOnUiThread {
            // The hot reload must not save other providers (e.g. FragmentManager would mark its
            // state as saved), and must not hand them restored state:
            assertThat(saveCount).isEqualTo(0)
            assertThat(rule.activity.savedStateRegistry.consumeRestoredStateForKey(PROVIDER_KEY))
                .isNull()
            rule.activity.savedStateRegistry.unregisterSavedStateProvider(PROVIDER_KEY)
        }
    }

    /** Connects the inspector, and checks that the composables were hot reloaded. */
    private suspend fun connectInspectorWithHotReload(): InspectorTester {
        val rememberedBefore = rule.runOnIdle { remembered }
        assertThat(rememberedBefore).isNotNull()
        // An inspector connected in an earlier test leaves a hook that adds a slot table to new
        // AndroidComposeViews. Remove the slot tables to make sure the inspector hot reloads:
        rule.runOnUiThread { removeSlotTables(rule.activity.window.decorView) }
        val tester = createInspectorTester().also { inspectorTester = it }
        rule.waitForIdle()
        assertThat(rule.runOnIdle { remembered }).isNotSameInstanceAs(rememberedBefore)
        return tester
    }

    private fun removeSlotTables(view: View) {
        if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") {
            view.setTag(R.id.inspection_slot_table_set, null)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                removeSlotTables(view.getChildAt(index))
            }
        }
    }

    @Composable
    private fun Remembered() {
        remembered = remember { Any() }
    }
}

@Composable
private fun Screen(name: String) {
    Text(name)
}
