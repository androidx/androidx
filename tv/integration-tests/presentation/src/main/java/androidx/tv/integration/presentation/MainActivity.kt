/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.integration.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Create the "data.json" file in "presentation/src/main/assets" directory and add the
        // content from this link: go/compose-tv-presentation-app-data
        val jsonData = assets.readAssetsFile("data.json")
        val deserializedData = getRootDataFromJson(jsonData)

        initializeData(deserializedData)

        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
