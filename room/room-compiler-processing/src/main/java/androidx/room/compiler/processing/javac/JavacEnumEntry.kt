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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XEnumEntry
import androidx.room.compiler.processing.XEnumTypeElement
import javax.lang.model.element.Element

internal class JavacEnumEntry(
    env: JavacProcessingEnv,
    entryElement: Element,
    override val enclosingElement: XEnumTypeElement
) : JavacElement(env, entryElement), XEnumEntry {

    override val name: String
        get() = element.simpleName.toString()

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(name, enclosingElement)
    }

    override val fallbackLocationText: String
        get() = "$name enum entry in ${enclosingElement.fallbackLocationText}"

    override val closestMemberContainer: XEnumTypeElement
        get() = enclosingElement
}