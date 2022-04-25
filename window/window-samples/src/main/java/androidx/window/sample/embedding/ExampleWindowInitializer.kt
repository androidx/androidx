/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample.embedding

import android.content.Context
import androidx.startup.Initializer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitController
import androidx.window.sample.R

/**
 * Initializes SplitController with a set of statically defined rules.
 */
@OptIn(ExperimentalWindowApi::class)
class ExampleWindowInitializer : Initializer<SplitController> {
    override fun create(context: Context): SplitController {
        SplitController.initialize(context, R.xml.main_split_config)
        return SplitController.getInstance()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}