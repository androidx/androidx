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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace

class StaticScrollingContentWithChromeInitialCompositionActivity : ComponentActivity() {

    private val onlyPerformComposition: Boolean
        get() =
            intent.action ==
                "androidx.compose.integration.macrobenchmark.target" +
                    ".STATIC_SCROLLING_CONTENT_WITH_CHROME_INITIAL_COMPOSITION_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (onlyPerformComposition) {
                ComposeOnlyLayout {
                    StaticScrollingContentWithChrome(
                        modifier =
                            Modifier.onPlaced { _ ->
                                    throw RuntimeException(
                                        "Content was placed, but should only be composed"
                                    )
                                }
                                .drawWithContent {
                                    throw RuntimeException(
                                        "Content was drawn, but should only be composed"
                                    )
                                }
                    )
                }
            } else {
                StaticScrollingContentWithChrome()
            }
        }
    }
}

/**
 * A layout that will compose all of the [content], but will not place (and therefore not layout or
 * draw) any of its children.
 *
 * This is useful for this benchmark as we care about the composition time. A major limitation of
 * this approach is that any content in a SubcomposeLayout will not be composed and will not
 * contribute to the overall measured time of this test.
 */
@Composable
private fun ComposeOnlyLayout(content: @Composable () -> Unit) {
    Layout(content) { _, _ -> layout(0, 0) {} }
}

@Preview
@Composable
private fun StaticScrollingContentWithChrome(modifier: Modifier = Modifier) =
    trace(sectionName = "StaticScrollingContentWithChrome") {
        Column(modifier) {
            TopBar()
            ScrollingContent(modifier = Modifier.weight(1f))
            BottomBar()
        }
    }

@Composable
private fun TopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    "Initial Composition Macrobench",
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1
                )
                Text(
                    "Static Scrolling Content w/ Chrome",
                    style = MaterialTheme.typography.caption,
                    maxLines = 1
                )
            }
        },
        navigationIcon = { Button(onClick = {}) { Icon(Icons.Default.Close, "Dismiss") } },
        actions = { Button(onClick = {}) { Icon(Icons.Default.MoreVert, "Actions") } }
    )
}

@Composable
private fun BottomBar(modifier: Modifier = Modifier) {
    BottomNavigation(modifier = modifier) {
        BottomNavigationItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, "Home") }
        )
        BottomNavigationItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Add, "Add") }
        )
    }
}

@Composable
private fun ScrollingContent(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        Item(
            color = Color.DarkGray,
            icon = Icons.Filled.Info,
            modifier =
                Modifier.padding(horizontal = 16.dp)
                    .aspectRatio(16f / 9f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
        )

        repeat(5) { iteration ->
            CardGroup(
                title = "Group ${4 * iteration}",
                groupIcon = Icons.Filled.Person,
                groupColor = Color(0xFF1967D2)
            )

            CardGroup(
                title = "Group ${4 * iteration + 1}",
                groupIcon = Icons.Filled.Favorite,
                groupColor = Color(0xFFC5221F)
            )

            CardGroup(
                title = "Group ${4 * iteration + 2}",
                groupIcon = Icons.Filled.Star,
                groupColor = Color(0xFFF29900)
            )

            CardGroup(
                title = "Group ${4 * iteration + 3}",
                groupIcon = Icons.Filled.Place,
                groupColor = Color(0xFF188038)
            )
        }
    }
}

@Composable
private fun CardGroup(
    title: String,
    groupIcon: ImageVector,
    groupColor: Color,
    modifier: Modifier = Modifier,
    count: Int = 10
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.h6, modifier = Modifier.padding(16.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)
        ) {
            repeat(count) {
                Item(
                    color = groupColor,
                    icon = groupIcon,
                    modifier =
                        Modifier.padding(horizontal = 4.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun Item(color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White)
    }
}
