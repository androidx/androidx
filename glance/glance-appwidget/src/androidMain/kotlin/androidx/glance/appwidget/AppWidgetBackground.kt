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

package androidx.glance.appwidget

import androidx.glance.GlanceModifier

/**
 * Define the current view as the background of the App Widget.
 *
 * By definition, the background of the App Widget is the view with the id `@android:id/background`.
 *
 * There can be only one view with this modifier in a given App Widget.
 *
 * See the documentation on
 * [Enable smoother transitions](https://developer.android.com/guide/topics/appwidgets/enhance#enable-smoother-transitions)
 * and
 * [Implement rounded corners](https://developer.android.com/guide/topics/appwidgets#rounded-corner)
 * for details on why it is important to label a view as being the background, and why you will want
 * to set the [cornerRadius] of the same view.
 */
fun GlanceModifier.appWidgetBackground(): GlanceModifier =
    this.then(AppWidgetBackgroundModifier)

internal object AppWidgetBackgroundModifier : GlanceModifier.Element
