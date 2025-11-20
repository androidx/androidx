/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.testapp.common

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.xr.compose.testapp.ui.components.ColumnWithCenterText
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme

class AnotherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val showBottomBar = intent.getBooleanExtra("SHOW_BOTTOM_BAR", false)
            val title = intent.getStringExtra("TITLE") ?: "Another Activity"
            val bottomBarText = intent.getStringExtra("BOTTOM_BAR_TEXT") ?: ""
            val insideText =
                intent.getStringExtra("INSIDE_TEXT") ?: "Activity inside a spatial panel"
            IntegrationTestsAppTheme {
                CommonTestScaffold(
                    title = title,
                    showBottomBar = showBottomBar,
                    bottomBarText = bottomBarText,
                    onClickBackArrow = null,
                ) { padding ->
                    ColumnWithCenterText(padding = padding, text = insideText)
                }
            }
        }
    }
}
