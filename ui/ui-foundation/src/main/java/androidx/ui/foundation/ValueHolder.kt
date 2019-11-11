/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

/**
 * A value holder that is responsible for containing one value if type T.
 *
 * This interface is particularly useful to implement in `@Model` classes, to provide for users
 * of this class a choice to consume value (therefore being recomposed every time value changes)
 * or consume ValueHolder and decide themselves when to read this value.
 *
 * On of the examples might be when you want to reflect some animation / dragging changes in ui
 * but you don't want to be recomposed every time, but only relayout. To achieve this, might
 * require ValueHolder to be passed to you and read it during layout stage, avoiding recomposition.
 *
 * @property value variable value that this holder holds
 */
interface ValueHolder<T : Any> {
    val value: T
}