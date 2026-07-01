/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

class ThemeStartupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val applyMaterialTheme = intent.getBooleanExtra(EXTRA_APPLY_MATERIAL_THEME, false)

        setContent {
            if (applyMaterialTheme) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val colorScheme =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        androidx.compose.material3.dynamicLightColorScheme(context)
                    } else {
                        androidx.compose.material3.lightColorScheme()
                    }

                MaterialTheme(colorScheme = colorScheme) {
                    Box(modifier = Modifier.fillMaxSize()) { Text(text = "With MaterialTheme") }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) { Text(text = "Without MaterialTheme") }
            }
        }
    }

    companion object {
        const val EXTRA_APPLY_MATERIAL_THEME =
            "androidx.compose.material3.integration.macrobenchmark.target.EXTRA_APPLY_MATERIAL_THEME"
    }
}
