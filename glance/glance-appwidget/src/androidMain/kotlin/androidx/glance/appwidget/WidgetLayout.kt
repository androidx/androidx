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
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.EmittableWithChildren
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.findModifier
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.appwidget.proto.LayoutProto
import androidx.glance.appwidget.proto.LayoutProto.LayoutNode
import androidx.glance.appwidget.proto.LayoutProto.LayoutType
import androidx.glance.appwidget.proto.LayoutProtoSerializer
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.EmittableText
import androidx.glance.unit.Dimension
import java.io.File

/**
 * Replace the stored layout proto for this widget with the layout tree from the provided root node.
 *
 * @return true if the new tree differs from the previously stored tree
 */
internal suspend fun updateWidgetLayout(
    context: Context,
    appWidgetId: Int,
    root: Emittable
): Boolean {
    val tree = createNode(root)
    val fileKey = layoutDatastoreKey(appWidgetId)
    val previous = GlanceState.getValue(context, LayoutDefinition, fileKey)
    GlanceState.updateValue(context, LayoutDefinition, fileKey) { tree }
    return previous != tree
}

/**
 * Returns the proto layout tree corresponding to the provided root node.
 */
internal fun createNode(element: Emittable): LayoutNode {
    val width = element.modifier.findModifier<WidthModifier>()?.width?.toProto()
    val height = element.modifier.findModifier<HeightModifier>()?.height?.toProto()
    val type = element.getLayoutType()
    val nodeBuilder = LayoutNode.newBuilder().setType(type)
    width?.let { nodeBuilder.width = it }
    height?.let { nodeBuilder.height = it }

    if (element is EmittableWithChildren) {
        nodeBuilder.addAllChildren(element.children.map { createNode(it) })
    }
    return nodeBuilder.build()
}

private fun layoutDatastoreKey(appWidgetId: Int) = "appWidgetLayout-$appWidgetId"

private fun Emittable.getLayoutType(): LayoutType =
    when (this) {
        is EmittableBox -> LayoutType.BOX
        is EmittableButton -> LayoutType.BUTTON
        is EmittableRow -> LayoutType.ROW
        is EmittableColumn -> LayoutType.COLUMN
        is EmittableText -> LayoutType.TEXT
        is EmittableLazyListItem -> LayoutType.LIST_ITEM
        is EmittableLazyColumn -> LayoutType.LAZY_COLUMN
        is EmittableAndroidRemoteViews -> LayoutType.ANDROID_REMOTE_VIEWS
        is EmittableCheckBox -> LayoutType.CHECK_BOX
        is EmittableSpacer -> LayoutType.SPACER
        is EmittableSwitch -> LayoutType.SWITCH
        is EmittableImage -> LayoutType.IMAGE
        is RemoteViewsRoot -> LayoutType.REMOTE_VIEWS_ROOT
        else ->
            throw IllegalArgumentException("Unknown element type ${this.javaClass.canonicalName}")
    }

private fun Dimension.toProto(): LayoutProto.Dimension =
    when (this) {
        is Dimension.Dp -> LayoutProto.Dimension.newBuilder()
            .setDpDimension(LayoutProto.Dimension.Dp.newBuilder().setValue(this.dp.value))
            .build()
        is Dimension.Wrap -> LayoutProto.Dimension.newBuilder()
            .setWrapDimension(LayoutProto.Dimension.Wrap.getDefaultInstance())
            .build()
        is Dimension.Fill -> LayoutProto.Dimension.newBuilder()
            .setFillDimension(LayoutProto.Dimension.Fill.getDefaultInstance())
            .build()
        is Dimension.Expand -> LayoutProto.Dimension.newBuilder()
            .setExpandDimension(LayoutProto.Dimension.Expand.getDefaultInstance())
            .build()
        is Dimension.Resource -> LayoutProto.Dimension.newBuilder()
            .setResourceDimension(LayoutProto.Dimension.Resource.newBuilder().setValue(this.res))
            .build()
    }

private object LayoutDefinition : GlanceStateDefinition<LayoutProto.LayoutNode> {
    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(fileKey)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> getDataStore(context: Context, fileKey: String): DataStore<T> =
        DataStoreFactory.create(serializer = LayoutProtoSerializer) {
            context.dataStoreFile(fileKey)
        } as DataStore<T>
}
