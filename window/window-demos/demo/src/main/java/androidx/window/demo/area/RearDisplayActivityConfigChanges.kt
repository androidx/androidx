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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_ACTIVITY_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNAVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.common.EdgeToEdgeActivity
import androidx.window.demo.common.infolog.InfoLogAdapter
import androidx.window.demo.databinding.ActivityRearDisplayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Demo Activity that showcases listening for RearDisplay Status as well as enabling/disabling
 * RearDisplay mode. This Activity implements [WindowAreaSessionCallback] for simplicity.
 *
 * This Activity overrides configuration changes for simplicity.
 */
@OptIn(ExperimentalWindowApi::class)
class RearDisplayActivityConfigChanges : EdgeToEdgeActivity(), WindowAreaSessionCallback {

    private lateinit var windowAreaController: WindowAreaController
    private var rearDisplaySession: WindowAreaSession? = null
    private val infoLogAdapter = InfoLogAdapter()
    private lateinit var binding: ActivityRearDisplayBinding
    private lateinit var executor: Executor
    private var currentWindowAreaInfo: WindowAreaInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRearDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        binding.rearStatusRecyclerView.adapter = infoLogAdapter

        lifecycleScope.launch(Dispatchers.Main) {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collect from windowInfoRepo when the lifecycle is STARTED
                // and stops collection when the lifecycle is STOPPED
                windowAreaController.windowAreaInfos.collect { windowAreaInfos ->
                    infoLogAdapter.appendAndNotify(
                        getCurrentTimeString(),
                        "number of areas: " + windowAreaInfos.size
                    )
                    windowAreaInfos.forEach { windowAreaInfo ->
                        if (windowAreaInfo.type == WindowAreaInfo.Type.TYPE_REAR_FACING) {
                            currentWindowAreaInfo = windowAreaInfo
                            val transferCapability =
                                windowAreaInfo.getCapability(OPERATION_TRANSFER_ACTIVITY_TO_AREA)
                            infoLogAdapter.append(
                                getCurrentTimeString(),
                                transferCapability.status.toString() +
                                    " : " +
                                    windowAreaInfo.metrics.toString()
                            )
                            updateRearDisplayButton()
                        }
                    }
                    infoLogAdapter.notifyDataSetChanged()
                }
            }
        }

        binding.rearDisplayButton.setOnClickListener {
            if (rearDisplaySession != null) {
                rearDisplaySession?.close()
            } else {
                currentWindowAreaInfo?.let {
                    windowAreaController.transferActivityToWindowArea(
                        it.token,
                        this,
                        executor,
                        this
                    )
                }
            }
        }

        binding.rearDisplaySessionButton.setOnClickListener {
            if (rearDisplaySession == null) {
                try {
                    rearDisplaySession =
                        currentWindowAreaInfo?.getActiveSession(OPERATION_TRANSFER_ACTIVITY_TO_AREA)
                    updateRearDisplayButton()
                } catch (e: IllegalStateException) {
                    infoLogAdapter.appendAndNotify(getCurrentTimeString(), e.toString())
                }
            }
        }
    }

    override fun onSessionStarted(session: WindowAreaSession) {
        rearDisplaySession = session
        infoLogAdapter.appendAndNotify(
            getCurrentTimeString(),
            "RearDisplay Session has been started"
        )
        updateRearDisplayButton()
    }

    override fun onSessionEnded(t: Throwable?) {
        rearDisplaySession = null
        infoLogAdapter.appendAndNotify(getCurrentTimeString(), "RearDisplay Session has ended")
        updateRearDisplayButton()
    }

    private fun updateRearDisplayButton() {
        if (rearDisplaySession != null) {
            binding.rearDisplayButton.isEnabled = true
            binding.rearDisplayButton.text = "Disable RearDisplay Mode"
            return
        }
        currentWindowAreaInfo?.let { windowAreaInfo ->
            when (windowAreaInfo.getCapability(OPERATION_TRANSFER_ACTIVITY_TO_AREA).status) {
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
                else -> {
                    binding.rearDisplayButton.isEnabled = false
                    binding.rearDisplayButton.text = "RearDisplay is not supported on this device"
                }
            }
        }
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return currentDate.toString()
    }

    private companion object {
        private val TAG = RearDisplayActivityConfigChanges::class.java.simpleName
    }
}
