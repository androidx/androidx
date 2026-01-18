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

package androidx.window.demo.area

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.window.area.WindowArea
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.common.EdgeToEdgeActivity
import androidx.window.demo.common.infolog.InfoLogAdapter
import androidx.window.demo.databinding.ActivityRearDisplayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Demo Activity that showcases listening for RearDisplay Status as well as enabling/disabling
 * RearDisplay mode.
 */
@OptIn(ExperimentalWindowApi::class)
class RearDisplayActivityConfigChanges : EdgeToEdgeActivity() {

    private lateinit var windowAreaController: WindowAreaController
    private var rearDisplayWindowArea: WindowArea? = null
    private var rearDisplayStatus: WindowAreaCapability.Status =
        WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED
    private val infoLogAdapter = InfoLogAdapter()
    private lateinit var binding: ActivityRearDisplayBinding
    private lateinit var executor: Executor

    private val windowAreaListener =
        Consumer<List<WindowArea>> { windowAreas ->
            for (windowArea in windowAreas) {
                if (windowArea.type == WindowArea.Type.TYPE_REAR_FACING) {
                    rearDisplayWindowArea = windowArea
                    break
                }
            }
            val status = getRearDisplayStatus(rearDisplayWindowArea)
            infoLogAdapter.append(getCurrentTimeString(), status.toString())
            infoLogAdapter.notifyDataSetChanged()
            rearDisplayStatus = status
            updateRearDisplayButton()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRearDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        binding.rearStatusRecyclerView.adapter = infoLogAdapter

        binding.rearDisplayButton.setOnClickListener {
            if (rearDisplayStatus == WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE) {
                WindowAreaController.getOrCreate()
                    .transferToWindowArea(
                        windowAreaToken = null,
                        activity = this@RearDisplayActivityConfigChanges,
                    )
            } else {
                rearDisplayWindowArea?.let { windowArea ->
                    WindowAreaController.getOrCreate()
                        .transferToWindowArea(windowAreaToken = windowArea.token, activity = this)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        windowAreaController.addWindowAreasListener(executor, windowAreaListener)
    }

    override fun onStop() {
        super.onStop()
        windowAreaController.removeWindowAreasListener(windowAreaListener)
    }

    private fun updateRearDisplayButton() {
        when (rearDisplayStatus) {
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED -> {
                binding.rearDisplayButton.isEnabled = false
                binding.rearDisplayButton.text = "RearDisplay is not supported on this device"
            }
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNAVAILABLE -> {
                binding.rearDisplayButton.isEnabled = false
                binding.rearDisplayButton.text = "RearDisplay is not currently available"
            }
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE -> {
                binding.rearDisplayButton.isEnabled = true
                binding.rearDisplayButton.text = "Enable RearDisplay Mode"
            }
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE -> {
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

    private fun getRearDisplayStatus(windowArea: WindowArea?): WindowAreaCapability.Status {
        val status =
            windowArea
                ?.getCapability(WindowAreaCapability.Operation.OPERATION_TRANSFER_TO_AREA)
                ?.status
        return status ?: WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED
    }

    private companion object {
        private val TAG = RearDisplayActivityConfigChanges::class.java.simpleName
    }
}
