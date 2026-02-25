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
package androidx.glance.appwidget.demos

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClExpressiveToolbarAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    @Composable
    fun WidgetContent() {
        ClExpressiveToolbarLayout(
            centerButton =
                ClExpressiveToolBarButton(
                    iconRes = CLIcons.sampleAddIcon,
                    contentDescription = "Add notes",
                    onClick = ActionUtils.actionStartDemoActivity("add notes button"),
                ),
            cornerButtons =
                listOf(
                    ClExpressiveToolBarButton(
                        iconRes = CLIcons.sampleMicIcon,
                        contentDescription = "mic",
                        onClick = ActionUtils.actionStartDemoActivity("mic button"),
                    ),
                    ClExpressiveToolBarButton(
                        iconRes = CLIcons.sampleCameraIcon,
                        contentDescription = "camera",
                        onClick = ActionUtils.actionStartDemoActivity("camera button"),
                    ),
                    ClExpressiveToolBarButton(
                        iconRes = CLIcons.sampleShareIcon,
                        contentDescription = "share",
                        onClick = ActionUtils.actionStartDemoActivity("share button"),
                    ),
                    ClExpressiveToolBarButton(
                        iconRes = CLIcons.clSampleFileUploadIcon,
                        contentDescription = "file upload",
                        onClick = ActionUtils.actionStartDemoActivity("file upload button"),
                    ),
                ),
        )
    }
}

class ClExpressiveToolbarAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClExpressiveToolbarAppWidget()
}

@Composable
fun ClExpressiveToolbarLayout(
    centerButton: ClExpressiveToolBarButton,
    cornerButtons: List<ClExpressiveToolBarButton>,
) {
    checkCornerButtonsListSize(cornerButtons)

    val widgetSize = LocalSize.current
    val cookieBackgroundSize = widgetSize.height.coerceAtMost(widgetSize.width)

    val backgroundModifier =
        GlanceModifier.size(cookieBackgroundSize).appWidgetBackground().fourSidedCookieBackground()

    when (ClExpressiveToolbarLayoutSize.fromLocalSize()) {
        ClExpressiveToolbarLayoutSize.SMALL ->
            CenterButtonOnlyLayout(centerButton = centerButton, modifier = backgroundModifier)

        ClExpressiveToolbarLayoutSize.MEDIUM ->
            AllButtonsScaledLayout(
                centerButton = centerButton,
                cornerButtons = cornerButtons,
                cookieBackgroundSize = cookieBackgroundSize,
                modifier = backgroundModifier,
            )
    }
}

@Composable
private fun GlanceModifier.fourSidedCookieBackground(): GlanceModifier {
    return this.background(
        imageProvider = ImageProvider(CLIcons.clFourSideCookieBackground),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground),
    )
}

@Composable
private fun CenterButtonOnlyLayout(
    centerButton: ClExpressiveToolBarButton,
    modifier: GlanceModifier,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        CenterButton(
            button = centerButton,
            buttonBackgroundSize = 48.dp,
            clickableSize = 48.dp,
            iconSize = 24.dp,
            filled = false,
            shape = RoundedCornerShape.MEDIUM,
        )
    }
}

@Composable
private fun AllButtonsScaledLayout(
    centerButton: ClExpressiveToolBarButton,
    cornerButtons: List<ClExpressiveToolBarButton>,
    cookieBackgroundSize: Dp,
    modifier: GlanceModifier,
) {
    val buttonBackgroundSize =
        ClExpressiveToolBarLayoutDimens.scaledButtonBackground(cookieBackgroundSize)
    val iconSize = ClExpressiveToolBarLayoutDimens.scaledIconSize(cookieBackgroundSize)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        CenterButton(
            button = centerButton,
            buttonBackgroundSize = buttonBackgroundSize,
            clickableSize = ClExpressiveToolBarLayoutDimens.minCenterButtonTapTarget,
            iconSize = iconSize,
            filled = Build.VERSION.SDK_INT > Build.VERSION_CODES.S,
            shape = RoundedCornerShape.MEDIUM,
        )
        CornerButtonsGrid(
            cornerButtons = cornerButtons,
            buttonBackgroundSize = buttonBackgroundSize,
            iconSize = iconSize,
            clickableSize =
                buttonBackgroundSize.coerceAtLeast(
                    ClExpressiveToolBarLayoutDimens.minCornerButtonTapTarget
                ),
            modifier = GlanceModifier.fillMaxSize(),
        )
    }
}

data class ClExpressiveToolBarButton(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val onClick: Action,
    val text: String? = null,
)

@Composable
private fun CornerButtonsGrid(
    cornerButtons: List<ClExpressiveToolBarButton>,
    buttonBackgroundSize: Dp,
    clickableSize: Dp,
    iconSize: Dp,
    modifier: GlanceModifier,
) {
    checkCornerButtonsListSize(cornerButtons)

    TwoRowGrid(
        spacing = 0.dp,
        modifier = modifier,
        items =
            cornerButtons.map {
                {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.fillMaxSize(),
                    ) {
                        CornerButton(
                            toolBarButton = it,
                            buttonBackgroundSize = buttonBackgroundSize,
                            iconSize = iconSize,
                            clickableSize = clickableSize,
                        )
                    }
                }
            },
    )
}

@Composable
private fun CornerButton(
    toolBarButton: ClExpressiveToolBarButton,
    buttonBackgroundSize: Dp,
    clickableSize: Dp,
    iconSize: Dp,
) {
    IconButton(
        imageProvider = ImageProvider(toolBarButton.iconRes),
        contentDescription = toolBarButton.contentDescription,
        iconSize = iconSize,
        backgroundSize = buttonBackgroundSize,
        backgroundColor = ColorProvider(Color.Transparent, Color.Transparent),
        contentColor = GlanceTheme.colors.primary,
        roundedCornerShape = RoundedCornerShape.FULL,
        onClick = toolBarButton.onClick,
        modifier = GlanceModifier.size(clickableSize),
    )
}

@Composable
private fun CenterButton(
    button: ClExpressiveToolBarButton,
    clickableSize: Dp,
    buttonBackgroundSize: Dp,
    iconSize: Dp,
    shape: RoundedCornerShape,
    filled: Boolean,
) {
    val backgroundColor =
        if (filled) {
            GlanceTheme.colors.tertiary
        } else {
            ColorProvider(Color.Transparent, Color.Transparent)
        }

    val contentColor =
        if (filled) {
            GlanceTheme.colors.onTertiary
        } else {
            GlanceTheme.colors.primary
        }

    IconButton(
        imageProvider = ImageProvider(button.iconRes),
        contentDescription = button.contentDescription,
        iconSize = iconSize,
        backgroundSize = buttonBackgroundSize,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        roundedCornerShape = shape,
        onClick = button.onClick,
        modifier = GlanceModifier.size(clickableSize),
    )
}

@Composable
private fun IconButton(
    imageProvider: ImageProvider,
    contentDescription: String,
    backgroundSize: Dp,
    iconSize: Dp,
    roundedCornerShape: RoundedCornerShape,
    onClick: Action,
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    contentColor: ColorProvider,
) {
    Box( // clickable area
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .cornerRadius(roundedCornerShape.cornerRadius)
                .semantics { this.contentDescription = contentDescription }
                .clickable(onClick),
    ) {
        Box( // colored background
            contentAlignment = Alignment.Center,
            modifier =
                GlanceModifier.size(backgroundSize)
                    .background(backgroundColor)
                    .cornerRadius(roundedCornerShape.cornerRadius),
        ) {
            Image(
                provider = imageProvider,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = GlanceModifier.size(iconSize),
            )
        }
    }
}

private enum class ClExpressiveToolbarLayoutSize {
    SMALL,
    MEDIUM;

    companion object {
        @Composable
        fun fromLocalSize(): ClExpressiveToolbarLayoutSize {
            val size = LocalSize.current
            val boxSize = size.width.coerceAtMost(size.height)

            return when {
                boxSize < 140.dp -> SMALL
                else -> MEDIUM
            }
        }
    }
}

private object ClExpressiveToolBarLayoutDimens {
    val minCornerButtonTapTarget = 48.dp
    val minCenterButtonTapTarget = 60.dp

    fun scaledIconSize(backgroundSize: Dp) = Dp((13 * backgroundSize.value) / 100)

    fun scaledButtonBackground(backgroundSize: Dp) = Dp((26 * backgroundSize.value) / 100)
}

private fun checkCornerButtonsListSize(cornerButtons: List<ClExpressiveToolBarButton>) {
    if (cornerButtons.size != 4) {
        Log.w(
            "ClExpressiveToolbarLayout",
            "Expected 4 corner buttons, but passed ${cornerButtons.size}",
        )
    }
}
