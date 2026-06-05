/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear

import kotlin.reflect.KClass

/**
 * Annotation used to associate a [GlanceWearWidget] class with its corresponding
 * [GlanceWearWidgetService].
 *
 * This association must be used in [GlanceWearWidgetService] to specify which associated widget
 * class it has in its `widget` property. It must match the exact class name of the widget class
 * used in the property.
 *
 * If this is not specified, correct results of some methods in [GlanceWearWidgetManager] are not
 * guaranteed.
 *
 * For example:
 * ```
 * @AssociateWithGlanceWearWidget(MyGlanceWearWidget::class)
 * class MyGlanceWearWidgetService : GlanceWearWidgetService() {
 *     override val widget = MyGlanceWearWidget()
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class AssociateWithGlanceWearWidget(
    public val value: KClass<out GlanceWearWidget>
)
