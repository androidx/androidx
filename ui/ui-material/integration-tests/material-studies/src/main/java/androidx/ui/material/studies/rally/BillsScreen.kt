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

package androidx.ui.material.studies.rally

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Card
import androidx.ui.unit.dp

/**
 * The Bills screen.
 */
@Composable
fun BillsBody() {
    VerticalScroller {
        Column {
            Stack(LayoutPadding(16.dp)) {
                val accountsProportion = listOf(0.65f, 0.25f, 0.03f, 0.05f)
                val colors = listOf(0xFF1EB980, 0xFF005D57, 0xFF04B97F, 0xFF37EFBA).map {
                    Color(it)
                }
                Container(modifier = LayoutGravity.Center, height = 300.dp, expanded = true) {
                    DrawAnimatedCircle(accountsProportion, colors)
                }
                Column(modifier = LayoutGravity.Center) {
                    Text(
                        text = "Due",
                        style = MaterialTheme.typography().body1,
                        modifier = LayoutGravity.Center
                    )
                    Text(
                        text = "$1,810.00",
                        style = MaterialTheme.typography().h3,
                        modifier = LayoutGravity.Center
                    )
                }
            }
            Spacer(LayoutHeight(10.dp))
            Card {
                // TODO: change to proper bill items
                Column(modifier = LayoutPadding(12.dp)) {
                    AccountRow(
                        name = "RedPay Credit",
                        number = "Jan 29",
                        amount = "-45.36",
                        color = Color(0xFF005D57)
                    )
                    RallyDivider()
                    AccountRow(
                        name = "Rent",
                        number = "Feb 9",
                        amount = "-1,200.00",
                        color = Color(0xFF04B97F)
                    )
                    RallyDivider()
                    AccountRow(
                        name = "TabFine Credit",
                        number = "Feb 22",
                        amount = "-87.33",
                        color = Color(0xFF37EFBA)
                    )
                    RallyDivider()
                    AccountRow(
                        name = "ABC Loans",
                        number = "Feb 29",
                        amount = "-400.00",
                        color = Color(0xFF005D57)
                    )
                }
            }
        }
    }
}