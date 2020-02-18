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

import android.annotation.SuppressLint
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.EdgeInsets
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
import androidx.ui.material.surface.Card
import androidx.ui.unit.dp
import java.util.Locale

@Composable
fun OverviewBody() {
    VerticalScroller {
        Column(modifier = LayoutPadding(16.dp)) {
            AlertCard()
            Spacer(LayoutHeight(RallyDefaultPadding))
            AccountsCard()
            Spacer(LayoutHeight(RallyDefaultPadding))
            BillsCard()
        }
    }
}

/**
 * The Alerts card within the Rally Overview screen.
 */
@Composable
private fun AlertCard() {
    var openDialog by state { false }
    val alertMessage = "Heads up, you've used up 90% of your Shopping budget for this month."

    if (openDialog) {
        RallyAlertDialog(
            onDismiss = {
                openDialog = false
            },
            bodyText = alertMessage,
            buttonText = "Dismiss".toUpperCase(Locale.getDefault())
        )
    }
    Card {
        Column {
            AlertHeader({ openDialog = true })
            RallyDivider(
                modifier = LayoutPadding(start = RallyDefaultPadding, end = RallyDefaultPadding)
            )
            AlertItem(alertMessage)
        }
    }
}

@Composable
private fun AlertHeader(onClickSeeAll: () -> Unit) {
    Row(
        modifier = LayoutPadding(RallyDefaultPadding) + LayoutWidth.Fill,
        arrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography().subtitle2,
            modifier = LayoutGravity.Center
        )
        TextButton(
            onClick = onClickSeeAll,
            paddings = EdgeInsets(0.dp),
            modifier = LayoutGravity.Center
        ) {
            Text("SEE ALL")
        }
    }
}

@Composable
private fun AlertItem(message: String) {
    // TODO: Make alerts into a data structure
    Row(
        modifier = LayoutPadding(RallyDefaultPadding),
        arrangement = Arrangement.SpaceBetween
    ) {
        Text(
            style = MaterialTheme.typography().h3,
            modifier = LayoutFlexible(1f),
            text = message
        )
        IconButton(
            vectorImage = Icons.Filled.Sort,
            onClick = {},
            modifier = LayoutGravity.Top
        )
    }
}

/**
 * Base structure for cards in the Overview screen.
 */
@SuppressLint("UnnecessaryLambdaCreation")
@Composable
private fun <T> OverviewScreenCard(
    title: String,
    amount: Float,
    onClickSeeAll: () -> Unit,
    data: List<T>,
    row: @Composable() (T) -> Unit
) {
    Card {
        Column {
            Column(modifier = LayoutPadding(RallyDefaultPadding)) {
                Text(text = title, style = MaterialTheme.typography().subtitle2)
                val amountText = "$" + formatAmount(amount)
                Text(text = amountText, style = MaterialTheme.typography().h2)
            }
            Divider(color = rallyGreen, height = 1.dp)
            Column(LayoutPadding(start = 16.dp, top = 4.dp, end = 8.dp)) {
                data.take(3).forEach { row(it) }
                SeeAllButton(onClick = onClickSeeAll)
            }
        }
    }
}

/**
 * The Accounts card within the Rally Overview screen.
 */
@Composable
private fun AccountsCard() {
    val amount = UserData.accounts.map { account -> account.balance }.sum()
    OverviewScreenCard(
        title = "Accounts",
        amount = amount,
        onClickSeeAll = {
            // TODO: Figure out navigation
        },
        data = UserData.accounts
    ) { account ->
        AccountRow(
            name = account.name,
            number = account.number,
            amount = account.balance,
            color = account.color
        )
    }
}

/**
 * The Bills card within the Rally Overview screen.
 */
@Composable
private fun BillsCard() {
    val amount = UserData.bills.map { bill -> bill.amount }.sum()
    OverviewScreenCard(
        title = "Bills",
        amount = amount,
        onClickSeeAll = {
            // TODO: Figure out navigation
        },
        data = UserData.bills
    ) { bill ->
        BillRow(
            name = bill.name,
            due = bill.due,
            amount = bill.amount,
            color = bill.color
        )
    }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = LayoutHeight(44.dp) + LayoutWidth.Fill
    ) {
        Text("SEE ALL")
    }
}

private val RallyDefaultPadding = 12.dp
