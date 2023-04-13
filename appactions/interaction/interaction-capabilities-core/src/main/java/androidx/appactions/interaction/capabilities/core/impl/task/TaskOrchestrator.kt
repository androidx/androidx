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
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.SessionContext
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.FulfillmentResult
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback
import androidx.appactions.interaction.capabilities.core.impl.UiHandleRegistry
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger
import androidx.appactions.interaction.capabilities.core.impl.utils.LoggerInternal
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.InvalidResolverException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.MissingEntityConverterException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.MissingRequiredArgException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.MissingSearchActionConverterException
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TouchEventMetadata
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.jvm.Throws

/**
 * TaskOrchestrator is responsible for holding session state, and processing assistant / manual
 * input updates to update session state.
 *
 * TaskOrchestrator is also responsible to communicating state updates to developer provided
 * listeners.
 *
 * Only one request can be processed at a time.
 */
internal class TaskOrchestrator<ArgumentsT, OutputT, ConfirmationT>(
    private val sessionId: String,
    private val actionSpec: ActionSpec<*, ArgumentsT, OutputT>,
    private val appAction: AppActionsContext.AppAction,
    private val taskHandler: TaskHandler<ConfirmationT>,
    private val externalSession: BaseExecutionSession<ArgumentsT, OutputT>,
) {
    /** This enum describes the current status of the TaskOrchestrator. */
    internal enum class Status {
        UNINITIATED,
        IN_PROGRESS,
        COMPLETED,
        DESTROYED,
    }
    /**
     * A [reader-writer lock](https://en.wikipedia.org/wiki/Readers%E2%80%93writer_lock) to protect
     * the synchronizing operation on [currentValuesMap]
     */
    private val valuesMapLock = ReentrantReadWriteLock()

    /** Map of argument name to the [CurrentValue] which wraps the argument name and status . */
    @GuardedBy("valuesMapLock")
    private val currentValuesMap = mutableMapOf<String, List<CurrentValue>>()

    /**
     * The callback that should be invoked when manual input processing finishes. This sends the
     * processing results to the AppInteraction SDKs. Note, this field is not provided on
     * construction because the callback is not available at the time when the developer creates the
     * capability.
     */
    private var touchEventCallback: TouchEventCallback? = null

    /** Current status of the overall task (i.e. status of the task). */
    internal var status: Status = Status.UNINITIATED
        private set

    // Set a TouchEventCallback instance. This callback is invoked when state changes from manual
    // input.
    internal fun setTouchEventCallback(touchEventCallback: TouchEventCallback?) {
        this.touchEventCallback = touchEventCallback
    }

    private val inProgressLock = Any()

    @GuardedBy("inProgressLock")
    private var inProgress = false

    /** Returns whether or not a request is currently being processed */
    internal fun isIdle(): Boolean = synchronized(inProgressLock) { !inProgress }

    internal val appDialogState: AppActionsContext.AppDialogState
        get() =
            AppActionsContext.AppDialogState.newBuilder()
                .addAllParams(
                    valuesMapLock.read {
                        appAction.paramsList.map { intentParam ->
                            val dialogParameterBuilder =
                                AppActionsContext.DialogParameter.newBuilder()
                                    .setName(intentParam.name)
                            currentValuesMap[intentParam.name]?.let {
                                dialogParameterBuilder.addAllCurrentValue(it)
                            }
                            dialogParameterBuilder.build()
                        }
                    },
                )
                .setFulfillmentIdentifier(appAction.identifier)
                .build()

    /**
     * processes the provided UpdateRequest asynchronously.
     *
     * Returns when the request handling is completed.
     *
     * An unhandled exception when handling an UpdateRequest will cause all future update requests
     * to fail.
     *
     * This method should never be called when isIdle() returns false.
     */
    internal suspend fun processUpdateRequest(updateRequest: UpdateRequest) {
        synchronized(inProgressLock) {
            if (inProgress) {
                throw IllegalStateException(
                    "processUpdateRequest should never be called when the task orchestrator" +
                        " isn't idle.",
                )
            }
            inProgress = true
        }
        try {
            if (updateRequest.assistantRequest != null) {
                processAssistantUpdateRequest(updateRequest.assistantRequest)
            } else if (updateRequest.touchEventRequest != null) {
                processTouchEventUpdateRequest(updateRequest.touchEventRequest)
            } else {
                throw IllegalArgumentException("unknown UpdateRequest type")
            }
        } finally {
            synchronized(inProgressLock) { inProgress = false }
        }
    }

    private suspend fun <T> withUiHandleRegistered(block: suspend () -> T): T {
        UiHandleRegistry.registerUiHandle(externalSession, sessionId)
        try {
            return block()
        } finally {
            UiHandleRegistry.unregisterUiHandle(externalSession)
        }
    }

    /** Processes an assistant update request. */
    @Suppress("DEPRECATION")
    private suspend fun processAssistantUpdateRequest(
        assistantUpdateRequest: AssistantUpdateRequest,
    ) = withUiHandleRegistered {
        val argumentsWrapper = assistantUpdateRequest.argumentsWrapper
        val callback = assistantUpdateRequest.callbackInternal
        val fulfillmentResult: FulfillmentResult
        if (argumentsWrapper.requestMetadata == null) {
            fulfillmentResult = FulfillmentResult(ErrorStatusInternal.INVALID_REQUEST_TYPE)
        } else {
            fulfillmentResult = when (argumentsWrapper.requestMetadata.requestType()) {
                FulfillmentRequest.Fulfillment.Type.UNRECOGNIZED,
                FulfillmentRequest.Fulfillment.Type.UNKNOWN_TYPE,
                ->
                    FulfillmentResult(ErrorStatusInternal.INVALID_REQUEST_TYPE)

                FulfillmentRequest.Fulfillment.Type.SYNC -> handleSync(argumentsWrapper)
                FulfillmentRequest.Fulfillment.Type.CONFIRM -> handleConfirm()
                FulfillmentRequest.Fulfillment.Type.CANCEL,
                FulfillmentRequest.Fulfillment.Type.TERMINATE,
                -> {
                    terminate()
                    FulfillmentResult(FulfillmentResponse.getDefaultInstance())
                }
            }
        }
        fulfillmentResult.applyToCallback(callback)
    }

    private suspend fun processTouchEventUpdateRequest(
        touchEventUpdateRequest: TouchEventUpdateRequest,
    ) = withUiHandleRegistered {
        val paramValuesMap = touchEventUpdateRequest.paramValuesMap
        if (
            touchEventCallback == null ||
            paramValuesMap.isEmpty() ||
            status !== Status.IN_PROGRESS
        ) {
            return@withUiHandleRegistered
        }
        valuesMapLock.write {
            for ((argName, value) in paramValuesMap) {
                currentValuesMap[argName] =
                    value.map {
                        TaskCapabilityUtils.toCurrentValue(it, CurrentValue.Status.ACCEPTED)
                    }
            }
        }

        try {
            if (!anyParamsOfStatus(CurrentValue.Status.DISAMBIG)) {
                val fulfillmentValuesMap =
                    TaskCapabilityUtils.paramValuesMapToFulfillmentValuesMap(
                        getCurrentPendingArguments(),
                    )
                processFulfillmentValues(fulfillmentValuesMap)
            }
            val fulfillmentResponse = maybeConfirmOrExecute()
            LoggerInternal.log(CapabilityLogger.LogLevel.INFO, LOG_TAG, "Manual input success")
            if (touchEventCallback != null) {
                touchEventCallback!!.onSuccess(
                    fulfillmentResponse,
                    TouchEventMetadata.getDefaultInstance(),
                )
            } else {
                LoggerInternal.log(
                    CapabilityLogger.LogLevel.ERROR,
                    LOG_TAG,
                    "Manual input null callback",
                )
            }
        } catch (t: Throwable) {
            LoggerInternal.log(CapabilityLogger.LogLevel.ERROR, LOG_TAG, "Manual input fail")
            if (touchEventCallback != null) {
                touchEventCallback!!.onError(ErrorStatusInternal.TOUCH_EVENT_REQUEST_FAILURE)
            } else {
                LoggerInternal.log(
                    CapabilityLogger.LogLevel.ERROR,
                    LOG_TAG,
                    "Manual input null callback",
                )
            }
        }
    }

    // TODO: add cleanup logic if any
    internal fun terminate() {
        externalSession.onDestroy()
        status = Status.DESTROYED
    }

    /**
     * If slot filling is incomplete, the future contains default FulfillmentResponse.
     *
     * Otherwise, the future contains a FulfillmentResponse containing BIC or BIO data.
     */
    @Throws(StructConversionException::class, MissingRequiredArgException::class)
    private suspend fun maybeConfirmOrExecute(): FulfillmentResponse {
        val finalArguments = getCurrentAcceptedArguments()
        if (
            anyParamsOfStatus(CurrentValue.Status.REJECTED) ||
            !TaskCapabilityUtils.isSlotFillingComplete(finalArguments, appAction.paramsList)
        ) {
            return FulfillmentResponse.getDefaultInstance()
        }
        return if (taskHandler.onReadyToConfirmListener != null) {
            getFulfillmentResponseForConfirmation(finalArguments)
        } else {
            getFulfillmentResponseForExecution(finalArguments)
        }
    }

    private fun maybeInitializeTask() {
        if (status === Status.UNINITIATED) {
            externalSession.onCreate(SessionContext())
        }
        status = Status.IN_PROGRESS
    }

    /**
     * Handles a SYNC request from assistant.
     *
     * Control-flow logic for a single task turn. Note, a task may start and finish in the same
     * turn, so the logic should include onEnter, arg validation, and onExit.
     */
    private suspend fun handleSync(argumentsWrapper: ArgumentsWrapper): FulfillmentResult {
        maybeInitializeTask()
        clearMissingArgs(argumentsWrapper)
        return try {
            processFulfillmentValues(argumentsWrapper.paramValues)
            val fulfillmentResponse = maybeConfirmOrExecute()
            LoggerInternal.log(CapabilityLogger.LogLevel.INFO, LOG_TAG, "Task sync success")
            FulfillmentResult(fulfillmentResponse)
        } catch (t: Throwable) {
            LoggerInternal.log(CapabilityLogger.LogLevel.ERROR, LOG_TAG, "Task sync fail", t)
            FulfillmentResult(ErrorStatusInternal.SYNC_REQUEST_FAILURE)
        }
    }

    /**
     * Control-flow logic for a single task turn in which the user has confirmed in the previous
     * turn.
     */
    private suspend fun handleConfirm(): FulfillmentResult {
        val finalArguments = getCurrentAcceptedArguments()
        return try {
            val fulfillmentResponse = getFulfillmentResponseForExecution(finalArguments)
            LoggerInternal.log(CapabilityLogger.LogLevel.INFO, LOG_TAG, "Task confirm success")
            FulfillmentResult(fulfillmentResponse)
        } catch (t: Throwable) {
            LoggerInternal.log(CapabilityLogger.LogLevel.ERROR, LOG_TAG, "Task confirm fail")
            FulfillmentResult(ErrorStatusInternal.CONFIRMATION_REQUEST_FAILURE)
        }
    }

    private fun clearMissingArgs(assistantArgs: ArgumentsWrapper) {
        valuesMapLock.write {
            val argsCleared =
                currentValuesMap.keys.filter { !assistantArgs.paramValues.containsKey(it) }
            for (arg in argsCleared) {
                currentValuesMap.remove(arg)
                // TODO(b/234170829): notify listener#onReceived of the cleared arguments
            }
        }
    }

    /**
     * Main processing chain for both assistant requests and manual input requests. All pending
     * parameters contained in fulfillmentValuesMap are chained together in a serial fashion. We use
     * Futures here to make sure long running app processing (such as argument grounding or argument
     * validation) are executed asynchronously.
     */
    @Throws(
        MissingEntityConverterException::class,
        MissingSearchActionConverterException::class,
        StructConversionException::class,
        InvalidResolverException::class,
    )
    private suspend fun processFulfillmentValues(
        fulfillmentValuesMap: Map<String, List<FulfillmentRequest.Fulfillment.FulfillmentValue>>,
    ) {
        var currentResult = SlotProcessingResult(true, emptyList())
        for ((name, fulfillmentValues) in fulfillmentValuesMap) {
            currentResult =
                maybeProcessSlotAndUpdateCurrentValues(currentResult, name, fulfillmentValues)
        }
    }

    @Throws(
        MissingEntityConverterException::class,
        MissingSearchActionConverterException::class,
        StructConversionException::class,
        InvalidResolverException::class,
    )
    private suspend fun maybeProcessSlotAndUpdateCurrentValues(
        previousResult: SlotProcessingResult,
        slotKey: String,
        newSlotValues: List<FulfillmentRequest.Fulfillment.FulfillmentValue>,
    ): SlotProcessingResult {
        val currentSlotValues =
            valuesMapLock.read { currentValuesMap.getOrDefault(slotKey, emptyList()) }
        val modifiedSlotValues =
            TaskCapabilityUtils.getMaybeModifiedSlotValues(currentSlotValues, newSlotValues)
        if (TaskCapabilityUtils.canSkipSlotProcessing(currentSlotValues, modifiedSlotValues)) {
            return previousResult
        }
        val pendingArgs =
            TaskCapabilityUtils.fulfillmentValuesToCurrentValues(
                modifiedSlotValues,
                CurrentValue.Status.PENDING,
            )
        val currentResult = processSlot(slotKey, previousResult, pendingArgs)
        valuesMapLock.write { currentValuesMap[slotKey] = currentResult.processedValues }
        return currentResult
    }

    /**
     * Process pending param values for a slot.
     *
     * If the previous slot was accepted, go through grounding/validation with TaskSlotProcessor,
     * otherwise just return the pending values as is.
     */
    @Throws(
        MissingEntityConverterException::class,
        MissingSearchActionConverterException::class,
        StructConversionException::class,
        InvalidResolverException::class,
    )
    private suspend fun processSlot(
        name: String,
        previousResult: SlotProcessingResult,
        pendingArgs: List<CurrentValue>,
    ): SlotProcessingResult {
        return if (!previousResult.isSuccessful) {
            SlotProcessingResult(false, pendingArgs)
        } else {
            TaskSlotProcessor.processSlot(name, pendingArgs, taskHandler.taskParamMap)
        }
    }

    /**
     * Retrieve all ParamValue from accepted slots in currentValuesMap.
     *
     * A slot is considered accepted if all CurrentValues in the slot has ACCEPTED status.
     */
    private fun getCurrentAcceptedArguments(): Map<String, List<ParamValue>> =
        valuesMapLock
            .read {
                currentValuesMap.filterValues { currentValues ->
                    currentValues.all { it.status == CurrentValue.Status.ACCEPTED }
                }
            }
            .mapValues { currentValue -> currentValue.value.map { it.value } }

    /**
     * Retrieve all ParamValue from pending slots in currentValuesMap.
     *
     * A slot is considered pending if any CurrentValues in the slot has PENDING status.
     */
    private fun getCurrentPendingArguments(): Map<String, List<ParamValue>> =
        valuesMapLock
            .read {
                currentValuesMap.filterValues { currentValues ->
                    currentValues.any { it.status == CurrentValue.Status.PENDING }
                }
            }
            .mapValues { currentValues -> currentValues.value.map { it.value } }

    /** Returns true if any CurrentValue in currentValuesMap has the given Status. */
    private fun anyParamsOfStatus(status: CurrentValue.Status) =
        valuesMapLock.read {
            currentValuesMap.values.any { currentValues ->
                currentValues.any { it.status == status }
            }
        }

    @Throws(StructConversionException::class, MissingRequiredArgException::class)
    private suspend fun getFulfillmentResponseForConfirmation(
        finalArguments: Map<String, List<ParamValue>>,
    ): FulfillmentResponse {
        val result = taskHandler.onReadyToConfirmListener!!.onReadyToConfirm(finalArguments)
        val fulfillmentResponse = FulfillmentResponse.newBuilder()
        convertToConfirmationOutput(result)?.let { fulfillmentResponse.confirmationData = it }
        return fulfillmentResponse.build()
    }

    @Throws(StructConversionException::class)
    private suspend fun getFulfillmentResponseForExecution(
        finalArguments: Map<String, List<ParamValue>>,
    ): FulfillmentResponse {
        val result = externalSession.onExecute(actionSpec.buildArguments(finalArguments))
        status = Status.COMPLETED
        val fulfillmentResponse =
            FulfillmentResponse.newBuilder().setStartDictation(result.shouldStartDictation)
        convertToExecutionOutput(result)?.let { fulfillmentResponse.executionOutput = it }
        return fulfillmentResponse.build()
    }

    /**
     * Convert from java capabilities [ExecutionResult] to [FulfillmentResponse.StructuredOutput]
     * proto.
     */
    private fun convertToExecutionOutput(
        executionResult: ExecutionResult<OutputT>,
    ): FulfillmentResponse.StructuredOutput? =
        executionResult.output?.let { actionSpec.convertOutputToProto(it) }

    /**
     * Convert from java capabilities [ConfirmationOutput] to [FulfillmentResponse.StructuredOutput]
     * proto.
     */
    private fun convertToConfirmationOutput(
        confirmationOutput: ConfirmationOutput<ConfirmationT>,
    ): FulfillmentResponse.StructuredOutput? {
        val confirmation = confirmationOutput.confirmation ?: return null
        return FulfillmentResponse.StructuredOutput.newBuilder()
            .addAllOutputValues(
                taskHandler.confirmationDataBindings.entries.map {
                    FulfillmentResponse.StructuredOutput.OutputValue.newBuilder()
                        .setName(it.key)
                        .addAllValues(it.value.invoke(confirmation))
                        .build()
                },
            )
            .build()
    }

    companion object {
        private const val LOG_TAG = "TaskOrchestrator"
    }
}
