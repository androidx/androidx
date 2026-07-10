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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.components.OutlineButton
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class RemoteComposeDemoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = RemoteComposeDemoWidget()
}

/** Demonstrates the [TitleBar] component. */
class RemoteComposeDemoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent { DemoList() }
    }
}

/** TODO: eventually remove this demo */
@Composable
private fun DemoBox(rounded: Boolean = false) {
    @Composable
    fun RoundedBox() {
        val modifier =
            GlanceModifier.fillMaxSize()
                .cornerRadius(30.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .semantics { this.contentDescription = "Semantics" }
                .cornerRadius(30.dp)

        Box(
            GlanceModifier.size(150.dp, 100.dp).background(Color.Yellow),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier) { Text("Hello world") }
        }
    }

    @Composable
    fun SimpleBox() {
        Box(GlanceModifier.size(100.dp).background(ColorProvider(Color.Magenta))) {}
    }

    if (rounded) {
        RoundedBox()
    } else {
        SimpleBox()
    }
}

@Composable
private fun DemoList() {
    LazyColumn(
        modifier = GlanceModifier.fillMaxSize().background(Color.Cyan),
        verticalScrollMode = VerticalScrollMode.Normal,
    ) {
        items(10) { index: Int ->
            Text(
                "Item $index",
                GlanceModifier.height(48.dp).background(Color.Gray).padding(4.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TextAndButtonComponentDemo() {
    val noOnClick = {}
    val icon = ImageProvider(R.drawable.ic_demo_app)

    Column(GlanceModifier.fillMaxSize()) {
        //        item {
        Column {
            Text("A list of demos")
            Text(
                "This text is followd by a 16dp spacer. This text is 14sp",
                style = TextStyle(fontSize = 14.sp),
            )
            Spacer(GlanceModifier.size(16.dp))

            Text("Button Test. A button with 16dp padding")
            Button("First Button", noOnClick, modifier = GlanceModifier.padding(16.dp))

            // Material 3
            Text("a button with 8dp padding, and a button with 16dp padding")
            FilledButton("Filled button", noOnClick, GlanceModifier.padding(8.dp))
            FilledButton(
                "Icon Filled button",
                icon = icon,
                onClick = noOnClick,
                modifier = GlanceModifier.padding(16.dp),
            )

            Text("Circle Image Button with drawable resource ic_demo_app")
            CircleIconButton(
                imageProvider = icon,
                //                    contentColor =  ColorProvider(Color.Cyan),
                //                    backgroundColor = ColorProvider(Color.Magenta),
                onClick = noOnClick,
                contentDescription = "content description for circleiconbutton",
            )
            Text("Outline Button")
            OutlineButton(
                text = "OutlineButton",
                contentColor = GlanceTheme.colors.primary,
                onClick = noOnClick,
                modifier = GlanceModifier.padding(16.dp),
            )
            OutlineButton(
                text = "OutlineButton",
                contentColor = GlanceTheme.colors.primary,
                onClick = noOnClick,
                icon = icon,
                modifier = GlanceModifier.padding(16.dp),
            )
            Spacer(GlanceModifier.size(8.dp))

            Box(
                GlanceModifier.width(40.dp).height(150.dp).background(Color.Magenta).padding(8.dp)
            ) {
                // no content, just eat up space
            }
        }
    }
}
