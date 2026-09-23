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

package androidx.wear.compose.prototypes.windowinsets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.WindowInsets as AndroidWindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.AppScaffoldDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScreenScaffoldDefaults
import androidx.wear.compose.material3.StatusBarSuppression
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimeSource
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.utils.WearApiVersionHelper
import java.time.LocalDate
import java.time.LocalTime

private val generalScreens =
    listOf(
        Screen.Recents,
        Screen.GlobalStatusBarSandbox,
        Screen.HorizontalPager,
        Screen.VerticalPager,
        Screen.SelfRenderedSandbox,
    )

private val statusBarSuppressionScreens =
    listOf(
        Screen.StepperInScaffold,
        Screen.StepperOutOfScaffold,
        Screen.TimePickerInScaffold,
        Screen.TimePickerOutOfScaffold,
        Screen.DatePickerInScaffold,
        Screen.DatePickerOutOfScaffold,
        Screen.DialogInScaffold,
        Screen.DialogOutOfScaffold,
        Screen.CustomSuppressStatusBarInScaffold,
        Screen.CustomSuppressStatusBarOutOfScaffold,
        Screen.PagerWithSuppression,
    )

/** AppScaffold-level TimeText choices. */
enum class AppTimeTextChoice(val label: String) {
    Default("Default (GSB)"),
    InAppDefault("In-App Default"),
    Custom("Custom"),
    Empty("Empty"),
}

fun AppTimeTextChoice.cycle(): AppTimeTextChoice =
    when (this) {
        AppTimeTextChoice.Default -> AppTimeTextChoice.InAppDefault
        AppTimeTextChoice.InAppDefault -> AppTimeTextChoice.Custom
        AppTimeTextChoice.Custom -> AppTimeTextChoice.Empty
        AppTimeTextChoice.Empty -> AppTimeTextChoice.Default
    }

@Composable
fun resolveAppTimeText(
    choice: AppTimeTextChoice,
    customText: String = "App 10:10",
): @Composable () -> Unit {
    val customTimeSource =
        remember(customText) {
            object : TimeSource {
                @Composable override fun currentTime(): String = customText
            }
        }
    return when (choice) {
        AppTimeTextChoice.Default -> AppScaffoldDefaults.timeText
        AppTimeTextChoice.InAppDefault -> ({ TimeText() })
        AppTimeTextChoice.Custom -> ({ TimeText(timeSource = customTimeSource) })
        AppTimeTextChoice.Empty -> ({})
    }
}

/** ScreenScaffold-level TimeText choices (nullable, supports Inherit). */
enum class ScreenTimeTextChoice(val label: String) {
    Inherit("Inherit"),
    InAppDefault("In-App Default"),
    Custom("Custom"),
    Empty("Empty"),
}

fun ScreenTimeTextChoice.cycle(): ScreenTimeTextChoice =
    when (this) {
        ScreenTimeTextChoice.Inherit -> ScreenTimeTextChoice.InAppDefault
        ScreenTimeTextChoice.InAppDefault -> ScreenTimeTextChoice.Custom
        ScreenTimeTextChoice.Custom -> ScreenTimeTextChoice.Empty
        ScreenTimeTextChoice.Empty -> ScreenTimeTextChoice.Inherit
    }

@Composable
fun resolveScreenTimeText(
    choice: ScreenTimeTextChoice,
    customText: String = "Screen 11:11",
): (@Composable () -> Unit)? {
    val customTimeSource =
        remember(customText) {
            object : TimeSource {
                @Composable override fun currentTime(): String = customText
            }
        }
    return when (choice) {
        ScreenTimeTextChoice.Inherit -> null
        ScreenTimeTextChoice.InAppDefault -> ({ TimeText() })
        ScreenTimeTextChoice.Custom -> ({ TimeText(timeSource = customTimeSource) })
        ScreenTimeTextChoice.Empty -> ({})
    }
}

/** Checks if the global status bar is enabled on the device. */
@SuppressLint("BanUncheckedReflection")
fun isDeviceStatusBarEnabled(context: Context): Boolean =
    try {
        val apiResult =
            WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_CINNAMON_BUN_2)

        val settingsClass = Class.forName("com.google.wear.settings.WearSettings")
        val settingsMethod = settingsClass.getMethod("isStatusBarEnabled", Context::class.java)
        val settingsResult = settingsMethod.invoke(null, context) as Boolean

        context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            apiResult &&
            settingsResult
    } catch (_: Throwable) {
        false
    }

/**
 * Resolves the effective status bar visibility for a screen or dialog given device, app, and screen
 * settings. GSB flows strictly top-down: if disabled at the app level, by a screen lambda, or
 * inside a dialog, it cannot be re-enabled.
 */
@Composable
fun resolveEffectiveStatusBarVisibility(
    screenTimeTextChoice: ScreenTimeTextChoice = ScreenTimeTextChoice.Inherit,
    appTimeTextChoice: AppTimeTextChoice = AppTimeTextChoice.Default,
    isInDialog: Boolean = false,
    isSuppressed: Boolean = false,
): Boolean {
    val context = LocalContext.current
    val deviceSupported = remember(context) { isDeviceStatusBarEnabled(context) }
    if (!deviceSupported) return false
    if (isInDialog || isSuppressed) return false
    if (appTimeTextChoice != AppTimeTextChoice.Default) return false
    if (screenTimeTextChoice != ScreenTimeTextChoice.Inherit) return false
    return true
}

/**
 * Visual overlay highlighting the top padding clearance area with distinct colors for Show vs Hide.
 */
@Composable
fun StatusBarInsetOverlay(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    isStatusBarVisible: Boolean = true,
) {
    val topPadding = contentPadding.calculateTopPadding()
    if (topPadding > 0.dp) {
        val overlayColor =
            if (isStatusBarVisible) {
                // Green when Status Bar is effectively shown
                Color(0xFF2E7D32).copy(alpha = 0.40f)
            } else {
                // Red when Status Bar is effectively hidden / suppressed
                Color(0xFFC62828).copy(alpha = 0.40f)
            }
        Box(modifier = modifier.fillMaxWidth().height(topPadding).background(overlayColor))
    }
}

/** Helper modifier to draw a debug border around screen/dialog boundaries. */
@Composable
fun Modifier.screenDebugBorder(color: Color, width: androidx.compose.ui.unit.Dp = 2.dp): Modifier {
    val isRound = LocalConfiguration.current.isScreenRound
    val shape = if (isRound) CircleShape else RectangleShape
    return this.border(width, color, shape)
}

@Composable
private fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (contentPadding != null) {
            val defaultTop = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
            val statusBarTop =
                WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
            val finalTop = contentPadding.calculateTopPadding()
            Text(
                text = "Final: $finalTop | SB: $statusBarTop | Def: $defaultTop",
                style = MaterialTheme.typography.bodyExtraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}

private data class CheckResult(
    val value: Boolean?,
    val error: Throwable? = null,
) {
    val display: String
        get() =
            when {
                error != null -> "Err (${error.javaClass.simpleName})"
                value != null -> value.toString()
                else -> "N/A"
            }
}

private data class StatusBarDiagnostics(
    val localStatusBarEnabled: Boolean,
    val apiVersionResult: CheckResult,
    val wearSettingsResult: CheckResult,
    val sysPropResult: CheckResult,
    val manifestMetaResult: CheckResult,
    val flagsResult: CheckResult,
    val sdkInt: Int,
) {
    val exceptions: List<Pair<String, Throwable>>
        get() {
            val list = mutableListOf<Pair<String, Throwable>>()
            apiVersionResult.error?.let { list.add("WearApiVersionHelper" to it) }
            wearSettingsResult.error?.let { list.add("WearSettings" to it) }
            sysPropResult.error?.let { list.add("SystemProperties" to it) }
            manifestMetaResult.error?.let { list.add("ManifestMetaData" to it) }
            flagsResult.error?.let { list.add("Flags.enableStatusBar" to it) }
            return list
        }
}

@SuppressLint("BanUncheckedReflection")
@Composable
private fun rememberStatusBarDiagnostics(): StatusBarDiagnostics {
    val context = LocalContext.current

    return remember(context) {
        // 1. WearApiVersionHelper check
        val apiCheck =
            try {
                val result =
                    WearApiVersionHelper.isApiVersionAtLeast(
                        WearApiVersionHelper.WEAR_CINNAMON_BUN_2
                    )
                CheckResult(value = result)
            } catch (t: Throwable) {
                val root =
                    if (t is java.lang.reflect.InvocationTargetException) t.targetException ?: t
                    else t
                CheckResult(value = null, error = root)
            }

        // 2. WearSettings check
        val settingsCheck =
            try {
                val settingsClass = Class.forName("com.google.wear.settings.WearSettings")
                val method = settingsClass.getMethod("isStatusBarEnabled", Context::class.java)
                val result = method.invoke(null, context) as Boolean
                CheckResult(value = result)
            } catch (t: Throwable) {
                val root =
                    if (t is java.lang.reflect.InvocationTargetException) t.targetException ?: t
                    else t
                CheckResult(value = null, error = root)
            }

        // 3. System Property: persist.global_status_bar_opt_in
        val propCheck =
            try {
                val sysPropClass = Class.forName("android.os.SystemProperties")
                val method =
                    sysPropClass.getMethod(
                        "getBoolean",
                        String::class.java,
                        Boolean::class.javaPrimitiveType,
                    )
                val result =
                    method.invoke(null, "persist.global_status_bar_opt_in", false) as Boolean
                CheckResult(value = result)
            } catch (t: Throwable) {
                val root =
                    if (t is java.lang.reflect.InvocationTargetException) t.targetException ?: t
                    else t
                CheckResult(value = null, error = root)
            }

        // 4. Manifest Metadata: com.google.wear.ENABLE_GLOBAL_STATUS_BAR
        val manifestCheck =
            try {
                val appInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getApplicationInfo(
                            context.packageName,
                            android.content.pm.PackageManager.ApplicationInfoFlags.of(
                                android.content.pm.PackageManager.GET_META_DATA.toLong()
                            ),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getApplicationInfo(
                            context.packageName,
                            android.content.pm.PackageManager.GET_META_DATA,
                        )
                    }
                val metaData = appInfo.metaData
                val value =
                    metaData?.getBoolean("com.google.wear.ENABLE_GLOBAL_STATUS_BAR", false) ?: false
                CheckResult(value = value)
            } catch (t: Throwable) {
                CheckResult(value = null, error = t)
            }

        // 5. Trunk Flag: Flags.enableStatusBar() (Internal to SystemUI process)
        val flagsCheck =
            try {
                val flagsClass =
                    Class.forName("com.google.android.clockwork.libs.systemui.flags.Flags")
                val method = flagsClass.getMethod("enableStatusBar")
                val result = method.invoke(null) as Boolean
                CheckResult(value = result)
            } catch (t: Throwable) {
                val root =
                    if (t is java.lang.reflect.InvocationTargetException) t.targetException ?: t
                    else t
                if (root is ClassNotFoundException) {
                    // Expected when running in app process as class is private to SystemUI
                    CheckResult(value = null)
                } else {
                    CheckResult(value = null, error = root)
                }
            }

        val localEnabled =
            context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                apiCheck.value == true &&
                settingsCheck.value == true

        StatusBarDiagnostics(
            localStatusBarEnabled = localEnabled,
            apiVersionResult = apiCheck,
            wearSettingsResult = settingsCheck,
            sysPropResult = propCheck,
            manifestMetaResult = manifestCheck,
            flagsResult = flagsCheck,
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
}

@Composable
fun MenuScreen(onNavigateTo: (Screen) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val diagnostics = rememberStatusBarDiagnostics()
    var showDetails by remember { mutableStateOf(false) }

    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader(
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text(
                    text = "Prototypes",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Button(
                onClick = { showDetails = !showDetails },
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                colors =
                    if (diagnostics.localStatusBarEnabled) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20),
                            contentColor = Color.White,
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F0000),
                            contentColor = Color.White,
                        )
                    },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text =
                            if (diagnostics.localStatusBarEnabled) "Status Bar: ENABLED"
                            else "Status Bar: DISABLED",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text =
                            "CB2: ${diagnostics.apiVersionResult.display} | Settings: ${diagnostics.wearSettingsResult.display}",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text =
                            if (showDetails) "▲ Hide debug breakdown" else "▼ Show debug breakdown",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
        if (showDetails) {
            item {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(Color.DarkGray.copy(alpha = 0.7f))
                            .padding(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        "WearSettings Check:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Cyan,
                    )
                    Text(
                        "• SysProp (persist.global...): ${diagnostics.sysPropResult.display}",
                        style = MaterialTheme.typography.bodyExtraSmall,
                    )
                    Text(
                        "• Manifest MetaData: ${diagnostics.manifestMetaResult.display}",
                        style = MaterialTheme.typography.bodyExtraSmall,
                    )
                    Text(
                        "• SystemUI Flag: ${diagnostics.flagsResult.display}",
                        style = MaterialTheme.typography.bodyExtraSmall,
                    )
                    Text(
                        "• SDK_INT: ${diagnostics.sdkInt}",
                        style = MaterialTheme.typography.bodyExtraSmall,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Exceptions Caught:",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (diagnostics.exceptions.isEmpty()) Color.Green
                            else Color(0xFFFF8A80),
                    )
                    if (diagnostics.exceptions.isEmpty()) {
                        Text(
                            "None (clean)",
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = Color.Green,
                        )
                    } else {
                        diagnostics.exceptions.forEach { (name, ex) ->
                            Text(
                                text =
                                    "[$name]\n${ex.javaClass.simpleName}: ${ex.message ?: "null"}" +
                                        (ex.cause?.let {
                                            "\nCause: ${it.javaClass.simpleName}: ${it.message}"
                                        } ?: ""),
                                style = MaterialTheme.typography.bodyExtraSmall,
                                color = Color(0xFFFF8A80),
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        items(generalScreens) { screen ->
            Button(
                onClick = { onNavigateTo(screen) },
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                label = { Text(screen.title) },
            )
        }
        item {
            ListHeader(
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text(
                    text = "Status Bar Suppression",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(statusBarSuppressionScreens) { screen ->
            Button(
                onClick = { onNavigateTo(screen) },
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                label = { Text(screen.title) },
            )
        }
    }
}

/**
 * Global Status Bar Sandbox testing dynamic app-level baseline toggle, per-screen lambda overrides,
 * scroll-away behavior, and top-down dialog suppression coordination.
 */
@SuppressLint("FrequentlyChangingValue")
@Composable
fun GlobalStatusBarSandboxScreen(onBack: () -> Unit) {
    val view = LocalView.current
    var appTimeTextChoice by remember { mutableStateOf(AppTimeTextChoice.Default) }
    var screenTimeTextChoice by remember { mutableStateOf(ScreenTimeTextChoice.Inherit) }
    var currentSubScreen by remember { mutableStateOf("menu") }

    // Dialog state triggers
    var showSimpleDialog by remember { mutableStateOf(false) }
    var showNestedDialog1 by remember { mutableStateOf(false) }
    var showNestedDialog2 by remember { mutableStateOf(false) }

    val menuScrollState = rememberTransformingLazyColumnState()
    val menuScrollInfoProvider = remember { ScrollInfoProvider(menuScrollState) }

    val overridesScrollState = rememberTransformingLazyColumnState()
    val overridesScrollInfoProvider = remember { ScrollInfoProvider(overridesScrollState) }

    AppScaffold(timeText = resolveAppTimeText(appTimeTextChoice, customText = "App 10:10")) {
        // Simple raw Dialog (No inner ScreenScaffold - tests hard suppression and restore)
        Dialog(
            visible = showSimpleDialog,
            onDismissRequest = { showSimpleDialog = false },
        ) {
            Box(
                modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFFFF5252)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Simple Dialog", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Dialog sits above TimeText; GSB suppressed unconditionally.",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showSimpleDialog = false }) {
                        Text("Close Simple Dialog", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Nested Dialogs
        Dialog(
            visible = showNestedDialog1,
            onDismissRequest = { showNestedDialog1 = false },
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color(0xFF3E2723))
                        .screenDebugBorder(Color(0xFFFFD740)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Nested Dialog 1", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "GSB suppressed across dialog stack",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showNestedDialog2 = true }) {
                        Text("Open Dialog 2", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = { showNestedDialog1 = false }) {
                        Text("Close Dialog 1", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Dialog(
                visible = showNestedDialog2,
                onDismissRequest = { showNestedDialog2 = false },
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color(0xFF263238))
                            .screenDebugBorder(Color(0xFFFF4081)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text("Nested Dialog 2", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "GSB stays suppressed until both are closed",
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showNestedDialog2 = false }) {
                            Text("Close Dialog 2", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        SwipeToDismissBox(
            onDismissed = {
                if (currentSubScreen == "menu") {
                    onBack()
                } else {
                    currentSubScreen = "menu"
                }
            }
        ) { isBackground ->
            val screenToShow = if (isBackground) "menu" else currentSubScreen
            when (screenToShow) {
                "menu" -> {
                    val spec = rememberTransformationSpec()
                    ScreenScaffold(scrollInfoProvider = menuScrollInfoProvider) { contentPadding ->
                        androidx.compose.runtime.LaunchedEffect(
                            menuScrollState.anchorItemIndex,
                            menuScrollState.anchorItemScrollOffset,
                            contentPadding.calculateTopPadding(),
                        ) {
                            android.util.Log.d(
                                "WearInsetsDebug",
                                "Screens.kt MenuScreen: anchorIndex=${menuScrollState.anchorItemIndex}, anchorOffset=${menuScrollState.anchorItemScrollOffset}, topPadding=${contentPadding.calculateTopPadding()}",
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF00E5FF))
                        ) {
                            TransformingLazyColumn(
                                state = menuScrollState,
                                contentPadding = contentPadding,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                item { ScreenTitle("GSB Sandbox", contentPadding = contentPadding) }
                                item {
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                4.dp,
                                                Alignment.CenterHorizontally,
                                            ),
                                    ) {
                                        Button(
                                            onClick = { showSimpleDialog = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Simple Dlg",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        Button(
                                            onClick = { showNestedDialog1 = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Nested Dlgs",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                                item {
                                    Button(
                                        onClick = {
                                            appTimeTextChoice = appTimeTextChoice.cycle()
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    ) {
                                        Text(
                                            "App TimeText: ${appTimeTextChoice.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                item {
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                4.dp,
                                                Alignment.CenterHorizontally,
                                            ),
                                    ) {
                                        Button(
                                            onClick = { currentSubScreen = "screenOverrides" },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Screen Overrides",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        Button(
                                            onClick = onBack,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Exit Sandbox",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                4.dp,
                                                Alignment.CenterHorizontally,
                                            ),
                                    ) {
                                        Button(
                                            onClick = {
                                                view.windowInsetsController?.hide(
                                                    AndroidWindowInsets.Type.statusBars()
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Direct Hide",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                view.windowInsetsController?.show(
                                                    AndroidWindowInsets.Type.statusBars()
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Direct Show",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                                items(25) { index ->
                                    Button(
                                        onClick = {},
                                        modifier =
                                            Modifier.fillMaxWidth().transformedHeight(this, spec),
                                        transformation = SurfaceTransformation(spec),
                                    ) {
                                        Text(
                                            "Menu item $index",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                            StatusBarInsetOverlay(
                                contentPadding,
                                isStatusBarVisible =
                                    resolveEffectiveStatusBarVisibility(
                                        screenTimeTextChoice = ScreenTimeTextChoice.Inherit,
                                        appTimeTextChoice = appTimeTextChoice,
                                    ),
                            )
                        }
                    }
                }
                "screenOverrides" -> {
                    val spec = rememberTransformationSpec()
                    val resolvedTimeText =
                        resolveScreenTimeText(
                            screenTimeTextChoice,
                            customText = "Screen 11:11",
                        )
                    ScreenScaffold(
                        timeText = resolvedTimeText,
                        scrollInfoProvider = overridesScrollInfoProvider,
                    ) { contentPadding ->
                        Box(
                            modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFFFF9100))
                        ) {
                            TransformingLazyColumn(
                                state = overridesScrollState,
                                contentPadding = contentPadding,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                item {
                                    ScreenTitle("Screen Overrides", contentPadding = contentPadding)
                                }
                                item {
                                    Button(
                                        onClick = {
                                            screenTimeTextChoice = screenTimeTextChoice.cycle()
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    ) {
                                        Text(
                                            "Screen TimeText: ${screenTimeTextChoice.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                item {
                                    Text(
                                        "Top-down rule: ScreenScaffold can inherit or disable GSB, but cannot turn GSB back on if AppScaffold disabled it.",
                                        style = MaterialTheme.typography.bodyExtraSmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier =
                                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                                item {
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                4.dp,
                                                Alignment.CenterHorizontally,
                                            ),
                                    ) {
                                        Button(
                                            onClick = { showSimpleDialog = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Simple Dlg",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        Button(
                                            onClick = { currentSubScreen = "menu" },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                "Back to Menu",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                                items(25) { index ->
                                    Button(
                                        onClick = {},
                                        modifier =
                                            Modifier.fillMaxWidth().transformedHeight(this, spec),
                                        transformation = SurfaceTransformation(spec),
                                    ) {
                                        Text(
                                            "Override item $index",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                            StatusBarInsetOverlay(
                                contentPadding,
                                isStatusBarVisible =
                                    resolveEffectiveStatusBarVisibility(
                                        screenTimeTextChoice = screenTimeTextChoice,
                                        appTimeTextChoice = appTimeTextChoice,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Horizontal Pager demo testing per-page status bar isolation and paging transitions. */
@Composable
fun HorizontalPagerScreen(onBack: () -> Unit) {
    var page1Choice by remember { mutableStateOf(ScreenTimeTextChoice.Inherit) }
    var page2Choice by remember { mutableStateOf(ScreenTimeTextChoice.Empty) }
    var page3Choice by remember { mutableStateOf(ScreenTimeTextChoice.InAppDefault) }

    AppScaffold {
        SwipeToDismissBox(onDismissed = onBack) { isBackground ->
            if (!isBackground) {
                val pagerState = rememberPagerState(pageCount = { 3 })

                val pageContent =
                    @Composable { page: Int ->
                        val myChoice =
                            when (page) {
                                0 -> page1Choice
                                1 -> page2Choice
                                else -> page3Choice
                            }

                        ScreenScaffold(
                            timeText =
                                resolveScreenTimeText(
                                    myChoice,
                                    customText = "Page ${page + 1}",
                                )
                        ) { contentPadding ->
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().screenDebugBorder(Color(0xFF7C4DFF))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "Horizontal Page ${page + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier =
                                            Modifier.background(Color.Blue.copy(alpha = 0.35f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    val defaultTop =
                                        ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                                    val statusBarTop =
                                        WindowInsets.statusBarsIgnoringVisibility
                                            .asPaddingValues()
                                            .calculateTopPadding()
                                    val finalTop = contentPadding.calculateTopPadding()
                                    Text(
                                        "Choice: ${myChoice.label}\nFinal: $finalTop | SB: $statusBarTop | Def: $defaultTop",
                                        style = MaterialTheme.typography.bodyExtraSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            when (page) {
                                                0 -> page1Choice = page1Choice.cycle()
                                                1 -> page2Choice = page2Choice.cycle()
                                                else -> page3Choice = page3Choice.cycle()
                                            }
                                        }
                                    ) {
                                        Text("Cycle TimeText")
                                    }
                                }
                                StatusBarInsetOverlay(
                                    contentPadding,
                                    isStatusBarVisible =
                                        resolveEffectiveStatusBarVisibility(
                                            screenTimeTextChoice = myChoice,
                                            appTimeTextChoice = AppTimeTextChoice.Default,
                                        ),
                                )
                            }
                        }
                    }

                HorizontalPagerScaffold(pagerState = pagerState) {
                    HorizontalPager(
                        state = pagerState,
                        flingBehavior =
                            PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                    ) { page ->
                        AnimatedPage(pageIndex = page, pagerState = pagerState) {
                            pageContent(page)
                        }
                    }
                }
            }
        }
    }
}

/** Vertical Pager demo testing per-page status bar isolation and vertical paging transitions. */
@Composable
fun VerticalPagerScreen(onBack: () -> Unit) {
    var page1Choice by remember { mutableStateOf(ScreenTimeTextChoice.Inherit) }
    var page2Choice by remember { mutableStateOf(ScreenTimeTextChoice.Empty) }
    var page3Choice by remember { mutableStateOf(ScreenTimeTextChoice.InAppDefault) }

    AppScaffold {
        SwipeToDismissBox(onDismissed = onBack) { isBackground ->
            if (!isBackground) {
                val pagerState = rememberPagerState(pageCount = { 3 })

                val pageContent =
                    @Composable { page: Int ->
                        val myChoice =
                            when (page) {
                                0 -> page1Choice
                                1 -> page2Choice
                                else -> page3Choice
                            }

                        ScreenScaffold(
                            timeText =
                                resolveScreenTimeText(
                                    myChoice,
                                    customText = "Page ${page + 1}",
                                )
                        ) { contentPadding ->
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().screenDebugBorder(Color(0xFF536DFE))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "Vertical Page ${page + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier =
                                            Modifier.background(Color.Blue.copy(alpha = 0.35f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    val defaultTop =
                                        ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                                    val statusBarTop =
                                        WindowInsets.statusBarsIgnoringVisibility
                                            .asPaddingValues()
                                            .calculateTopPadding()
                                    val finalTop = contentPadding.calculateTopPadding()
                                    Text(
                                        "Choice: ${myChoice.label}\nFinal: $finalTop | SB: $statusBarTop | Def: $defaultTop",
                                        style = MaterialTheme.typography.bodyExtraSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            when (page) {
                                                0 -> page1Choice = page1Choice.cycle()
                                                1 -> page2Choice = page2Choice.cycle()
                                                else -> page3Choice = page3Choice.cycle()
                                            }
                                        }
                                    ) {
                                        Text("Cycle TimeText")
                                    }
                                }
                                StatusBarInsetOverlay(
                                    contentPadding,
                                    isStatusBarVisible =
                                        resolveEffectiveStatusBarVisibility(
                                            screenTimeTextChoice = myChoice,
                                            appTimeTextChoice = AppTimeTextChoice.Default,
                                        ),
                                )
                            }
                        }
                    }

                VerticalPagerScaffold(pagerState = pagerState) {
                    VerticalPager(
                        state = pagerState,
                        flingBehavior =
                            PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                    ) { page ->
                        AnimatedPage(pageIndex = page, pagerState = pagerState) {
                            pageContent(page)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Self Rendered Sandbox testing fallback behavior when AppScaffold uses a non-default TimeText
 * lambda (disabling GSB) or hardware unsupported, verifying local TimeText fallback, custom
 * TimeSource, and Dialog overlay interactions.
 */
@Composable
fun SelfRenderedSandboxScreen(onBack: () -> Unit) {
    var showSimpleDialog by remember { mutableStateOf(false) }
    var showScaffoldDialog by remember { mutableStateOf(false) }
    var showNestedDialog1 by remember { mutableStateOf(false) }
    var showNestedDialog2 by remember { mutableStateOf(false) }
    var timeTextChoice by remember { mutableStateOf(AppTimeTextChoice.InAppDefault) }

    val listState = rememberTransformingLazyColumnState()
    val scrollInfoProvider = remember { ScrollInfoProvider(listState) }

    val appTimeText = resolveAppTimeText(timeTextChoice, customText = "Self-Rendered 10:10")

    AppScaffold(timeText = appTimeText) {
        // Simple raw Dialog
        Dialog(
            visible = showSimpleDialog,
            onDismissRequest = { showSimpleDialog = false },
        ) {
            Box(
                modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFFFF5252)),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = { showSimpleDialog = false }) {
                    Text("Close Simple Dialog", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Dialog with ScreenScaffold (Scrollable)
        Dialog(
            visible = showScaffoldDialog,
            onDismissRequest = { showScaffoldDialog = false },
        ) {
            val dialogScrollState = rememberTransformingLazyColumnState()
            val dialogScrollInfoProvider = remember { ScrollInfoProvider(dialogScrollState) }
            val spec = rememberTransformationSpec()
            ScreenScaffold(scrollInfoProvider = dialogScrollInfoProvider) { dialogPadding ->
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color(0xFF1A237E))
                            .screenDebugBorder(Color(0xFF448AFF))
                ) {
                    TransformingLazyColumn(
                        state = dialogScrollState,
                        contentPadding = dialogPadding,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { ScreenTitle("Scaffold Dialog", contentPadding = dialogPadding) }
                        item {
                            Button(
                                onClick = { showScaffoldDialog = false },
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                                transformation = SurfaceTransformation(spec),
                            ) {
                                Text("Close Dialog", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        items(20) { index ->
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                                transformation = SurfaceTransformation(spec),
                            ) {
                                Text(
                                    "Dialog Item $index",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    StatusBarInsetOverlay(
                        dialogPadding,
                        isStatusBarVisible = false,
                    )
                }
            }
        }

        // Nested Dialogs
        Dialog(
            visible = showNestedDialog1,
            onDismissRequest = { showNestedDialog1 = false },
        ) {
            val d1ScrollState = rememberTransformingLazyColumnState()
            val d1ScrollInfoProvider = remember { ScrollInfoProvider(d1ScrollState) }
            val spec = rememberTransformationSpec()
            ScreenScaffold(scrollInfoProvider = d1ScrollInfoProvider) { d1Padding ->
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color(0xFF3E2723))
                            .screenDebugBorder(Color(0xFFFFD740))
                ) {
                    TransformingLazyColumn(
                        state = d1ScrollState,
                        contentPadding = d1Padding,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            ScreenTitle(
                                "Nested Dialog 1",
                                contentPadding = d1Padding,
                            )
                        }
                        item {
                            Button(
                                onClick = { showNestedDialog2 = true },
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                                transformation = SurfaceTransformation(spec),
                            ) {
                                Text("Open Dialog 2", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Button(
                                onClick = { showNestedDialog1 = false },
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                                transformation = SurfaceTransformation(spec),
                            ) {
                                Text("Close Dialog 1", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        items(15) { index ->
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                                transformation = SurfaceTransformation(spec),
                            ) {
                                Text("D1 Item $index", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    StatusBarInsetOverlay(
                        d1Padding,
                        isStatusBarVisible = false,
                    )
                }
            }

            Dialog(
                visible = showNestedDialog2,
                onDismissRequest = { showNestedDialog2 = false },
            ) {
                val d2ScrollState = rememberTransformingLazyColumnState()
                val d2ScrollInfoProvider = remember { ScrollInfoProvider(d2ScrollState) }
                val d2Spec = rememberTransformationSpec()
                ScreenScaffold(scrollInfoProvider = d2ScrollInfoProvider) { d2Padding ->
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color(0xFF263238))
                                .screenDebugBorder(Color(0xFFFF4081))
                    ) {
                        TransformingLazyColumn(
                            state = d2ScrollState,
                            contentPadding = d2Padding,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item {
                                ScreenTitle(
                                    "Nested Dialog 2",
                                    contentPadding = d2Padding,
                                )
                            }
                            item {
                                Button(
                                    onClick = { showNestedDialog2 = false },
                                    modifier =
                                        Modifier.fillMaxWidth().transformedHeight(this, d2Spec),
                                    transformation = SurfaceTransformation(d2Spec),
                                ) {
                                    Text(
                                        "Close Dialog 2",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            items(15) { index ->
                                Button(
                                    onClick = {},
                                    modifier =
                                        Modifier.fillMaxWidth().transformedHeight(this, d2Spec),
                                    transformation = SurfaceTransformation(d2Spec),
                                ) {
                                    Text(
                                        "D2 Item $index",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                        StatusBarInsetOverlay(
                            d2Padding,
                            isStatusBarVisible = false,
                        )
                    }
                }
            }
        }

        SwipeToDismissBox(onDismissed = onBack) { isBackground ->
            if (!isBackground) {
                ScreenScaffold(scrollInfoProvider = scrollInfoProvider) { contentPadding ->
                    Box(modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFFFFEA00))) {
                        TransformingLazyColumn(
                            state = listState,
                            contentPadding = contentPadding,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item {
                                ScreenTitle(
                                    "Self-Rendered Sandbox",
                                    contentPadding = contentPadding,
                                )
                            }
                            item {
                                Text(
                                    "App TimeText Choice: ${timeTextChoice.label}\n(GSB disabled by AppScaffold lambda)",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .background(Color.DarkGray.copy(alpha = 0.5f))
                                            .padding(4.dp),
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                ) {
                                    Button(
                                        onClick = { showScaffoldDialog = true },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            "Scaffold Dlg",
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    Button(
                                        onClick = { showSimpleDialog = true },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            "Simple Dlg",
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                            item {
                                Button(
                                    onClick = { showNestedDialog1 = true },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                ) {
                                    Text(
                                        "Nested Dlgs",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            item {
                                Button(
                                    onClick = {
                                        timeTextChoice =
                                            when (timeTextChoice) {
                                                AppTimeTextChoice.InAppDefault ->
                                                    AppTimeTextChoice.Custom
                                                AppTimeTextChoice.Custom -> AppTimeTextChoice.Empty
                                                else -> AppTimeTextChoice.InAppDefault
                                            }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                ) {
                                    Text("Cycle TimeText: ${timeTextChoice.label}")
                                }
                            }
                            item {
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                ) {
                                    Text("Exit Sandbox")
                                }
                            }
                            items(25) { index ->
                                Button(
                                    onClick = {},
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                ) {
                                    Text("Self-rendered item $index")
                                }
                            }
                        }
                        StatusBarInsetOverlay(
                            contentPadding,
                            isStatusBarVisible = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentsScreen(recents: List<Screen>, onNavigateTo: (Screen) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader(
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text(
                    text = "Recents",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (recents.isEmpty()) {
            item {
                Text(
                    text = "No recent screens",
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                            .transformedHeight(this, transformationSpec),
                )
            }
        }
        items(recents) { screen ->
            Button(
                onClick = { onNavigateTo(screen) },
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                label = { Text(screen.title) },
            )
        }
    }
}

@Composable
fun StepperInScaffoldScreen(onBack: () -> Unit) {
    var stepperValue by remember { mutableFloatStateOf(5f) }
    AppScaffold {
        ScreenScaffold { contentPadding ->
            SwipeToDismissBox(onDismissed = onBack) { isBackground ->
                if (!isBackground) {
                    Box(modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF4CAF50))) {
                        Stepper(
                            value = stepperValue,
                            onValueChange = { stepperValue = it },
                            steps = 9,
                            valueRange = 1f..10f,
                            increaseIcon = {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            },
                            decreaseIcon = {
                                Text("-", style = MaterialTheme.typography.titleMedium)
                            },
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    "Stepper (In Scaffold)",
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "${stepperValue.toInt()}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "GSB suppressed internally via Stepper's StatusBarSuppression()",
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = onBack) {
                                    Text("Back", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        StatusBarInsetOverlay(contentPadding, isStatusBarVisible = false)
                    }
                }
            }
        }
    }
}

@Composable
fun StepperOutOfScaffoldScreen(onBack: () -> Unit) {
    var stepperValue by remember { mutableFloatStateOf(5f) }
    SwipeToDismissBox(onDismissed = onBack) { isBackground ->
        if (!isBackground) {
            Box(modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFFFF9800))) {
                Stepper(
                    value = stepperValue,
                    onValueChange = { stepperValue = it },
                    steps = 9,
                    valueRange = 1f..10f,
                    increaseIcon = { Text("+", style = MaterialTheme.typography.titleMedium) },
                    decreaseIcon = { Text("-", style = MaterialTheme.typography.titleMedium) },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Text(
                            "Stepper (Out of Scaffold)",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "${stepperValue.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "GSB hard-hidden via StatusBarOrchestrator fallback",
                            style = MaterialTheme.typography.bodyExtraSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = onBack) {
                            Text("Back", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerInScaffoldScreen(onBack: () -> Unit) {
    var pickedTime by remember { mutableStateOf<LocalTime?>(null) }
    AppScaffold {
        ScreenScaffold { contentPadding ->
            SwipeToDismissBox(onDismissed = onBack) { isBackground ->
                if (!isBackground) {
                    Box(
                        modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF3F51B5)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (pickedTime != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                Text(
                                    "Time Selected",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "$pickedTime",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Status bar restored after picker exited",
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { pickedTime = null }) {
                                    Text("Change Time")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = onBack) {
                                    Text("Back")
                                }
                            }
                        } else {
                            TimePicker(
                                initialTime = remember { LocalTime.of(10, 10) },
                                onTimePicked = { pickedTime = it },
                            )
                        }
                        StatusBarInsetOverlay(
                            contentPadding,
                            isStatusBarVisible = pickedTime != null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerOutOfScaffoldScreen(onBack: () -> Unit) {
    var pickedTime by remember { mutableStateOf<LocalTime?>(null) }
    SwipeToDismissBox(onDismissed = onBack) { isBackground ->
        if (!isBackground) {
            Box(
                modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF673AB7)),
                contentAlignment = Alignment.Center,
            ) {
                if (pickedTime != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(
                            "Time Selected (Out of Scaffold)",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "$pickedTime",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { pickedTime = null }) {
                            Text("Change Time")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = onBack) {
                            Text("Back")
                        }
                    }
                } else {
                    TimePicker(
                        initialTime = remember { LocalTime.of(10, 10) },
                        onTimePicked = { pickedTime = it },
                    )
                }
            }
        }
    }
}

@Composable
fun DatePickerInScaffoldScreen(onBack: () -> Unit) {
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    AppScaffold {
        ScreenScaffold { contentPadding ->
            SwipeToDismissBox(onDismissed = onBack) { isBackground ->
                if (!isBackground) {
                    Box(
                        modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF009688)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (pickedDate != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                Text(
                                    "Date Selected",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "$pickedDate",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Status bar restored after picker exited",
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { pickedDate = null }) {
                                    Text("Change Date")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = onBack) {
                                    Text("Back")
                                }
                            }
                        } else {
                            DatePicker(
                                initialDate = remember { LocalDate.of(2026, 9, 4) },
                                onDatePicked = { pickedDate = it },
                            )
                        }
                        StatusBarInsetOverlay(
                            contentPadding,
                            isStatusBarVisible = pickedDate != null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerOutOfScaffoldScreen(onBack: () -> Unit) {
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    SwipeToDismissBox(onDismissed = onBack) { isBackground ->
        if (!isBackground) {
            Box(
                modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF00796B)),
                contentAlignment = Alignment.Center,
            ) {
                if (pickedDate != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(
                            "Date Selected (Out of Scaffold)",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "$pickedDate",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { pickedDate = null }) {
                            Text("Change Date")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = onBack) {
                            Text("Back")
                        }
                    }
                } else {
                    DatePicker(
                        initialDate = remember { LocalDate.of(2026, 9, 4) },
                        onDatePicked = { pickedDate = it },
                    )
                }
            }
        }
    }
}

@Composable
fun DialogInScaffoldScreen(onBack: () -> Unit) {
    var showSimpleDialog by remember { mutableStateOf(false) }
    var showPickerDialog by remember { mutableStateOf(false) }
    val listState = rememberTransformingLazyColumnState()
    val scrollInfoProvider = remember { ScrollInfoProvider(listState) }
    val transformationSpec = rememberTransformationSpec()

    AppScaffold {
        ScreenScaffold(scrollInfoProvider = scrollInfoProvider) { contentPadding ->
            Dialog(
                visible = showSimpleDialog,
                onDismissRequest = { showSimpleDialog = false },
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .screenDebugBorder(Color(0xFFE040FB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text("Simple Dialog", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "GSB suppressed via Dialog StatusBarSuppression()",
                            style = MaterialTheme.typography.bodyExtraSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showSimpleDialog = false }) {
                            Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Dialog(
                visible = showPickerDialog,
                onDismissRequest = { showPickerDialog = false },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(
                        initialTime = remember { LocalTime.of(12, 0) },
                        onTimePicked = { showPickerDialog = false },
                    )
                }
            }

            SwipeToDismissBox(onDismissed = onBack) { isBackground ->
                if (!isBackground) {
                    Box(modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF2196F3))) {
                        TransformingLazyColumn(
                            state = listState,
                            contentPadding = contentPadding,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item {
                                ScreenTitle("Dialog (In Scaffold)", contentPadding = contentPadding)
                            }
                            item {
                                Text(
                                    "Status Bar is VISIBLE on background screen.",
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            item {
                                Button(
                                    onClick = { showSimpleDialog = true },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text("Simple Dialog")
                                }
                            }
                            item {
                                Button(
                                    onClick = { showPickerDialog = true },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text("Dialog + TimePicker")
                                }
                            }
                            item {
                                Button(
                                    onClick = onBack,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text("Back")
                                }
                            }
                        }
                        StatusBarInsetOverlay(contentPadding, isStatusBarVisible = true)
                    }
                }
            }
        }
    }
}

@Composable
fun DialogOutOfScaffoldScreen(onBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var showPickerDialog by remember { mutableStateOf(false) }
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    Dialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(Color(0xFF212121))
                    .screenDebugBorder(Color(0xFFFF5252)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    "Dialog Active (Out of Scaffold)",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "LocalScaffoldState is null.\nStatusBarOrchestrator hard-hides GSB on dialog window without crashing!",
                    style = MaterialTheme.typography.bodyExtraSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showDialog = false }) {
                    Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    Dialog(
        visible = showPickerDialog,
        onDismissRequest = { showPickerDialog = false },
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            TimePicker(
                initialTime = remember { LocalTime.of(12, 0) },
                onTimePicked = { showPickerDialog = false },
            )
        }
    }

    SwipeToDismissBox(onDismissed = onBack) { isBackground ->
        if (!isBackground) {
            Box(modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF9C27B0))) {
                TransformingLazyColumn(
                    state = listState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ScreenTitle("Dialog (Out of Scaffold)")
                    }
                    item {
                        Text(
                            "Rendered without AppScaffold.\nTests StatusBarSuppression() fallback to StatusBarOrchestrator.",
                            style = MaterialTheme.typography.bodyExtraSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        Button(
                            onClick = { showDialog = true },
                            modifier =
                                Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Open Dialog")
                        }
                    }
                    item {
                        Button(
                            onClick = { showPickerDialog = true },
                            modifier =
                                Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Open Dialog + TimePicker")
                        }
                    }
                    item {
                        Button(
                            onClick = onBack,
                            modifier =
                                Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSuppressStatusBarInScaffoldScreen(onBack: () -> Unit) {
    var isSuppressed by remember { mutableStateOf(true) }

    if (isSuppressed) {
        StatusBarSuppression()
    }

    AppScaffold {
        ScreenScaffold { contentPadding ->
            SwipeToDismissBox(onDismissed = onBack) { isBackground ->
                if (!isBackground) {
                    Box(
                        modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFF00BCD4)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { 0.75f },
                            modifier = Modifier.fillMaxSize(),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        ) {
                            Text(
                                "SuppressStatusBar (In Scaffold)",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = if (isSuppressed) "SUPPRESSED" else "VISIBLE",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSuppressed) Color(0xFFFF5252) else Color(0xFF4CAF50),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Toggling conditionally calls StatusBarSuppression() inside AppScaffold",
                                style = MaterialTheme.typography.bodyExtraSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { isSuppressed = !isSuppressed }) {
                                Text(if (isSuppressed) "Show Status Bar" else "Suppress Status Bar")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = onBack) {
                                Text("Back", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        StatusBarInsetOverlay(contentPadding, isStatusBarVisible = !isSuppressed)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSuppressStatusBarOutOfScaffoldScreen(onBack: () -> Unit) {
    var isSuppressed by remember { mutableStateOf(true) }

    if (isSuppressed) {
        StatusBarSuppression()
    }

    SwipeToDismissBox(onDismissed = onBack) { isBackground ->
        if (!isBackground) {
            Box(
                modifier = Modifier.fillMaxSize().screenDebugBorder(Color(0xFFE91E63)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                ) {
                    Text(
                        "SuppressStatusBar (Out of Scaffold)",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (isSuppressed) "SUPPRESSED" else "VISIBLE",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSuppressed) Color(0xFFFF5252) else Color(0xFF4CAF50),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Directly calls StatusBarSuppression() without AppScaffold; uses StatusBarOrchestrator fallback",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { isSuppressed = !isSuppressed }) {
                        Text(if (isSuppressed) "Show Status Bar" else "Suppress Status Bar")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onBack) {
                        Text("Back", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PagerWithSuppressionScreen(onBack: () -> Unit) {
    AppScaffold {
        SwipeToDismissBox(onDismissed = onBack) { isBackground ->
            if (!isBackground) {
                val pagerState = rememberPagerState(pageCount = { 3 })
                var stepperValue by remember { mutableFloatStateOf(5f) }

                HorizontalPagerScaffold(pagerState = pagerState) {
                    HorizontalPager(
                        state = pagerState,
                        flingBehavior =
                            PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                    ) { page ->
                        AnimatedPage(pageIndex = page, pagerState = pagerState) {
                            when (page) {
                                0 -> {
                                    ScreenScaffold { contentPadding ->
                                        Box(
                                            modifier =
                                                Modifier.fillMaxSize()
                                                    .screenDebugBorder(Color(0xFF4CAF50)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            ) {
                                                Text(
                                                    "Page 1 (Normal)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                )
                                                Text(
                                                    "Status Bar: VISIBLE",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(0xFF4CAF50),
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    "Swipe right → to Page 2 (Stepper)",
                                                    style = MaterialTheme.typography.bodyExtraSmall,
                                                    textAlign = TextAlign.Center,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(onClick = onBack) {
                                                    Text(
                                                        "Back",
                                                        style = MaterialTheme.typography.labelSmall,
                                                    )
                                                }
                                            }
                                            StatusBarInsetOverlay(
                                                contentPadding,
                                                isStatusBarVisible = true,
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    ScreenScaffold { contentPadding ->
                                        Box(
                                            modifier =
                                                Modifier.fillMaxSize()
                                                    .screenDebugBorder(Color(0xFFFF9800)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Stepper(
                                                value = stepperValue,
                                                onValueChange = { stepperValue = it },
                                                steps = 9,
                                                valueRange = 1f..10f,
                                                increaseIcon = {
                                                    Text(
                                                        "+",
                                                        style =
                                                            MaterialTheme.typography.titleMedium,
                                                    )
                                                },
                                                decreaseIcon = {
                                                    Text(
                                                        "-",
                                                        style =
                                                            MaterialTheme.typography.titleMedium,
                                                    )
                                                },
                                            ) {
                                                Column(
                                                    horizontalAlignment =
                                                        Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier =
                                                        Modifier.fillMaxWidth()
                                                            .padding(horizontal = 16.dp),
                                                ) {
                                                    Text(
                                                        "Page 2 (Stepper)",
                                                        style = MaterialTheme.typography.titleSmall,
                                                    )
                                                    Text(
                                                        "Status Bar: SUPPRESSED",
                                                        style =
                                                            MaterialTheme.typography.labelMedium,
                                                        color = Color(0xFFFF5252),
                                                    )
                                                    Text(
                                                        "${stepperValue.toInt()}",
                                                        style =
                                                            MaterialTheme.typography.displaySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                    Text(
                                                        "Scoped via LocalScreenIsActive",
                                                        style =
                                                            MaterialTheme.typography.bodyExtraSmall,
                                                        textAlign = TextAlign.Center,
                                                        color =
                                                            MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                    )
                                                }
                                            }
                                            StatusBarInsetOverlay(
                                                contentPadding,
                                                isStatusBarVisible = false,
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    ScreenScaffold { contentPadding ->
                                        Box(
                                            modifier =
                                                Modifier.fillMaxSize()
                                                    .screenDebugBorder(Color(0xFF2196F3)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            ) {
                                                Text(
                                                    "Page 3 (Normal)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                )
                                                Text(
                                                    "Status Bar: VISIBLE",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(0xFF4CAF50),
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    "← Swipe left back to Page 2 (Stepper)",
                                                    style = MaterialTheme.typography.bodyExtraSmall,
                                                    textAlign = TextAlign.Center,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            StatusBarInsetOverlay(
                                                contentPadding,
                                                isStatusBarVisible = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
