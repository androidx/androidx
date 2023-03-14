/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.appactions.interaction.capabilities.core.task.impl

import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.impl.ActionCapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.AppActionsContext.AppAction

internal class TaskCapabilitySession<
    ArgumentT,
    OutputT,
    ConfirmationT,
    > (
    val actionSpec: ActionSpec<*, ArgumentT, OutputT>,
    val appAction: AppAction,
    val taskHandler: TaskHandler<ConfirmationT>,
    val externalSession: BaseSession<ArgumentT, OutputT>,
) : ActionCapabilitySession, TaskUpdateHandler {
    override val state: AppAction
        get() = sessionOrchestrator.getAppAction()

    // single-turn capability does not have status
    override val status: ActionCapabilitySession.Status
        get() = sessionOrchestrator.getStatus()

    /** synchronize on this lock to enqueue assistant/manual input requests. */
    private val requestLock = Any()

    /** Contains session state and request processing logic. */
    private val sessionOrchestrator: TaskOrchestrator<
        ArgumentT, OutputT, ConfirmationT,> =
        TaskOrchestrator(
            actionSpec,
            appAction,
            taskHandler,
            externalSession,
            Runnable::run,
        )
    private var pendingAssistantRequest: AssistantUpdateRequest? = null
    private var pendingTouchEventRequest: TouchEventUpdateRequest? = null

    override fun execute(argumentsWrapper: ArgumentsWrapper, callback: CallbackInternal) {
        enqueueAssistantRequest(AssistantUpdateRequest.create(argumentsWrapper, callback))
    }

    override fun updateParamValues(paramValuesMap: Map<String, List<ParamValue>>) {
        enqueueTouchEventRequest(TouchEventUpdateRequest(paramValuesMap))
    }

    override fun setTouchEventCallback(callback: TouchEventCallback) {
        sessionOrchestrator.setTouchEventCallback(callback)
    }

    /**
     * If there is a pendingAssistantRequest, we will overwrite that request (and send CANCELLED
     * response to that request).
     *
     * <p>This is done because assistant requests contain the full state, so we can safely ignore
     * existing requests if a new one arrives.
     */
    private fun enqueueAssistantRequest(request: AssistantUpdateRequest) {
        synchronized(requestLock) {
            pendingAssistantRequest?.callbackInternal()?.onError(ErrorStatusInternal.CANCELLED)
            pendingAssistantRequest = request
            dispatchPendingRequestIfIdle()
        }
    }

    private fun enqueueTouchEventRequest(request: TouchEventUpdateRequest) {
        synchronized(requestLock) {
            if (pendingTouchEventRequest == null) {
                pendingTouchEventRequest = request
            } else {
                pendingTouchEventRequest =
                    TouchEventUpdateRequest.merge(pendingTouchEventRequest!!, request)
            }
            dispatchPendingRequestIfIdle()
        }
    }

    /**
     * If sessionOrchestrator is idle, select the next request to dispatch to sessionOrchestrator (if
     * there are any pending requests).
     *
     * <p>If sessionOrchestrator is not idle, do nothing, since this method will automatically be
     * called when sessionOrchestrator becomes idle.
     */
    private fun dispatchPendingRequestIfIdle() {
        synchronized(requestLock) {
            if (!sessionOrchestrator.isIdle()) {
                return
            }
            var nextRequest: UpdateRequest? = null
            if (pendingAssistantRequest != null) {
                nextRequest = UpdateRequest.of(pendingAssistantRequest)
                pendingAssistantRequest = null
            } else if (pendingTouchEventRequest != null) {
                nextRequest = UpdateRequest.of(pendingTouchEventRequest)
                pendingTouchEventRequest = null
            }
            if (nextRequest != null) {
                Futures.addCallback(
                    sessionOrchestrator.processUpdateRequest(nextRequest),
                    object : FutureCallback<Void?> {
                        override fun onSuccess(unused: Void?) {
                            dispatchPendingRequestIfIdle()
                        }

                        /**
                         * A fatal exception has occurred, cause by one of the following:
                         *
                         * <ul>
                         *   <li>1. The developer listener threw some runtime exception
                         *   <li>2. The SDK encountered some uncaught internal exception
                         * </ul>
                         *
                         * <p>In both cases, this exception will be rethrown which will crash
                         * the app.
                         */
                        override fun onFailure(t: Throwable) {
                            throw IllegalStateException(
                                "unhandled exception in request processing",
                                t,
                            )
                        }
                    },
                    Runnable::run,
                )
            }
        }
    }
}
