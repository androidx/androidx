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

package androidx.serialization.compiler.processing

import com.google.auto.common.MoreTypes
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/** Get the element represented by this type using [MoreTypes.asElement]. */
internal fun TypeMirror.asElement(): Element {
    return MoreTypes.asElement(this)
}

/** Convenience wrapper for [asElement] that casts to type element. */
internal fun TypeMirror.asTypeElement(): TypeElement {
    return asElement().asTypeElement()
}
