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

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.ui.unit.DpSize
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.EmittableLazyItemWithChildren
import androidx.glance.EmittableWithChildren
import androidx.glance.appwidget.EmittableSizeBox
import androidx.glance.appwidget.GlanceComponents
import androidx.glance.appwidget.RemoteViewsRoot
import androidx.glance.appwidget.components.EmittableM3IconButton
import androidx.glance.appwidget.components.EmittableM3TextButton
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.appwidget.remotecompose.Components.translateBox
import androidx.glance.appwidget.remotecompose.Components.translateButton
import androidx.glance.appwidget.remotecompose.Components.translateColumn
import androidx.glance.appwidget.remotecompose.Components.translateEmittableM3IconButton
import androidx.glance.appwidget.remotecompose.Components.translateEmittableSizeBox
import androidx.glance.appwidget.remotecompose.Components.translateEmittableSpacer
import androidx.glance.appwidget.remotecompose.Components.translateImage
import androidx.glance.appwidget.remotecompose.Components.translateLazyColumn
import androidx.glance.appwidget.remotecompose.Components.translateLazyListItem
import androidx.glance.appwidget.remotecompose.Components.translateRcMaterial3Button
import androidx.glance.appwidget.remotecompose.Components.translateRow
import androidx.glance.appwidget.remotecompose.Components.translateText
import androidx.glance.appwidget.remotecompose.Components.translateUnknownElementToSpacer
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.GlanceRemoteComposeProfile
import androidx.glance.appwidget.remotecompose.components.RcBox
import androidx.glance.appwidget.remotecompose.components.RcButton
import androidx.glance.appwidget.remotecompose.components.RcColumn
import androidx.glance.appwidget.remotecompose.components.RcElement
import androidx.glance.appwidget.remotecompose.components.RcImage
import androidx.glance.appwidget.remotecompose.components.RcLazyColumn
import androidx.glance.appwidget.remotecompose.components.RcMaterial3IconButton
import androidx.glance.appwidget.remotecompose.components.RcMaterial3TextButton
import androidx.glance.appwidget.remotecompose.components.RcRow
import androidx.glance.appwidget.remotecompose.components.RcSpacer
import androidx.glance.appwidget.remotecompose.components.RcText
import androidx.glance.appwidget.toPixels
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.text.EmittableText

internal const val TAG = "GlanceRemoteCompose"

internal object GlanceRemoteComposeTranslator {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun translateCompositionUsingRemoteCompose(
        remoteViewsRoot: RemoteViewsRoot,
        context: Context,
        appWidgetId: Int,
        glanceComponents: GlanceComponents,
        actionBroadcastReceiver: ComponentName,
    ): RemoteViews {

        // 1) Create a remote compose document from our glance tree
        val translation =
            translateEmittableTreeToRemoteCompose(
                context = context,
                remoteViewRoot = remoteViewsRoot,
                appWidgetId = appWidgetId,
                glanceComponents = glanceComponents,
                actionBroadcastReceiver = actionBroadcastReceiver,
            )

        // 2) Create remoteviews and set the drawinstructions on them.
        return DrawInstructionRemoteViews.create(translation)
    }

    /** Step one of translation. Produce a remote compose document */
    @VisibleForTesting
    internal fun translateEmittableTreeToRemoteCompose(
        context: Context,
        remoteViewRoot: RemoteViewsRoot,
        appWidgetId: Int,
        glanceComponents: GlanceComponents,
        actionBroadcastReceiver: ComponentName,
    ): GlanceToRemoteComposeTranslation {

        val description = "todo: configure description field" // TODO!

        val topLevelChildren = remoteViewRoot.children

        @Suppress("ListIterator")
        val result: GlanceToRemoteComposeTranslation =
            if (topLevelChildren.any { emittable -> emittable is EmittableSizeBox }) {
                val documentsBySize: List<Pair<DpSize, GlanceToRemoteComposeTranslation.Single>> =
                    translateEmittableSizeBoxesToRemoteCompose(
                        context = context,
                        description = description,
                        roots = topLevelChildren.filterIsInstance<EmittableSizeBox>(),
                        appWidgetId = appWidgetId,
                        glanceComponents = glanceComponents,
                        actionBroadcastReceiver = actionBroadcastReceiver,
                    )

                GlanceToRemoteComposeTranslation.SizeMap(documentsBySize)
            } else if (topLevelChildren.size == 1) {
                val (doc: RemoteComposeContext, map: TranslationActionMap) =
                    translateSingleRootEmittableTreeToRemoteCompose(
                        context = context,
                        description = description,
                        rootEmittable = topLevelChildren[0],
                        appWidgetId = appWidgetId,
                        layoutSize = DpSize.Zero,
                        glanceComponents = glanceComponents,
                        actionBroadcastReceiver = actionBroadcastReceiver,
                    )

                GlanceToRemoteComposeTranslation.Single(doc, actionMap = map)
            } else {
                throw IllegalStateException("Unexpected emittable tree: $topLevelChildren")
            }

        return result
    }

    internal fun translateEmittable(
        emittable: Emittable,
        translationContext: TranslationContext,
    ): RcElement {
        val root: RcElement =
            when (emittable) {
                is EmittableSpacer -> translateEmittableSpacer(emittable, translationContext)
                is EmittableBox -> translateBox(emittable, translationContext)
                is EmittableColumn -> translateColumn(emittable, translationContext)
                is EmittableLazyColumn -> translateLazyColumn(emittable, translationContext)
                is EmittableLazyListItem,
                is EmittableLazyItemWithChildren ->
                    translateLazyListItem(emittable as EmittableWithChildren, translationContext)
                //                TODO("handle emittable lazy items")
                is EmittableRow -> translateRow(emittable, translationContext)
                is EmittableText -> translateText(emittable, translationContext)
                is EmittableImage -> translateImage(emittable, translationContext)
                is EmittableButton -> translateButton(emittable, translationContext)
                is EmittableSizeBox -> translateEmittableSizeBox(emittable, translationContext)
                is EmittableM3IconButton ->
                    translateEmittableM3IconButton(emittable, translationContext)
                is EmittableM3TextButton ->
                    translateRcMaterial3Button(emittable, translationContext)
                else -> {
                    Log.w(TAG, "RemoteComposeContext.translate: Emittable $emittable not supported")
                    translateUnknownElementToSpacer(emittable, translationContext)
                }
            }

        return root
    }

    private fun translateEmittableSizeBoxesToRemoteCompose(
        context: Context,
        appWidgetId: Int,
        description: String,
        roots: List<EmittableSizeBox>,
        glanceComponents: GlanceComponents,
        actionBroadcastReceiver: ComponentName,
    ): List<Pair<DpSize, GlanceToRemoteComposeTranslation.Single>> {

        @Suppress("ListIterator")
        return roots.map { sizeBox: EmittableSizeBox ->
            val size: DpSize = sizeBox.size
            val onlyChild = sizeBox.children.single()

            val (remoteComposeContext: RemoteComposeContext, map: TranslationActionMap) =
                translateSingleRootEmittableTreeToRemoteCompose(
                    context = context,
                    rootEmittable = onlyChild,
                    description = description,
                    appWidgetId = appWidgetId,
                    layoutSize = size,
                    glanceComponents = glanceComponents,
                    actionBroadcastReceiver = actionBroadcastReceiver,
                )

            return@map Pair(
                size,
                GlanceToRemoteComposeTranslation.Single(remoteComposeContext, map),
            )
        }
    }

    /** In this step, we create a remote compose document and populate it from our glance tree. */
    private fun translateSingleRootEmittableTreeToRemoteCompose(
        context: Context,
        rootEmittable: Emittable,
        layoutSize: DpSize,
        appWidgetId: Int,
        description: String,
        glanceComponents: GlanceComponents,
        actionBroadcastReceiver: ComponentName,
    ): Pair<RemoteComposeContext, TranslationActionMap> {
        val widthPx: Int = layoutSize.width.toPixels(context)
        val heightPx: Int = layoutSize.height.toPixels(context)

        val actionMap: TranslationActionMap = mutableListOf()
        val rcContext =
            RemoteComposeContext(
                creationDisplayInfo =
                    CreationDisplayInfo(
                        widthPx,
                        heightPx,
                        context.resources.displayMetrics.density.toInt(),
                    ),
                contentDescription = description,
                profile = GlanceRemoteComposeProfile,
            ) {
                val translationContext =
                    TranslationContext(
                        context = context,
                        remoteComposeContext = this,
                        appWidgetId = appWidgetId,
                        actionMap = actionMap,
                        layoutSize = layoutSize,
                        glanceComponents = glanceComponents,
                        actionBroadcastReceiver = actionBroadcastReceiver,
                    )

                // step 1: translate everything into a tree of RcElement. This will allow us to
                // extract metadata such as images and strings that need to go at the start of the
                // document
                val translatedRoot: RcElement =
                    translateEmittable(
                        emittable = rootEmittable,
                        translationContext = translationContext,
                    )

                // step 2: write the document using our tree of nodes
                root { translatedRoot.writeComponent(translationContext) }
            }

        if (DebugRemoteCompose) {
            printAndCopyDoc(rcContext = rcContext, androidContext = context) // TODO: debug code
        }

        return rcContext to actionMap
    }
}

private object Components {

    fun translateEmittableSizeBox(
        emittable: EmittableSizeBox,
        translationContext: TranslationContext,
    ): RcElement {
        val translationContext = translationContext

        // This method treats emittableSizeBox as a box. IT has a bit more of a semantic meaning
        // than that, and we should do another pass on how this is handled.
        // TODO: See SizeBox.kt
        Log.w(TAG, "Warning: revisit translateEmittableSizeBox, temporary impl")

        if (emittable.children.size == 1) {
            return GlanceRemoteComposeTranslator.translateEmittable(
                emittable.children[0],
                translationContext,
            )
        } else {
            throw IllegalStateException("couldn't translate emittableSizeBox: $emittable")
        }
    }

    fun translateText(emittable: EmittableText, translationContext: TranslationContext): RcElement {
        return RcText.create(emittable, translationContext)
    }

    fun translateButton(
        button: EmittableButton,
        translationContext: TranslationContext,
    ): RcElement {
        return RcButton(button, translationContext)
    }

    fun translateBox(emittable: EmittableBox, translationContext: TranslationContext): RcElement {
        return RcBox(emittable, translationContext, modifierOverride = null)
    }

    fun translateEmittableSpacer(
        emittable: EmittableSpacer,
        translationContext: TranslationContext,
    ): RcElement {
        return RcSpacer(emittable, translationContext)
    }

    fun translateUnknownElementToSpacer(
        emittable: Emittable,
        translationContext: TranslationContext,
    ): RcElement {
        return RcSpacer(emittable, translationContext)
    }

    fun translateColumn(
        emittable: EmittableColumn,
        translationContext: TranslationContext,
    ): RcElement {
        return RcColumn(emittable, translationContext)
    }

    fun translateLazyColumn(
        emittable: EmittableLazyColumn,
        translationContext: TranslationContext,
    ): RcElement {
        return RcLazyColumn(emittable, translationContext)
    }

    fun translateRow(emittable: EmittableRow, translationContext: TranslationContext): RcElement {
        return RcRow(emittable, translationContext, modifierOverride = null)
    }

    fun translateImage(
        emittable: EmittableImage,
        translationContext: TranslationContext,
    ): RcElement {
        //        val provider: ImageProvider = emittable.provider ?: return // nothing to do if no
        // provider

        return RcImage(emittable, translationContext)
    }

    /** A lazyListItem may only have one child */
    fun translateLazyListItem(
        emittable: EmittableWithChildren,
        translationContext: TranslationContext,
    ): RcElement {
        check(emittable.children.size == 1, { "A LazyListItem may only contain one child" })
        return GlanceRemoteComposeTranslator.translateEmittable(
            emittable.children.first(),
            translationContext,
        )
    }

    fun translateRcMaterial3Button(
        button: EmittableM3TextButton,
        context: TranslationContext,
    ): RcElement {
        return RcMaterial3TextButton(button, context)
    }

    fun translateEmittableM3IconButton(
        button: EmittableM3IconButton,
        context: TranslationContext,
    ): RcElement {
        return RcMaterial3IconButton(button, context)
    }
}
