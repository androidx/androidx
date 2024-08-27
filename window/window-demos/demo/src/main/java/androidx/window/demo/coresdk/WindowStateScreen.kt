/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.demo.coresdk

import android.graphics.Rect
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.demo.R
import androidx.window.demo.common.DemoTheme
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.launch

/** Composes the main screen for displaying window state information. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WindowStateScreen(viewModel: WindowStateViewModel = viewModel()) {
    val windowStates by viewModel.windowStates.collectAsState()
    val listState = rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Window State Callbacks") },
                actions = {
                    IconButton(onClick = viewModel::clearWindowStates) {
                        Icon(Icons.Filled.Delete, "Clear list")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
            )
        },
        floatingActionButton = {
            val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
            if (showButton) {
                val coroutineScope = rememberCoroutineScope()
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    onClick = { coroutineScope.launch { listState.scrollToItem(0) } },
                ) {
                    Text("Back to latest!", modifier = Modifier.padding(8.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        WindowStateList(
            windowStates,
            padding,
            listState = listState,
            onWindowStateItemClick = viewModel::onWindowStateItemClick,
        )
    }
}

/** Previews composable for the [WindowStateScreen] in both light and dark modes. */
@PreviewLightDark
@Composable
fun WindowStateScreenPreview() {
    val windowStates =
        listOf(
            WindowState(
                name = stringResource(R.string.application_configuration_title),
                applicationDisplayRotation = ROTATION_270,
                activityDisplayRotation = ROTATION_270,
                applicationDisplayBounds = Rect(0, 0, 2208, 1840),
                activityDisplayBounds = Rect(0, 0, 2208, 1840),
            ),
            WindowState(
                name = stringResource(R.string.activity_display_listener_title),
                applicationDisplayRotation = ROTATION_270,
                activityDisplayRotation = ROTATION_270,
                applicationDisplayBounds = Rect(0, 0, 2208, 1840),
                activityDisplayBounds = Rect(0, 0, 2208, 1840),
            ),
            WindowState(
                name = stringResource(R.string.display_feature_title),
                applicationDisplayBounds = Rect(0, 0, 960, 2142),
                activityDisplayBounds = Rect(0, 0, 960, 2142),
            ),
            WindowState(
                name = stringResource(R.string.latest_configuration_title),
                applicationDisplayBounds = Rect(0, 0, 960, 2142),
                activityDisplayBounds = Rect(0, 0, 960, 2142),
            ),
        )
    DemoTheme { WindowStateScreen(viewModel = viewModel { WindowStateViewModel(windowStates) }) }
}

/**
 * Composes a scrollable list of [WindowStateCard] items.
 *
 * @param windowStates list of [WindowState] objects to display.
 * @param contentPadding padding to apply to the lazy list.
 * @param listState state object for the lazy list.
 * @param onWindowStateItemClick callback when a [WindowState] item is clicked.
 */
@Composable
fun WindowStateList(
    windowStates: List<WindowState>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    onWindowStateItemClick: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
        state = listState,
    ) {
        itemsIndexed(windowStates) { index, state ->
            WindowStateCard(
                number = windowStates.size - index,
                state = state,
                lastState = windowStates.getOrNull(index + 1) ?: state,
                toggleExpand = { onWindowStateItemClick(index) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Composes a card displaying information about a [WindowState].
 *
 * @param number the number to display for this [WindowState].
 * @param state the [WindowState] to display.
 * @param lastState the previous [WindowState] for comparison.
 * @param toggleExpand callback to toggle the expanded state of the card.
 * @param modifier modifier for this card.
 */
@Composable
private fun WindowStateCard(
    number: Int,
    state: WindowState,
    lastState: WindowState,
    toggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded = state.isDetailsExpanded
    Card(modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
        Row(
            modifier =
                Modifier.padding(horizontal = 8.dp)
                    .animateContentSize(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(0.85f), verticalAlignment = Alignment.CenterVertically) {
                Text("#$number")
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    if (!isExpanded) {
                        WindowStateSummary(state, lastState)
                    } else {
                        WindowStateDetail(state, lastState)
                    }
                }
            }
            IconButton(onClick = toggleExpand, modifier = Modifier.weight(0.07f)) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Show less" else "Show more",
                )
            }
        }
    }
}

/**
 * Composes a summary view for a collapsed [WindowStateCard].
 *
 * @param state the [WindowState] to display.
 * @param lastState the previous [WindowState] for comparison.
 */
@Composable
private fun WindowStateSummary(state: WindowState, lastState: WindowState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            state.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            state.timeStamp.toSimpleTimeStr(),
            style = MaterialTheme.typography.bodySmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
    Text(
        buildAnnotatedString {
            rotationString(
                state.applicationDisplayRotation,
                highlight =
                    state.applicationDisplayRotation != lastState.applicationDisplayRotation,
            )
            append(" / ")
            rotationString(
                state.activityDisplayRotation,
                highlight = state.activityDisplayRotation != lastState.activityDisplayRotation,
            )
            append(" / ")
            boundsString(
                state.applicationDisplayBounds,
                highlight = state.applicationDisplayBounds != lastState.applicationDisplayBounds,
            )
            append(" / ")
            boundsString(
                state.activityDisplayBounds,
                highlight = state.activityDisplayBounds != lastState.activityDisplayBounds,
            )
        },
        style = MaterialTheme.typography.bodySmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

/**
 * Composes a detailed view for an expanded [WindowStateCard].
 *
 * @param state the [WindowState] to display.
 * @param lastState the previous [WindowState] for comparison.
 */
@Composable
private fun WindowStateDetail(state: WindowState, lastState: WindowState) {
    val timestampTitle = stringResource(R.string.timestamp_title)
    val applicationRotationTitle = stringResource(R.string.application_display_rotation_title)
    val activityRotationTitle = stringResource(R.string.activity_display_rotation_title)
    val applicationBoundsTittle = stringResource(R.string.application_display_bounds_title)
    val activityBoundsTittle = stringResource(R.string.activity_display_bounds_title)

    Text(
        state.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
    )
    Column {
        Text(
            "$timestampTitle ${state.timeStamp}",
            style = MaterialTheme.typography.bodySmall,
        )
        DisplayRotationView(
            applicationRotationTitle,
            currentRotation = state.applicationDisplayRotation,
            lastRotation = lastState.applicationDisplayRotation,
        )
        DisplayRotationView(
            activityRotationTitle,
            currentRotation = state.activityDisplayRotation,
            lastRotation = lastState.activityDisplayRotation,
        )
        DisplayBoundsView(
            applicationBoundsTittle,
            currentBounds = state.applicationDisplayBounds,
            lastBound = lastState.applicationDisplayBounds,
        )
        DisplayBoundsView(
            activityBoundsTittle,
            currentBounds = state.activityDisplayBounds,
            lastBound = lastState.activityDisplayBounds,
        )
        Text(
            "Callback details: ${state.callbackDetails}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Composes a view for displaying rotation information in [WindowStateDetail].
 *
 * @param title the title for this rotation information.
 * @param currentRotation the current rotation value to display.
 * @param lastRotation the previous rotation value for comparison.
 */
@Composable
private fun DisplayRotationView(title: String, currentRotation: Int, lastRotation: Int) {
    val shouldHighlightChange = currentRotation != lastRotation
    Row {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Text(
            buildAnnotatedString { rotationString(currentRotation, shouldHighlightChange) },
            style = MaterialTheme.typography.bodySmall,
        )
        if (shouldHighlightChange) {
            Text(
                text = lastRotation.toRotationStr(),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough,
                    ),
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/** Formats a rotation value into an annotated string. */
private fun AnnotatedString.Builder.rotationString(rotation: Int, highlight: Boolean) {
    val color = if (highlight) Color.Red else Color.Unspecified
    withStyle(style = SpanStyle(color = color)) { append(rotation.toRotationStr()) }
}

/**
 * Composes a view for displaying bounds information in [WindowStateDetail].
 *
 * @param title the title for this bounds information.
 * @param currentBounds the current bounds value to display.
 * @param lastBound the previous bounds value for comparison.
 */
@Composable
private fun DisplayBoundsView(title: String, currentBounds: Rect, lastBound: Rect) {
    val shouldHighlightChange = currentBounds != lastBound
    Row {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Column {
            Text(
                buildAnnotatedString { boundsString(currentBounds, shouldHighlightChange) },
                style = MaterialTheme.typography.bodySmall,
            )
            if (shouldHighlightChange) {
                Text(
                    text = "$lastBound",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough,
                        ),
                )
            }
        }
    }
}

/** Formats a bounds value into an annotated string. */
private fun AnnotatedString.Builder.boundsString(bound: Rect, highlight: Boolean) {
    val color = if (highlight) Color.Red else Color.Unspecified
    withStyle(style = SpanStyle(color = color)) { append("$bound") }
}

/** Converts an integer rotation value to a human-readable string representation. */
private fun Int.toRotationStr(): String =
    when (this) {
        ROTATION_0 -> "0°"
        ROTATION_90 -> "90°"
        ROTATION_180 -> "180°"
        ROTATION_270 -> "270°"
        else -> "Unknown"
    }

/** Converts a [Date] to a simple time string for summary view. */
private fun Date.toSimpleTimeStr(): String = SimpleDateFormat("(HH:mm:ss)").format(this)
