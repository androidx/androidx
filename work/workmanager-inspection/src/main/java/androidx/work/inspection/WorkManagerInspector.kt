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
import androidx.work.impl.WorkManagerImpl
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.Command.OneOfCase.TRACK_WORK_MANAGER
import androidx.work.inspection.WorkManagerInspectorProtocol.ErrorResponse
import androidx.work.inspection.WorkManagerInspectorProtocol.Event
import androidx.work.inspection.WorkManagerInspectorProtocol.Response
import androidx.work.inspection.WorkManagerInspectorProtocol.TrackWorkManagerResponse
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkAddedEvent
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkRemovedEvent
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkUpdatedEvent
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Inspector to work with WorkManager
 */
class WorkManagerInspector(
    connection: Connection,
    environment: InspectorEnvironment
) : Inspector(connection), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val workManager: WorkManagerImpl
    private val executor = Executors.newSingleThreadExecutor()

    init {
        workManager = environment.findInstances(Application::class.java).first()
            .let { application -> WorkManager.getInstance(application) as WorkManagerImpl }
        Handler(Looper.getMainLooper()).post {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
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
                    .safeObserve(this, executor) { oldList, newList ->
                        updateWorkIdList(oldList ?: listOf(), newList)
                    }
                callback.reply(response.toByteArray())
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

    private fun <T> LiveData<T>.safeObserve(
        owner: LifecycleOwner,
        executor: ExecutorService,
        listener: (oldValue: T?, newValue: T) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            observe(owner,
                object : Observer<T> {
                    private var lastValue: T? = null
                    override fun onChanged(t: T) {
                        executor.submit {
                            listener(lastValue, t)
                            lastValue = t
                        }
                    }
                }
            )
        }
    }

    private fun createWorkInfoProto(id: String): WorkManagerInspectorProtocol.WorkInfo {
        val workInfoBuilder = WorkManagerInspectorProtocol.WorkInfo.newBuilder()
        val workSpec = workManager.workDatabase.workSpecDao().getWorkSpec(id)

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

        return workInfoBuilder.build()
    }

    private fun observeWorkUpdates(id: String) {
        val workInfoLiveData = workManager.getWorkInfoByIdLiveData(UUID.fromString(id))

        workInfoLiveData.safeObserve(this, executor) { oldWorkInfo, newWorkInfo ->
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
            .safeObserve(this, executor) { _, newScheduledTime ->
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
            val addEvent = WorkAddedEvent.newBuilder().setWork(createWorkInfoProto(addedId))
                .build()
            val event = Event.newBuilder().setWorkAdded(addEvent).build()
            connection.sendEvent(event.toByteArray())
            observeWorkUpdates(addedId)
        }
    }

    override fun onDispose() {
        super.onDispose()
        Handler(Looper.getMainLooper()).post {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
