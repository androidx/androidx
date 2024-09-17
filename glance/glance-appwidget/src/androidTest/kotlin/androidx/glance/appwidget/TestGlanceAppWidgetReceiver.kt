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
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId

class TestGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TestGlanceAppWidget

    companion object {
        var ignoreBroadcasts = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (VERBOSE_LOG) {
            Log.d(RECEIVER_TEST_TAG, "Test Receiver onReceive ${intent.action}")
        }

        if (ignoreBroadcasts) {
            Log.w("TestGlanceAppWidgetReceiver", "Ignored $intent")
            return
        }
        super.onReceive(context, intent)
    }
}

object TestGlanceAppWidget : GlanceAppWidget() {
    public override var errorUiLayout: Int = 0

    override var sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            onProvideGlance?.invoke(this) ?: provideContent(uiDefinition)
        } finally {
            onProvideGlance = null
        }
    }

    var onProvideGlance: (suspend TestGlanceAppWidget.() -> Unit)? = null

    private var onDeleteBlock: ((GlanceId) -> Unit)? = null

    var onProvidePreview: (@Composable TestGlanceAppWidget.(Int) -> Unit)? = null

    fun setOnDeleteBlock(block: (GlanceId) -> Unit) {
        onDeleteBlock = block
    }

    fun resetOnDeleteBlock() {
        onDeleteBlock = null
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        onDeleteBlock?.apply { this(glanceId) }
    }

    var uiDefinition: @Composable () -> Unit = {}

    inline fun withErrorLayout(layout: Int, block: () -> Unit) {
        val previousErrorLayout = errorUiLayout
        errorUiLayout = layout
        try {
            block()
        } finally {
            errorUiLayout = previousErrorLayout
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { onProvidePreview?.invoke(this, widgetCategory) }
    }

    inline fun withProvidePreview(
        noinline previewBlock: @Composable TestGlanceAppWidget.(Int) -> Unit,
        withBlock: () -> Unit
    ) {
        val previousProvidePreview = onProvidePreview
        onProvidePreview = previewBlock
        try {
            withBlock()
        } finally {
            onProvidePreview = previousProvidePreview
        }
    }
}
