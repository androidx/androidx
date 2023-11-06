/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget.preview

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.preview.ComposableInvoker.invokeComposable
import kotlinx.coroutines.runBlocking

private const val TOOLS_NS_URI = "http://schemas.android.com/tools"

/**
 * View adapter that renders a glance `@Composable`. The `@Composable` is found by reading the
 * `tools:composableName` attribute that contains the FQN of the function.
 */
internal class GlanceAppWidgetViewAdapter : AppWidgetHostView {

    constructor(context: Context, attrs: AttributeSet) : super(context) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        @Suppress("UNUSED_PARAMETER") defStyleAttr: Int
    ) : super(context) {
        init(attrs)
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    internal fun init(
        className: String,
        methodName: String,
    ) {
        val content = @Composable {
            val composer = currentComposer
            invokeComposable(
                className,
                methodName,
                composer)
        }

        val remoteViews = runBlocking {
            GlanceRemoteViews().compose(
                context = context,
                size = DpSize.Unspecified,
                content = content).remoteViews
        }
        val view = remoteViews.apply(context, this)
        addView(view)
    }

    private fun init(attrs: AttributeSet) {
        val composableName = attrs.getAttributeValue(TOOLS_NS_URI, "composableName") ?: return
        val className = composableName.substringBeforeLast('.')
        val methodName = composableName.substringAfterLast('.')

        init(className, methodName)
    }
}
