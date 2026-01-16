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

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.unit.ColorProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteComposeButtons {

    @Composable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun M3TextButtonElement(
        text: String,
        onClick: Action?,
        modifier: GlanceModifier,
        enabled: Boolean = true,
        icon: ImageProvider?,
        contentColor: ColorProvider,
        backgroundResource: Int,
        backgroundTint: ColorProvider,
        maxLines: Int,
        isOutlineButton: Boolean,
    ) {
        GlanceNode(
            factory = ::EmittableM3TextButton,
            update = {
                this.set(text) { this.text = it }
                this.set(onClick) { this.onClick = it }
                this.set(modifier) { this.modifier = it }
                this.set(enabled) { this.enabled = it }
                this.set(icon) { this.icon = it }
                this.set(contentColor) { this.contentColor = it }
                this.set(backgroundResource) { this.backgroundResource = it }
                this.set(backgroundTint) { this.backgroundTint = it }
                this.set(maxLines) { this.maxLines = it }
                this.set(isOutlineButton) { this.isOutlineButton = it }
            },
        )
    }

    @Composable
    internal fun M3IconButtonElement(
        imageProvider: ImageProvider,
        contentDescription: String?,
        contentColor: ColorProvider,
        backgroundColor: ColorProvider?,
        shape: IconButtonShape,
        onClick: Action,
        modifier: GlanceModifier,
        enabled: Boolean,
    ) {
        GlanceNode(
            factory = ::EmittableM3IconButton,
            update = {
                this.set(imageProvider) { this.imageProvider = it }
                this.set(contentDescription) { this.contentDescription = it }
                this.set(contentColor) { this.contentColor = it }
                this.set(backgroundColor) { this.backgroundColor = it }
                this.set(shape) { this.shape = it }
                this.set(onClick) { this.onClick = it }
                this.set(modifier) { this.modifier = it }
                this.set(enabled) { this.enabled = it }
            },
        )
    }
}

/** Only valid for RemoteCompose backend */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmittableM3IconButton
private constructor(
    public var imageProvider: ImageProvider?,
    public var contentDescription: String?,
    public var contentColor: ColorProvider?,
    public var backgroundColor: ColorProvider?,
    public var shape: IconButtonShape,
    public var onClick: Action?,
    public override var modifier: GlanceModifier,
    public var enabled: Boolean,
) : Emittable {

    public constructor() :
        this(
            imageProvider = null,
            contentDescription = null,
            contentColor = null,
            backgroundColor = null,
            shape = IconButtonShape.Circle,
            onClick = null,
            modifier = GlanceModifier,
            enabled = false,
        )

    override fun copy(): Emittable {
        return EmittableM3IconButton(
            imageProvider = imageProvider,
            contentDescription = contentDescription,
            contentColor = contentColor,
            backgroundColor = backgroundColor,
            shape = shape,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

/** Only valid for RemoteCompose backend */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmittableM3TextButton
private constructor(
    public var text: String,
    public var onClick: Action?,
    public override var modifier: GlanceModifier,
    public var enabled: Boolean = true,
    public var icon: ImageProvider?,
    public var contentColor: ColorProvider,
    public @DrawableRes var backgroundResource: Int,
    public var backgroundTint: ColorProvider,
    public var maxLines: Int,
    public var isOutlineButton: Boolean,
) : Emittable {

    /** No-arg constructor for when its constructed as a [GlanceNode] */
    public constructor() :
        this(
            text = "",
            onClick = null,
            modifier = GlanceModifier,
            enabled = false,
            icon = null,
            contentColor = ColorProvider(androidx.compose.ui.graphics.Color.Transparent),
            backgroundResource = 0,
            backgroundTint = ColorProvider(androidx.compose.ui.graphics.Color.Transparent),
            maxLines = -1,
            isOutlineButton = false,
        )

    override fun copy(): Emittable {
        return EmittableM3TextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            icon = icon,
            contentColor = contentColor,
            backgroundResource = backgroundResource,
            backgroundTint = backgroundTint,
            maxLines = maxLines,
            isOutlineButton = isOutlineButton,
        )
    }
}
