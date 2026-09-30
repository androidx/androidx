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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeSource
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.remote.integration.demos.bookends.BookendsSpec

/*
 * The player-side bookend: plugins for the embedded player (`RcPlayer`) that render the `Custom`
 * components emitted by the `material3` package with the Wear Compose Material3 components.
 */

/** Base class for plugins that work directly from the [RcCustomComponent]. */
private abstract class WearPlugin(override val name: String) :
    CustomComposablePlugin<RcCustomComponent> {
    @Composable override fun extract(component: RcCustomComponent): RcCustomComponent = component
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

private object ButtonPlugin : WearPlugin(BookendsSpec.BUTTON) {
    private val Enabled = IntProperty(BookendsSpec.PROP_ENABLED, default = 1)
    private val Label = StringProperty(BookendsSpec.PROP_LABEL)
    private val SecondaryLabel = StringProperty(BookendsSpec.PROP_SECONDARY_LABEL)
    private val IconBitmap = IntProperty(BookendsSpec.PROP_ICON, default = -1)

    // Clicks are dispatched from the Material3 Button rather than a wrapping clickable.
    override val handlesClick: Boolean = true

    @Composable
    override fun Content(
        data: RcCustomComponent,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        val enabled by component.intState(Enabled)
        val label by component.textState(Label)
        val secondaryLabel by component.textState(SecondaryLabel)
        val iconBitmap by component.intState(IconBitmap)
        val hasSecondaryLabel = component.hasProperty(SecondaryLabel)

        Button(
            onClick = { component.onClick?.invoke() },
            modifier = modifier.fillMaxWidth(),
            onLongClick = component.onLongClick,
            enabled = enabled != 0,
            secondaryLabel = if (hasSecondaryLabel) ({ Text(secondaryLabel) }) else null,
            icon = if (iconBitmap != -1) ({ BitmapIcon(iconBitmap) }) else null,
            label = {
                // Note: Wear Compose Material3 Button centers a single label by providing
                // LocalTextConfiguration(textAlign = TextAlign.Center) inside a Row with
                // Arrangement.Start (whereas RemoteMaterial3 RemoteButton uses
                // RemoteRow(RemoteArrangement.Center)). For TextAlign.Center to take effect inside
                // a Row, the Text must fill the available width.
                val align = LocalTextConfiguration.current.textAlign
                val fillWidth = align == TextAlign.Center || align == TextAlign.End
                Text(label, modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            },
        )
    }
}

/** An icon showing the document bitmap [bitmapId], resolved by the player's image loader. */
@Composable
private fun BitmapIcon(bitmapId: Int) {
    val drawable by LocalRcImageLoader.current.loadImage(bitmapId)
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
    val painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    // Untinted, like the bitmap drawn by the remote-material3 implementation.
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
        tint = Color.Unspecified,
    )
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
    CustomPluginRegistry(TimeTextPlugin, ButtonPlugin, TextPlugin)
