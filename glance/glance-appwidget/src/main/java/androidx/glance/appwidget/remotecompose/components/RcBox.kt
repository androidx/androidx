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
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.appwidget.remotecompose.toBoxLayoutEnum
import androidx.glance.layout.EmittableBox

internal class RcBox(
    emittable: EmittableBox,
    translationContext: TranslationContext,
    modifierOverride: RecordingModifier?,
) : RcElement(translationContext) {

    override val outputModifier: RecordingModifier

    private val children = mutableListOf<RcElement>()
    private val horizontalAlign: Int =
        emittable.contentAlignment.horizontal.toBoxLayoutEnum() // horizontal alignment
    private val verticalAlign: Int =
        emittable.contentAlignment.vertical.toBoxLayoutEnum() // vertical alignment

    init {
        outputModifier =
            modifierOverride
                ?: convertGlanceModifierToRemoteComposeModifier(
                    modifiers = emittable.modifier,
                    translationContext = translationContext,
                )

        children.addAll(emittable.translateChildren(translationContext))
    }

    override fun writeComponent(translationContext: TranslationContext) {
        val rcContext = translationContext.remoteComposeContext
        rcContext.box(outputModifier, horizontalAlign, verticalAlign) {
            @Suppress("ListIterator")
            children.forEach { child -> child.writeComponent(translationContext) }
        }
    }
}
