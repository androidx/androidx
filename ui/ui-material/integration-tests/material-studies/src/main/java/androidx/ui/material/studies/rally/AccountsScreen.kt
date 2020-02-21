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
import androidx.ui.foundation.Box
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
import androidx.ui.material.surface.Card
import androidx.ui.unit.dp

/**
 * The Accounts screen.
 */
@Composable
fun AccountsBody() {
    VerticalScroller {
        Column {
            Stack(LayoutPadding(16.dp)) {
                val accountsProportion = listOf(0.595f, 0.045f, 0.095f, 0.195f, 0.045f)
                val colors = listOf(0xFF1EB980, 0xFF005D57, 0xFF04B97F, 0xFF37EFBA,
                    0xFFFAFFBF).map { Color(it) }
                Box(LayoutHeight(300.dp) + LayoutGravity.Center + LayoutWidth.Fill) {
                    DrawAnimatedCircle(accountsProportion, colors)
                }
                Column(modifier = LayoutGravity.Center) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography().body1,
                        modifier = LayoutGravity.Center
                    )
                    Text(
                        text = "$12,132.49",
                        style = MaterialTheme.typography().h3,
                        modifier = LayoutGravity.Center
                    )
                }
            }
            Spacer(LayoutHeight(10.dp))
            Card {
                Column(modifier = LayoutPadding(12.dp)) {
                    AccountRow(
                        name = "Checking",
                        number = "1234",
                        amount = "2,215.13",
                        color = Color(0xFF005D57)
                    )
                    RallyDivider()
                    AccountRow(
                        name = "Home Savings",
                        number = "5678",
                        amount = "8,676.88",
                        color = Color(0xFF04B97F)
                    )
                    RallyDivider()
                    AccountRow(
                        name = "Car Savings",
                        number = "9012",
                        amount = "987.48",
                        color = Color(0xFF37EFBA)
                    )
                    RallyDivider()
                    AccountRow(
                        name = "Vacation",
                        number = "3456",
                        amount = "253",
                        color = Color(0xFF005D57)
                    )
                }
            }
        }
    }
}