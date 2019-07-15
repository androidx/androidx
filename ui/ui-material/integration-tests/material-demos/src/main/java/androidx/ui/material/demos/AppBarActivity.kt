/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.setContent
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.HeightSpacer
import androidx.ui.material.MaterialTheme
import androidx.ui.material.samples.SimpleBottomAppBarCenterFab
import androidx.ui.material.samples.SimpleBottomAppBarEndFab
import androidx.ui.material.samples.SimpleBottomAppBarNoFab
import androidx.ui.material.samples.SimpleTopAppBar
import androidx.ui.material.themeTextStyle
import androidx.ui.painting.imageFromResource

class AppBarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    val favouriteImage = { imageFromResource(resources, R.drawable.ic_favorite) }
                    val navigationImage = { imageFromResource(resources, R.drawable.ic_menu) }
                    FlexColumn {
                        expanded(1f) {
                            Column {
                                SpacedText("TopAppBar")
                                HeightSpacer(height = 28.dp)
                                SimpleTopAppBar(favouriteImage, navigationImage)
                            }
                            Column {
                                SpacedText("BottomAppBar - No FAB")
                                HeightSpacer(height = 28.dp)
                                SimpleBottomAppBarNoFab(favouriteImage, navigationImage)
                            }
                            Column {
                                SpacedText("BottomAppBar - Center FAB")
                                SimpleBottomAppBarCenterFab(favouriteImage, navigationImage)
                            }
                            Column {
                                SpacedText("BottomAppBar - End FAB")
                                SimpleBottomAppBarEndFab(favouriteImage)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SpacedText(text: String) {
        HeightSpacer(height = 12.dp)
        Text(text, style = +themeTextStyle { h6 })
        HeightSpacer(height = 12.dp)
    }
}
