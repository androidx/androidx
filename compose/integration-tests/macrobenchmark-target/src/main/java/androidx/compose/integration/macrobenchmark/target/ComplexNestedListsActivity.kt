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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage

class ComplexNestedListsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting()
                }
            }
        }

        launchIdlenessTracking()
    }

    internal fun ComponentActivity.launchIdlenessTracking() {
        val contentView: View = findViewById(android.R.id.content)
        val callback: Choreographer.FrameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (Recomposer.runningRecomposers.value.any { it.hasPendingWork }) {
                        contentView.contentDescription = "COMPOSE-BUSY"
                    } else {
                        contentView.contentDescription = "COMPOSE-IDLE"
                    }
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        Choreographer.getInstance().postFrameCallback(callback)
    }
}

@Composable
private fun Greeting() {
    LazyColumn(Modifier.semantics { contentDescription = "IamLazy" }) {
        items(1000) {
            LazyRow {
                items(10) { Video(modifier = Modifier.width(200.dp).height(120.dp).padding(16.dp)) }
            }
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Black))
        }
    }
}

@Composable
private fun Video(
    modifier: Modifier = Modifier,
    imageRes: Int = R.drawable.simple_image,
    duration: String = "100",
    onVideoClick: () -> Unit = {},
    shimmerModifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onVideoClick
            )
    ) {
        VideoImageBox(
            modifier = Modifier.then(shimmerModifier),
            imageRes = imageRes,
            duration = duration
        )
    }
}

@Composable
private fun VideoImageBox(
    modifier: Modifier,
    imageRes: Int,
    duration: String,
) {
    Card(
        modifier =
            modifier
                .aspectRatio(16f / 9)
                .shadow(
                    elevation = 12.dp,
                    spotColor = Color.Gray,
                    shape = RoundedCornerShape(size = 12.dp)
                )
    ) {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (image, durationBox) = createRefs()

            AsyncImage(
                modifier =
                    Modifier.constrainAs(image) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                model = imageRes,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Row(
                modifier =
                    Modifier.constrainAs(durationBox) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = duration,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    modifier = Modifier.size(12.dp).padding(2.dp),
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
