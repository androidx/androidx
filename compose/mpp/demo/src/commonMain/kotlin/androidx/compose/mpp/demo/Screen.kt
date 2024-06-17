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

package androidx.compose.mpp.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

sealed interface Screen {
    val title: String

    class Example(
        override val title: String,
        val backgroundColor: Color? = null,
        val content: @Composable () -> Unit
    ) : Screen {

        @Composable
        override fun Content(title: String, navigate: (String) -> Unit, back: (() -> Unit)) {
            ExampleScaffold(
                title = title,
                back = back,
                backgroundColor = backgroundColor,
                content = content
            )
        }
    }

    class Selection(
        override val title: String,
        val screens: List<Screen>
    ) : Screen {
        constructor(title: String, vararg screens: Screen) : this(title, listOf(*screens))

        fun mergedWith(screens: List<Screen>): Selection {
            return Selection(title, screens + this.screens)
        }

        @Composable
        override fun Content(title: String, navigate: (String) -> Unit, back: (() -> Unit)) {
            SelectionScaffold(
                title = title,
                back = back,
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(screens) {
                        Text(it.title, Modifier.clickable {
                            navigate(it.title)
                        }.padding(16.dp).fillMaxWidth())
                    }
                }
            }
        }
    }

    class Fullscreen(
        override val title: String,
        val content: @Composable (back: () -> Unit) -> Unit
    ) : Screen {

        @Composable
        override fun Content(title: String, navigate: (String) -> Unit, back: (() -> Unit)) {
            content(back)
        }
    }

    class Dialog(
        override val title: String,
        val content: @Composable () -> Unit
    ) : Screen {

        @Composable
        override fun Content(title: String, navigate: (String) -> Unit, back: (() -> Unit)) {
            content()
        }
    }

    @Composable
    fun Content(title: String, navigate: (String) -> Unit, back: (() -> Unit))
}

@Composable
private fun ExampleScaffold(
    title: String,
    back: () -> Unit,
    backgroundColor: Color?,
    content: @Composable () -> Unit
) {
    Scaffold(
        /*
        Without using TopAppBar, this is recommended approach to apply multiplatform window insets
        to Material2 Scaffold (otherwise there will be empty space above top app bar - as is here)
        */
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        topBar = { ExampleTopBar(title, back) },
        backgroundColor = backgroundColor ?: MaterialTheme.colors.background
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            content()
        }
    }
}

@Composable
private fun ExampleTopBar(
    title: String,
    back: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { ArrowBackIcon(back) }
    )
}

@Composable
private fun SelectionScaffold(
    title: String,
    back: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = { SelectionTopBar(title, back) },
    ) { innerPadding ->
        /*
         * In case of applying WindowInsets as content padding, it is strongly recommended to wrap
         * content of scaffold into box with these modifiers to support proper layout when device rotated
         */
        val horizontalInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
        Box(Modifier.fillMaxSize().windowInsetsPadding(horizontalInsets).padding(innerPadding)) {
            content()
        }
    }
}

@Composable
private fun SelectionTopBar(
    title: String,
    back: (() -> Unit)? = null,
) {
    /*
     * This is recommended approach of applying multiplatform window insets to Material2 Scaffold
     * with using top app bar.
     * By that way, it is possible to fill area above top app bar with its background - as it works
     * out of box in android development or with Material3 Scaffold
     */
    TopAppBar(
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            .union(WindowInsets(left = 20.dp))
            .asPaddingValues(),
        content = {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Row(
                    Modifier.fillMaxHeight().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (back != null) {
                        ArrowBackIcon(back)
                        Spacer(Modifier.width(16.dp))
                    }
                    ProvideTextStyle(value = MaterialTheme.typography.h6) {
                        Text(title)
                    }
                }
            }
        }
    )
}

@Composable
private fun ArrowBackIcon(back: () -> Unit) {
    Icon(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        modifier = Modifier.clickable(onClick = back)
    )
}

