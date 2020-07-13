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
import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.work.WorkManager
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.Command.OneOfCase.TRACK_WORK_MANAGER
import androidx.work.inspection.WorkManagerInspectorProtocol.ErrorResponse
import androidx.work.inspection.WorkManagerInspectorProtocol.Response
import androidx.work.inspection.WorkManagerInspectorProtocol.TrackWorkManagerResponse

/**
 * Inspector to work with WorkManager
 */
class WorkManagerInspector(
    connection: Connection,
    environment: InspectorEnvironment
) : Inspector(connection) {

    private val workManager: WorkManager

    init {
        workManager = environment.findInstances(Application::class.java).first()
            .let { application -> WorkManager.getInstance(application) }
    }

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        val command = Command.parseFrom(data)
        when (command.oneOfCase) {
            TRACK_WORK_MANAGER -> {
                val response = Response.newBuilder()
                    .setTrackWorkManager(TrackWorkManagerResponse.getDefaultInstance())
                    .build()
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
}
