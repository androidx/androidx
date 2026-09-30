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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.integration.demos.bookends.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.remote.player.compose.embedded.ColorProperty
import androidx.compose.remote.player.compose.embedded.CustomComposablePlugin
import androidx.compose.remote.player.compose.embedded.CustomPluginRegistry
import androidx.compose.remote.player.compose.embedded.IntProperty
import androidx.compose.remote.player.compose.embedded.LocalRcImageLoader
import androidx.compose.remote.player.compose.embedded.RcCustomComponent
import androidx.compose.remote.player.compose.embedded.StringProperty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeSource
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.remote.integration.demos.bookends.BookendsSpec

/*
 * The player-side bookend: CustomComposablePlugins for the embedded player that render the
 * `Custom` components emitted by the `material3` package with the Wear Compose Material3
 * components, using the host's default theme.
 */

/** The scroll state of the enclosing ScreenScaffold, shared with its list and scroll indicator. */
private val LocalScrollState = compositionLocalOf<TransformingLazyColumnState?> { null }

/** The content padding provided by the enclosing ScreenScaffold. */
private val LocalContentPadding = compositionLocalOf<PaddingValues?> { null }

/** The transformation for the current list item, applied by surface components (e.g. Button). */
private val LocalSurfaceTransformation = compositionLocalOf<SurfaceTransformation?> { null }

private val SlotId = IntProperty(BookendsSpec.PROP_SLOT_ID, default = -1)

/** Returns the slot child of this component with [slotId], if present. */
private fun RcCustomComponent.slot(slotId: Int): RcCustomComponent? {
    for (i in 0 until childCount) {
        val child = customChild(i) ?: continue
        if (child.config == BookendsSpec.SLOT && child.ints[SlotId.id] == slotId) return child
    }
    return null
}

/** Base class for plugins that work directly from the [RcCustomComponent]. */
private abstract class WearPlugin(override val name: String) :
    CustomComposablePlugin<RcCustomComponent> {
    @Composable override fun extract(component: RcCustomComponent): RcCustomComponent = component
}

private object AppScaffoldPlugin : WearPlugin(BookendsSpec.APP_SCAFFOLD) {
    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val timeText = component.slot(BookendsSpec.SLOT_TIME_TEXT)
        val content = component.slot(BookendsSpec.SLOT_CONTENT)
        AppScaffold(modifier = modifier.fillMaxSize(), timeText = { timeText?.Children() }) {
            content?.Children()
        }
    }
}

private object ScreenScaffoldPlugin : WearPlugin(BookendsSpec.SCREEN_SCAFFOLD) {
    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val timeText = component.slot(BookendsSpec.SLOT_TIME_TEXT)
        val scrollIndicator = component.slot(BookendsSpec.SLOT_SCROLL_INDICATOR)
        val content = component.slot(BookendsSpec.SLOT_CONTENT)
        val scrollState = rememberTransformingLazyColumnState()

        CompositionLocalProvider(LocalScrollState provides scrollState) {
            ScreenScaffold(
                scrollState = scrollState,
                modifier = modifier.fillMaxSize(),
                timeText = timeText?.let { slot -> { slot.Children() } },
                scrollIndicator =
                    scrollIndicator?.let { slot ->
                        { Box(Modifier.align(Alignment.CenterEnd)) { slot.Children() } }
                    },
            ) { contentPadding ->
                CompositionLocalProvider(LocalContentPadding provides contentPadding) {
                    content?.Children()
                }
            }
        }
    }
}

private object TimeTextPlugin : WearPlugin(BookendsSpec.TIME_TEXT) {
    private val Time = StringProperty(BookendsSpec.PROP_TEXT)

    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        if (!component.hasProperty(Time)) {
            CompositionLocalProvider(
                LocalContext provides LocalContext.current.applicationContext
            ) {
                TimeText(modifier = modifier)
            }
        } else {
            val time by component.textState(Time)
            val timeSource =
                remember(time) {
                    object : TimeSource {
                        @Composable override fun currentTime(): String = time
                    }
                }
            TimeText(modifier = modifier, timeSource = timeSource)
        }
    }
}

private object ScrollIndicatorPlugin : WearPlugin(BookendsSpec.SCROLL_INDICATOR) {
    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val scrollState = LocalScrollState.current ?: return
        ScrollIndicator(state = scrollState, modifier = modifier)
    }
}

private object TransformingLazyColumnPlugin : WearPlugin(BookendsSpec.TRANSFORMING_LAZY_COLUMN) {
    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val scrollState = LocalScrollState.current ?: rememberTransformingLazyColumnState()
        val contentPadding = LocalContentPadding.current ?: PaddingValues()
        val transformationSpec = rememberTransformationSpec()

        TransformingLazyColumn(
            modifier = modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = contentPadding,
        ) {
            items(component.childCount) { index ->
                val item = component.customChild(index)
                CompositionLocalProvider(
                    LocalSurfaceTransformation provides SurfaceTransformation(transformationSpec)
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .transformedHeight(this@items, transformationSpec),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item != null) item.Children() else component.Child(index)
                    }
                }
            }
        }
    }
}

private object ButtonPlugin : WearPlugin(BookendsSpec.BUTTON) {
    private val Enabled = IntProperty(BookendsSpec.PROP_ENABLED, default = 1)

    // Clicks are dispatched from the Material3 Button rather than a wrapping clickable.
    override val handlesClick: Boolean = true

    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val enabled by component.intState(Enabled)
        val label = component.slot(BookendsSpec.SLOT_LABEL)
        val secondaryLabel = component.slot(BookendsSpec.SLOT_SECONDARY_LABEL)
        val icon = component.slot(BookendsSpec.SLOT_ICON)

        Button(
            onClick = { component.onClick?.invoke() },
            modifier = modifier.fillMaxWidth(),
            onLongClick = component.onLongClick,
            enabled = enabled != 0,
            secondaryLabel = secondaryLabel?.let { slot -> { slot.Children() } },
            icon = icon?.let { slot -> { slot.Children() } },
            transformation = LocalSurfaceTransformation.current,
            label = { label?.Children() },
        )
    }
}

private object IconPlugin : WearPlugin(BookendsSpec.ICON) {
    private val BitmapId = IntProperty(BookendsSpec.PROP_BITMAP, default = -1)
    private val ContentDescription = StringProperty(BookendsSpec.PROP_CONTENT_DESCRIPTION)
    private val Tint = ColorProperty(BookendsSpec.PROP_TINT)

    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val bitmapId by component.intState(BitmapId)
        val contentDescription by component.textState(ContentDescription)
        val hasContentDescription = component.hasProperty(ContentDescription)
        val tint by component.colorState(Tint)
        val drawable by LocalRcImageLoader.current.loadImage(bitmapId)
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
        val painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }

        Icon(
            painter = painter,
            contentDescription = if (hasContentDescription) contentDescription else null,
            modifier = modifier.size(ButtonDefaults.IconSize),
            tint = tint,
        )
    }
}

private object TextPlugin : WearPlugin(BookendsSpec.TEXT) {
    private val TextValue = StringProperty(BookendsSpec.PROP_TEXT)
    private val TextColor = ColorProperty(BookendsSpec.PROP_COLOR)
    private val TextAlignValue = IntProperty(BookendsSpec.PROP_TEXT_ALIGN, default = 0)
    // 0 means unset: use the limit of the enclosing Material3 component.
    private val MaxLines = IntProperty(BookendsSpec.PROP_MAX_LINES, default = 0)

    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val text by component.textState(TextValue)
        val color by component.colorState(TextColor)
        val textAlign by component.intState(TextAlignValue)
        val maxLines by component.intState(MaxLines)
        val align = BookendsSpec.textAlignFromWire(textAlign)
        val resolvedAlign = align ?: LocalTextConfiguration.current.textAlign
        // Note: Wear Compose Material3 Button centers a single label by providing
        // LocalTextConfiguration(textAlign = TextAlign.Center) inside a Row with Arrangement.Start
        // (whereas RemoteMaterial3 RemoteButton uses RemoteRow(RemoteArrangement.Center)). For
        // TextAlign.Center or TextAlign.End from LocalTextConfiguration to take effect inside a
        // Row, the Text must fill the available width.
        val fillWidth =
            align != null || resolvedAlign == TextAlign.Center || resolvedAlign == TextAlign.End

        Text(
            text = text,
            modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
            color = color,
            textAlign = resolvedAlign,
            maxLines = if (maxLines > 0) maxLines else LocalTextConfiguration.current.maxLines,
        )
    }
}

/** The plugins rendering the bookends Wear Material3 components with Wear Compose Material3. */
internal val WearMaterial3Plugins: CustomPluginRegistry =
    CustomPluginRegistry(
        AppScaffoldPlugin,
        ScreenScaffoldPlugin,
        TimeTextPlugin,
        ScrollIndicatorPlugin,
        TransformingLazyColumnPlugin,
        ButtonPlugin,
        IconPlugin,
        TextPlugin,
    )
