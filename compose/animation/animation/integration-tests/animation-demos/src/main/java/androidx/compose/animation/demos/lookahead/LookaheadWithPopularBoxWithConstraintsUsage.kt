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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.demos.R
import androidx.compose.animation.demos.gesture.pastelColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun LookaheadWithPopularBoxWithConstraintsUsage() {
    val padding by
        produceState(initialValue = 0.dp) {
            while (true) {
                delay(2000)
                if (value == 0.dp) {
                    value = 100.dp
                } else {
                    value = 0.dp
                }
            }
        }
    LookaheadScope {
        Box(
            Modifier.fillMaxSize()
                .animateBounds(this, Modifier.padding(padding))
                .background(pastelColors[3])
        ) {
            DetailsContent()
        }
    }
}

@Composable
fun DetailsContent() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints {
            Surface {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                ) {
                    Header(this@BoxWithConstraints.maxHeight)
                    Content(this@BoxWithConstraints.maxHeight)
                }
            }
        }
    }
}

@Composable
fun Content(containerHeight: Dp) {
    Column {
        Spacer(modifier = Modifier.heightIn(8.dp))
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "John Doe",
                modifier = Modifier.paddingFromBaseline(20.dp),
                fontWeight = FontWeight.Bold
            )
        }
        Property("last name", "Doe")
        Property("first name", "John")
        Spacer(modifier = Modifier.height((containerHeight - 320.dp).coerceAtLeast(0.dp)))
    }
}

@Composable
fun Property(label: String, value: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Divider()
        Text(
            text = label,
            modifier = Modifier.paddingFromBaseline(24.dp),
            style = MaterialTheme.typography.caption
        )
        Text(
            text = value,
            modifier = Modifier.paddingFromBaseline(24.dp),
        )
    }
}

@Composable
fun Header(containerHeight: Dp) {
    val height by animateDpAsState(containerHeight)
    Image(
        modifier = Modifier.heightIn(max = height / 2).fillMaxWidth(),
        painter = painterResource(id = R.drawable.android),
        contentScale = ContentScale.Crop,
        contentDescription = null
    )
}
