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

package androidx.navigation3

import androidx.compose.runtime.Composable

/**
 * Entry maintains and stores the key and the content represented by that key. Entries should be
 * created as part of a [NavDisplay.entryProvider](reference/androidx/navigation/NavDisplay).
 *
 * @param key key for this entry
 * @param featureMap map of the available features from a display
 * @param content content for this entry to be displayed when this entry is active
 */
public open class NavEntry<T : Any>(
    public open val key: T,
    public open val featureMap: Map<String, Any> = emptyMap(),
    public open val content: @Composable (T) -> Unit
)
