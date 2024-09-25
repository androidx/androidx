/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.multiprocess

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.AppWidgetSession
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceComponents
import androidx.glance.appwidget.R
import androidx.glance.session.GlanceSessionManager
import androidx.glance.session.SessionManager
import androidx.work.multiprocess.RemoteWorkerService

/**
 * MultiProcessGlanceAppWidget can be used with [androidx.glance.appwidget.GlanceAppWidgetReceiver]
 * to support multiprocess use cases where different widget receivers run in different processes.
 * Note that the worker must still run in the same process as the receiver.
 */
public abstract class MultiProcessGlanceAppWidget(
    @LayoutRes internal open val errorUiLayout: Int = R.layout.glance_error_layout,
) : GlanceAppWidget(errorUiLayout) {
    /**
     * Override [getMultiProcessConfig] to provide a
     * [androidx.work.multiprocess.RemoteWorkerService] that runs in the same process as the
     * [androidx.glance.appwidget.GlanceAppWidgetReceiver] that this is attached to.
     *
     * If null, then this widget will be run with normal WorkManager, i.e. the same behavior as
     * GlanceAppWidget.
     */
    public open fun getMultiProcessConfig(context: Context): MultiProcessConfig? = null

    @RestrictTo(Scope.LIBRARY_GROUP)
    final override fun getComponents(context: Context): GlanceComponents? =
        getMultiProcessConfig(context)?.toGlanceComponents()

    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final override fun getSessionManager(context: Context): SessionManager =
        if (getMultiProcessConfig(context) != null) {
            RemoteSessionManager
        } else {
            GlanceSessionManager
        }

    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final override fun createAppWidgetSession(
        context: Context,
        id: AppWidgetId,
        options: Bundle?
    ): AppWidgetSession {
        return getMultiProcessConfig(context)?.let { config ->
            RemoteAppWidgetSession(this, config.remoteWorkerService, id, options)
        } ?: AppWidgetSession(this, id, options)
    }
}

/**
 * This class specifies the components to be used when creating layouts for
 * [MultiProcessGlanceAppWidget].
 */
public class MultiProcessConfig(
    /**
     * The remote worker service used to run jobs for a [MultiProcessGlanceAppWidget]. This must be
     * set to run in the same `android:process` as the
     * [androidx.glance.appwidget.GlanceAppWidgetReceiver] and Glance components.
     */
    public val remoteWorkerService: ComponentName,
    /**
     * Action Trampoline Activity. Must be set to run in the same process as
     * [androidx.glance.appwidget.GlanceAppWidgetReceiver] and [remoteWorkerService].
     */
    public val actionTrampolineActivity: ComponentName,
    /**
     * Invisible Action Trampoline Activity. Must be set to run in the same process as
     * [androidx.glance.appwidget.GlanceAppWidgetReceiver] and [remoteWorkerService].
     */
    public val invisibleActionTrampolineActivity: ComponentName,
    /**
     * Action callback BroadcastReceiver. Must be set to run in the same process as
     * [androidx.glance.appwidget.GlanceAppWidgetReceiver] and [remoteWorkerService].
     */
    public val actionCallbackBroadcastReceiver: ComponentName,
    /**
     * Glance RemoteViewsService. Must be set to run in the same process as
     * [androidx.glance.appwidget.GlanceAppWidgetReceiver] and [remoteWorkerService].
     */
    public val remoteViewsService: ComponentName,
) {

    public companion object {
        public fun getDefault(context: Context): MultiProcessConfig =
            GlanceComponents.getDefault(context).run {
                MultiProcessConfig(
                    remoteWorkerService = ComponentName(context, RemoteWorkerService::class.java),
                    actionTrampolineActivity = actionTrampolineActivity,
                    invisibleActionTrampolineActivity = invisibleActionTrampolineActivity,
                    actionCallbackBroadcastReceiver = actionCallbackBroadcastReceiver,
                    remoteViewsService = remoteViewsService,
                )
            }
    }

    internal fun toGlanceComponents() =
        GlanceComponents(
            actionTrampolineActivity,
            invisibleActionTrampolineActivity,
            actionTrampolineActivity,
            remoteViewsService
        )
}
