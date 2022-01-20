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

package androidx.glance.appwidget.translators

import android.widget.RemoteViews
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.insertView

import androidx.glance.appwidget.EmittableCircularProgressIndicator

internal fun RemoteViews.translateEmittableCircularProgressIndicator(
    translationContext: TranslationContext,
    element: EmittableCircularProgressIndicator
) {
    val viewDef = insertView(translationContext,
                             LayoutType.CircularProgressIndicator,
                             element.modifier)
    setProgressBar(viewDef.mainViewId, 0, 0, true)
    applyModifiers(translationContext, this, element.modifier, viewDef)
}
