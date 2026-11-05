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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.kruth.assertThat
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class DynamicFeatureNavEntryDecoratorTest {

    @Test
    fun decorator_nonDynamicEntry_rendersContentDirectly() = compositionTest {
        val manager = TestSplitInstallManager()
        var renderedScreen: String? = null

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = {},
            )
        val provider = decorator.entryProvider {
            entry(StaticKey) {
                renderedScreen = "static_screen"
            }
        }

        compose {
            val entries =
                rememberDecoratedNavEntries(
                    backStack = listOf(StaticKey),
                    entryDecorators = listOf(decorator),
                    entryProvider = provider,
                )
            entries.last().Content()
        }

        advanceEffectsAndRecompose()

        assertThat(renderedScreen).isEqualTo("static_screen")
        assertThat(manager.requestedModules).isEmpty()
    }

    @Test
    fun decorator_whenModuleAlreadyInstalled_rendersDynamicScreenImmediately() = compositionTest {
        val manager =
            TestSplitInstallManager().apply {
                installedModules.add("feature_installed")
            }
        DecoratorTestEntryProvider.renderedScreen = null
        var loadingRendered = false

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = {},
            )
        val provider = decorator.entryProvider {
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_installed",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
                loadingContent = { loadingRendered = true },
            )
        }

        compose {
            val entries =
                rememberDecoratedNavEntries(
                    backStack = listOf(DecoratorDynamicKey),
                    entryDecorators = listOf(decorator),
                    entryProvider = provider,
                )
            entries.last().Content()
        }

        advanceEffectsAndRecompose()

        assertThat(DecoratorTestEntryProvider.renderedScreen).isEqualTo("dynamic_real_screen")
        assertThat(loadingRendered).isFalse()
        assertThat(manager.requestedModules).isEmpty()
    }

    @Test
    fun decorator_whenModuleNotInstalled_startsInstallAndRendersModuleLoadingContent() =
        compositionTest {
            val manager = TestSplitInstallManager()
            DecoratorTestEntryProvider.renderedScreen = null
            var moduleLoadingRendered = false
            var ambientSessionState: SplitInstallSessionState? = null

            val decorator =
                DynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            val provider = decorator.entryProvider {
                dynamicFeatureModule(
                    initialKey = DecoratorDynamicKey,
                    moduleName = "feature_downloading",
                    entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
                    loadingContent = { sessionState ->
                        moduleLoadingRendered = true
                        ambientSessionState = sessionState
                    },
                )
            }

            compose {
                val entries =
                    rememberDecoratedNavEntries(
                        backStack = listOf(DecoratorDynamicKey),
                        entryDecorators = listOf(decorator),
                        entryProvider = provider,
                    )
                entries.last().Content()
            }

            advanceEffectsAndRecompose()

            assertThat(manager.requestedModules).containsExactly("feature_downloading")
            assertThat(moduleLoadingRendered).isTrue()
            assertThat(DecoratorTestEntryProvider.renderedScreen).isNull()

            val downloadingState =
                SplitInstallSessionState.create(
                    /* sessionId = */ 7,
                    /* status = */ SplitInstallSessionStatus.DOWNLOADING,
                    /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                    /* bytesDownloaded = */ 50L,
                    /* totalBytesToDownload = */ 100L,
                    /* moduleNames = */ listOf("feature_downloading"),
                    /* languages = */ emptyList(),
                )
            manager.emitState(downloadingState)
            advanceEffectsAndRecompose()

            val finalAmbientSessionState = checkNotNull(ambientSessionState)
            assertThat(finalAmbientSessionState.sessionId()).isEqualTo(7)
            assertThat(finalAmbientSessionState.status())
                .isEqualTo(SplitInstallSessionStatus.DOWNLOADING)
        }

    @Test
    fun decorator_whenModuleLoadingContentIsNull_rendersDefaultLoadingContent() = compositionTest {
        val manager = TestSplitInstallManager()
        var defaultLoadingRendered = false

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = { defaultLoadingRendered = true },
            )
        val provider = decorator.entryProvider {
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_default_loading",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
                loadingContent = null,
            )
        }

        compose {
            val entries =
                rememberDecoratedNavEntries(
                    backStack = listOf(DecoratorDynamicKey),
                    entryDecorators = listOf(decorator),
                    entryProvider = provider,
                )
            entries.last().Content()
        }

        advanceEffectsAndRecompose()

        assertThat(defaultLoadingRendered).isTrue()
        assertThat(manager.requestedModules).containsExactly("feature_default_loading")
    }

    @Test
    fun decorator_whenSessionStateTransitionsToInstalled_switchesFromLoadingToRealScreen() =
        compositionTest {
            val manager = TestSplitInstallManager()
            DecoratorTestEntryProvider.renderedScreen = null
            var activeView = "none"

            val decorator =
                DynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            val provider = decorator.entryProvider {
                dynamicFeatureModule(
                    initialKey = DecoratorDynamicKey,
                    moduleName = "feature_transition",
                    entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
                    loadingContent = { activeView = "loading" },
                )
            }

            compose {
                CompositionLocalProvider(
                    LocalContext provides RuntimeEnvironment.getApplication()
                ) {
                    val entries =
                        rememberDecoratedNavEntries(
                            backStack = listOf(DecoratorDynamicKey),
                            entryDecorators = listOf(decorator),
                            entryProvider = provider,
                        )
                    entries.last().Content()
                }
            }

            advanceEffectsAndRecompose()
            assertThat(activeView).isEqualTo("loading")
            assertThat(DecoratorTestEntryProvider.renderedScreen).isNull()

            val installedState =
                SplitInstallSessionState.create(
                    /* sessionId = */ 1,
                    /* status = */ SplitInstallSessionStatus.INSTALLED,
                    /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                    /* bytesDownloaded = */ 100L,
                    /* totalBytesToDownload = */ 100L,
                    /* moduleNames = */ listOf("feature_transition"),
                    /* languages = */ emptyList(),
                )
            manager.emitState(installedState)
            advanceEffectsAndRecompose()

            assertThat(DecoratorTestEntryProvider.renderedScreen).isEqualTo("dynamic_real_screen")
        }

    @Test
    fun decorator_onDisposeWhileDownloading_cancelsInstallSession() = compositionTest {
        val manager =
            TestSplitInstallManager().apply {
                nextSessionId = 42
            }
        var showDynamicScreen by mutableStateOf(true)

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = {},
            )
        val provider = decorator.entryProvider {
            entry(StaticKey) {}
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_cancel_on_dispose",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
                loadingContent = {},
            )
        }

        compose {
            val backStack =
                if (showDynamicScreen) {
                    listOf(StaticKey, DecoratorDynamicKey)
                } else {
                    listOf(StaticKey)
                }
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = listOf(decorator),
                    entryProvider = provider,
                )
            entries.last().Content()
        }

        advanceEffectsAndRecompose()
        assertThat(manager.requestedModules).containsExactly("feature_cancel_on_dispose")
        assertThat(manager.cancelledSessionIds).isEmpty()

        showDynamicScreen = false
        advanceEffectsAndRecompose()

        assertThat(manager.cancelledSessionIds).containsExactly(42)
    }

    @Test
    fun decorator_onDisposeAfterInstalled_doesNotCancelSession() = compositionTest {
        val manager =
            TestSplitInstallManager().apply {
                nextSessionId = 99
            }
        var showDynamicScreen by mutableStateOf(true)

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = {},
            )
        val provider = decorator.entryProvider {
            entry(StaticKey) {}
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_installed_before_dispose",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
                loadingContent = {},
            )
        }

        compose {
            CompositionLocalProvider(LocalContext provides RuntimeEnvironment.getApplication()) {
                val backStack =
                    if (showDynamicScreen) {
                        listOf(StaticKey, DecoratorDynamicKey)
                    } else {
                        listOf(StaticKey)
                    }
                val entries =
                    rememberDecoratedNavEntries(
                        backStack = backStack,
                        entryDecorators = listOf(decorator),
                        entryProvider = provider,
                    )
                entries.last().Content()
            }
        }

        advanceEffectsAndRecompose()

        val installedState =
            SplitInstallSessionState.create(
                /* sessionId = */ 99,
                /* status = */ SplitInstallSessionStatus.INSTALLED,
                /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                /* bytesDownloaded = */ 100L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("feature_installed_before_dispose"),
                /* languages = */ emptyList(),
            )
        manager.emitState(installedState)
        advanceEffectsAndRecompose()

        showDynamicScreen = false
        advanceEffectsAndRecompose()

        assertThat(manager.cancelledSessionIds).isEmpty()
    }

    @Test
    fun decorator_withCustomKeyType_interceptsAndRenders() = compositionTest {
        val manager = TestSplitInstallManager()
        CustomKeyDecoratorEntryProvider.renderedScreen = null
        var loadingRendered = false

        val decorator =
            DynamicFeatureNavEntryDecorator<CustomTestKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = { loadingRendered = true },
            )
        val provider = decorator.entryProvider {
            dynamicFeatureModule(
                initialKey = CustomTestKey("custom_initial"),
                moduleName = "custom_feature",
                entryProviderClassName = CustomKeyDecoratorEntryProvider::class.java.name,
            )
        }

        compose {
            CompositionLocalProvider(LocalContext provides RuntimeEnvironment.getApplication()) {
                val entries =
                    rememberDecoratedNavEntries(
                        backStack = listOf(CustomTestKey("custom_initial")),
                        entryDecorators = listOf(decorator),
                        entryProvider = provider,
                    )
                entries.last().Content()
            }
        }

        advanceEffectsAndRecompose()
        assertThat(loadingRendered).isTrue()
        assertThat(CustomKeyDecoratorEntryProvider.renderedScreen).isNull()

        val installedState =
            SplitInstallSessionState.create(
                /* sessionId = */ 1,
                /* status = */ SplitInstallSessionStatus.INSTALLED,
                /* errorCode = */ SplitInstallErrorCode.NO_ERROR,
                /* bytesDownloaded = */ 100L,
                /* totalBytesToDownload = */ 100L,
                /* moduleNames = */ listOf("custom_feature"),
                /* languages = */ emptyList(),
            )
        manager.emitState(installedState)
        advanceEffectsAndRecompose()

        assertThat(CustomKeyDecoratorEntryProvider.renderedScreen).isEqualTo("custom_real_screen")
    }

    @Test
    fun decorator_whenModuleInstalledWithMissingProviderClass_throwsIllegalStateException() =
        compositionTest {
            val manager =
                TestSplitInstallManager().apply {
                    installedModules.add("feature_missing_class")
                }

            val decorator =
                DynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            val provider = decorator.entryProvider {
                dynamicFeatureModule(
                    initialKey = DecoratorDynamicKey,
                    moduleName = "feature_missing_class",
                    entryProviderClassName =
                        "androidx.navigation3.dynamicfeatures.NonExistentEntryBuilder",
                )
            }

            val exception =
                assertFailsWith<IllegalStateException> {
                    compose {
                        val entries =
                            rememberDecoratedNavEntries(
                                backStack = listOf(DecoratorDynamicKey),
                                entryDecorators = listOf(decorator),
                                entryProvider = provider,
                            )
                        entries.last().Content()
                    }
                }
            assertThat(exception.message)
                .isEqualTo(
                    "Cannot instantiate the DynamicFeatureModuleEntryProvider with class name " +
                        "androidx.navigation3.dynamicfeatures.NonExistentEntryBuilder"
                )
        }

    @Test
    fun decorator_whenStartInstallFails_emitsFailedSessionState() = compositionTest {
        val manager =
            TestSplitInstallManager().apply {
                startInstallException = SplitInstallException(SplitInstallErrorCode.NETWORK_ERROR)
            }
        var observedState: SplitInstallSessionState? = null

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = { state -> observedState = state },
            )
        val provider = decorator.entryProvider {
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_start_install_failed",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
            )
        }

        compose {
            val entries =
                rememberDecoratedNavEntries(
                    backStack = listOf(DecoratorDynamicKey),
                    entryDecorators = listOf(decorator),
                    entryProvider = provider,
                )
            entries.last().Content()
        }

        advanceEffectsAndRecompose()

        val finalState = checkNotNull(observedState)
        assertThat(finalState.status()).isEqualTo(SplitInstallSessionStatus.FAILED)
        assertThat(finalState.errorCode()).isEqualTo(SplitInstallErrorCode.NETWORK_ERROR)
    }

    @Test
    fun decorator_whenGetSessionStatesFails_emitsFailedSessionState() = compositionTest {
        val manager =
            TestSplitInstallManager().apply {
                getSessionStatesException =
                    SplitInstallException(SplitInstallErrorCode.PLAY_STORE_NOT_FOUND)
            }
        var observedState: SplitInstallSessionState? = null

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = { state -> observedState = state },
            )
        val provider = decorator.entryProvider {
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_session_states_failed",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
            )
        }

        compose {
            val entries =
                rememberDecoratedNavEntries(
                    backStack = listOf(DecoratorDynamicKey),
                    entryDecorators = listOf(decorator),
                    entryProvider = provider,
                )
            entries.last().Content()
        }

        advanceEffectsAndRecompose()

        val finalState = checkNotNull(observedState)
        assertThat(finalState.status()).isEqualTo(SplitInstallSessionStatus.FAILED)
        assertThat(finalState.errorCode()).isEqualTo(SplitInstallErrorCode.PLAY_STORE_NOT_FOUND)
    }

    @Test
    fun decorator_whenStartInstallReturnsZeroSessionId_rendersDynamicScreen() = compositionTest {
        val manager =
            TestSplitInstallManager().apply {
                nextSessionId = 0
            }
        DecoratorTestEntryProvider.renderedScreen = null

        val decorator =
            DynamicFeatureNavEntryDecorator<NavKey>(
                splitInstallManager = manager.instance,
                defaultLoadingContent = {},
            )
        val provider = decorator.entryProvider {
            dynamicFeatureModule(
                initialKey = DecoratorDynamicKey,
                moduleName = "feature_already_installed_at_start_install",
                entryProviderClassName = DecoratorTestEntryProvider::class.java.name,
            )
        }

        compose {
            CompositionLocalProvider(LocalContext provides RuntimeEnvironment.getApplication()) {
                val entries =
                    rememberDecoratedNavEntries(
                        backStack = listOf(DecoratorDynamicKey),
                        entryDecorators = listOf(decorator),
                        entryProvider = provider,
                    )
                entries.last().Content()
            }
        }

        advanceEffectsAndRecompose()

        assertThat(DecoratorTestEntryProvider.renderedScreen).isEqualTo("dynamic_real_screen")
    }

    @Test
    fun decorator_entryProviderWithCustomFallback_delegatesUnknownKeysToFallback() =
        compositionTest {
            val manager = TestSplitInstallManager()
            var renderedScreen: String? = null

            val decorator =
                DynamicFeatureNavEntryDecorator<NavKey>(
                    splitInstallManager = manager.instance,
                    defaultLoadingContent = {},
                )
            val provider =
                decorator.entryProvider(
                    fallback = { key ->
                        NavEntry(key) {
                            renderedScreen = "fallback_$key"
                        }
                    }
                ) {
                    entry(StaticKey) {
                        renderedScreen = "static_screen"
                    }
                }

            compose {
                val entries =
                    rememberDecoratedNavEntries(
                        backStack = listOf(DecoratorDynamicKey),
                        entryDecorators = listOf(decorator),
                        entryProvider = provider,
                    )
                entries.last().Content()
            }

            advanceEffectsAndRecompose()

            assertThat(renderedScreen).isEqualTo("fallback_$DecoratorDynamicKey")
        }
}

private data object StaticKey : NavKey

private data object DecoratorDynamicKey : NavKey

public data class CustomTestKey(val name: String)

public class DecoratorTestEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
    override fun EntryProviderScope<NavKey>.buildEntries() {
        entry(DecoratorDynamicKey) {
            renderedScreen = "dynamic_real_screen"
        }
    }

    companion object {
        var renderedScreen: String? = null
    }
}

public class CustomKeyDecoratorEntryProvider : DynamicFeatureModuleEntryProvider<CustomTestKey> {
    override fun EntryProviderScope<CustomTestKey>.buildEntries() {
        entry(CustomTestKey("custom_initial")) {
            renderedScreen = "custom_real_screen"
        }
    }

    companion object {
        var renderedScreen: String? = null
    }
}

// Existing FakeSplitInstallManager api is not as flexible as we'd want for testing the decorator.
internal class TestSplitInstallManager {
    val installedModules = mutableSetOf<String>()
    val requestedModules = mutableListOf<String>()
    val cancelledSessionIds = mutableListOf<Int>()
    var nextSessionId: Int = 1
    var startInstallException: Exception? = null
    var getSessionStatesException: Exception? = null

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
                "getSessionStates" ->
                    getSessionStatesException?.let {
                        Tasks.forException<List<SplitInstallSessionState>>(it)
                    } ?: Tasks.forResult(emptyList<SplitInstallSessionState>())
                "startInstall" -> {
                    val request = args[0] as SplitInstallRequest
                    requestedModules.addAll(request.moduleNames)
                    startInstallException?.let { Tasks.forException<Int>(it) }
                        ?: Tasks.forResult(nextSessionId++)
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

private fun CompositionTestScope.advanceEffectsAndRecompose() {
    advanceTimeBy(1000)
    ShadowLooper.idleMainLooper()
    advanceTimeBy(1000)
    advance()
}
