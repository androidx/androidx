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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AnimatedText
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.rememberAnimatedTextFontRegistry
import kotlinx.coroutines.launch

val BaselineProfileScreens =
    listOf(
        BaselineProfileScreen { CardScreen() },
        BaselineProfileScreen { AnimatedIconButtonScreen() },
        BaselineProfileScreen {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AnimatedTextScreen()
        },
    )

@Composable
private fun CardScreen() {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ListHeader { Text("Card") }
        Card(
            onClick = {},
            onLongClick = {},
            onLongClickLabel = "Long click",
            colors = CardDefaults.cardColors()
        ) {
            Text("Card")
        }

        OutlinedCard(onClick = {}) { Text("Outlined card") }

        AppCard(
            onClick = {},
            appName = { Text("AppName") },
            title = {},
            time = { Text("02:34") },
            appImage = {
                Icon(
                    painter = painterResource(id = android.R.drawable.star_big_off),
                    contentDescription = "Star icon",
                    modifier =
                        Modifier.size(CardDefaults.AppImageSize)
                            .wrapContentSize(align = Alignment.Center),
                )
            },
        ) {
            Text("AppCard")
        }

        TitleCard(
            onClick = {},
            title = { Text("Title") },
            subtitle = { Text("Subtitle") },
            colors =
                CardDefaults.imageCardColors(
                    containerPainter =
                        CardDefaults.imageWithScrimBackgroundPainter(
                            backgroundImagePainter =
                                painterResource(id = R.drawable.backgroundimage)
                        ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    titleColor = MaterialTheme.colorScheme.onSurface
                ),
        ) {
            Text("TitleCard")
        }

        TitleCard(
            onClick = { /* Do something */ },
            title = { Text("Card title") },
            time = { Text("now") },
            colors =
                CardDefaults.imageCardColors(
                    containerPainter =
                        CardDefaults.imageWithScrimBackgroundPainter(
                            backgroundImagePainter =
                                painterResource(id = R.drawable.backgroundimage)
                        ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    titleColor = MaterialTheme.colorScheme.onSurface
                ),
            contentPadding = CardDefaults.ImageContentPadding,
            modifier = Modifier.semantics { contentDescription = "Background image" }
        ) {
            Text("Card content")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun AnimatedTextScreen() {
    val scope = rememberCoroutineScope()
    val animatable = remember { Animatable(0.5f) }
    val textStyle = remember {
        TextStyle.Default.copy(
            fontFamily = Font(R.font.robotoflex_variable).toFontFamily(),
            fontSize = 50.sp
        )
    }
    val fontRegistry =
        rememberAnimatedTextFontRegistry(
            startFontVariationSettings =
                FontVariation.Settings(FontVariation.width(10f), FontVariation.weight(100)),
            endFontVariationSettings =
                FontVariation.Settings(FontVariation.width(100f), FontVariation.weight(900)),
            textStyle = textStyle
        )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var textValue by remember { mutableIntStateOf(150) }
        Box(
            modifier =
                Modifier.weight(1f)
                    .semantics { contentDescription = "minusContentDescription" }
                    .clickable {
                        textValue -= 1
                        scope.launch {
                            animatable.animateTo(0f)
                            animatable.animateTo(0.5f)
                        }
                    },
            contentAlignment = Alignment.Center
        ) {
            Text("-", fontSize = 30.sp, textAlign = TextAlign.Center)
        }
        Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
            AnimatedText(
                text = textValue.toString(),
                fontRegistry = fontRegistry,
                progressFraction = { animatable.value }
            )
        }
        Box(
            modifier =
                Modifier.weight(1f)
                    .semantics { contentDescription = "plusContentDescription" }
                    .clickable {
                        textValue += 1
                        scope.launch {
                            animatable.animateTo(1f)
                            animatable.animateTo(0.5f)
                        }
                    },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 30.sp)
        }
    }
}

@Composable
fun AnimatedIconButtonScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(4) {
            IconButton(
                modifier =
                    Modifier.semantics { contentDescription = numberedContentDescription(it) },
                colors = IconButtonDefaults.filledIconButtonColors(),
                shapes = IconButtonDefaults.animatedShapes(),
                onClick = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_favorite_rounded),
                    contentDescription = null,
                    modifier = Modifier.size(IconButtonDefaults.DefaultIconSize)
                )
            }
        }
    }
}

/** Represents a screen used for generating a baseline profile. */
class BaselineProfileScreen(
    val content: @Composable () -> Unit,
)

private fun numberedContentDescription(n: Int) = "find-me-$n"
