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

package androidx.glance.appwidget.components

import android.os.Build
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.NoRippleOverride
import androidx.glance.action.clickable
import androidx.glance.appwidget.R
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.enabled
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Remote view backend rendering for material buttons. */
internal object RemoteViewButtons {

    @Composable
    internal fun M3TextButton(
        text: String,
        onClick: Action,
        modifier: GlanceModifier,
        enabled: Boolean = true,
        icon: ImageProvider?,
        contentColor: ColorProvider,
        @DrawableRes backgroundResource: Int,
        backgroundTint: ColorProvider,
        maxLines: Int,
    ) {
        val iconSize = 18.dp
        val totalHorizontalPadding = if (icon != null) 24.dp else 16.dp

        val Text =
            @Composable {
                Text(
                    text = text,
                    style = TextStyle(color = contentColor, fontSize = 14.sp, FontWeight.Medium),
                    maxLines = maxLines,
                )
            }

        Box(
            modifier =
                modifier
                    .padding(
                        start = 16.dp,
                        end = totalHorizontalPadding,
                        top = 10.dp,
                        bottom = 10.dp,
                    )
                    .background(
                        imageProvider = ImageProvider(backgroundResource),
                        colorFilter = ColorFilter.tint(backgroundTint),
                    )
                    .enabled(enabled)
                    .clickable(
                        onClick = onClick,
                        rippleOverride =
                            if (isAtLeastApi31) NoRippleOverride
                            else R.drawable.glance_component_m3_button_ripple,
                    )
                    .then(maybeRoundCorners(R.dimen.glance_component_button_corners)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Image(
                        provider = icon,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier = GlanceModifier.size(iconSize),
                    ) // TODO: do we need a content description for a button icon?
                    Spacer(GlanceModifier.width(8.dp))
                    Text()
                }
            } else {
                Box(GlanceModifier.size(iconSize)) {
                    // for accessibility only: force button to be the same min height as the icon
                    // version.
                    // remove once b/290677181 is addressed
                }
                Text()
            }
        }
    }

    @Composable
    fun M3IconButton(
        imageProvider: ImageProvider,
        contentDescription: String?,
        contentColor: ColorProvider,
        backgroundColor: ColorProvider?,
        shape: IconButtonShape,
        onClick: Action,
        modifier: GlanceModifier,
        enabled: Boolean,
    ) {

        val backgroundModifier =
            if (backgroundColor == null) GlanceModifier
            else
                GlanceModifier.background(
                    ImageProvider(shape.shape),
                    colorFilter = ColorFilter.tint(backgroundColor),
                )

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                GlanceModifier.size(
                        shape.defaultSize
                    ) // acts as a default if not overridden by [modifier]
                    .then(modifier)
                    .then(backgroundModifier)
                    .clickable(onClick = onClick, rippleOverride = shape.ripple)
                    .enabled(enabled)
                    .then(maybeRoundCorners(shape.cornerRadius)),
        ) {
            Image(
                provider = imageProvider,
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = GlanceModifier.size(24.dp),
            )
        }
    }
}

private fun maybeRoundCorners(@DimenRes radius: Int) =
    if (isAtLeastApi31) GlanceModifier.cornerRadius(radius) else GlanceModifier

internal val isAtLeastApi31
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
