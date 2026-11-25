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
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate

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
 * by the platform and this operation completes as a safe no-op, once [widgetName] has been
 * resolved.
 *
 * Each call counts against the platform's preview update rate limit, so call this only when the
 * preview content changes, for example after a data change rather than on every app start.
 *
 * Picker categories with the same layout, such as home screen and search box, are set with one
 * platform call. Categories with different layouts, such as home screen and keyguard, are set
 * independently: once every category has been attempted, the first failure is thrown with the
 * others attached as suppressed exceptions. If the platform rejects a preview, typically because of
 * its rate limit, the remaining categories are not attempted, as they would be rejected too. A
 * failure never clears a preview: the picker keeps the last accepted one, or the static preview
 * declared by the widget.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param previewData Declarative template data payload implementing [GlanceAdaptiveTemplate] to
 *   render as a preview.
 * @throws IllegalArgumentException if [widgetName] cannot be resolved, see
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @throws IllegalStateException if this manager was not created for AppWidget hosts, if
 *   [widgetName] cannot be resolved (see [GlanceAdaptiveWidgetReceiver.widgetName]), or if the
 *   platform rejects a preview, typically because of its rate limit. Do not retry immediately: call
 *   this again only when the preview content changes.
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
 * It is up to the launcher to accept the request, typically after asking the user to confirm, so
 * `true` only means that the request was handed to the launcher. There is no success or failure
 * callback: once pinned, the widget is updated through [GlanceAdaptiveWidgetReceiver.onUpdate] like
 * a widget placed from the picker, and the platform reports nothing if the user dismisses the
 * request. Every failure that can happen before the request is shown throws from this call.
 *
 * Pinning is only supported by AppWidget hosts, so this operation is provided by this artifact as
 * an extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * On devices running Android 7.1 and earlier (pre-API 26), pinning is not supported by the platform
 * and this returns `false`. It also returns `false` if the launcher does not support pinning, if
 * the receiver declaring [widgetName] is disabled, or if the launcher refuses the request.
 *
 * [widgetId] and [options] are stored in the options of the pinned widget once the launcher reports
 * its appWidgetId, which the platform does only once. If that report is lost, or storing them keeps
 * failing, the widget keeps the widgetId generated by [GlanceAdaptiveWidgetReceiver] and does not
 * receive [options], so render a sensible default for an unknown widgetId.
 *
 * [widgetId] may already be used by placed widgets: every widget sharing a widgetId receives the
 * updates targeted at it.
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
 * @return `true` if the request was handed to the launcher, `false` otherwise.
 * @throws IllegalArgumentException if [widgetId] is blank, if [widgetName] cannot be resolved (see
 *   [GlanceAdaptiveWidgetReceiver.widgetName]), or if its provider does not declare the home screen
 *   widget category.
 * @throws IllegalStateException if this manager was not created for AppWidget hosts, if the app has
 *   no foreground activity or foreground service, or if [widgetName] cannot be resolved (see
 *   [GlanceAdaptiveWidgetReceiver.widgetName]).
 * @see android.appwidget.AppWidgetManager.requestPinAppWidget
 */
public suspend fun GlanceAdaptiveWidgetManager.requestPin(
    widgetName: String,
    widgetId: String? = null,
    initialData: GlanceAdaptiveTemplate? = null,
    options: Bundle = Bundle.EMPTY,
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
    )
}

/**
 * Completes the configuration of a widget instance from within its configuration [Activity].
 *
 * Stores [widgetId] and [options] in the options of the widget being configured, then sets the
 * [Activity.RESULT_OK] result expected by the AppWidget host. Call this before [Activity.finish].
 * This automatically triggers [GlanceAdaptiveWidgetReceiver.onUpdate], where the configured
 * [widgetId] and [options] are available via [GlanceAdaptiveWidgetManager.getActiveInstances].
 * Unlike standard [android.appwidget.AppWidgetProvider] configuration activities, callers do not
 * need to push an initial widget update before finishing the [Activity].
 *
 * Failures to read or store the options of the widget are propagated, and the result is not set if
 * this throws. Exceptions reach the caller, never the host: if the [Activity] then finishes without
 * [Activity.RESULT_OK], the host removes a newly placed widget, or keeps the previous configuration
 * of a reconfigured one.
 *
 * Example usage in a configuration [Activity]:
 * ```kotlin
 * GlanceAdaptiveWidgetManager(this).setConfigurationResult(
 *     activity = this,
 *     widgetId = "bucharest_weather",
 *     options = bundleOf("location_id" to "bucharest"),
 * )
 * finish()
 * ```
 *
 * @param activity Configuration [Activity] started with
 *   [android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE].
 * @param widgetId Optional developer-assigned widget instance String identifier for the configured
 *   widget. If omitted, the one already stored for the widget is kept, or a unique identifier is
 *   generated if there is none. It must not be blank.
 * @param options Optional configuration options to store in the options of the configured widget.
 * @throws IllegalArgumentException if [activity]'s intent does not carry a valid
 *   [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID], or if [widgetId] is blank.
 * @throws IllegalStateException if this manager was not created for AppWidget hosts.
 * @see GlanceAdaptiveWidgetReceiver.onUpdate
 * @see GlanceAdaptiveWidgetManager.getActiveInstances
 * @see GlanceAdaptiveWidgetManager.pushUpdate
 */
public fun GlanceAdaptiveWidgetManager.setConfigurationResult(
    activity: Activity,
    widgetId: String? = null,
    options: Bundle = Bundle.EMPTY,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    check(delegate is GlanceAdaptiveAppWidgetDelegate) {
        "setConfigurationResult requires a GlanceAdaptiveWidgetManager created for AppWidget hosts"
    }
    delegate.setConfigurationResult(activity = activity, widgetId = widgetId, options = options)
}

/**
 * Registers [renderer] to render [T] templates for [widgetName] on [surface], overriding the
 * default renderer.
 *
 * Registration is process-wide. Registering from [android.app.Application.onCreate] is recommended
 * so it is in place before any widget renders. Calling this again for the same [widgetName], [T],
 * and [surface] replaces the previous renderer, and placed widgets use it on their next update.
 * Does not check whether [widgetName] matches a declared receiver in the manifest.
 *
 * @param T concrete [GlanceAdaptiveTemplate] type matched by exact runtime class
 * @param widgetName non-blank widget identifier matching [GlanceAdaptiveWidgetReceiver.widgetName]
 * @param surface target [AppWidgetSurface]
 * @param renderer renders [T] into [AppWidgetRenderOutput]; must handle `Dimensions(0, 0)`
 * @throws IllegalStateException if this manager was not created for AppWidget hosts
 * @throws IllegalArgumentException if [widgetName] is blank or [T] is an interface or abstract
 *   class
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
        "registerRenderer requires a GlanceAdaptiveWidgetManager created for AppWidget hosts"
    }
    AppWidgetTemplateRegistry.register(
        widgetName = widgetName,
        templateClass = templateClass,
        renderer = renderer,
        surface = surface,
    )
}

/**
 * Removes the custom renderer registered for [widgetName], [T], and [surface], restoring the
 * default renderer on subsequent updates.
 *
 * Applies process-wide. Does nothing if no custom renderer is registered for [widgetName], [T], and
 * [surface]. Does not check whether [widgetName] matches a declared receiver in the manifest.
 *
 * Intended mainly for test cleanup or disabling a custom renderer at runtime; apps that register a
 * renderer in [android.app.Application.onCreate] do not need to unregister it.
 *
 * @param T concrete [GlanceAdaptiveTemplate] type matched by exact runtime class
 * @param widgetName non-blank widget identifier matching [GlanceAdaptiveWidgetReceiver.widgetName]
 * @param surface target [AppWidgetSurface]
 * @throws IllegalStateException if this manager was not created for AppWidget hosts
 * @throws IllegalArgumentException if [widgetName] is blank or [T] is an interface or abstract
 *   class
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : GlanceAdaptiveTemplate> GlanceAdaptiveWidgetManager
    .unregisterRenderer(
    widgetName: String,
    surface: AppWidgetSurface,
): Unit = unregisterRendererForClass(widgetName, surface, T::class.java)

/** Non-inline implementation of [unregisterRenderer], taking the erased [templateClass]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@PublishedApi
internal fun <T : GlanceAdaptiveTemplate> GlanceAdaptiveWidgetManager.unregisterRendererForClass(
    widgetName: String,
    surface: AppWidgetSurface,
    templateClass: Class<T>,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    check(delegate is GlanceAdaptiveAppWidgetDelegate) {
        "unregisterRenderer requires a GlanceAdaptiveWidgetManager created for AppWidget hosts"
    }
    AppWidgetTemplateRegistry.unregister(
        widgetName = widgetName,
        templateClass = templateClass,
        surface = surface,
    )
}
