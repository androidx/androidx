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

package androidx.template.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text

/**
 * A basic [GlanceTemplate] implementation based around a single item, using [Data].
 */
abstract class SingleItemTemplate : GlanceTemplate<SingleItemTemplate.Data>() {

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor)
            ) {
                Text(it.title)
                Button(text = it.buttonText, onClick = it.buttonAction)
            }
        }
    }

    @Composable
    override fun WidgetLayoutVertical() {
        getData(currentState()).let {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor)
            ) {
                Text(it.title)
                Image(provider = it.image, contentDescription = "test")
                Button(text = it.buttonText, onClick = it.buttonAction)
            }
        }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
        getData(currentState()).let {
            Row(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor)
            ) {
                Column(modifier = GlanceModifier.padding(8.dp)) {
                    Text(it.title)
                    Button(text = it.buttonText, onClick = it.buttonAction)
                }
                Image(provider = it.image, contentDescription = "test")
            }
        }
    }

    open class Data(
        var title: String,
        var buttonText: String,
        var buttonAction: Action,
        var image: ImageProvider,
        var backgroundColor: Int
    )
}
