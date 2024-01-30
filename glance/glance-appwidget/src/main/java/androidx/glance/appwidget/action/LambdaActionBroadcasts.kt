/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget.action

import android.content.ComponentName
import android.content.Intent

/**
 * This class contains constants that are used for broadcast intents that trigger lambda actions.
 *
 * When applying a lambda action, we create the intent based on the actionkey assigned to that
 * lambda. When the action is triggered, a broadcast is sent to the GlanceAppWidgetReceiver to
 * trigger the lambda in the corresponding session.
 */
internal object LambdaActionBroadcasts {
    internal const val ActionTriggerLambda = "ACTION_TRIGGER_LAMBDA"
    internal const val ExtraActionKey = "EXTRA_ACTION_KEY"
    internal const val ExtraAppWidgetId = "EXTRA_APPWIDGET_ID"

    internal fun createIntent(
        receiver: ComponentName,
        actionKey: String,
        appWidgetId: Int,
    ) = Intent()
        .setComponent(receiver)
        .setAction(ActionTriggerLambda)
        .putExtra(ExtraActionKey, actionKey)
        .putExtra(ExtraAppWidgetId, appWidgetId)
}
