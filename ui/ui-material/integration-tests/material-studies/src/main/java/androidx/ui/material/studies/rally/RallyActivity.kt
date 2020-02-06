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

package androidx.ui.material.studies.rally

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.setContent
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.Spacer
import androidx.ui.material.Scaffold
import androidx.ui.material.Tab
import androidx.ui.material.TabRow
import androidx.ui.unit.dp

/**
 * This Activity recreates the Rally Material Study from
 * https://material.io/design/material-studies/rally.html
 */
class RallyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RallyApp()
        }
    }

    @Composable
    fun RallyApp() {
        RallyTheme {
            val allScreens = RallyScreenState.values().toList()
            var currentScreen by state { RallyScreenState.Overview }
            Scaffold(
                topAppBar = {
                    TabRow(allScreens, selectedIndex = currentScreen.ordinal) { i, screen ->
                        Tab(
                            text = screen.name,
                            selected = currentScreen.ordinal == i,
                            onSelected = {
                                currentScreen = screen
                            }
                        )
                    }
                }
            ) {
                currentScreen.body()
            }
        }
    }
}

@Composable
fun RallyBody() {
    VerticalScroller {
        Column(modifier = LayoutPadding(16.dp)) {
            RallyAlertCard()
            Spacer(LayoutHeight(10.dp))
            RallyAccountsOverviewCard()
            Spacer(LayoutHeight(10.dp))
            RallyBillsOverviewCard()
        }
    }
}

private enum class RallyScreenState {
    Overview, Accounts, Bills
}

@Composable
private fun RallyScreenState.body() = when (this) {
    RallyScreenState.Overview -> RallyBody()
    RallyScreenState.Accounts -> RallyAccountsCard()
    RallyScreenState.Bills -> RallyBillsCard()
}
