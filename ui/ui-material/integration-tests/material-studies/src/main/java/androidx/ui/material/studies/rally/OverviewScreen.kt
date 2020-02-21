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
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TextButton
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Sort
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Card
import androidx.ui.unit.dp
import java.util.Locale

@Composable
fun OverviewBody() {
    VerticalScroller {
        Column(modifier = LayoutPadding(16.dp)) {
            AlertCard()
            Spacer(LayoutHeight(10.dp))
            AccountsCard()
            Spacer(LayoutHeight(10.dp))
            BillsCard()
        }
    }
}

/**
 * The Alerts card within the Rally Overview screen.
 */
@Composable
private fun AlertCard() {
    val openDialog = state { false }
    val alertMessage = "Heads up, you've used up 90% of your Shopping budget for this month."

    if (openDialog.value) {
        RallyAlertDialog(
            onDismiss = {
                openDialog.value = false
            },
            bodyText = alertMessage,
            buttonText = "Dismiss".toUpperCase(Locale.getDefault())
        )
    }
    Card {
        Column {
            Ripple(bounded = true) {
                Clickable(onClick = { openDialog.value = true }) {
                    Container {
                        Row(
                            modifier = LayoutPadding(12.dp) + LayoutWidth.Fill,
                            arrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Alerts", style = MaterialTheme.typography().subtitle2)
                            TextButton(onClick = { }) {
                                Text("See All")
                            }
                        }
                    }
                }
            }
            Divider(
                LayoutPadding(left = 12.dp, right = 12.dp),
                color = MaterialTheme.colors().background,
                height = 2.dp
            )
            Row(LayoutPadding(12.dp)) {
                Text(
                    style = MaterialTheme.typography().body1,
                    modifier = LayoutFlexible(1f),
                    text = alertMessage
                )
                IconButton(
                    vectorImage = Icons.Filled.Sort,
                    onClick = {},
                    modifier = LayoutGravity.Top
                )
            }
        }
    }
}

/**
 * The Accounts card within the Rally Overview screen.
 */
@Composable
private fun AccountsCard() {
    Card {
        Column {
            Column(modifier = LayoutPadding(12.dp)) {
                Text(text = "Accounts", style = MaterialTheme.typography().body1)
                Text(text = "$12,132.49", style = MaterialTheme.typography().h3)
            }
            Divider(color = rallyGreen, height = 1.dp)
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
                TextButton(onClick = { }) {
                    Text("See All")
                }
            }
        }
    }
}

/**
 * The Bills card within the Rally Overview screen.
 */
@Composable
fun BillsCard() {
    Card {
        Column {
            Column(modifier = LayoutPadding(12.dp)) {
                Text(text = "Bills", style = MaterialTheme.typography().subtitle2)
                Text(text = "$1,810.00", style = MaterialTheme.typography().h3)
            }
            Divider(color = rallyGreen, height = 1.dp)
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
                TextButton(onClick = { }) {
                    Text("See All")
                }
            }
        }
    }
}