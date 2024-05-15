/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

class VectorsListActivity : ComponentActivity() {

    private var contents = arrayListOf<String>()
    init {
        for (index in 0..1000) {
            contents.add("compose$index")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeLazyList()
        }

        launchIdlenessTracking()
    }

    @Composable
    private fun ComposeLazyList() {
        LazyColumn(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = "IamLazy" },
        ) {
            items(
                count = 1000,
                key = { index ->
                    index
                },
                contentType = {
                    "test"
                }
            ) { index ->
                val text = contents[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(Dp(64F))
                        .wrapContentHeight()
                        .padding(
                            vertical = Dp(12F),
                            horizontal = Dp(12F)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.ic_launcher
                        ),
                        modifier = Modifier
                            .height(Dp(40F))
                            .width(Dp(40F)),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        contentDescription = ""
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = Dp(16F))
                    ) {
                        Text(
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            text = text
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(
                                    id = R.drawable.ic_cloud14 // Drawable id
                                ),
                                modifier = Modifier
                                    .height(Dp(16F))
                                    .width(Dp(16F))
                                    .padding(end = Dp(1F)),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center,
                                contentDescription = ""
                            )
                            Text(
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis,
                                text = text
                            )
                        }
                    }
                    Image(
                        painter = painterResource(
                            id = R.drawable.ic_favourite // Drawable id
                        ),
                        modifier = Modifier
                            .height(Dp(24F))
                            .width(Dp(24F)),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterEnd,
                        contentDescription = ""
                    )
                    Image(
                        painter = painterResource(
                            id = R.drawable.ic_more // Drawable id
                        ),
                        modifier = Modifier
                            .height(Dp(24F))
                            .width(Dp(24F)),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterEnd,
                        contentDescription = ""
                    )
                }
            }
        }
    }
}
