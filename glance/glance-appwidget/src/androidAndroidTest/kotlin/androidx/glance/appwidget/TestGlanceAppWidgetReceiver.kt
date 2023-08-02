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
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId

class TestGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TestGlanceAppWidget
}

object TestGlanceAppWidget : GlanceAppWidget(errorUiLayout = 0) {

    override var sizeMode: SizeMode = SizeMode.Single

    @Composable
    override fun Content() {
        uiDefinition()
    }

    private var onDeleteBlock: ((GlanceId) -> Unit)? = null

    fun setOnDeleteBlock(block: (GlanceId) -> Unit) {
        onDeleteBlock = block
    }

    fun resetOnDeleteBlock() {
        onDeleteBlock = null
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        onDeleteBlock?.apply { this(glanceId) }
    }

    var uiDefinition: @Composable () -> Unit = { }
}
