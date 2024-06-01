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

package androidx.compose.testutils

import androidx.compose.ui.Modifier

fun Modifier.toList(): List<Modifier.Element> =
    foldIn(mutableListOf()) { acc, e -> acc.apply { acc.add(e) } }

@Suppress("ModifierFactoryReturnType") fun Modifier.first(): Modifier.Element = toList().first()

/**
 * Asserts that creating two modifier with the same inputs will resolve to true when compared. In a
 * similar fashion, toggling the inputs will resolve to false. Ideally, all modifier elements should
 * preserve this property so when creating a modifier, an additional test should be included to
 * guarantee that.
 */
fun assertModifierIsPure(createModifier: (toggle: Boolean) -> Modifier) {
    val first = createModifier(true)
    val second = createModifier(false)
    val third = createModifier(true)

    assert(first == third) { "Modifier with same inputs should resolve true to equals call" }
    assert(first != second) { "Modifier with different inputs should resolve false to equals call" }
}
