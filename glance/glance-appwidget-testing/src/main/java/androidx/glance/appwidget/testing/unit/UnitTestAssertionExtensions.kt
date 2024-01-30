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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.testing.GlanceNodeAssertion
import androidx.glance.testing.unit.GlanceMappedNode
import androidx.glance.testing.unit.MappedNode

// This file contains (appWidget-specific) convenience assertion shorthands for unit tests that
// delegate calls to "assert(matcher)". For assertions common to surfaces, see equivalent file in
// base layer testing library.

internal typealias UnitTestAssertion = GlanceNodeAssertion<MappedNode, GlanceMappedNode>

/**
 * Asserts that a given node is checkable and is checked.
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertIsChecked(): UnitTestAssertion = assert(isChecked())

/**
 * Asserts that a given node is checkable and is not checked.
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertIsNotChecked(): UnitTestAssertion = assert(isNotChecked())

/**
 * Asserts that a given node has a clickable set with action that starts an activity.
 *
 * @param intent the intent for launching an activity that is expected to have been passed in the
 *               `actionStartActivity` method call
 * @param parameters the parameters associated with the action that are expected to have been passed
 *                   in the `actionStartActivity` method call
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 *                        an activity start.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasStartActivityClickAction(
    intent: Intent,
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null
): UnitTestAssertion = assert(
    hasStartActivityClickAction(
        intent = intent,
        parameters = parameters,
        activityOptions = activityOptions
    )
)

/**
 * Asserts that a given node has a clickable set with action that starts a service.
 *
 * @param serviceClass class of the service to launch that is expected to have been passed in the
 *                    `actionStartService` method call.
 * @param isForegroundService if the service to launch is expected to have been set as foreground
 *                            service in the `actionStartService` method call.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasStartServiceClickAction(
    serviceClass: Class<out Service>,
    isForegroundService: Boolean = false
): UnitTestAssertion = assert(hasStartServiceAction(serviceClass, isForegroundService))

/**
 * Asserts that a given node has a clickable set with action that starts a service.
 *
 * @param componentName component of the service to launch that is expected to have been passed in
 *                      the `actionStartService` method call.
 * @param isForegroundService if the service to launch is expected to have been set as foreground
 *                            service in the `actionStartService` method call.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasStartServiceClickAction(
    componentName: ComponentName,
    isForegroundService: Boolean = false
): UnitTestAssertion = assert(hasStartServiceAction(componentName, isForegroundService))

/**
 * Asserts that a given node has a clickable set with action that starts a service.
 *
 * @param intent the intent for launching the service that is expected to have been passed in
 *               the `actionStartService` method call.
 * @param isForegroundService if the service to launch is expected to have been set as foreground
 *                            service in the `actionStartService` method call.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasStartServiceClickAction(
    intent: Intent,
    isForegroundService: Boolean = false
): UnitTestAssertion = assert(hasStartServiceAction(intent, isForegroundService))

/**
 * Asserts that a given node has a clickable set with action that sends a broadcast.
 *
 * @param receiverClass class of the broadcast receiver that is expected to have been passed in the
 *                      `actionSendBroadcast` method call.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasSendBroadcastClickAction(
    receiverClass: Class<out BroadcastReceiver>
): UnitTestAssertion = assert(hasSendBroadcastAction(receiverClass))

/**
 * Asserts that a given node has a clickable set with action that sends a broadcast.
 *
 * @param intentAction the intent action of the broadcast receiver that is expected to  have been
 *                     passed in the `actionSendBroadcast` method call.
 * @param componentName optional [ComponentName] of the target broadcast receiver that is expected
 *                      to have been passed in the `actionSendBroadcast` method call.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasSendBroadcastClickAction(
    intentAction: String,
    componentName: ComponentName? = null
): UnitTestAssertion = assert(hasSendBroadcastAction(intentAction, componentName))

/**
 * Asserts that a given node has a clickable set with action that sends a broadcast.
 *
 * @param componentName [ComponentName] of the target broadcast receiver that is expected to have
 *                      been passed in the `actionSendBroadcast` method call.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasSendBroadcastClickAction(
    componentName: ComponentName
): UnitTestAssertion = assert(hasSendBroadcastAction(componentName))

/**
 * Asserts that a given node has a clickable set with action that sends a broadcast.
 *
 * @param intent the intent for sending broadcast  that is expected to  have been passed in the
 *              `actionSendBroadcast` method call. Note: intent is only matched using filterEquals.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasSendBroadcastClickAction(
    intent: Intent
): UnitTestAssertion = assert(hasSendBroadcastAction(intent))
