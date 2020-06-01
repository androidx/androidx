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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.layout.padding
import androidx.ui.material.Scaffold

/**
 * This Activity recreates the Rally Material Study from
 * https://material.io/design/material-studies/rally.html
 */
class RallyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RallyApp()
        }
    }
}

@Composable
fun RallyApp() {
    RallyTheme {
        val allScreens = RallyScreenState.values().toList()
        var currentScreen by state { RallyScreenState.Overview }
        Scaffold(
            topBar = {
                RallyTopAppBar(
                    allScreens = allScreens,
                    onTabSelected = { screen -> currentScreen = screen },
                    currentScreen = currentScreen
                )
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                currentScreen.body()
            }
        }
    }
}