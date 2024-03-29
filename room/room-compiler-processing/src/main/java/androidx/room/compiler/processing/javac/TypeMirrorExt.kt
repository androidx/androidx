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

package androidx.room.compiler.processing.javac

import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.SimpleTypeVisitor8

internal fun TypeMirror.extendsBound(): TypeMirror? {
    return this.accept(
        object : SimpleTypeVisitor8<TypeMirror?, Void?>() {
            override fun visitWildcard(type: WildcardType, ignored: Void?): TypeMirror? {
                return type.extendsBound ?: type.superBound
            }
        },
        null
    )
}
