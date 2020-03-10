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
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Card
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
                AnimatedCircle(
                    LayoutGravity.Center + LayoutHeight(300.dp) + LayoutWidth.Fill,
                    accountsProportion,
                    colors
                )
                Column(modifier = LayoutGravity.Center) {
                    Text(
                        text = "Due",
                        style = MaterialTheme.typography().body1,
                        modifier = LayoutGravity.Center
                    )
                    Text(
                        text = "$1,810.00",
                        style = MaterialTheme.typography().h2,
                        modifier = LayoutGravity.Center
                    )
                }
            }
            Spacer(LayoutHeight(10.dp))
            Card {
                Column(modifier = LayoutPadding(12.dp)) {
                    UserData.bills.forEach { bill ->
                        BillRow(
                            name = bill.name,
                            due = bill.due,
                            amount = bill.amount,
                            color = bill.color
                        )
                    }
                }
            }
        }
    }
}