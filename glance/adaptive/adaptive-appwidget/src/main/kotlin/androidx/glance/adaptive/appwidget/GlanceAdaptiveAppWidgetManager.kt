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
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.appwidget.ui.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import java.lang.reflect.Modifier

/**
 * Creates a [GlanceAdaptiveWidgetManager] configured with the platform AppWidget delegate for phone
 * widget operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceAdaptiveWidgetManager(context: Context): GlanceAdaptiveWidgetManager =
    GlanceAdaptiveWidgetManager(BaseWidgetDelegate(context))

/**
 * Sets dynamic preview data rendered in host widget pickers for the specified widget definition.
 *
 * Dynamic previews are only supported by AppWidget hosts, so this operation is provided by this
 * artifact as an extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * On devices running Android 14 and earlier (pre-API 35), dynamic widget previews are not supported
 * by the platform and this operation completes as a safe no-op. It is also a no-op if this manager
 * is not backed by an AppWidget delegate, for example if it was created for another surface.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param previewData Declarative template data payload implementing [GlanceAdaptiveTemplate] to
 *   render as a preview.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun GlanceAdaptiveWidgetManager.setPreview(
    widgetName: String,
    previewData: GlanceAdaptiveTemplate,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    if (delegate !is GlanceAdaptiveAppWidgetDelegate) {
        Log.w(TAG, "Ignoring setPreview for widgetName $widgetName: no AppWidget delegate")
        return
    }
    delegate.setPreview(widgetName = widgetName, previewData = previewData)
}

/**
 * Requests that the launcher pin the specified widget definition to the home screen.
 *
 * It is up to the launcher to accept the request, typically after asking the user to confirm, so
 * `true` only means that the request was sent. [successCallback] is sent once the widget is pinned.
 *
 * Pinning is only supported by AppWidget hosts, so this operation is provided by this artifact as
 * an extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * On devices running Android 7.1 and earlier (pre-API 26), pinning is not supported by the platform
 * and this returns `false`. It also returns `false` if the launcher does not support pinning, if no
 * receiver matching [widgetName] can be placed on the home screen, or if this manager is not backed
 * by an AppWidget delegate.
 *
 * Widget names should be unique per app. If several receivers match [widgetName], a warning is
 * logged and the first one that can be placed on the home screen is pinned.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param widgetId Optional developer-assigned widget instance String identifier for the pinned
 *   widget. If omitted or blank, the [GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID] in [options] is
 *   used, or a unique identifier is generated if there is none.
 * @param initialData Optional declarative template data payload implementing
 *   [GlanceAdaptiveTemplate] to render as the preview shown while the launcher asks the user to
 *   confirm. If omitted, the launcher shows the preview declared by the widget instead.
 * @param options Optional initial configuration options to store in the options of the pinned
 *   widget.
 * @param successCallback Optional [PendingIntent] sent once the widget is pinned and [widgetId] and
 *   [options] are stored.
 * @return `true` if the request was sent to the launcher, `false` otherwise.
 * @throws IllegalStateException if the app has no foreground activity or foreground service.
 * @see android.appwidget.AppWidgetManager.requestPinAppWidget
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun GlanceAdaptiveWidgetManager.requestPin(
    widgetName: String,
    widgetId: String? = null,
    initialData: GlanceAdaptiveTemplate? = null,
    options: Bundle = Bundle.EMPTY,
    successCallback: PendingIntent? = null,
): Boolean {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    if (delegate !is GlanceAdaptiveAppWidgetDelegate) {
        Log.w(TAG, "Ignoring requestPin for widgetName $widgetName: no AppWidget delegate")
        return false
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
 *   widget. If omitted or blank, the [GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID] in [options] or
 *   already stored for the widget is used, or a unique identifier is generated if there is none.
 * @param options Optional configuration options to store in the options of the configured widget.
 * @throws IllegalArgumentException if [activity]'s intent does not carry a valid
 *   [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID].
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
 * Draws templates of type [T] for [widgetName] on [surface] with [selector] and [renderer] instead
 * of the library default.
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
 *   [selector] and [renderer]. Templates are matched by their exact runtime class.
 * @param A The archetype type produced by [selector] and drawn by [renderer].
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param surface The [AppWidgetSurface] to draw [T] on.
 * @param selector Resolves the layout archetype. The
 *   [androidx.glance.adaptive.core.ui.selection.GlanceSurface.dimensions] of the surface it
 *   receives may be `(0, 0)` when the host has not reported a positive size or during widget picker
 *   preview rendering, so it must handle zero dimensions.
 * @param renderer Draws the archetype chosen by [selector] as [AppWidgetRenderOutput].
 * @throws IllegalStateException if this manager is not backed by an AppWidget delegate, for example
 *   if it was created for another surface.
 * @throws IllegalArgumentException if [T] is an interface or an abstract class, as no template
 *   instance can have it as its runtime class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : GlanceAdaptiveTemplate, A : Any> GlanceAdaptiveWidgetManager
    .registerRenderer(
    widgetName: String,
    surface: AppWidgetSurface,
    // TODO(b/577977039): Take a single parameter pairing the selector and the renderer once its
    //  shape is agreed.
    selector: ArchetypeSelector<T, AppWidgetSurface, A>,
    renderer: TemplateRenderer<T, AppWidgetSurface, A, AppWidgetRenderOutput>,
): Unit = registerRendererForClass(widgetName, surface, T::class.java, selector, renderer)

/** Non-inline implementation of [registerRenderer], taking the erased [templateClass]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@PublishedApi
internal fun <T : GlanceAdaptiveTemplate, A : Any> GlanceAdaptiveWidgetManager
    .registerRendererForClass(
    widgetName: String,
    surface: AppWidgetSurface,
    templateClass: Class<T>,
    selector: ArchetypeSelector<T, AppWidgetSurface, A>,
    renderer: TemplateRenderer<T, AppWidgetSurface, A, AppWidgetRenderOutput>,
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
        selector = selector,
        renderer = renderer,
        surface = surface,
    )
}

private const val TAG = "GlanceAdaptiveAppWidget"
