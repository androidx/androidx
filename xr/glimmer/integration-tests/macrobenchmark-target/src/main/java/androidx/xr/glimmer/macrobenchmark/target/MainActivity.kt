/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.macrobenchmark.target

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.ActionCard
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.CardDefaults
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.LocalTextStyle
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.googlefonts.createGoogleSansFlexTypography
import androidx.xr.glimmer.list.GlimmerLazyColumn
import androidx.xr.glimmer.pager.GlimmerHorizontalPager
import androidx.xr.glimmer.pager.rememberGlimmerPagerState
import androidx.xr.glimmer.stack.VerticalStack

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO(b/438995221): Remove this line when this flag is turned on by default.
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = true

        val currentScreen = intent.getDestinationScreen()

        setContent {
            GlimmerTheme(typography = createGoogleSansFlexTypography()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (currentScreen) {
                        Screen.List -> ListScreen()
                        Screen.Pager -> PagerScreen()
                        Screen.Stack -> StackScreen()
                    }
                }
            }
        }
    }

    private fun Intent?.getDestinationScreen(): Screen =
        when (val destination = this?.getStringExtra(EXTRA_DESTINATION)) {
            DESTINATION_LIST -> Screen.List
            DESTINATION_PAGER -> Screen.Pager
            DESTINATION_STACK -> Screen.Stack
            else -> throw IllegalArgumentException("Unknown destination '$destination'")
        }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_LIST = "list"
        const val DESTINATION_PAGER = "pager"
        const val DESTINATION_STACK = "stack"
    }

    @Composable
    private fun ListScreen(modifier: Modifier = Modifier) {
        GlimmerLazyColumn(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(20) { index ->
                ListItem(onClick = {}, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Item: $index")
                }
            }
        }
    }

    @Composable
    private fun PagerScreen(modifier: Modifier = Modifier) {
        val pagerState = rememberGlimmerPagerState(pageCount = { 5 })
        GlimmerHorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
            ActionCard(
                title = {
                    Text(
                        text = "Page: $page",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                },
                action = {
                    Button(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.FavoriteIcon,
                                contentDescription = "icon",
                                modifier = Modifier.size(GlimmerTheme.iconSizes.medium),
                            )
                        },
                    ) {
                        Text(
                            text = "Page Action",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    }
                },
            ) {
                Text(
                    text = "This is a card with a title and action button.",
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                )
            }
        }
    }

    @Composable
    private fun StackScreen(modifier: Modifier = Modifier) {
        VerticalStack(modifier = modifier.fillMaxSize()) {
            items(5) { item ->
                Card(
                    title = {
                        Text(
                            "Stack: $item",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    },
                    leadingIcon = { Icon(Icons.CalendarIcon, "Localized description") },
                    modifier = Modifier.itemDecoration(CardDefaults.shape),
                ) {
                    Text(
                        "This is a card with a title and leading icon",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
        }
    }
}

enum class Screen {
    List,
    Pager,
    Stack,
}
