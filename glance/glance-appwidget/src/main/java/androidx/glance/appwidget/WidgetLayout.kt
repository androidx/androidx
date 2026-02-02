/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionModifier
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.EmittableLazyList
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.appwidget.lazy.EmittableLazyVerticalGrid
import androidx.glance.appwidget.lazy.EmittableLazyVerticalGridListItem
import androidx.glance.appwidget.proto.LayoutProto
import androidx.glance.appwidget.proto.LayoutProto.LayoutDefinition
import androidx.glance.appwidget.proto.LayoutProto.LayoutNode
import androidx.glance.appwidget.proto.LayoutProto.LayoutType
import androidx.glance.appwidget.proto.LayoutProto.NodeIdentity
import androidx.glance.appwidget.proto.LayoutProtoSerializer
import androidx.glance.findModifier
import androidx.glance.isDecorative
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.EmittableText
import androidx.glance.unit.Dimension
import java.io.File
import java.io.IOException

/**
 * Manager for layout configurations and their associated layout indexes.
 *
 * An instance of this object should be created for each update of an App Widget ID. The same
 * instance must be used for all the variants of the layout. It will detect layout changes and
 * ensure the same layout IDs are not re-used.
 *
 * Layout indexes are numbers between 0 and [TopLevelLayoutsCount]-1, which need to be passed to
 * [translateComposition] to generate the [android.widget.RemoteViews] corresponding to a given
 * layout.
 */
internal class LayoutConfiguration
private constructor(
    private val context: Context,
    /**
     * Map the known layout configs to a unique layout index. It will contain all the layouts stored
     * in files, and the layouts currently in use.
     */
    private val layoutConfig: MutableMap<LayoutNode, Int>,
    private var nextIndex: Int,
    private val appWidgetId: Int,
    /** Set of layout indexes that have been assigned since the creation of this object. */
    private val usedLayoutIds: MutableSet<Int> = mutableSetOf(),
    /** Set of all layout ids in [layoutConfig]. None of them can be re-used. */
    private val existingLayoutIds: MutableSet<Int> = mutableSetOf(),
) {

    internal companion object {

        /** Creates a [LayoutConfiguration] retrieving known layouts from file, if they exist. */
        internal suspend fun load(context: Context, appWidgetId: Int): LayoutConfiguration {
            val config =
                try {
                    GlanceState.getValue(
                        context,
                        LayoutStateDefinition,
                        layoutDatastoreKey(appWidgetId)
                    )
                } catch (ex: CorruptionException) {
                    Log.e(
                        GlanceAppWidgetTag,
                        "Set of layout structures for App Widget id $appWidgetId is corrupted",
                        ex
                    )
                    LayoutProto.LayoutConfig.getDefaultInstance()
                } catch (ex: IOException) {
                    Log.e(
                        GlanceAppWidgetTag,
                        "I/O error reading set of layout structures for App Widget id $appWidgetId",
                        ex
                    )
                    LayoutProto.LayoutConfig.getDefaultInstance()
                }
            val layouts = config.layoutList.associate { it.layout to it.layoutIndex }.toMutableMap()
            return LayoutConfiguration(
                context,
                layouts,
                nextIndex = config.nextIndex,
                appWidgetId = appWidgetId,
                existingLayoutIds = layouts.values.toMutableSet()
            )
        }

        /** Create a new, empty, [LayoutConfiguration]. */
        internal fun create(context: Context, appWidgetId: Int): LayoutConfiguration =
            LayoutConfiguration(
                context,
                layoutConfig = mutableMapOf(),
                nextIndex = 0,
                appWidgetId = appWidgetId,
            )

        /** Create a new, pre-defined [LayoutConfiguration]. */
        @VisibleForTesting
        internal fun create(
            context: Context,
            appWidgetId: Int,
            nextIndex: Int,
            existingLayoutIds: Collection<Int> = emptyList()
        ): LayoutConfiguration =
            LayoutConfiguration(
                context,
                appWidgetId = appWidgetId,
                layoutConfig = mutableMapOf(),
                nextIndex = nextIndex,
                existingLayoutIds = existingLayoutIds.toMutableSet(),
            )

        /** @return the file after delete() has been called on it. This is for testing. */
        fun delete(context: Context, id: GlanceId): Boolean {

            if (id is AppWidgetId && id.isRealId) {
                val key = layoutDatastoreKey(id.appWidgetId)
                val file = context.dataStoreFile(key)
                try {
                    return file.delete()
                } catch (e: Exception) {
                    // This is a minor error, File.delete() shouldn't throw an exception and these
                    // files
                    // are <1kb.
                    Log.d(
                        GlanceAppWidgetTag,
                        "Could not delete LayoutConfiguration dataStoreFile when cleaning up" +
                            "old appwidget id $id",
                        e
                    )
                }
            }
            return false
        }
    }

    /**
     * Add a layout to the set of known layouts.
     *
     * The layout index is retricted to the range 0 - [TopLevelLayoutsCount]-1. Once the layout
     * index reaches [TopLevelLayoutsCount], it cycles back to 0, making sure we are not re-using
     * any layout index used either for the current or previous set of layouts. The number of layout
     * indexes we have should be sufficient to mostly avoid collisions, but there is still a risk if
     * many updates are not rendered, or if all the indexes are used for lazy list items.
     *
     * @return the layout index that should be used to generate it
     */
    fun addLayout(layoutRoot: Emittable): Int {
        val root = createNode(context, layoutRoot)
        synchronized(this) {
            layoutConfig[root]?.let { index ->
                usedLayoutIds += index
                return index
            }
            var index = nextIndex
            while (index in existingLayoutIds) {
                index = (index + 1) % TopLevelLayoutsCount
                require(index != nextIndex) {
                    "Cannot assign a valid layout index to the new layout: no free index left."
                }
            }
            nextIndex = (index + 1) % TopLevelLayoutsCount
            usedLayoutIds += index
            existingLayoutIds += index
            layoutConfig[root] = index
            return index
        }
    }

    /** Save the known layouts to file at the end of the layout generation. */
    suspend fun save() {
        GlanceState.updateValue(context, LayoutStateDefinition, layoutDatastoreKey(appWidgetId)) {
            config ->
            config
                .toBuilder()
                .apply {
                    nextIndex = nextIndex
                    clearLayout()
                    layoutConfig.entries.forEach { (node, index) ->
                        if (index in usedLayoutIds) {
                            addLayout(
                                LayoutDefinition.newBuilder().apply {
                                    layout = node
                                    layoutIndex = index
                                }
                            )
                        }
                    }
                }
                .build()
        }
    }
}

/**
 * Returns the proto layout tree corresponding to the provided root node.
 *
 * A node should change if either the [LayoutType] selected by the translation of that node changes,
 * if the [SizeSelector] used to find the stub to be replaced changes or if the [ContainerSelector]
 * used to find the container's layout changes.
 *
 * Note: The number of children, although an element in [ContainerSelector] is not used, as this
 * will anyway invalidate the structure.
 */
internal fun createNode(context: Context, element: Emittable): LayoutNode =
    LayoutNode.newBuilder()
        .apply {
            type = element.getLayoutType()
            width = element.modifier.widthModifier.toProto(context)
            height = element.modifier.heightModifier.toProto(context)
            hasAction = element.modifier.findModifier<ActionModifier>() != null
            if (element.modifier.findModifier<AppWidgetBackgroundModifier>() != null) {
                identity = NodeIdentity.BACKGROUND_NODE
            }
            when (element) {
                is EmittableImage -> setImageNode(element)
                is EmittableColumn -> setColumnNode(element)
                is EmittableRow -> setRowNode(element)
                is EmittableBox -> setBoxNode(element)
                is EmittableLazyColumn -> setLazyListColumn(element)
            }
            if (element is EmittableWithChildren && element !is EmittableLazyList) {
                addAllChildren(element.children.map { createNode(context, it) })
            }
        }
        .build()

private fun LayoutNode.Builder.setImageNode(element: EmittableImage) {
    imageScale =
        when (element.contentScale) {
            androidx.glance.layout.ContentScale.Fit -> LayoutProto.ContentScale.FIT
            androidx.glance.layout.ContentScale.Crop -> LayoutProto.ContentScale.CROP
            androidx.glance.layout.ContentScale.FillBounds -> LayoutProto.ContentScale.FILL_BOUNDS
            else -> error("Unknown content scale ${element.contentScale}")
        }
    hasImageDescription = !element.isDecorative()
    hasImageColorFilter = element.colorFilterParams != null
}

private fun LayoutNode.Builder.setColumnNode(element: EmittableColumn) {
    horizontalAlignment = element.horizontalAlignment.toProto()
}

private fun LayoutNode.Builder.setLazyListColumn(element: EmittableLazyColumn) {
    horizontalAlignment = element.horizontalAlignment.toProto()
}

private fun LayoutNode.Builder.setRowNode(element: EmittableRow) {
    verticalAlignment = element.verticalAlignment.toProto()
}

private fun LayoutNode.Builder.setBoxNode(element: EmittableBox) {
    horizontalAlignment = element.contentAlignment.horizontal.toProto()
    verticalAlignment = element.contentAlignment.vertical.toProto()
}

private val GlanceModifier.widthModifier: Dimension
    get() = findModifier<WidthModifier>()?.width ?: Dimension.Wrap

private val GlanceModifier.heightModifier: Dimension
    get() = findModifier<HeightModifier>()?.height ?: Dimension.Wrap

@VisibleForTesting
internal fun layoutDatastoreKey(appWidgetId: Int): String = "appWidgetLayout-$appWidgetId"

private object LayoutStateDefinition : GlanceStateDefinition<LayoutProto.LayoutConfig> {
    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(fileKey)

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<LayoutProto.LayoutConfig> =
        DataStoreFactory.create(serializer = LayoutProtoSerializer) {
            context.dataStoreFile(fileKey)
        }
}

private fun Alignment.Vertical.toProto() =
    when (this) {
        Alignment.Vertical.Top -> LayoutProto.VerticalAlignment.TOP
        Alignment.Vertical.CenterVertically -> LayoutProto.VerticalAlignment.CENTER_VERTICALLY
        Alignment.Vertical.Bottom -> LayoutProto.VerticalAlignment.BOTTOM
        else -> error("unknown vertical alignment $this")
    }

private fun Alignment.Horizontal.toProto() =
    when (this) {
        Alignment.Horizontal.Start -> LayoutProto.HorizontalAlignment.START
        Alignment.Horizontal.CenterHorizontally ->
            LayoutProto.HorizontalAlignment.CENTER_HORIZONTALLY
        Alignment.Horizontal.End -> LayoutProto.HorizontalAlignment.END
        else -> error("unknown horizontal alignment $this")
    }

private fun Emittable.getLayoutType(): LayoutProto.LayoutType =
    when (this) {
        is EmittableBox -> LayoutProto.LayoutType.BOX
        is EmittableButton -> LayoutProto.LayoutType.BUTTON
        is EmittableRow -> {
            if (modifier.isSelectableGroup) {
                LayoutProto.LayoutType.RADIO_ROW
            } else {
                LayoutProto.LayoutType.ROW
            }
        }
        is EmittableColumn -> {
            if (modifier.isSelectableGroup) {
                LayoutProto.LayoutType.RADIO_COLUMN
            } else {
                LayoutProto.LayoutType.COLUMN
            }
        }
        is EmittableText -> LayoutProto.LayoutType.TEXT
        is EmittableLazyListItem -> LayoutProto.LayoutType.LIST_ITEM
        is EmittableLazyColumn -> LayoutProto.LayoutType.LAZY_COLUMN
        is EmittableAndroidRemoteViews -> LayoutProto.LayoutType.ANDROID_REMOTE_VIEWS
        is EmittableCheckBox -> LayoutProto.LayoutType.CHECK_BOX
        is EmittableSpacer -> LayoutProto.LayoutType.SPACER
        is EmittableSwitch -> LayoutProto.LayoutType.SWITCH
        is EmittableImage -> LayoutProto.LayoutType.IMAGE
        is EmittableLinearProgressIndicator -> LayoutProto.LayoutType.LINEAR_PROGRESS_INDICATOR
        is EmittableCircularProgressIndicator -> LayoutProto.LayoutType.CIRCULAR_PROGRESS_INDICATOR
        is EmittableLazyVerticalGrid -> LayoutProto.LayoutType.LAZY_VERTICAL_GRID
        is EmittableLazyVerticalGridListItem -> LayoutProto.LayoutType.LIST_ITEM
        is RemoteViewsRoot -> LayoutProto.LayoutType.REMOTE_VIEWS_ROOT
        is EmittableRadioButton -> LayoutProto.LayoutType.RADIO_BUTTON
        is EmittableSizeBox -> LayoutProto.LayoutType.SIZE_BOX
        else ->
            throw IllegalArgumentException("Unknown element type ${this.javaClass.canonicalName}")
    }

private fun Dimension.toProto(context: Context): LayoutProto.DimensionType {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return WidgetLayoutImpl31.toProto(this)
    }
    return when (resolveDimension(context)) {
        is Dimension.Dp -> LayoutProto.DimensionType.EXACT
        is Dimension.Wrap -> LayoutProto.DimensionType.WRAP
        is Dimension.Fill -> LayoutProto.DimensionType.FILL
        is Dimension.Expand -> LayoutProto.DimensionType.EXPAND
        else -> error("After resolution, no other type should be present")
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object WidgetLayoutImpl31 {
    fun toProto(dimension: Dimension) =
        if (dimension is Dimension.Expand) {
            LayoutProto.DimensionType.EXPAND
        } else {
            LayoutProto.DimensionType.WRAP
        }
}
