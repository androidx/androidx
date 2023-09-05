/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget.testing.unit

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.glance.EmittableCheckable
import androidx.glance.action.ActionModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.EmittableCircularProgressIndicator
import androidx.glance.appwidget.EmittableLinearProgressIndicator
import androidx.glance.appwidget.action.SendBroadcastActionAction
import androidx.glance.appwidget.action.SendBroadcastClassAction
import androidx.glance.appwidget.action.SendBroadcastComponentAction
import androidx.glance.appwidget.action.SendBroadcastIntentAction
import androidx.glance.appwidget.action.StartActivityIntentAction
import androidx.glance.appwidget.action.StartServiceClassAction
import androidx.glance.appwidget.action.StartServiceComponentAction
import androidx.glance.appwidget.action.StartServiceIntentAction
import androidx.glance.testing.GlanceNodeAssertionsProvider
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.MappedNode

/**
 * Returns a matcher that matches if a node is checkable (e.g. radio button, switch, checkbox)
 * and is checked.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 */
fun isChecked(): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "is checked"
) { node ->
    val emittable = node.value.emittable
    emittable is EmittableCheckable && emittable.checked
}

/**
 * Returns a matcher that matches if a node is checkable (e.g. radio button, switch, checkbox)
 * but is not checked.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 */
fun isNotChecked(): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "is not checked"
) { node ->
    val emittable = node.value.emittable
    emittable is EmittableCheckable && !emittable.checked
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that starts an activity.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param intent the intent for launching an activity that is expected to have been passed in the
 *               `actionStartActivity` method call. Note: Only matches if intents are same per
 *               `filterEquals`.
 * @param parameters the parameters associated with the action that are expected to have been passed
 *                   in the `actionStartActivity` method call
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 *                        an activity start.
 */
// Other variants in the base layer (glance-testing).
fun hasStartActivityClickAction(
    intent: Intent,
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    if (activityOptions != null) {
        "has start activity click action with intent: $intent, " +
            "parameters: $parameters and bundle: $activityOptions"
    } else {
        "has start activity click action with intent: $intent and parameters: $parameters"
    }
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is StartActivityIntentAction) {
                var result = action.intent.filterEquals(intent) &&
                    action.parameters == parameters
                if (activityOptions != null) {
                    result = result && activityOptions == action.activityOptions
                }
                return@any result
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that starts a service.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param serviceClass class of the service to launch that is expected to have been passed in the
 *                    `actionStartService` method call.
 * @param isForegroundService if the service to launch is expected to have been set as foreground
 *                            service in the `actionStartService` method call.
 */
fun hasStartServiceAction(
    serviceClass: Class<out Service>,
    isForegroundService: Boolean = false
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = if (isForegroundService) {
        "has start service action for foreground service: ${serviceClass.name}"
    } else {
        "has start service action for non-foreground service: ${serviceClass.name}"
    }
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is StartServiceClassAction) {
                return@any action.serviceClass == serviceClass &&
                    action.isForegroundService == isForegroundService
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that starts a service.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param componentName component of the service to launch that is expected to have been passed in
 *                      the `actionStartService` method call.
 * @param isForegroundService if the service to launch is expected to have been set as foreground
 *                            service in the `actionStartService` method call.
 */
internal fun hasStartServiceAction(
    componentName: ComponentName,
    isForegroundService: Boolean = false
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = if (isForegroundService) {
        "has start service action for foreground service component: $componentName"
    } else {
        "has start service action for non-foreground service component: $componentName"
    }
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is StartServiceComponentAction) {
                return@any action.componentName == componentName &&
                    action.isForegroundService == isForegroundService
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that starts a service.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param intent the intent for launching the service that is expected to have been passed in
 *               the `actionStartService` method call.
 * @param isForegroundService if the service to launch is expected to have been set as foreground
 *                            service in the `actionStartService` method call.
 */
fun hasStartServiceAction(
    intent: Intent,
    isForegroundService: Boolean = false
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = if (isForegroundService) {
        "has start service action for foreground service: $intent"
    } else {
        "has start service action for non-foreground service: $intent"
    }
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is StartServiceIntentAction) {
                return@any action.intent == intent &&
                    action.isForegroundService == isForegroundService
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that sends a broadcast.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param receiverClass class of the broadcast receiver that is expected to have been passed in the
 *                      actionSendBroadcast` method call.
 */
fun hasSendBroadcastAction(
    receiverClass: Class<out BroadcastReceiver>
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "has send broadcast action for receiver class: ${receiverClass.name}"
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is SendBroadcastClassAction) {
                return@any action.receiverClass == receiverClass
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that sends a broadcast.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param intentAction the intent action of the broadcast receiver that is expected to  have been
 *                     passed in the `actionSendBroadcast` method call.
 * @param componentName optional [ComponentName] of the target broadcast receiver that is expected
 *                      to have been passed in the actionSendBroadcast` method call.
 */
fun hasSendBroadcastAction(
    intentAction: String,
    componentName: ComponentName? = null
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description =
    if (componentName != null) {
        "has send broadcast action with intent action: $intentAction and component: $componentName"
    } else {
        "has send broadcast action with intent action: $intentAction"
    }
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is SendBroadcastActionAction) {
                var result = action.action == intentAction
                if (componentName != null) {
                    result = result && action.componentName == componentName
                }
                return@any result
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that sends a broadcast.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param componentName [ComponentName] of the target broadcast receiver that is expected to have
 *                      been passed in the actionSendBroadcast` method call.
 */
fun hasSendBroadcastAction(
    componentName: ComponentName
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "has send broadcast action with component: $componentName"
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is SendBroadcastComponentAction) {
                return@any action.componentName == componentName
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a node has a clickable set with action that sends a broadcast.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param intent the intent for sending broadcast  that is expected to  have been passed in the
 *              `actionSendBroadcast` method call. Note: intent is only matched using filterEquals.
 */
fun hasSendBroadcastAction(
    intent: Intent
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "has send broadcast action with intent: $intent"
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is SendBroadcastIntentAction) {
                return@any action.intent.filterEquals(intent)
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a given node is a linear progress indicator with given progress
 * value.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param progress the expected value of the current progress
 */
fun isLinearProgressIndicator(
    /*@FloatRange(from = 0.0, to = 1.0)*/
    progress: Float
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "is a linear progress indicator with progress value: $progress"
) { node ->
    val emittable = node.value.emittable
    emittable is EmittableLinearProgressIndicator &&
        !emittable.indeterminate &&
        emittable.progress == progress
}

/**
 * Returns a matcher that matches if a given node is an indeterminate progress bar.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 */
fun isIndeterminateLinearProgressIndicator(): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "is an indeterminate linear progress indicator"
) { node ->
    val emittable = node.value.emittable
    emittable is EmittableLinearProgressIndicator && emittable.indeterminate
}

/**
 * Returns a matcher that matches if a given node is an indeterminate circular progress indicator.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 */
fun isIndeterminateCircularProgressIndicator(): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "is an indeterminate circular progress indicator"
) { node ->
    node.value.emittable is EmittableCircularProgressIndicator
}
