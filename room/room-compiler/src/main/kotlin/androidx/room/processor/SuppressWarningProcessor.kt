/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.compiler.processing.XElement
import androidx.room.vo.Warning

/**
 * A visitor that reads SuppressWarnings annotations and keeps the ones we know about.
 */
object SuppressWarningProcessor {

    fun getSuppressedWarnings(element: XElement): Set<Warning> = buildSet {
        element.getAnnotation(SuppressWarnings::class)?.value?.let {
            addAll(it.value.mapNotNull(Warning.Companion::fromPublicKey))
        }
        element.getAnnotation(Suppress::class)?.value?.let {
            addAll(it.names.mapNotNull(Warning.Companion::fromPublicKey))
        }
    }
}
