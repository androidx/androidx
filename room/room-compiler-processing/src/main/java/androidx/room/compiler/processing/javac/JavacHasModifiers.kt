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

import androidx.room.compiler.processing.XHasModifiers
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

/**
 * Implementation of [XHasModifiers] for java elements
 */
internal class JavacHasModifiers(private val element: Element) : XHasModifiers {

    override fun isPublic(): Boolean {
        return element.modifiers.contains(Modifier.PUBLIC)
    }

    override fun isProtected(): Boolean {
        return element.modifiers.contains(Modifier.PROTECTED)
    }

    override fun isAbstract(): Boolean {
        return element.modifiers.contains(Modifier.ABSTRACT)
    }

    override fun isPrivate(): Boolean {
        return element.modifiers.contains(Modifier.PRIVATE)
    }

    override fun isStatic(): Boolean {
        return element.modifiers.contains(Modifier.STATIC)
    }

    override fun isTransient(): Boolean {
        return element.modifiers.contains(Modifier.TRANSIENT)
    }

    override fun isFinal(): Boolean {
        return element.modifiers.contains(Modifier.FINAL)
    }
}