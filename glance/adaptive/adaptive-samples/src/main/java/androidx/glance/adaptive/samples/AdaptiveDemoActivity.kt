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

package androidx.glance.adaptive.samples

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.appwidget.requestPin
import androidx.glance.adaptive.appwidget.setPreview
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager as CoreWidgetManager
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AdaptiveDemoActivity"
private val ContentMaxWidth = 600.dp

/**
 * Demo app for Glance Adaptive widgets.
 *
 * Organizes templates under a unified three-level flow (template catalog -> use-case picker -> live
 * preview & renderer configuration -> pin), publishes picker previews through
 * [GlanceAdaptiveWidgetManager.setPreview], and pins widgets through
 * [GlanceAdaptiveWidgetManager.requestPin]. Opened from a Quick Actions demo widget, it shows which
 * tile or button was tapped.
 */
class AdaptiveDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        if (
            savedInstanceState == null &&
                (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
        ) {
            showWidgetAction(intent)
        }
        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            val colorScheme =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
                        dynamicDarkColorScheme(this)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(this)
                    darkTheme -> darkColorScheme()
                    else -> lightColorScheme()
                }
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DemoScreen()
                }
            }
        }
    }

    /** Shows the Quick Actions widget tile or button that [intent] was started from, if any. */
    private fun showWidgetAction(intent: Intent) {
        val label = intent.getStringExtra(QuickActionsSample.EXTRA_QUICK_ACTION) ?: return
        Toast.makeText(
                this,
                getString(R.string.quick_actions_action_tapped, label),
                Toast.LENGTH_SHORT,
            )
            .show()
    }
}

private data class TemplateGroup(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val pickerInstructionsRes: Int,
    val previewPresets: List<WidgetPreviewPreset>,
    val defaultPreviewPreset: WidgetPreviewPreset,
    val useCases: List<TemplateUseCase>,
)

private data class TemplateUseCase(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val remoteViewsWidgetName: String,
    val remoteComposeWidgetName: String? = null,
    @StringRes val remoteComposeUnavailableReasonRes: Int? = null,
    val buildPayload: () -> GlanceAdaptiveTemplate,
) {
    /**
     * Returns the string resource explaining why [WidgetBackend.RemoteCompose] cannot be selected
     * on this device, or `null` when RemoteCompose is available for this use case.
     */
    @StringRes
    fun remoteComposeDisabledReasonRes(): Int? =
        when {
            remoteComposeWidgetName == null ->
                remoteComposeUnavailableReasonRes ?: R.string.remotecompose_unsupported_sdk
            !isRemoteComposeSupported -> R.string.remotecompose_unsupported_sdk
            else -> null
        }

    fun resolveWidgetName(backend: WidgetBackend): String =
        when (backend) {
            WidgetBackend.RemoteCompose ->
                remoteComposeWidgetName?.takeIf { isRemoteComposeSupported }
                    ?: remoteViewsWidgetName
            WidgetBackend.RemoteViews -> remoteViewsWidgetName
        }

    /** Distinct widget names whose launcher picker previews should be published on this device. */
    fun previewWidgetNames(): List<String> = buildList {
        add(remoteViewsWidgetName)
        if (remoteComposeDisabledReasonRes() == null) {
            remoteComposeWidgetName?.let { rcName ->
                if (rcName !in this) {
                    add(rcName)
                }
            }
        }
    }
}

internal enum class WidgetBackend {
    RemoteCompose,
    RemoteViews,
}

private val isRemoteComposeSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN

private val TrackPreviewPresets =
    listOf(
        WidgetPreviewPreset.Home1x1,
        WidgetPreviewPreset.Home2x2,
        WidgetPreviewPreset.Home4x2,
        WidgetPreviewPreset.Home4x3,
        WidgetPreviewPreset.LockMini,
    )

private val DemoTemplateGroups =
    listOf(
        TemplateGroup(
            id = "track",
            titleRes = R.string.track_template_entry,
            subtitleRes = R.string.track_template_entry_subtitle,
            pickerInstructionsRes = R.string.track_use_case_instructions,
            previewPresets = TrackPreviewPresets,
            defaultPreviewPreset = WidgetPreviewPreset.Home4x2,
            useCases =
                listOf(
                    TemplateUseCase(
                        id = "track_steps",
                        titleRes = R.string.track_steps_use_case_title,
                        subtitleRes = R.string.track_steps_use_case_subtitle,
                        remoteViewsWidgetName = TrackRemoteViewsWidgetReceiver.WIDGET_NAME,
                        remoteComposeWidgetName = TrackRemoteComposeWidgetReceiver.WIDGET_NAME,
                        buildPayload = { TrackSample.SampleData },
                    )
                ),
        ),
        TemplateGroup(
            id = "quick_actions",
            titleRes = R.string.quick_actions_template_entry,
            subtitleRes = R.string.quick_actions_template_entry_subtitle,
            pickerInstructionsRes = R.string.quick_actions_use_case_instructions,
            previewPresets = WidgetPreviewPreset.entries,
            defaultPreviewPreset = WidgetPreviewPreset.Home4x2,
            useCases =
                listOf(
                    TemplateUseCase(
                        id = "quick_actions_workouts",
                        titleRes = R.string.quick_actions_workouts_use_case_title,
                        subtitleRes = R.string.quick_actions_workouts_use_case_subtitle,
                        remoteViewsWidgetName = WorkoutsQuickActionsWidgetReceiver.WIDGET_NAME,
                        remoteComposeWidgetName = null,
                        remoteComposeUnavailableReasonRes =
                            R.string.remotecompose_quick_actions_unavailable,
                        buildPayload = { WorkoutsQuickActionsWidgetReceiver.SampleData },
                    ),
                    TemplateUseCase(
                        id = "quick_actions_contacts",
                        titleRes = R.string.quick_actions_contacts_use_case_title,
                        subtitleRes = R.string.quick_actions_contacts_use_case_subtitle,
                        remoteViewsWidgetName = ContactsQuickActionsWidgetReceiver.WIDGET_NAME,
                        remoteComposeWidgetName = null,
                        remoteComposeUnavailableReasonRes =
                            R.string.remotecompose_quick_actions_unavailable,
                        buildPayload = { ContactsQuickActionsWidgetReceiver.SampleData },
                    ),
                ),
        ),
    )

/**
 * Ensures picker previews are published at most once per process on a process-scoped IO scope so
 * mid-flight configuration changes do not cancel publication after [previewsPublished] flips true
 * or hit the platform `setWidgetPreview` rate limit on subsequent relaunches.
 */
private val previewsPublished = AtomicBoolean(false)
private val previewPublishScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Composable
private fun DemoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUseCaseId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!previewsPublished.compareAndSet(false, true)) return@LaunchedEffect
        val appContext = context.applicationContext ?: context
        previewPublishScope.launch {
            val manager = GlanceAdaptiveWidgetManager(appContext)
            for (group in DemoTemplateGroups) {
                for (useCase in group.useCases) {
                    val preview = useCase.buildPayload()
                    for (widgetName in useCase.previewWidgetNames()) {
                        publishPreviewSafely(manager, widgetName, preview)
                    }
                }
            }
        }
    }

    val selectedTemplate = DemoTemplateGroups.find { it.id == selectedTemplateId }
    val selectedUseCase = selectedTemplate?.useCases?.find { it.id == selectedUseCaseId }

    BackHandler(enabled = selectedTemplate != null) {
        if (selectedUseCase != null) {
            selectedUseCaseId = null
        } else {
            selectedTemplateId = null
        }
    }

    when {
        selectedTemplate != null && selectedUseCase != null -> {
            UseCaseConfigScreen(
                useCase = selectedUseCase,
                previewPresets = selectedTemplate.previewPresets,
                defaultPreviewPreset = selectedTemplate.defaultPreviewPreset,
                onBack = { selectedUseCaseId = null },
                onPin = { widgetName ->
                    scope.launch {
                        pinWidget(
                            context = context,
                            widgetName = widgetName,
                            payload = selectedUseCase.buildPayload(),
                        )
                    }
                },
            )
        }
        selectedTemplate != null -> {
            UseCasePickerScreen(
                template = selectedTemplate,
                onBack = { selectedTemplateId = null },
                onSelectUseCase = { useCase -> selectedUseCaseId = useCase.id },
            )
        }
        else ->
            HomeScreen(
                templates = DemoTemplateGroups,
                onSelectTemplate = { template ->
                    selectedTemplateId = template.id
                    selectedUseCaseId = null
                },
            )
    }
}

@Composable
private fun HomeScreen(
    templates: List<TemplateGroup>,
    onSelectTemplate: (TemplateGroup) -> Unit,
) {
    val itemModifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = itemModifier.padding(vertical = 8.dp),
            )
        }
        item {
            Text(
                stringResource(R.string.home_template_instructions),
                style = MaterialTheme.typography.bodyMedium,
                modifier = itemModifier,
            )
        }

        items(templates, key = { it.id }) { template ->
            Column(modifier = itemModifier) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                WidgetRow(
                    label = stringResource(template.titleRes),
                    subtitle = stringResource(template.subtitleRes),
                    onClick = { onSelectTemplate(template) },
                )
            }
        }
    }
}

@Composable
private fun UseCasePickerScreen(
    template: TemplateGroup,
    onBack: () -> Unit,
    onSelectUseCase: (TemplateUseCase) -> Unit,
) {
    val itemModifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Box(modifier = itemModifier) {
                ScreenHeader(title = stringResource(template.titleRes), onBack = onBack)
            }
        }
        item {
            Text(
                stringResource(template.pickerInstructionsRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = itemModifier,
            )
        }

        items(template.useCases, key = { it.id }) { useCase ->
            Column(modifier = itemModifier) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                WidgetRow(
                    label = stringResource(useCase.titleRes),
                    subtitle = stringResource(useCase.subtitleRes),
                    onClick = { onSelectUseCase(useCase) },
                )
            }
        }
    }
}

@Composable
private fun UseCaseConfigScreen(
    useCase: TemplateUseCase,
    previewPresets: List<WidgetPreviewPreset>,
    defaultPreviewPreset: WidgetPreviewPreset,
    onBack: () -> Unit,
    onPin: (widgetName: String) -> Unit,
) {
    val remoteComposeDisabledReasonRes = useCase.remoteComposeDisabledReasonRes()
    val remoteComposeAvailable = remoteComposeDisabledReasonRes == null

    var selectedBackend by
        rememberSaveable(useCase.id, remoteComposeAvailable) {
            mutableStateOf(
                if (remoteComposeAvailable) {
                    WidgetBackend.RemoteCompose
                } else {
                    WidgetBackend.RemoteViews
                }
            )
        }
    var selectedPreset by rememberSaveable(useCase.id) { mutableStateOf(defaultPreviewPreset) }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(title = stringResource(useCase.titleRes), onBack = onBack)

            Text(
                text = stringResource(useCase.subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            WidgetLivePreviewSection(
                useCaseKey = useCase.id,
                backend = selectedBackend,
                presets = previewPresets,
                selectedPreset = selectedPreset,
                onSelectPreset = { selectedPreset = it },
                buildPayload = useCase.buildPayload,
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.widget_config_renderer_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            BackendOptionRow(
                label = stringResource(R.string.widget_config_renderer_remotecompose),
                subtitle = remoteComposeDisabledReasonRes?.let { stringResource(it) },
                selected = selectedBackend == WidgetBackend.RemoteCompose,
                enabled = remoteComposeAvailable,
                onClick = { selectedBackend = WidgetBackend.RemoteCompose },
            )

            BackendOptionRow(
                label = stringResource(R.string.widget_config_renderer_remoteviews),
                subtitle = null,
                selected = selectedBackend == WidgetBackend.RemoteViews,
                enabled = true,
                onClick = { selectedBackend = WidgetBackend.RemoteViews },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onPin(useCase.resolveWidgetName(selectedBackend)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.widget_config_pin_button))
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.navigate_back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun BackendOptionRow(
    label: String,
    subtitle: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WidgetRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private suspend fun publishPreviewSafely(
    manager: CoreWidgetManager,
    widgetName: String,
    previewData: GlanceAdaptiveTemplate,
) {
    try {
        manager.setPreview(widgetName = widgetName, previewData = previewData)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        // Platform setWidgetPreview is rate-limited across rapid relaunches and may also surface
        // platform/Binder errors; keep the previously published or static XML preview rather than
        // crashing the demo.
        Log.w(TAG, "Failed to publish preview for $widgetName", e)
    }
}

private suspend fun pinWidget(
    context: Context,
    widgetName: String,
    payload: GlanceAdaptiveTemplate,
) {
    val pinned =
        try {
            GlanceAdaptiveWidgetManager(context)
                .requestPin(widgetName = widgetName, initialData = payload)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to pin widget $widgetName", e)
            false
        }
    if (!pinned) {
        Toast.makeText(context, R.string.pin_widget_failed, Toast.LENGTH_SHORT).show()
    }
}
