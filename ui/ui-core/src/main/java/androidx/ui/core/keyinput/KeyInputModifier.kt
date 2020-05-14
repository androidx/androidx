/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.keyinput

import androidx.ui.core.Modifier
import androidx.ui.core.composed

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a Composable will
 * allow it to intercept hardware key events. In the future, this will have a parameter to allow
 * receiving and filtering events.
 */
fun Modifier.keyInputFilter(): Modifier = composed {
    KeyInputModifier()
}

internal class KeyInputModifier : Modifier.Element {
    var keyInputNode: ModifiedKeyInputNode? = null
}