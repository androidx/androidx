/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.work.inspection

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.WorkManager
import androidx.work.impl.WorkContinuationImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.Command.OneOfCase.CANCEL_WORK
import androidx.work.inspection.WorkManagerInspectorProtocol.Command.OneOfCase.TRACK_WORK_MANAGER
import androidx.work.inspection.WorkManagerInspectorProtocol.ErrorResponse
import androidx.work.inspection.WorkManagerInspectorProtocol.Event
import androidx.work.inspection.WorkManagerInspectorProtocol.Response
import androidx.work.inspection.WorkManagerInspectorProtocol.TrackWorkManagerResponse
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkAddedEvent
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkRemovedEvent
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkUpdatedEvent
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

/**
 * Inspector to work with WorkManager
 */
class WorkManagerInspector(
    connection: Connection,
    environment: InspectorEnvironment
) : Inspector(connection), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val workManager: WorkManagerImpl
    private val executor = environment.executors().primary()

    private val stackTraceMap = mutableMapOf<String, List<StackTraceElement>>()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        workManager = environment.artTooling().findInstances(Application::class.java).first()
            .let { application -> WorkManager.getInstance(application) as WorkManagerImpl }

        mainHandler.post {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        environment.artTooling().registerEntryHook(
            WorkContinuationImpl::class.java,
            "enqueue()Landroidx/work/Operation;"
        ) { obj, _ ->
            val stackTrace = Throwable().stackTrace
            executor.execute {
                (obj as? WorkContinuationImpl)?.allIds?.forEach { id ->
                    stackTraceMap[id] = stackTrace.toList().prune()
                }
            }
        }
    }

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        val command = Command.parseFrom(data)
        when (command.oneOfCase) {
            TRACK_WORK_MANAGER -> {
                val response = Response.newBuilder()
                    .setTrackWorkManager(TrackWorkManagerResponse.getDefaultInstance())
                    .build()
                workManager
                    .workDatabase
                    .workSpecDao()
                    .allWorkSpecIdsLiveData
                    .safeObserveWhileNotNull(this, executor) { oldList, newList ->
                        updateWorkIdList(oldList ?: listOf(), newList)
                    }
                callback.reply(response.toByteArray())
            }
            CANCEL_WORK -> {
                val response = Response.newBuilder()
                    .setTrackWorkManager(TrackWorkManagerResponse.getDefaultInstance())
                    .build()
                workManager.cancelWorkById(UUID.fromString(command.cancelWork.id)).result
                    .addListener(Runnable { callback.reply(response.toByteArray()) }, executor)
            }
            else -> {
                val errorResponse = ErrorResponse.newBuilder()
                    .setContent("Unrecognised command type: ONEOF_NOT_SET")
                    .build()
                val response = Response.newBuilder()
                    .setError(errorResponse)
                    .build()
                callback.reply(response.toByteArray())
            }
        }
    }

    /**
     * Allows to observe LiveDatas from non-main thread.
     * <p>
     * Observation will last until "null" value is dispatched, then
     * observer will be automatically removed.
     */
    private fun <T> LiveData<T>.safeObserveWhileNotNull(
        owner: LifecycleOwner,
        executor: Executor,
        listener: (oldValue: T?, newValue: T) -> Unit
    ) {
        mainHandler.post {
            observe(
                owner,
                object : Observer<T> {
                    private var lastValue: T? = null
                    override fun onChanged(t: T) {
                        if (t == null) {
                            removeObserver(this)
                        } else {
                            executor.execute {
                                listener(lastValue, t)
                                lastValue = t
                            }
                        }
                    }
                }
            )
        }
    }

    /**
     * Prune internal [StackTraceElement]s above [WorkContinuationImpl.enqueue] or from
     * work manager libraries.
     */
    private fun List<StackTraceElement>.prune(): List<StackTraceElement> {
        val entryHookIndex = indexOfFirst {
            it.className.startsWith("androidx.work.impl.WorkContinuationImpl") &&
                it.methodName == "enqueue"
        }
        if (entryHookIndex != -1) {
            return subList(entryHookIndex + 1, size)
                .dropWhile { it.className.startsWith("androidx.work") }
        }
        return this
    }

    private fun createWorkInfoProto(id: String): WorkManagerInspectorProtocol.WorkInfo? {
        // work can be removed by the time we try to access it, so if null was return let's just
        // skip it
        val workSpec = workManager.workDatabase.workSpecDao().getWorkSpec(id) ?: return null

        val workInfoBuilder = WorkManagerInspectorProtocol.WorkInfo.newBuilder()
        workInfoBuilder.id = id
        workInfoBuilder.state = workSpec.state.toProto()
        workInfoBuilder.workerClassName = workSpec.workerClassName
        workInfoBuilder.data = workSpec.output.toProto()
        workInfoBuilder.runAttemptCount = workSpec.runAttemptCount
        workInfoBuilder.isPeriodic = workSpec.isPeriodic
        workInfoBuilder.constraints = workSpec.constraints.toProto()
        workManager.getWorkInfoById(UUID.fromString(id)).let {
            workInfoBuilder.addAllTags(it.get().tags)
        }

        val workStackBuilder = WorkManagerInspectorProtocol.CallStack.newBuilder()
        stackTraceMap[id]?.let { stack ->
            workStackBuilder.addAllFrames(stack.map { it.toProto() })
        }
        workInfoBuilder.callStack = workStackBuilder.build()

        workInfoBuilder.scheduleRequestedAt = WorkSpec.SCHEDULE_NOT_REQUESTED_YET
        workManager.workDatabase.dependencyDao().getPrerequisites(id).let {
            workInfoBuilder.addAllPrerequisites(it)
        }

        workManager.workDatabase.dependencyDao().getDependentWorkIds(id).let {
            workInfoBuilder.addAllDependents(it)
        }

        workManager.workDatabase.workNameDao().getNamesForWorkSpecId(id).let {
            workInfoBuilder.addAllNames(it)
        }

        return workInfoBuilder.build()
    }

    private fun observeWorkUpdates(id: String) {
        val workInfoLiveData = workManager.getWorkInfoByIdLiveData(UUID.fromString(id))

        workInfoLiveData.safeObserveWhileNotNull(this, executor) { oldWorkInfo, newWorkInfo ->
            if (oldWorkInfo?.state != newWorkInfo.state) {
                val updateWorkEvent = WorkUpdatedEvent.newBuilder()
                    .setId(id)
                    .setState(
                        WorkManagerInspectorProtocol.WorkInfo.State
                            .forNumber(newWorkInfo.state.ordinal + 1)
                    )
                    .build()
                connection.sendEvent(
                    Event.newBuilder().setWorkUpdated(updateWorkEvent).build().toByteArray()
                )
            }
            if (oldWorkInfo?.runAttemptCount != newWorkInfo.runAttemptCount) {
                val updateWorkEvent = WorkUpdatedEvent.newBuilder()
                    .setId(id)
                    .setRunAttemptCount(newWorkInfo.runAttemptCount)
                    .build()
                connection.sendEvent(
                    Event.newBuilder().setWorkUpdated(updateWorkEvent).build().toByteArray()
                )
            }
            if (oldWorkInfo?.outputData != newWorkInfo.outputData) {
                val updateWorkEvent = WorkUpdatedEvent.newBuilder()
                    .setId(id)
                    .setData(newWorkInfo.outputData.toProto())
                    .build()
                connection.sendEvent(
                    Event.newBuilder().setWorkUpdated(updateWorkEvent).build().toByteArray()
                )
            }
        }

        workManager.workDatabase
            .workSpecDao()
            .getScheduleRequestedAtLiveData(id)
            .safeObserveWhileNotNull(this, executor) { _, newScheduledTime ->
                val updateWorkEvent = WorkUpdatedEvent.newBuilder()
                    .setId(id)
                    .setScheduleRequestedAt(newScheduledTime)
                    .build()
                connection.sendEvent(
                    Event.newBuilder().setWorkUpdated(updateWorkEvent).build().toByteArray()
                )
            }
    }

    private fun updateWorkIdList(oldWorkIds: List<String>, newWorkIds: List<String>) {
        for (removedId in oldWorkIds.minus(newWorkIds)) {
            val removeEvent = WorkRemovedEvent.newBuilder().setId(removedId).build()
            val event = Event.newBuilder().setWorkRemoved(removeEvent).build()
            connection.sendEvent(event.toByteArray())
        }
        for (addedId in newWorkIds.minus(oldWorkIds)) {
            val workInfoProto = createWorkInfoProto(addedId) ?: continue
            val addEvent = WorkAddedEvent.newBuilder().setWork(workInfoProto)
                .build()
            val event = Event.newBuilder().setWorkAdded(addEvent).build()
            connection.sendEvent(event.toByteArray())
            observeWorkUpdates(addedId)
        }
    }

    override fun onDispose() {
        super.onDispose()
        val latch = CountDownLatch(1)
        mainHandler.post {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            latch.countDown()
        }
        // await to make sure that all observers that registered by inspector are gone
        // otherwise they can post message to "disposed" inspector
        latch.await()
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
