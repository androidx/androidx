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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_PRESENT_ON_AREA
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaInfo.Type.Companion.TYPE_REAR_FACING
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.R
import androidx.window.demo.common.EdgeToEdgeActivity
import androidx.window.demo.common.infolog.InfoLogAdapter
import androidx.window.demo.databinding.ActivityRearDisplayPresentationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Demo Activity that showcases listening for the status of the [OPERATION_PRESENT_ON_AREA]
 * operation on a [WindowAreaInfo] of type [TYPE_REAR_FACING] as well as enabling/disabling a
 * presentation session on that window area. This Activity implements
 * [WindowAreaPresentationSessionCallback] for simplicity.
 *
 * This Activity overrides configuration changes for simplicity.
 */
@OptIn(ExperimentalWindowApi::class)
class RearDisplayPresentationActivity :
    EdgeToEdgeActivity(), WindowAreaPresentationSessionCallback {

    private var activePresentation: WindowAreaSessionPresenter? = null
    private var currentWindowAreaInfo: WindowAreaInfo? = null
    private lateinit var windowAreaController: WindowAreaController
    private val infoLogAdapter = InfoLogAdapter()

    private lateinit var binding: ActivityRearDisplayPresentationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowAreaController = WindowAreaController.getOrCreate()

        binding = ActivityRearDisplayPresentationBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                        if (windowAreaInfo.type == TYPE_REAR_FACING) {
                            currentWindowAreaInfo = windowAreaInfo
                            val presentCapability =
                                windowAreaInfo.getCapability(OPERATION_PRESENT_ON_AREA)
                            infoLogAdapter.append(
                                getCurrentTimeString(),
                                presentCapability.status.toString() +
                                    " : " +
                                    windowAreaInfo.metrics.toString()
                            )
                            updateRearDisplayPresentationButton()
                        }
                    }
                    infoLogAdapter.notifyDataSetChanged()
                }
            }
        }

        binding.rearDisplayPresentationButton.setOnClickListener {
            if (activePresentation != null) {
                activePresentation?.close()
            } else {
                currentWindowAreaInfo?.let {
                    windowAreaController.presentContentOnWindowArea(
                        it.token,
                        this@RearDisplayPresentationActivity,
                        { obj: Runnable -> obj.run() },
                        this@RearDisplayPresentationActivity
                    )
                }
            }
        }
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        infoLogAdapter.appendAndNotify(
            getCurrentTimeString(),
            "Presentation session has been started"
        )

        activePresentation = session
        val concurrentContext: Context = session.context
        val contentView =
            LayoutInflater.from(concurrentContext).inflate(R.layout.concurrent_presentation, null)
        session.setContentView(contentView)
        activePresentation = session
        updateRearDisplayPresentationButton()
    }

    override fun onContainerVisibilityChanged(isVisible: Boolean) {
        infoLogAdapter.appendAndNotify(
            getCurrentTimeString(),
            "Presentation content is visible: $isVisible"
        )
    }

    override fun onSessionEnded(t: Throwable?) {
        infoLogAdapter.appendAndNotify(
            getCurrentTimeString(),
            "Presentation session has been ended"
        )
        activePresentation = null
    }

    private fun updateRearDisplayPresentationButton() {
        if (activePresentation != null) {
            binding.rearDisplayPresentationButton.isEnabled = true
            binding.rearDisplayPresentationButton.text = "Disable rear display presentation"
            return
        }
        when (currentWindowAreaInfo?.getCapability(OPERATION_PRESENT_ON_AREA)?.status) {
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED -> {
                binding.rearDisplayPresentationButton.isEnabled = false
                binding.rearDisplayPresentationButton.text =
                    "Rear display presentation mode is not supported on this device"
            }
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNAVAILABLE -> {
                binding.rearDisplayPresentationButton.isEnabled = false
                binding.rearDisplayPresentationButton.text =
                    "Rear display presentation is not currently available"
            }
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE -> {
                binding.rearDisplayPresentationButton.isEnabled = true
                binding.rearDisplayPresentationButton.text = "Enable rear display presentation mode"
            }
        }
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return currentDate.toString()
    }
}
