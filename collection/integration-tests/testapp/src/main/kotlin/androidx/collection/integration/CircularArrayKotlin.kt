/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.integration

import androidx.collection.CircularArray

/** Integration test that uses CircularArray to assert source compatibility. */
@Suppress("unused")
fun circularArraySourceCompatibility(): Boolean {
    val circularArray = CircularArray<Int>()

    @Suppress("UsePropertyAccessSyntax", "ReplaceCallWithBinaryOperator", "ReplaceGetOrSet")
    return circularArray.first.equals(null) &&
        circularArray.getFirst().equals(null) &&
        circularArray.last.equals(null) &&
        circularArray.getLast().equals(null) &&
        circularArray[0].equals(circularArray.get(1)) &&
        circularArray.isEmpty && circularArray.isEmpty() &&
        circularArray.size().equals(0)
}
