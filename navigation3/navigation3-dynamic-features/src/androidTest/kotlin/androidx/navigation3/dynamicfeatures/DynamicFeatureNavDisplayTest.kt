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

package androidx.navigation3.dynamicfeatures

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DynamicFeatureNavDisplayTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun navDisplay_endToEndJourney_installsAndNavigatesToSubScreenAndBack() {
        val manager = TestSplitInstallManager()
        lateinit var backStack: MutableList<NavKey>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = backStack::removeLastOrNull,
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            entry(BaseHomeKey) {
                                Column {
                                    Text("Base Home")
                                    Button(onClick = { backStack.add(FeatureAInitialKey) }) {
                                        Text("Open Feature A")
                                    }
                                }
                            }
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                                loadingContent = { state ->
                                    val progress =
                                        if (state.totalBytesToDownload() > 0) {
                                            (state.bytesDownloaded() * 100) /
                                                state.totalBytesToDownload()
                                        } else {
                                            0L
                                        }
                                    Text("Loading featureA: $progress%")
                                },
                            )
                        },
                )
            }
        }

        // Verifying initial base screen
        composeTestRule.onNodeWithText("Base Home").assertIsDisplayed()
        assertThat(manager.requestedModules).isEmpty()

        // Navigating to uninstalled dynamic module initial destination
        composeTestRule.onNodeWithText("Open Feature A").performClick()
        composeTestRule.waitForIdle()

        // Verifying dynamic module is being installed
        assertThat(manager.requestedModules).containsExactly("featureA")
        composeTestRule.onNodeWithText("Loading featureA: 0%").assertIsDisplayed()

        // Emitting downloading progress and verifying loading UI updates
        val downloadingState =
            SplitInstallSessionState.create(
                /* sessionId = */ 1,
                /* status = */ SplitInstallSessionStatus.DOWNLOADING,
                /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                /* bytesDownloaded = */ 50L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("featureA"),
                /* languages = */ emptyList(),
            )
        composeTestRule.runOnIdle { manager.emitState(downloadingState) }
        composeTestRule.onNodeWithText("Loading featureA: 50%").assertIsDisplayed()

        // Emitting installed state and verifying in-place transition to Feature A initial screen
        val installedState =
            SplitInstallSessionState.create(
                /* sessionId = */ 1,
                /* status = */ SplitInstallSessionStatus.INSTALLED,
                /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                /* bytesDownloaded = */ 100L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("featureA"),
                /* languages = */ emptyList(),
            )
        composeTestRule.runOnIdle {
            manager.installedModules.add("featureA")
            manager.emitState(installedState)
        }
        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading featureA: 50%").assertDoesNotExist()

        // Navigating to internal sub-screen defined only in FeatureAEntryProvider
        composeTestRule.onNodeWithText("Go to SubScreen").performClick()
        composeTestRule.onNodeWithText("Feature A SubScreen").assertIsDisplayed()

        // Popping back to Feature A initial screen; verifying no re-install or loading UI
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()
        assertThat(manager.requestedModules).hasSize(1)

        // Popping back to Base Home
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Base Home").assertIsDisplayed()
    }

    @Test
    fun navDisplay_whenModuleAlreadyInstalled_rendersImmediatelyWithoutLoading() {
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("featureA")
            }
        var loadingComposed = false

        composeTestRule.setContent {
            val backStack = remember { mutableStateListOf<NavKey>(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            entry(BaseHomeKey) {
                                Button(onClick = { backStack.add(FeatureAInitialKey) }) {
                                    Text("Open Feature A")
                                }
                            }
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                                loadingContent = {
                                    loadingComposed = true
                                    Text("Loading featureA")
                                },
                            )
                        },
                )
            }
        }

        composeTestRule.onNodeWithText("Open Feature A").performClick()
        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()
        assertThat(loadingComposed).isFalse()
        assertThat(manager.requestedModules).isEmpty()
    }

    @Test
    fun navDisplay_popBackWhileDownloading_cancelsInstallSession() {
        val manager =
            TestSplitInstallManager().apply {
                nextSessionId = 42
            }
        lateinit var backStack: MutableList<NavKey>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf<NavKey>(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            entry(BaseHomeKey) {
                                Column {
                                    Text("Base Home")
                                    Button(onClick = { backStack.add(FeatureAInitialKey) }) {
                                        Text("Open Feature A")
                                    }
                                }
                            }
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                                loadingContent = { Text("Downloading featureA...") },
                            )
                        },
                )
            }
        }

        composeTestRule.onNodeWithText("Open Feature A").performClick()
        composeTestRule.onNodeWithText("Downloading featureA...").assertIsDisplayed()
        assertThat(manager.requestedModules).containsExactly("featureA")
        assertThat(manager.cancelledSessionIds).isEmpty()

        // Popping the dynamic destination while still downloading
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Base Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloading featureA...").assertDoesNotExist()

        assertThat(manager.cancelledSessionIds).containsExactly(42)
    }

    @Test
    fun navDisplay_whenModuleLoadingContentIsNull_rendersDefaultLoadingContent() {
        val manager = TestSplitInstallManager()

        composeTestRule.setContent {
            val backStack = remember { mutableStateListOf<NavKey>(FeatureAInitialKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = { state ->
                        Text("Default Loading: ${state.status()}")
                    },
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                                loadingContent = null,
                            )
                        },
                )
            }
        }

        composeTestRule
            .onNodeWithText("Default Loading: ${SplitInstallSessionStatus.PENDING}")
            .assertIsDisplayed()

        val installedState =
            SplitInstallSessionState.create(
                /* sessionId = */ 1,
                /* status = */ SplitInstallSessionStatus.INSTALLED,
                /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                /* bytesDownloaded = */ 100L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("featureA"),
                /* languages = */ emptyList(),
            )
        composeTestRule.runOnIdle {
            manager.installedModules.add("featureA")
            manager.emitState(installedState)
        }

        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Default Loading: ${SplitInstallSessionStatus.PENDING}")
            .assertDoesNotExist()
    }

    @Test
    fun navDisplay_preservesSaveableStateAcrossDynamicSubScreenNavigation() {
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("featureA")
            }
        lateinit var backStack: MutableList<NavKey>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(FeatureAInitialKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                            )
                        },
                )
            }
        }

        composeTestRule.onNodeWithText("Count: 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Increment").performClick()
        composeTestRule.onNodeWithText("Increment").performClick()
        composeTestRule.onNodeWithText("Count: 2").assertIsDisplayed()

        // Navigating to sub-screen
        composeTestRule.onNodeWithText("Go to SubScreen").performClick()
        composeTestRule.onNodeWithText("Feature A SubScreen").assertIsDisplayed()

        // Popping back to initial dynamic screen and verifying state was saved & restored
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Count: 2").assertIsDisplayed()
    }

    @Test
    fun navDisplay_multipleDynamicModules_uninstalledModuleDoesNotBlockInstalledModule() {
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("featureB")
            }
        lateinit var backStack: MutableList<NavKey>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            entry(BaseHomeKey) {
                                Button(onClick = { backStack.add(FeatureBInitialKey) }) {
                                    Text("Open Feature B")
                                }
                            }
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName =
                                    "androidx.navigation3.dynamicfeatures.NonExistentProviderClass",
                            )
                            dynamicFeatureModule(
                                initialKey = FeatureBInitialKey,
                                moduleName = "featureB",
                                entryProviderClassName = FeatureBEntryProvider::class.java.name,
                            )
                        },
                )
            }
        }

        composeTestRule.onNodeWithText("Open Feature B").performClick()
        composeTestRule.onNodeWithText("Feature B Initial Screen").assertIsDisplayed()

        composeTestRule.onNodeWithText("Go to Feature B SubScreen").performClick()
        composeTestRule.onNodeWithText("Feature B SubScreen").assertIsDisplayed()
    }

    @Test
    fun navDisplay_dynamicSubDestinationWithDialogSceneStrategy_rendersAndDismissesDialog() {
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("featureA")
            }
        lateinit var backStack: MutableList<NavKey>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(FeatureAInitialKey) }
            val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    sceneStrategies = listOf(dialogStrategy),
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                            )
                        },
                )
            }
        }

        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()

        composeTestRule.onNodeWithText("Open Dynamic Dialog").performClick()
        composeTestRule.waitForIdle()

        // Both the background screen and the dialog content must be visible simultaneously
        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Feature A Dialog Content").assertIsDisplayed()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Feature A Dialog Content").assertDoesNotExist()
        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()
    }

    @Test
    fun navDisplay_whenModuleAlreadyInstalled_preservesRealDynamicEntryMetadataAndContentKey() {
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("featureCustomMeta")
            }
        lateinit var backStack: MutableList<NavKey>
        var capturedEntry: NavEntry<NavKey>? = null

        val inspectingDecorator =
            NavEntryDecorator<NavKey> { entry ->
                if (entry.contentKey != BaseHomeKey.toString()) {
                    capturedEntry = entry
                }
                entry.Content()
            }

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            NavDisplay(
                backStack = backStack,
                entryDecorators =
                    listOf(
                        inspectingDecorator,
                        dynamicFeatureDecorator,
                    ),
                entryProvider =
                    dynamicFeatureDecorator.entryProvider {
                        entry(BaseHomeKey) { Text("Base Home") }
                        dynamicFeatureModule(
                            initialKey = FeatureCustomMetadataInitialKey,
                            moduleName = "featureCustomMeta",
                            entryProviderClassName =
                                FeatureCustomMetadataEntryProvider::class.java.name,
                            loadingContent = { Text("Loading featureCustomMeta") },
                        )
                    },
            )
        }

        composeTestRule.runOnIdle { backStack.add(FeatureCustomMetadataInitialKey) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Feature With Metadata Initial Screen").assertIsDisplayed()

        // Verify the entry decorated by NavDisplay has the metadata and contentKey from the
        // real entry defined in FeatureCustomMetadataEntryProvider, not the dummy initial entry.
        assertThat(capturedEntry).isNotNull()
        assertThat(capturedEntry?.contentKey).isEqualTo("custom_initial_content_key")
        assertThat(capturedEntry?.metadata?.get("custom_meta_key")).isEqualTo("custom_meta_value")
    }

    @Test
    fun navDisplay_whenModuleInstallsInFlight_switchesToRealEntryWithMetadataAndContentKey() {
        val manager = TestSplitInstallManager()
        lateinit var backStack: MutableList<NavKey>
        var capturedEntry: NavEntry<NavKey>? = null

        val inspectingDecorator = NavEntryDecorator { entry ->
            if (entry.contentKey != BaseHomeKey.toString()) {
                capturedEntry = entry
            }
            entry.Content()
        }

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            NavDisplay(
                backStack = backStack,
                entryDecorators =
                    listOf(
                        inspectingDecorator,
                        dynamicFeatureDecorator,
                    ),
                entryProvider =
                    dynamicFeatureDecorator.entryProvider {
                        entry(BaseHomeKey) { Text("Base Home") }
                        dynamicFeatureModule(
                            initialKey = FeatureCustomMetadataInitialKey,
                            moduleName = "featureCustomMeta",
                            entryProviderClassName =
                                FeatureCustomMetadataEntryProvider::class.java.name,
                            loadingContent = { Text("Loading featureCustomMeta") },
                        )
                    },
            )
        }

        composeTestRule.runOnIdle { backStack.add(FeatureCustomMetadataInitialKey) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Loading featureCustomMeta").assertIsDisplayed()

        // Complete installation
        val installedState =
            SplitInstallSessionState.create(
                /* sessionId = */ 1,
                /* status = */ SplitInstallSessionStatus.INSTALLED,
                /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                /* bytesDownloaded = */ 100L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("featureCustomMeta"),
                /* languages = */ emptyList(),
            )
        composeTestRule.runOnIdle {
            manager.installedModules.add("featureCustomMeta")
            manager.emitState(installedState)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Feature With Metadata Initial Screen").assertIsDisplayed()

        // After installation completes, NavDisplay / decoratedNavEntries should switch
        // to the real entry and reflect its contentKey and metadata
        assertThat(capturedEntry).isNotNull()
        assertThat(capturedEntry?.contentKey).isEqualTo("custom_initial_content_key")
        assertThat(capturedEntry?.metadata?.get("custom_meta_key")).isEqualTo("custom_meta_value")
    }

    @Test
    fun navDisplay_whenOnDynamicSubScreen_restoresAfterProcessDeath() {
        val restorationTester = StateRestorationTester(composeTestRule)
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("featureA")
            }
        lateinit var backStack: MutableList<NavKey>

        restorationTester.setContent {
            backStack = rememberNavBackStack(BaseHomeKey)
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = backStack::removeLastOrNull,
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            entry(BaseHomeKey) {
                                Column {
                                    Text("Base Home")
                                    Button(onClick = { backStack.add(FeatureAInitialKey) }) {
                                        Text("Open Feature A")
                                    }
                                }
                            }
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                            )
                        },
                )
            }
        }

        // Open dynamic module and navigate to its sub-screen
        composeTestRule.onNodeWithText("Open Feature A").performClick()
        composeTestRule.onNodeWithText("Feature A Initial Screen").assertIsDisplayed()

        composeTestRule.onNodeWithText("Go to SubScreen").performClick()
        composeTestRule.onNodeWithText("Feature A SubScreen").assertIsDisplayed()

        // Remove the initial key so the saved back stack only contains [BaseHomeKey,
        // FeatureASubScreenKey]
        composeTestRule.runOnIdle { backStack.remove(FeatureAInitialKey) }

        // Emulate process death and state restoration
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Feature A SubScreen").assertIsDisplayed()

        // Popping back should return directly to Base Home
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Base Home").assertIsDisplayed()
    }

    @Test
    fun navDisplay_whenInstallFails_rendersErrorStateAndPopBackRecovers() {
        val manager = TestSplitInstallManager()
        lateinit var backStack: MutableList<NavKey>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(BaseHomeKey) }
            val dynamicFeatureDecorator =
                rememberDynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            CompositionLocalProvider(LocalTestBackStack provides backStack) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            dynamicFeatureDecorator,
                        ),
                    onBack = backStack::removeLastOrNull,
                    entryProvider =
                        dynamicFeatureDecorator.entryProvider {
                            entry(BaseHomeKey) {
                                Column {
                                    Text("Base Home")
                                    Button(onClick = { backStack.add(FeatureAInitialKey) }) {
                                        Text("Open Feature A")
                                    }
                                }
                            }
                            dynamicFeatureModule(
                                initialKey = FeatureAInitialKey,
                                moduleName = "featureA",
                                entryProviderClassName = FeatureAEntryProvider::class.java.name,
                                loadingContent = { state ->
                                    if (state.status() == SplitInstallSessionStatus.FAILED) {
                                        Text("Failed: ${state.errorCode()}")
                                    } else {
                                        Text("Loading featureA")
                                    }
                                },
                            )
                        },
                )
            }
        }

        composeTestRule.onNodeWithText("Open Feature A").performClick()
        composeTestRule.onNodeWithText("Loading featureA").assertIsDisplayed()

        val failedState =
            SplitInstallSessionState.create(
                /* sessionId = */ 1,
                /* status = */ SplitInstallSessionStatus.FAILED,
                /* errorCode = */ SplitInstallErrorCode.NETWORK_ERROR,
                /* bytesDownloaded = */ 20L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("featureA"),
                /* languages = */ emptyList(),
            )
        composeTestRule.runOnIdle { manager.emitState(failedState) }

        composeTestRule
            .onNodeWithText("Failed: ${SplitInstallErrorCode.NETWORK_ERROR}")
            .assertIsDisplayed()

        // Popping back from failed loading screen returns to Base Home
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.onNodeWithText("Base Home").assertIsDisplayed()
    }
}

// A composition local of back stack used in tests to simplify backStack passing.
private val LocalTestBackStack = compositionLocalOf<MutableList<NavKey>?> { null }

@Serializable private data object BaseHomeKey : NavKey

@Serializable private data object FeatureAInitialKey : NavKey

@Serializable private data object FeatureASubScreenKey : NavKey

private data object FeatureADialogKey : NavKey

private data object FeatureBInitialKey : NavKey

private data object FeatureBSubScreenKey : NavKey

private data object FeatureCustomMetadataInitialKey : NavKey

private class FeatureCustomMetadataEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(
            key = FeatureCustomMetadataInitialKey,
            contentKey = "custom_initial_content_key",
            metadata = mapOf("custom_meta_key" to "custom_meta_value"),
        ) {
            Text("Feature With Metadata Initial Screen")
        }
    }
}

private class FeatureAEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(FeatureAInitialKey) {
            val backStack = LocalTestBackStack.current
            var count by rememberSaveable { mutableIntStateOf(0) }
            Column {
                Text("Feature A Initial Screen")
                Text("Count: $count")
                Button(onClick = { count++ }) { Text("Increment") }
                Button(onClick = { backStack?.add(FeatureASubScreenKey) }) {
                    Text("Go to SubScreen")
                }
                Button(onClick = { backStack?.add(FeatureADialogKey) }) {
                    Text("Open Dynamic Dialog")
                }
            }
        }
        entry(FeatureASubScreenKey) {
            Text("Feature A SubScreen")
        }
        entry(FeatureADialogKey, metadata = DialogSceneStrategy.dialog()) {
            Text("Feature A Dialog Content")
        }
    }
}

private class FeatureBEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(FeatureBInitialKey) {
            val backStack = LocalTestBackStack.current
            Column {
                Text("Feature B Initial Screen")
                Button(onClick = { backStack?.add(FeatureBSubScreenKey) }) {
                    Text("Go to Feature B SubScreen")
                }
            }
        }
        entry(FeatureBSubScreenKey) {
            Text("Feature B SubScreen")
        }
    }
}

private class TestSplitInstallManager {
    val installedModules = mutableSetOf<String>()
    val requestedModules = mutableListOf<String>()
    val cancelledSessionIds = mutableListOf<Int>()
    var nextSessionId: Int = 1

    private val listeners = mutableSetOf<SplitInstallStateUpdatedListener>()

    fun emitState(state: SplitInstallSessionState) {
        listeners.toList().forEach { it.onStateUpdate(state) }
    }

    val instance: SplitInstallManager =
        Proxy.newProxyInstance(
            SplitInstallManager::class.java.classLoader,
            arrayOf(SplitInstallManager::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "getInstalledModules" -> installedModules
                "getSessionStates" -> Tasks.forResult(emptyList<SplitInstallSessionState>())
                "startInstall" -> {
                    val request = args[0] as SplitInstallRequest
                    requestedModules.addAll(request.moduleNames)
                    Tasks.forResult(nextSessionId++)
                }
                "cancelInstall" -> {
                    val sessionId = args[0] as Int
                    cancelledSessionIds.add(sessionId)
                    Tasks.forResult<Void?>(null)
                }
                "registerListener" -> {
                    listeners.add(args[0] as SplitInstallStateUpdatedListener)
                    Unit
                }
                "unregisterListener" -> {
                    listeners.remove(args[0] as SplitInstallStateUpdatedListener)
                    Unit
                }
                "equals" -> proxy === args?.get(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "TestSplitInstallManager"
                else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as SplitInstallManager
}
