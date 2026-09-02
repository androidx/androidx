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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material3.TextConfiguration

@Composable
@RemoteComposable
internal fun RemoteSelectionButtonImpl(
    onClick: Action,
    modifier: RemoteModifier,
    enabled: RemoteBoolean,
    shape: RemoteShape,
    containerColor: RemoteColor,
    contentColor: RemoteColor,
    secondaryContentColor: RemoteColor,
    contentPadding: RemotePaddingValues,
    border: RemoteDp?,
    borderColor: RemoteColor?,
    role: Role,
    icon: (@Composable @RemoteComposable () -> Unit)?,
    secondaryLabel: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)?,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
    selectionControl: @Composable @RemoteComposable () -> Unit,
) {
    val containerModifier =
        RemoteModifier.clip(shape = shape)
            .clickable(
                action = onClick,
                enabled = (enabled.constantValueOrNull ?: false) && onClick != Action.Empty,
            )
            .padding(contentPadding)
            .semantics(mergeDescendants = true) { this.role = role }

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        modifier =
            modifier
                .buttonSizeModifier()
                .drawWithContent {
                    drawShapedBackground(
                        shape = shape,
                        color = containerColor,
                        enabled = enabled,
                        containerPainter = null,
                        disabledContainerPainter = null,
                        borderColor = borderColor,
                        borderStrokeWidth = border,
                    )
                    drawContent()
                }
                .then(containerModifier),
    ) {
        if (icon != null) {
            RemoteBox(
                modifier = RemoteModifier.wrapContentSize(),
                contentAlignment = RemoteAlignment.Center,
                content = icon,
            )
            RemoteBox(RemoteModifier.size(6.rdp))
        }
        RemoteColumn(modifier = RemoteModifier.weight(1f.rf)) {
            RemoteRow(
                content =
                    provideScopeContent(
                        contentColor = contentColor,
                        textStyle = RemoteMaterialTheme.typography.labelMedium,
                        textConfiguration =
                            TextConfiguration(
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                            ),
                        content = label,
                    )
            )
            if (secondaryLabel != null) {
                RemoteBox(RemoteModifier.size(1.rdp))
                RemoteRow(
                    content =
                        provideScopeContent(
                            contentColor = secondaryContentColor,
                            textStyle = RemoteMaterialTheme.typography.labelSmall,
                            textConfiguration =
                                TextConfiguration(
                                    textAlign = TextAlign.Start,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                ),
                            content = secondaryLabel,
                        )
                )
            }
        }
        RemoteBox(RemoteModifier.size(6.rdp))
        RemoteBox(
            modifier = RemoteModifier.wrapContentSize(),
            contentAlignment = RemoteAlignment.Center,
            content = selectionControl,
        )
    }
}

@Composable
@RemoteComposable
internal fun RemoteCheckboxControl(
    checked: RemoteBoolean,
    boxColor: RemoteColor,
    checkmarkColor: RemoteColor,
    modifier: RemoteModifier = RemoteModifier,
    progress: RemoteFloat = checked.select(1f.rf, 0f.rf),
) {
    RemoteCanvas(modifier = modifier.size(24.rdp)) {
        val topCornerPx = 3.rdp.toPx()
        val boxSizePx = 18.rdp.toPx()
        val cornerRadiusPx = 2.rdp.toPx()
        val strokeWidthPx = 2.rdp.toPx()

        val halfStrokeWidthPx = strokeWidthPx / 2f.rf
        val strokePaint = RemotePaint {
            style = PaintingStyle.Stroke
            strokeWidth = strokeWidthPx
            color = tween(boxColor, Color.Transparent.rc, progress)
        }
        drawRoundRect(
            paint = strokePaint,
            topLeft =
                RemoteOffset(topCornerPx + halfStrokeWidthPx, topCornerPx + halfStrokeWidthPx),
            size = RemoteSize(boxSizePx - strokeWidthPx, boxSizePx - strokeWidthPx),
            cornerRadius =
                RemoteOffset(
                    cornerRadiusPx - halfStrokeWidthPx,
                    cornerRadiusPx - halfStrokeWidthPx,
                ),
        )

        val fillPaint = RemotePaint {
            style = PaintingStyle.Fill
            color = tween(Color.Transparent.rc, boxColor, progress)
        }
        drawRoundRect(
            paint = fillPaint,
            topLeft = RemoteOffset(topCornerPx, topCornerPx),
            size = RemoteSize(boxSizePx, boxSizePx),
            cornerRadius = RemoteOffset(cornerRadiusPx, cornerRadiusPx),
        )

        val tickPaint = RemotePaint {
            style = PaintingStyle.Stroke
            strokeWidth = strokeWidthPx
            strokeCap = StrokeCap.Round
            color = tween(Color.Transparent.rc, checkmarkColor, progress)
        }
        drawLine(
            paint = tickPaint,
            start = RemoteOffset(7.4f.rdp.toPx(), 13.0f.rdp.toPx()),
            end = RemoteOffset(9.9f.rdp.toPx(), 15.5f.rdp.toPx()),
        )
        drawLine(
            paint = tickPaint,
            start = RemoteOffset(9.9f.rdp.toPx(), 15.5f.rdp.toPx()),
            end = RemoteOffset(16.5f.rdp.toPx(), 9.1f.rdp.toPx()),
        )
    }
}

@Composable
@RemoteComposable
internal fun RemoteRadioControl(
    selected: RemoteBoolean,
    controlColor: RemoteColor,
    modifier: RemoteModifier = RemoteModifier,
    progress: RemoteFloat = selected.select(1f.rf, 0f.rf),
) {
    RemoteCanvas(modifier = modifier.size(24.rdp)) {
        val centerOffset = RemoteOffset(12.rdp.toPx(), 12.rdp.toPx())
        val outerRadiusPx = 9.rdp.toPx()
        val strokeWidthPx = 2.rdp.toPx()
        val innerRadiusPx = lerp(0f.rf, 5.rdp.toPx(), progress)

        val ringPaint = RemotePaint {
            style = PaintingStyle.Stroke
            strokeWidth = strokeWidthPx
            color = controlColor
        }
        drawCircle(paint = ringPaint, radius = outerRadiusPx, center = centerOffset)

        val dotPaint = RemotePaint {
            style = PaintingStyle.Fill
            color = tween(Color.Transparent.rc, controlColor, progress)
        }
        drawCircle(paint = dotPaint, radius = innerRadiusPx, center = centerOffset)
    }
}

@Composable
@RemoteComposable
internal fun RemoteSwitchControl(
    checked: RemoteBoolean,
    trackColor: RemoteColor,
    trackBorderColor: RemoteColor,
    thumbColor: RemoteColor,
    thumbIconColor: RemoteColor,
    modifier: RemoteModifier = RemoteModifier,
    progress: RemoteFloat = checked.select(1f.rf, 0f.rf),
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    RemoteCanvas(modifier = modifier.size(32.rdp, 24.rdp)) {
        val trackWidthPx = 32.rdp.toPx()
        val trackHeightPx = 22.rdp.toPx()
        val trackRadiusPx = trackHeightPx / 2f.rf
        val trackTopPx = 1.rdp.toPx()
        val strokeWidthPx = 2.rdp.toPx()

        // Track fill
        val trackPaint = RemotePaint {
            style = PaintingStyle.Fill
            color = trackColor
        }
        drawRoundRect(
            paint = trackPaint,
            topLeft = RemoteOffset(0f.rf, trackTopPx),
            size = RemoteSize(trackWidthPx, trackHeightPx),
            cornerRadius = RemoteOffset(trackRadiusPx, trackRadiusPx),
        )

        // Track border
        val borderPaint = RemotePaint {
            style = PaintingStyle.Stroke
            strokeWidth = strokeWidthPx
            color = trackBorderColor
        }
        val insetPx = strokeWidthPx / 2f.rf
        drawRoundRect(
            paint = borderPaint,
            topLeft = RemoteOffset(insetPx, trackTopPx + insetPx),
            size = RemoteSize(trackWidthPx - strokeWidthPx, trackHeightPx - strokeWidthPx),
            cornerRadius = RemoteOffset(trackRadiusPx - insetPx, trackRadiusPx - insetPx),
        )

        // Thumb
        val thumbRadiusPx = lerp(6.rdp.toPx(), 9.rdp.toPx(), progress)
        val startThumbXPx = (if (isRtl) 21.rdp else 11.rdp).toPx()
        val endThumbXPx = (if (isRtl) 11.rdp else 21.rdp).toPx()
        val thumbCenterXPx = lerp(startThumbXPx, endThumbXPx, progress)
        val thumbCenterYPx = 12.rdp.toPx()
        val thumbCenter = RemoteOffset(thumbCenterXPx, thumbCenterYPx)

        val thumbPaint = RemotePaint {
            style = PaintingStyle.Fill
            color = thumbColor
        }
        drawCircle(paint = thumbPaint, radius = thumbRadiusPx, center = thumbCenter)

        // Thumb tick icon
        val tickPaint = RemotePaint {
            style = PaintingStyle.Stroke
            strokeWidth = strokeWidthPx
            strokeCap = StrokeCap.Round
            color = thumbIconColor
        }
        drawLine(
            paint = tickPaint,
            start =
                RemoteOffset(thumbCenterXPx - 4.6f.rdp.toPx(), thumbCenterYPx + 1.0f.rdp.toPx()),
            end = RemoteOffset(thumbCenterXPx - 2.0f.rdp.toPx(), thumbCenterYPx + 3.5f.rdp.toPx()),
        )
        drawLine(
            paint = tickPaint,
            start =
                RemoteOffset(thumbCenterXPx - 2.0f.rdp.toPx(), thumbCenterYPx + 3.5f.rdp.toPx()),
            end = RemoteOffset(thumbCenterXPx + 4.5f.rdp.toPx(), thumbCenterYPx - 2.9f.rdp.toPx()),
        )
    }
}
