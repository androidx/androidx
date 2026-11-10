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

package androidx.glance.adaptive.appwidget

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import java.lang.reflect.Modifier

/**
 * Creates a [GlanceAdaptiveWidgetManager] instance to work with the AppWidget framework on mobile
 * surfaces such as home screen and lock screen.
 */
public fun GlanceAdaptiveWidgetManager(context: Context): GlanceAdaptiveWidgetManager =
    GlanceAdaptiveWidgetManager(BaseWidgetDelegate(context))

/**
 * Sets dynamic preview data rendered in host widget pickers for the specified [widgetName].
 *
 * On devices running Android 14 and earlier (pre-API 35), dynamic widget previews are not supported
 * by the platform and this operation completes as a safe no-op.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param previewData Declarative template data payload implementing [GlanceAdaptiveTemplate] to
 *   render as a preview.
 * @throws IllegalArgumentException if [widgetName] cannot be resolved, see
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @throws IllegalStateException if this manager was not created for AppWidget hosts, or if
 *   [widgetName] cannot be resolved, see [GlanceAdaptiveWidgetReceiver.widgetName].
 * @see android.appwidget.AppWidgetManager.setWidgetPreview
 * @see GlanceAdaptiveWidgetManager.pushUpdate
 */
public suspend fun GlanceAdaptiveWidgetManager.setPreview(
    widgetName: String,
    previewData: GlanceAdaptiveTemplate,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    check(delegate is GlanceAdaptiveAppWidgetDelegate) {
        "setPreview requires a GlanceAdaptiveWidgetManager created for AppWidget hosts"
    }
    delegate.setPreview(widgetName = widgetName, previewData = previewData)
}

/**
 * Requests to pin the widget matching [widgetName] on the host (if supported).
 *
 * Note: Currently supported only by home screen launchers. On unsupported surfaces or on devices
 * running Android 7.1 and earlier (pre-API 26), widget pinning is not supported and this operation
 * completes as a safe no-op and returns `false`.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param widgetId Optional developer-assigned widget instance String identifier for the pinned
 *   widget. If omitted, a unique identifier is generated. It must not be blank.
 * @param initialData Optional declarative template data payload implementing
 *   [GlanceAdaptiveTemplate] to render as the preview shown while the launcher asks the user to
 *   confirm. If omitted, the launcher shows the preview declared by the widget instead.
 * @param options Optional initial configuration options to store in the options of the pinned
 *   widget.
 * @param successCallback Optional [PendingIntent] sent once the widget is pinned and [widgetId] and
 *   [options] are stored.
 * @return `true` if the request was sent to the launcher, `false` otherwise.
 * @throws IllegalArgumentException if [widgetId] is blank, or if [widgetName] cannot be resolved,
 *   see [GlanceAdaptiveWidgetReceiver.widgetName].
 * @throws IllegalStateException if the app has no foreground activity or foreground service, if
 *   this manager was not created for AppWidget hosts, or if [widgetName] cannot be resolved, see
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @see android.appwidget.AppWidgetManager.requestPinAppWidget
 */
public suspend fun GlanceAdaptiveWidgetManager.requestPin(
    widgetName: String,
    widgetId: String? = null,
    initialData: GlanceAdaptiveTemplate? = null,
    options: Bundle = Bundle.EMPTY,
    successCallback: PendingIntent? = null,
): Boolean {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    check(delegate is GlanceAdaptiveAppWidgetDelegate) {
        "requestPin requires a GlanceAdaptiveWidgetManager created for AppWidget hosts"
    }
    return delegate.requestPin(
        widgetName = widgetName,
        widgetId = widgetId,
        initialData = initialData,
        options = options,
        successCallback = successCallback,
    )
}

/**
 * Completes the configuration of a widget instance from within its configuration [Activity].
 *
 * Stores [widgetId] and [options] in the options of the widget being configured, then sets the
 * [Activity.RESULT_OK] result expected by the AppWidget host. Call this before [Activity.finish].
 * Failures to read or store the options of the widget are propagated, and the result is not set if
 * this throws. Exceptions reach the caller, never the host: if the [Activity] then finishes without
 * [Activity.RESULT_OK], the host removes a newly placed widget, or keeps the previous configuration
 * of a reconfigured one.
 *
 * This is a blocking call that makes synchronous [android.appwidget.AppWidgetManager] Binder calls
 * and returns only once the options are stored and the result is set. It is not a suspend function
 * so that the result is always set before the caller finishes the [Activity].
 *
 * Configuration activities are only used by AppWidget hosts, so this operation is provided by this
 * artifact as an extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * @param activity Configuration [Activity] started with
 *   [android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE].
 * @param widgetId Optional developer-assigned widget instance String identifier for the configured
 *   widget. If omitted, the one already stored for the widget is kept, or a unique identifier is
 *   generated if there is none. It must not be blank.
 * @param options Optional configuration options to store in the options of the configured widget.
 * @throws IllegalArgumentException if [activity]'s intent does not carry a valid
 *   [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID], or if [widgetId] is blank.
 * @throws IllegalStateException if this manager is not backed by an AppWidget delegate, for example
 *   if it was created for another surface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceAdaptiveWidgetManager.setConfigurationResult(
    activity: Activity,
    widgetId: String? = null,
    options: Bundle = Bundle.EMPTY,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    check(delegate is GlanceAdaptiveAppWidgetDelegate) {
        "setConfigurationResult requires a GlanceAdaptiveWidgetManager backed by an AppWidget " +
            "delegate"
    }
    delegate.setConfigurationResult(activity = activity, widgetId = widgetId, options = options)
}

/**
 * Draws templates of type [T] for [widgetName] on [surface] with [renderer] instead of the library
 * default.
 *
 * - Applies to the whole process, not only to this manager. Call it from
 *   [android.app.Application.onCreate], which runs before any widget renders, even when the process
 *   starts for a widget broadcast.
 * - The last registration for a widget, template and surface wins, including over the library
 *   default. Placed widgets switch to it on their next update.
 *
 * Renderers produce AppWidget-specific output, so this operation is provided by this artifact as an
 * extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * @param T The concrete template type implementing [GlanceAdaptiveTemplate], inferred from
 *   [renderer]. Templates are matched by their exact runtime class.
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param surface The [AppWidgetSurface] to draw [T] on.
 * @param renderer Draws [T] as [AppWidgetRenderOutput]. The
 *   [androidx.glance.adaptive.core.selection.GlanceSurface.dimensions] of the surface it receives
 *   may be `(0, 0)` when the host has not reported a positive size or during widget picker preview
 *   rendering, so it must handle zero dimensions.
 * @throws IllegalStateException if this manager is not backed by an AppWidget delegate, for example
 *   if it was created for another surface.
 * @throws IllegalArgumentException if [T] is an interface or an abstract class, as no template
 *   instance can have it as its runtime class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : GlanceAdaptiveTemplate> GlanceAdaptiveWidgetManager.registerRenderer(
    widgetName: String,
    surface: AppWidgetSurface,
    renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
): Unit = registerRendererForClass(widgetName, surface, T::class.java, renderer)

/** Non-inline implementation of [registerRenderer], taking the erased [templateClass]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@PublishedApi
internal fun <T : GlanceAdaptiveTemplate> GlanceAdaptiveWidgetManager.registerRendererForClass(
    widgetName: String,
    surface: AppWidgetSurface,
    templateClass: Class<T>,
    renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    check(delegate is GlanceAdaptiveAppWidgetDelegate) {
        "registerRenderer requires a GlanceAdaptiveWidgetManager backed by an AppWidget delegate"
    }
    require(!templateClass.isInterface && !Modifier.isAbstract(templateClass.modifiers)) {
        "registerRenderer requires a concrete template class, but got: ${templateClass.name}"
    }
    AppWidgetTemplateRegistry.register(
        widgetName = widgetName,
        templateClass = templateClass,
        renderer = renderer,
        surface = surface,
    )
}
