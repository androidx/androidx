/*
 * Copyright 2025 The Android Open Source Project
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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose.components

import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.appwidget.remotecompose.GlanceRemoteComposeTranslator
import androidx.glance.appwidget.remotecompose.TranslationContext

/**
 * A base class for an element that is being translated from a Glance emittable to remote compose.
 * These elements are built up into a tree, then written to the remote compose document. This
 * intermediate state is required because certain information needs to be placed at the start of the
 * remote compose document, so two passes through the element tree are needed.
 */
internal abstract class RcElement(translationContext: TranslationContext) {

    val viewId: Int = translationContext.nextId()
    abstract val outputModifier: RecordingModifier

    abstract fun writeComponent(translationContext: TranslationContext)
}

@Suppress("ListIterator")
internal fun EmittableWithChildren.translateChildren(
    translationContext: TranslationContext
): List<RcElement> {
    return children.map { child: Emittable ->
        GlanceRemoteComposeTranslator.translateEmittable(child, translationContext)
    }
}
