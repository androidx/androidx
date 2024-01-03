/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.integration.demos.common

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import kotlin.reflect.KClass

/**
 * Generic demo with a [title] that will be displayed in the list of demos.
 */
sealed class Demo(val title: String, val description: String? = null) {
    override fun toString() = title
}

/**
 * Demo that launches an [Activity] when selected.
 *
 * This should only be used for demos that need to customize the activity, the large majority of
 * demos should just use [ComposableDemo] instead.
 *
 * @property activityClass the KClass (Foo::class) of the activity that will be launched when
 * this demo is selected.
 */
class ActivityDemo<T : ComponentActivity>(title: String, val activityClass: KClass<T>) : Demo(title)

/**
 * A category of [Demo]s, that will display a list of [demos] when selected.
 */
class DemoCategory(
    title: String,
    val demos: List<Demo>
) : Demo(title)

/**
 * Parameters which are used by [Demo] screens.
 */
class DemoParameters(
    val navigateBack: () -> Unit,
    val swipeToDismissBoxState: SwipeToDismissBoxState
)

/**
 * Demo that displays [Composable] [content] when selected,
 * with a method to navigate back to the parent.
 */
class ComposableDemo(
    title: String,
    description: String? = null,
    val content: @Composable (params: DemoParameters) -> Unit,
) : Demo(title, description)

@Composable
fun Centralize(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
