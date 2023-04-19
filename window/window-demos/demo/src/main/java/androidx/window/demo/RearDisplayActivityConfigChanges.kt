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

package androidx.window.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_ACTIVITY_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNAVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaInfo.Type.Companion.TYPE_REAR_FACING
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.demo.common.infolog.InfoLogAdapter
import androidx.window.demo.databinding.ActivityRearDisplayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Demo Activity that showcases listening for RearDisplay Status
 * as well as enabling/disabling RearDisplay mode. This Activity
 * implements [WindowAreaSessionCallback] for simplicity.
 *
 * This Activity overrides configuration changes for simplicity.
 */
class RearDisplayActivityConfigChanges : AppCompatActivity(), WindowAreaSessionCallback {

    private lateinit var windowAreaController: WindowAreaController
    private var rearDisplaySession: WindowAreaSession? = null
    private var rearDisplayWindowAreaInfo: WindowAreaInfo? = null
    private var rearDisplayStatus: WindowAreaCapability.Status = WINDOW_AREA_STATUS_UNSUPPORTED
    private val infoLogAdapter = InfoLogAdapter()
    private lateinit var binding: ActivityRearDisplayBinding
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRearDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        binding.rearStatusRecyclerView.adapter = infoLogAdapter

        binding.rearDisplayButton.setOnClickListener {
            if (rearDisplayStatus == WINDOW_AREA_STATUS_ACTIVE) {
                if (rearDisplaySession == null) {
                    rearDisplaySession = rearDisplayWindowAreaInfo?.getActiveSession(
                        OPERATION_TRANSFER_ACTIVITY_TO_AREA
                    )
                }
                rearDisplaySession?.close()
            } else {
                rearDisplayWindowAreaInfo?.token?.let { token ->
                    windowAreaController.transferActivityToWindowArea(
                        token = token,
                        activity = this,
                        executor = executor,
                        windowAreaSessionCallback = this)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowAreaController
                    .windowAreaInfos
                    .map { windowAreaInfoList -> windowAreaInfoList.firstOrNull {
                        windowAreaInfo -> windowAreaInfo.type == TYPE_REAR_FACING
                    } }
                    .onEach { windowAreaInfo -> rearDisplayWindowAreaInfo = windowAreaInfo }
                    .map(this@RearDisplayActivityConfigChanges::getRearDisplayStatus)
                    .distinctUntilChanged()
                    .collect { status ->
                        infoLogAdapter.append(getCurrentTimeString(), status.toString())
                        infoLogAdapter.notifyDataSetChanged()
                        rearDisplayStatus = status
                        updateRearDisplayButton()
                    }
            }
        }
    }

    override fun onSessionStarted(session: WindowAreaSession) {
        rearDisplaySession = session
        infoLogAdapter.append(getCurrentTimeString(), "RearDisplay Session has been started")
        infoLogAdapter.notifyDataSetChanged()
        updateRearDisplayButton()
    }

    override fun onSessionEnded(t: Throwable?) {
        rearDisplaySession = null
        infoLogAdapter.append(getCurrentTimeString(), "RearDisplay Session has ended")
        infoLogAdapter.notifyDataSetChanged()
        updateRearDisplayButton()
    }

    private fun updateRearDisplayButton() {
        if (rearDisplaySession != null) {
            binding.rearDisplayButton.isEnabled = true
            binding.rearDisplayButton.text = "Disable RearDisplay Mode"
            return
        }
        when (rearDisplayStatus) {
            WINDOW_AREA_STATUS_UNSUPPORTED -> {
                binding.rearDisplayButton.isEnabled = false
                binding.rearDisplayButton.text = "RearDisplay is not supported on this device"
            }
            WINDOW_AREA_STATUS_UNAVAILABLE -> {
                binding.rearDisplayButton.isEnabled = false
                binding.rearDisplayButton.text = "RearDisplay is not currently available"
            }
            WINDOW_AREA_STATUS_AVAILABLE -> {
                binding.rearDisplayButton.isEnabled = true
                binding.rearDisplayButton.text = "Enable RearDisplay Mode"
            }
            WINDOW_AREA_STATUS_ACTIVE -> {
                binding.rearDisplayButton.isEnabled = true
                binding.rearDisplayButton.text = "Disable RearDisplay Mode"
            }
        }
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return currentDate.toString()
    }

    private fun getRearDisplayStatus(windowAreaInfo: WindowAreaInfo?): WindowAreaCapability.Status {
        val status = windowAreaInfo?.getCapability(OPERATION_TRANSFER_ACTIVITY_TO_AREA)?.status
        return status ?: WINDOW_AREA_STATUS_UNSUPPORTED
    }

    private companion object {
        private val TAG = RearDisplayActivityConfigChanges::class.java.simpleName
    }
}