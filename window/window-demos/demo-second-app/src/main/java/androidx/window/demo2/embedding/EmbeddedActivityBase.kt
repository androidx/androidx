/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.demo2.embedding

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.WindowSdkExtensions
import androidx.window.demo.common.EdgeToEdgeActivity
import androidx.window.demo.common.util.PictureInPictureUtil
import androidx.window.demo2.R
import androidx.window.demo2.databinding.ActivityEmbeddedBinding
import androidx.window.embedding.ActivityEmbeddingController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class EmbeddedActivityBase : EdgeToEdgeActivity() {
    lateinit var viewBinding: ActivityEmbeddedBinding
    private lateinit var activityEmbeddingController: ActivityEmbeddingController
    private lateinit var windowInfoView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityEmbeddedBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.buttonPip.setOnClickListener {
            PictureInPictureUtil.startPictureInPicture(this, false)
        }
        viewBinding.buttonStartActivity.setOnClickListener {
            startActivity(Intent(this, this.javaClass))
        }
        viewBinding.buttonStartActivityFromApplicationContext.setOnClickListener {
            application.startActivity(Intent(this, this.javaClass).setFlags(FLAG_ACTIVITY_NEW_TASK))
        }

        activityEmbeddingController = ActivityEmbeddingController.getInstance(this)
        initializeEmbeddedActivityInfoCallback()
    }

    private fun initializeEmbeddedActivityInfoCallback() {
        val extensionVersion = WindowSdkExtensions.getInstance().extensionVersion
        if (extensionVersion < 6) {
            // EmbeddedActivityWindowInfo is only available on 6+.
            return
        }

        windowInfoView = viewBinding.windowIntoText
        lifecycleScope.launch(Dispatchers.Main) {
            // Collect EmbeddedActivityWindowInfo when STARTED and stop when STOPPED.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // After register, the flow will be triggered immediately if the activity is
                // embedded.
                // However, if the activity is changed to non-embedded state in background (after
                // STOPPED), the flow will not report the change (because it has been unregistered).
                // Reset before start listening.
                resetWindowInfoView()
                activityEmbeddingController
                    .embeddedActivityWindowInfo(this@EmbeddedActivityBase)
                    .collect { info ->
                        if (info.isEmbedded) {
                            windowInfoView.text = info.toString()
                        } else {
                            resetWindowInfoView()
                        }
                    }
            }
        }
    }

    private fun resetWindowInfoView() {
        windowInfoView.text = getString(R.string.embedded_window_info_unavailable)
    }
}
