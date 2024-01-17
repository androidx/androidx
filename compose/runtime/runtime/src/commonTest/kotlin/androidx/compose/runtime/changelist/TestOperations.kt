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

package androidx.compose.runtime.changelist

import androidx.compose.runtime.changelist.Operation.TestOperation

internal object TestOperations {
    val NoArgsOperation = TestOperation()

    val OneIntOperation = TestOperation(ints = 1)
    val TwoIntsOperation = TestOperation(ints = 2)
    val ThreeIntsOperation = TestOperation(ints = 3)

    val OneObjectOperation = TestOperation(objects = 1)
    val TwoObjectsOperation = TestOperation(objects = 2)
    val ThreeObjectsOperation = TestOperation(objects = 3)

    val MixedOperation = TestOperation(ints = 2, objects = 2)
}
