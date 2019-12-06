/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures.fragment.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.dynamicfeatures.fragment.R
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode

/**
 * The default [androidx.fragment.app.Fragment] to display during installation progress.
 *
 * This `Fragment` provides a default UI and handles split install state changes so you don't
 * have to deal with this.
 *
 * To create a custom progress fragment, extend [AbstractProgressFragment].
 */
class DefaultProgressFragment :
    AbstractProgressFragment(R.layout.dynamic_feature_install_fragment) {

    internal companion object {
        private const val PROGRESS_MAX = 100
    }

    private lateinit var moduleName: TextView
    private lateinit var progressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        moduleName = view.findViewById(R.id.module_name)
        progressBar = view.findViewById(R.id.installation_progress)
        progressBar.isIndeterminate = true
        progressBar.max = PROGRESS_MAX

        val activityIcon = view.findViewById<ImageView>(R.id.progress_icon)
        val icon = try {
            requireActivity().packageManager.getActivityIcon(
                ComponentName(requireContext(), requireActivity().javaClass))
        } catch (e: PackageManager.NameNotFoundException) {
            requireActivity().packageManager.defaultActivityIcon
        }
        activityIcon.setImageDrawable(icon)
    }

    override fun onProgress(status: Int, bytesDownloaded: Long, bytesTotal: Long) {
        progressBar.visibility = View.VISIBLE
        if (bytesTotal > 0) {
            progressBar.isIndeterminate = false
            progressBar.progress = (PROGRESS_MAX * bytesDownloaded / bytesTotal).toInt()
        } else {
            progressBar.isIndeterminate = true
        }
    }

    override fun onCancelled() {
        progressBar.visibility = View.INVISIBLE
        moduleName.setText(R.string.installation_cancelled)
    }

    override fun onFailed(@SplitInstallErrorCode errorCode: Int) {
        progressBar.visibility = View.INVISIBLE
        moduleName.setText(R.string.installation_failed)
    }
}
