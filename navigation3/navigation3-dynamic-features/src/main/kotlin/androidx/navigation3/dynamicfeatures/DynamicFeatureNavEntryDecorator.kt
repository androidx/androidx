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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.get
import com.google.android.play.core.ktx.moduleNames
import com.google.android.play.core.ktx.requestProgressFlow
import com.google.android.play.core.ktx.sessionId
import com.google.android.play.core.ktx.status
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallHelper
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter

/**
 * [SplitInstallSessionState] strictly scoped inside [DynamicFeatureNavEntryDecorator] to provide
 * the current module's download session state to its [DynamicFeatureModule.loadingContent].
 */
internal val LocalInstallSessionState: ProvidableCompositionLocal<SplitInstallSessionState> =
    compositionLocalOf {
        error(
            "No LocalInstallSessionState provided. Make sure to use DynamicFeatureNavEntryDecorator."
        )
    }

/** Remembers a default [SplitInstallManager] tied to the current application context. */
@Composable
public fun rememberSplitInstallManager(): SplitInstallManager {
    val context = LocalContext.current
    return remember(context) {
        SplitInstallManagerFactory.create(context.applicationContext)
    }
}

/**
 * Remembers a [DynamicFeatureNavEntryDecorator] that manages dynamic feature module delivery using
 * a [SplitInstallManager].
 *
 * @param splitInstallManager The [SplitInstallManager] used to drive installation. Defaults to
 *   [rememberSplitInstallManager].
 * @param defaultLoadingContent Fallback content rendered while a module is not installed if it does
 *   not provide its own `loadingContent` in
 *   [DynamicFeatureEntryProviderScope.dynamicFeatureModule]. Receives [SplitInstallSessionState]
 *   updates from [SplitInstallManager] and should handle states such as
 *   [SplitInstallSessionStatus.DOWNLOADING], [SplitInstallSessionStatus.FAILED], and
 *   [SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION] (see
 *   [SplitInstallManager.startConfirmationDialogForResult]).
 */
@Composable
public fun <T : Any> rememberDynamicFeatureNavEntryDecorator(
    splitInstallManager: SplitInstallManager = rememberSplitInstallManager(),
    defaultLoadingContent: @Composable (sessionState: SplitInstallSessionState) -> Unit,
): DynamicFeatureNavEntryDecorator<T> {
    val updatedLoadingContent by rememberUpdatedState(defaultLoadingContent)
    return remember(splitInstallManager) {
        DynamicFeatureNavEntryDecorator(
            splitInstallManager = splitInstallManager,
            defaultLoadingContent =
                @Suppress("UnnecessaryLambdaCreation") { sessionState ->
                    updatedLoadingContent(sessionState)
                },
        )
    }
}

/**
 * A Navigation 3 [NavEntryDecorator] that intercepts dynamic destinations registered via
 * [DynamicFeatureEntryProviderScope.dynamicFeatureModule].
 *
 * - When the module is **installed**: lazily resolves and renders the dynamic module's initial
 *   destination in-place.
 * - When the module is **not installed**: initiates installation via [SplitInstallManager] and
 *   renders the module's `loadingContent` or `defaultLoadingContent` with the current
 *   [SplitInstallSessionState]. The loading content should handle states emitted by
 *   [SplitInstallManager], including [SplitInstallSessionStatus.FAILED] and
 *   [SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION] (see
 *   [SplitInstallManager.startConfirmationDialogForResult]).
 */
public class DynamicFeatureNavEntryDecorator<T : Any>
internal constructor(
    splitInstallManager: SplitInstallManager,
    private val dynamicEntryProvider: DynamicFeatureEntryProvider<T>,
) :
    NavEntryDecorator<T>(
        decorate = { entry: NavEntry<T> ->
            val loadingModule = entry.metadata[LoadingDynamicFeatureModuleMetadataKey]

            // loadingModule is non-null only when the dynamic feature is not installed and not
            // initialized.
            if (loadingModule != null) {
                var sessionState by
                    rememberSessionState(splitInstallManager, loadingModule.moduleName)
                if (sessionState.isInstalled) {
                    val context = LocalContext.current
                    SideEffect {
                        SplitCompat.install(context)
                        // Enable immediate usage of dynamic feature modules in an instant app
                        // context.
                        SplitInstallHelper.updateAppInfo(context)
                        dynamicEntryProvider.initialize(loadingModule)
                    }
                } else {
                    InstallModuleEffect(
                        splitInstallManager = splitInstallManager,
                        moduleName = loadingModule.moduleName,
                        sessionState = { sessionState },
                        onStateChange = { sessionState = it },
                    )
                }
                CompositionLocalProvider(LocalInstallSessionState provides sessionState) {
                    entry.Content() // loading content
                }
            } else {
                entry.Content() // real content
            }
        }
    ) {
    public constructor(
        splitInstallManager: SplitInstallManager,
        defaultLoadingContent: @Composable (sessionState: SplitInstallSessionState) -> Unit,
    ) : this(
        splitInstallManager,
        DynamicFeatureEntryProvider(
            installedModules = { splitInstallManager.installedModules },
            defaultLoadingContent = defaultLoadingContent,
        ),
    )

    /**
     * Provides a [DynamicFeatureEntryProviderScope] to build an entryProvider that resolves both
     * regular [NavEntry] destinations and dynamic feature modules registered with
     * [DynamicFeatureEntryProviderScope.dynamicFeatureModule].
     *
     * @param fallback the fallback [NavEntry] when neither this provider nor any registered dynamic
     *   module can find an entry associated with a given key on the backStack
     * @param builder the DSL extension that provides a [DynamicFeatureEntryProviderScope] to build
     *   an entryProvider
     * @return an entryProvider that provides the [NavEntry] associated with a given key
     */
    public fun entryProvider(
        fallback: (unknownScreen: T) -> NavEntry<T> = {
            throw IllegalStateException("Unknown screen $it")
        },
        builder: DynamicFeatureEntryProviderScope<T>.() -> Unit,
    ): (T) -> NavEntry<T> {
        dynamicEntryProvider.fallback = fallback
        return DynamicFeatureEntryProviderScope(dynamicEntryProvider).apply(builder).build()
    }
}

@Composable
private fun rememberSessionState(
    splitInstallManager: SplitInstallManager,
    moduleName: String,
): MutableState<SplitInstallSessionState> {
    val sessionState =
        remember(splitInstallManager, moduleName) {
            mutableStateOf(
                createSessionState(
                    moduleName = moduleName,
                    status = SplitInstallSessionStatus.PENDING,
                    errorCode = SplitInstallErrorCode.NO_ERROR,
                )
            )
        }
    LaunchedEffect(splitInstallManager, moduleName) {
        splitInstallManager
            .requestProgressFlow()
            .filter { it.moduleNames.contains(moduleName) }
            .catch { exception ->
                val errorCode =
                    (exception as? SplitInstallException)?.errorCode
                        ?: SplitInstallErrorCode.INTERNAL_ERROR
                sessionState.value =
                    createSessionState(
                        moduleName = moduleName,
                        status = SplitInstallSessionStatus.FAILED,
                        errorCode = errorCode,
                    )
            }
            .collect { sessionState.value = it }
    }
    return sessionState
}

@Composable
private fun InstallModuleEffect(
    splitInstallManager: SplitInstallManager,
    moduleName: String,
    sessionState: () -> SplitInstallSessionState,
    onStateChange: (SplitInstallSessionState) -> Unit,
) {
    var activeSessionId by remember(moduleName) { mutableIntStateOf(-1) }

    LaunchedEffect(moduleName) {
        splitInstallManager
            .startInstall(SplitInstallRequest.newBuilder().addModule(moduleName).build())
            .addOnSuccessListener { sessionId ->
                if (sessionId == 0) {
                    // The feature is already installed, emit synthetic INSTALLED state.
                    onStateChange(
                        createSessionState(
                            moduleName = moduleName,
                            status = SplitInstallSessionStatus.INSTALLED,
                            errorCode = SplitInstallErrorCode.NO_ERROR,
                        )
                    )
                } else {
                    activeSessionId = sessionId
                }
            }
            .addOnFailureListener { exception ->
                val errorCode =
                    (exception as? SplitInstallException)?.errorCode
                        ?: SplitInstallErrorCode.INTERNAL_ERROR
                onStateChange(
                    createSessionState(
                        moduleName = moduleName,
                        status = SplitInstallSessionStatus.FAILED,
                        errorCode = errorCode,
                    )
                )
            }
    }

    DisposableEffect(moduleName) {
        onDispose {
            val currentSessionState = sessionState()
            val sessionIdToCancel =
                if (currentSessionState.sessionId > 0) currentSessionState.sessionId
                else activeSessionId
            // This effect is also disposed when installation completes and the branch leaves
            // composition, so only cancel if the entry was popped before installation finished.
            if (!currentSessionState.isInstalled && sessionIdToCancel > 0) {
                splitInstallManager.cancelInstall(sessionIdToCancel)
            }
        }
    }
}

private val SplitInstallSessionState.isInstalled: Boolean
    get() = status == SplitInstallSessionStatus.INSTALLED

private fun createSessionState(
    moduleName: String,
    @SplitInstallSessionStatus status: Int,
    @SplitInstallErrorCode errorCode: Int,
): SplitInstallSessionState =
    SplitInstallSessionState.create(
        /* sessionId = */ 0,
        /* status = */ status,
        /* errorCode = */ errorCode,
        /* bytesDownloaded = */ 0L,
        /* totalBytesToDownload = */ 0L,
        /* moduleNames = */ listOf(moduleName),
        /* languages = */ emptyList(),
    )
