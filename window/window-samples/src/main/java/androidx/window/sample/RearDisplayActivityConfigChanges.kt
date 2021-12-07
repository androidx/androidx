/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaStatus
import androidx.window.core.ExperimentalWindowApi
import androidx.window.java.area.WindowAreaControllerJavaAdapter
import androidx.window.sample.databinding.ActivityRearDisplayBinding
import androidx.window.sample.infolog.InfoLogAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Demo Activity that showcases listening for RearDisplay Status
 * as well as enabling/disabling RearDisplay mode. This Activity
 * implements [WindowAreaSessionCallback] for simplicity.
 *
 * This Activity overrides configuration changes for simplicity.
 */
@OptIn(ExperimentalWindowApi::class)
class RearDisplayActivityConfigChanges : AppCompatActivity(), WindowAreaSessionCallback {

    private lateinit var windowAreaController: WindowAreaControllerJavaAdapter
    private var rearDisplaySession: WindowAreaSession? = null
    private val infoLogAdapter = InfoLogAdapter()
    private lateinit var binding: ActivityRearDisplayBinding
    private lateinit var executor: Executor

    private val rearDisplayStatusListener = Consumer<WindowAreaStatus> { status ->
        infoLogAdapter.append(getCurrentTimeString(), status.toString())
        infoLogAdapter.notifyDataSetChanged()
        updateRearDisplayButton(status)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRearDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaControllerJavaAdapter(WindowAreaController.getOrCreate())

        binding.rearStatusRecyclerView.adapter = infoLogAdapter

        binding.rearDisplayButton.setOnClickListener {
            if (rearDisplaySession != null) {
                rearDisplaySession?.close()
            } else {
                windowAreaController.startRearDisplayModeSession(
                    this,
                    executor,
                    this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        windowAreaController.addRearDisplayStatusListener(
            executor,
            rearDisplayStatusListener
        )
    }

    override fun onStop() {
        super.onStop()
        windowAreaController.removeRearDisplayStatusListener(rearDisplayStatusListener)
    }

    override fun onSessionStarted(session: WindowAreaSession) {
        rearDisplaySession = session
        infoLogAdapter.append(getCurrentTimeString(), "RearDisplay Session has been started")
        infoLogAdapter.notifyDataSetChanged()
    }

    override fun onSessionEnded() {
        rearDisplaySession = null
        infoLogAdapter.append(getCurrentTimeString(), "RearDisplay Session has ended")
        infoLogAdapter.notifyDataSetChanged()
    }

    private fun updateRearDisplayButton(status: WindowAreaStatus) {
        if (rearDisplaySession != null) {
            binding.rearDisplayButton.isEnabled = true
            binding.rearDisplayButton.text = "Disable RearDisplay Mode"
            return
        }
        when (status) {
            WindowAreaStatus.UNSUPPORTED -> {
                binding.rearDisplayButton.isEnabled = false
                binding.rearDisplayButton.text = "RearDisplay is not supported on this device"
            }
            WindowAreaStatus.UNAVAILABLE -> {
                binding.rearDisplayButton.isEnabled = false
                binding.rearDisplayButton.text = "RearDisplay is not currently available"
            }
            WindowAreaStatus.AVAILABLE -> {
                binding.rearDisplayButton.isEnabled = true
                binding.rearDisplayButton.text = "Enable RearDisplay Mode"
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