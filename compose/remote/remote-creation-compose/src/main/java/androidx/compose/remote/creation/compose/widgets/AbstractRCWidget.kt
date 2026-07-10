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

package androidx.compose.remote.creation.compose.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/** Represents a widget (either compose-based or procedural) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AbstractRCWidget {
    public companion object {
        public val ACTION: String = "androidx.compose.remote.ACTION_CLICKED"
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public abstract fun createRemoteView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        provider: RemoteComposeWidget,
        widgetId: Int,
    )

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public abstract fun updateRemoteView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        provider: RemoteComposeWidget,
        widgetId: Int,
        lambdaId: Int,
    )
}
