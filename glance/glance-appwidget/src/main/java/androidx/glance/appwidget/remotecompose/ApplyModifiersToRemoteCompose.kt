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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.BackgroundModifier
import androidx.glance.GlanceModifier
import androidx.glance.Visibility
import androidx.glance.VisibilityModifier
import androidx.glance.action.ActionModifier
import androidx.glance.appwidget.AppWidgetBackgroundModifier
import androidx.glance.appwidget.CornerRadiusModifier
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.action.applyActionForRemoteComposeElement
import androidx.glance.appwidget.isRtl
import androidx.glance.appwidget.toPixels
import androidx.glance.color.DayNightColorProvider
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dimension
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider

/**
 * In RemoteCompose, unlike Jetpack Compose, [RecordingModifier] is mutable.
 *
 * @param modifiers The [GlanceModifier] to convert.
 * @param translationContext
 * @return Takes the contents of [modifiers] maps them to a [RecordingModifier]
 */
internal fun convertGlanceModifierToRemoteComposeModifier(
    modifiers: GlanceModifier,
    translationContext: TranslationContext,
): RecordingModifier {

    val outputModifier = RecordingModifier()
    val context: Context = translationContext.context
    val rcContext: RemoteComposeContext = translationContext.remoteComposeContext

    var widthModifier: WidthModifier? = null
    var heightModifier: HeightModifier? = null
    var paddingModifiers: PaddingModifier? = null
    var visibility: Visibility = Visibility.Visible
    var actionModifier: ActionModifier? = null
    // TODO: implement the following
    //    var enabled: EnabledModifier? = null
    //    var semanticsModifier: SemanticsModifier? = null
    modifiers.foldIn(Unit) { _, modifier ->
        when (modifier) {
            is ActionModifier -> {
                if (actionModifier != null) {
                    Log.w(
                        GlanceAppWidgetTag,
                        "More than one clickable defined on the same GlanceModifier, " +
                            "only the last one will be used.",
                    )
                }
                actionModifier = modifier
            }
            is WidthModifier -> widthModifier = modifier
            is HeightModifier -> heightModifier = modifier
            is BackgroundModifier -> applyBackgroundModifier(context, modifier, outputModifier)
            is AppWidgetBackgroundModifier -> {
                Log.w(
                    TAG,
                    "Ignoring AppWidgetBackground modifier, not currently applicable to remote compose. ",
                )
            }
            is PaddingModifier -> {
                paddingModifiers = paddingModifiers?.let { it + modifier } ?: modifier
            }
            is VisibilityModifier -> visibility = modifier.visibility
            is CornerRadiusModifier -> {
                val cornerRadius = modifier.radius
                applyRoundedCorners(
                    context = context,
                    outputModifier = outputModifier,
                    radius = cornerRadius,
                )
            }
            // TODO: add support for the following modifiers
            //            is AppWidgetBackgroundModifier -> {
            //                // This modifier is handled somewhere else.
            //            }
            //            is SelectableGroupModifier -> {
            //                if (!translationContext.canUseSelectableGroup) {
            //                    error(
            //                        "GlanceModifier.selectableGroup() can only be used on Row or
            //   Column " +
            //                            "composables."
            //                    )
            //                }
            //            }
            //            is AlignmentModifier -> {
            //                // This modifier is handled somewhere else.
            //            }
            //            is EnabledModifier -> enabled = modifier
            //            is SemanticsModifier -> semanticsModifier = modifier

            else -> {
                Log.w(GlanceAppWidgetTag, "Unknown modifier '$modifier', nothing done.")
            }
        }
    }
    applySizeModifiers(
        context = context,
        widthModifier = widthModifier,
        heightModifier = heightModifier,
        outputModifier = outputModifier,
    )

    actionModifier?.let {
        val arbitraryId = translationContext.nextActionId()
        applyActionForRemoteComposeElement(
            rcTranslationContext = translationContext,
            action = it.action,
            arbitraryId = arbitraryId,
        )
        outputModifier.onClick(HostAction(arbitraryId))
    }
    paddingModifiers?.let { padding ->
        val absolutePadding = padding.toDp(context.resources).toAbsolute(context.isRtl)
        val displayMetrics = context.resources.displayMetrics
        outputModifier.padding(
            absolutePadding.left.toPixels(displayMetrics),
            absolutePadding.top.toPixels(displayMetrics),
            absolutePadding.right.toPixels(displayMetrics),
            absolutePadding.bottom.toPixels(displayMetrics),
        )
    }

    //    enabled?.let { rv.setBoolean(viewDef.mainViewId, "setEnabled", it.enabled) }
    //    semanticsModifier?.let { semantics ->
    //        val contentDescription: List<String>? =
    //            semantics.configuration.getOrNull(SemanticsProperties.ContentDescription)
    //        if (contentDescription != null) {
    //            rv.setContentDescription(viewDef.mainViewId, contentDescription.joinToString())
    //        }
    //    }

    val visibilityIdLong: Long = rcContext.addInteger(visibility.toRemoteComposeVisibility())
    val intId = Utils.idFromLong(visibilityIdLong).toInt()
    outputModifier.visibility(intId)

    /// DONE
    return outputModifier
}

internal fun androidx.glance.Visibility.toRemoteComposeVisibility(): Int {
    val value: Int =
        when (this) {
            Visibility.Visible -> Component.Visibility.VISIBLE
            Visibility.Invisible -> Component.Visibility.INVISIBLE
            Visibility.Gone -> Component.Visibility.GONE
        }

    return value
}

@VisibleForTesting
internal fun applySizeModifiers(
    context: Context,
    widthModifier: WidthModifier?,
    heightModifier: HeightModifier?,
    outputModifier: RecordingModifier,
): RecordingModifier {

    val width = widthModifier?.width
    val height = heightModifier?.height

    when (width) {
        is Dimension.Dp -> outputModifier.width(width.toPixels(context))
        is Dimension.Resource -> TODO("Dimension.resource not yet supported")
        Dimension.Expand ->
            outputModifier.horizontalWeight(RemoteComposeConstants.Text.DefaultWeight)
        Dimension.Fill -> outputModifier.fillMaxWidth()
        Dimension.Wrap -> outputModifier.wrapContentWidth()
        null -> Unit
    }
    when (height) {
        is Dimension.Dp -> outputModifier.height(height.toPixels(context))
        is Dimension.Resource -> TODO("Dimension.resource not yet supported")
        Dimension.Expand -> outputModifier.verticalWeight(RemoteComposeConstants.Text.DefaultWeight)
        Dimension.Fill -> outputModifier.fillMaxHeight()
        Dimension.Wrap -> outputModifier.wrapContentHeight()
        null -> Unit
    }

    return outputModifier
}

private fun Dimension.Dp.toPixels(context: Context) = dp.toPixels(context)

private fun applyBackgroundModifier(
    context: Context,
    modifier: BackgroundModifier,
    outputModifier: RecordingModifier,
) {
    //    val viewId = viewDef.mainViewId
    //
    fun applyBackgroundImageModifier(modifier: BackgroundModifier.Image) {
        //            val imageProvider = modifier.imageProvider
        //            if (imageProvider is AndroidResourceImageProvider) {
        //                rv.setViewBackgroundResource(viewId, imageProvider.resId)
        //            }
        //            // Otherwise, the background has been transformed arnd should be ignored
        //            // (removing modifiers is not really possible).

        // TODO: give a distinctive debug color to views that have a background image set.
        // TODO: Because RC doesn't have a background image modifier, we want to turn such
        // components into a Box { backgroundImage, foregroundView }, and better yet, not
        // use them at all
        outputModifier.background(Color(165, 252, 3, alpha = 255).toArgb())
        return
    }

    fun applyBackgroundColorModifier(colorProvider: ColorProvider) {
        when (colorProvider) {
            is FixedColorProvider -> outputModifier.background(colorProvider.color.toArgb())
            is ResourceColorProvider -> {
                outputModifier.background(colorProvider.getColor(context).toArgb())
                Log.i(TAG, "Eagerly resolving resourceId color in applyBackgroundColorModifier")
            }
            is DayNightColorProvider -> {
                val day = colorProvider.day.toArgb()
                val night = colorProvider.night.toArgb()
                Log.w(TAG, "TODO: support DayNightColorProvider in applyBackgroundModifier()")
            }
            else ->
                Log.w(GlanceAppWidgetTag, "Unexpected background color modifier: $colorProvider")
        }
    }

    //
    when (modifier) {
        is BackgroundModifier.Image -> applyBackgroundImageModifier(modifier)
        is BackgroundModifier.Color -> applyBackgroundColorModifier(modifier.colorProvider)
    }
}

private fun applyRoundedCorners(
    context: Context,
    outputModifier: RecordingModifier,
    radius: Dimension,
) {

    // TODO: this is probably broken, rewrite it, it needs the shape dimension i think

    when (radius) {
        is Dimension.Dp -> {
            val radiusPx = radius.toPixels(context).toFloat()
            outputModifier.clip(RoundedRectShape(radiusPx, radiusPx, radiusPx, radiusPx))
        }
        is Dimension.Resource -> {
            val res = radius.res
            val radiusPx = context.resources.getDimension(res)
            // TODO: do we want to resolve resources in the provider process or the player?
            outputModifier.clip(RoundedRectShape(radiusPx, radiusPx, radiusPx, radiusPx))
        }
        else -> error("Rounded corners should not be ${radius.javaClass.canonicalName}")
    }
}
