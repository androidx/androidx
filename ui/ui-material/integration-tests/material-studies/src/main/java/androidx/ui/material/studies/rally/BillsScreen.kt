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
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.material.Card
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.dp

/**
 * The Bills screen.
 */
@Composable
fun BillsBody(bills: List<Bill>) {
    VerticalScroller {
        Stack(Modifier.padding(16.dp)) {
            val accountsProportion = bills.extractProportions { it.amount }
            val colors = bills.map { it.color }
            AnimatedCircle(
                Modifier.gravity(Alignment.Center).preferredHeight(300.dp).fillMaxWidth(),
                accountsProportion,
                colors
            )
            Column(modifier = Modifier.gravity(Alignment.Center)) {
                Text(
                    text = "Due",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.gravity(Alignment.CenterHorizontally)
                )
                Text(
                    text = "$1,810.00",
                    style = MaterialTheme.typography.h2,
                    modifier = Modifier.gravity(Alignment.CenterHorizontally)
                )
            }
        }
        Spacer(Modifier.preferredHeight(10.dp))
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                bills.forEach { bill ->
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