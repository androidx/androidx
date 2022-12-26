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

package androidx.window.samples.embedding

import android.app.Activity
import androidx.annotation.Sampled
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.SplitController

@OptIn(ExperimentalWindowApi::class)
@Sampled
suspend fun expandPrimaryContainer() {
    SplitController.getInstance(primaryActivity).splitInfoList(primaryActivity)
        .collect { splitInfoList ->
            // Find all associated secondary ActivityStacks
            val associatedSecondaryActivityStacks = splitInfoList
                .mapTo(mutableSetOf()) { splitInfo -> splitInfo.secondaryActivityStack }
            // Finish them all.
            ActivityEmbeddingController.getInstance(primaryActivity)
                .finishActivityStacks(associatedSecondaryActivityStacks)
        }
}

val primaryActivity = Activity()
