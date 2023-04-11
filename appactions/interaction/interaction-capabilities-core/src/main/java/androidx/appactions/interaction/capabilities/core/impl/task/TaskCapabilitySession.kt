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

package androidx.appactions.interaction.capabilities.core.impl.task

import androidx.annotation.GuardedBy
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.AppDialogState
import androidx.appactions.interaction.proto.ParamValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class TaskCapabilitySession<
    ArgumentsT,
    OutputT,
    ConfirmationT,
>(
    override val sessionId: String,
    actionSpec: ActionSpec<*, ArgumentsT, OutputT>,
    appAction: AppAction,
    taskHandler: TaskHandler<ConfirmationT>,
    externalSession: BaseExecutionSession<ArgumentsT, OutputT>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : CapabilitySession, TaskUpdateHandler {
    override val state: AppDialogState
        get() = sessionOrchestrator.appDialogState

    // single-turn capability does not have status
    override val isActive: Boolean
        get() = when (sessionOrchestrator.status) {
            TaskOrchestrator.Status.COMPLETED,
            TaskOrchestrator.Status.DESTROYED -> false
            else -> true
        }

    override fun destroy() {
        // TODO(b/270751989): cancel current processing request immediately
        this.sessionOrchestrator.terminate()
    }

    override val uiHandle: Any = externalSession

    /** synchronize on this lock to enqueue assistant/manual input requests. */
    private val requestLock = Any()

    /** Contains session state and request processing logic. */
    private val sessionOrchestrator:
        TaskOrchestrator<
            ArgumentsT,
            OutputT,
            ConfirmationT,
        > =
        TaskOrchestrator(
            sessionId,
            actionSpec,
            appAction,
            taskHandler,
            externalSession,
        )

    @GuardedBy("requestLock") private var pendingAssistantRequest: AssistantUpdateRequest? = null
    @GuardedBy("requestLock") private var pendingTouchEventRequest: TouchEventUpdateRequest? = null

    override fun execute(argumentsWrapper: ArgumentsWrapper, callback: CallbackInternal) {
        enqueueAssistantRequest(AssistantUpdateRequest(argumentsWrapper, callback))
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
            pendingAssistantRequest?.callbackInternal?.onError(ErrorStatusInternal.CANCELLED)
            pendingAssistantRequest = request
            dispatchPendingRequestIfIdle()
        }
    }

    private fun enqueueTouchEventRequest(request: TouchEventUpdateRequest) {
        synchronized(requestLock) {
            pendingTouchEventRequest =
                if (pendingTouchEventRequest == null) request
                else pendingTouchEventRequest!!.mergeWith(request)
            dispatchPendingRequestIfIdle()
        }
    }

    /**
     * If sessionOrchestrator is idle, select the next request to dispatch to sessionOrchestrator
     * (if there are any pending requests).
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
                nextRequest = UpdateRequest(pendingAssistantRequest!!)
                pendingAssistantRequest = null
            } else if (pendingTouchEventRequest != null) {
                nextRequest = UpdateRequest(pendingTouchEventRequest!!)
                pendingTouchEventRequest = null
            }
            if (nextRequest != null) {
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    sessionOrchestrator.processUpdateRequest(nextRequest)
                    dispatchPendingRequestIfIdle()
                }
            }
        }
    }
}
