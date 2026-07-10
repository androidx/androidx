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

package androidx.xr.projected

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.RequiresApi
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/**
 * An Activity to request permissions on the host display. It finishes itself when it receives a
 * result from a permission request.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalProjectedApi::class)
internal class TrampolineRequestPermissionsOnHostActivity : Activity() {

    private lateinit var resultReceiver: ResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultReceiverNullable =
            intent.getParcelableExtra(
                ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER,
                ResultReceiver::class.java,
            )

        if (resultReceiverNullable == null) {
            finish()
            return
        }

        val permissions =
            intent.getStringArrayExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS)

        if (permissions.isNullOrEmpty()) {
            resultReceiverNullable.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        resultReceiver = resultReceiverNullable
        requestPermissions(
            permissions,
            PERMISSION_REQUEST_CODE,
            ProjectedContext.createProjectedDeviceContext(this).deviceId,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }
        resultReceiver.send(
            RESULT_OK,
            Bundle().apply {
                putStringArray(ProjectedPermissionsConstants.RESULT_DATA_PERMISSIONS, permissions)
                putIntArray(ProjectedPermissionsConstants.RESULT_DATA_GRANT_RESULTS, grantResults)
            },
        )
        finish()
    }

    private companion object {
        const val PERMISSION_REQUEST_CODE = 105
    }
}
