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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.navigation.dynamicfeatures.fragment.R
import androidx.navigation.fragment.findNavController
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode

/**
 * The default [androidx.fragment.app.Fragment] to display during installation progress.
 *
 * This `Fragment` provides a default UI and handles split install state changes so you don't
 * have to deal with this.
 *
 * To create a custom progress fragment, extend [AbstractProgressFragment].
 */
public class DefaultProgressFragment :
    AbstractProgressFragment(R.layout.dynamic_feature_install_fragment) {

    internal companion object {
        private const val PROGRESS_MAX = 100
        private const val TAG = "DefaultProgressFragment"
    }

    private var title: TextView? = null
    private var progressBar: ProgressBar? = null
    private var action: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            title = findViewById(R.id.progress_title)
            progressBar = findViewById(R.id.installation_progress)
            setActivityIcon(findViewById(R.id.progress_icon))
            action = findViewById(R.id.progress_action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        title = null
        progressBar = null
        action = null
    }

    private fun setActivityIcon(activityIcon: ImageView) {
        with(requireContext().packageManager) {
            val icon = try {
                getActivityIcon(ComponentName(requireContext(), requireActivity().javaClass))
            } catch (e: PackageManager.NameNotFoundException) {
                defaultActivityIcon
            }
            activityIcon.setImageDrawable(icon)
        }
    }

    override fun onProgress(status: Int, bytesDownloaded: Long, bytesTotal: Long) {
        progressBar?.run {
            visibility = View.VISIBLE
            if (bytesTotal == 0L) {
                isIndeterminate = true
            } else {
                progress = (PROGRESS_MAX * bytesDownloaded / bytesTotal).toInt()
                isIndeterminate = false
            }
        }
    }

    override fun onCancelled() {
        displayErrorState(R.string.installation_cancelled)
        displayAction(R.string.retry) { navigate() }
    }

    override fun onFailed(@SplitInstallErrorCode errorCode: Int) {
        Log.w(TAG, "Installation failed with error $errorCode")
        displayErrorState(R.string.installation_failed)
        displayAction(R.string.ok) { findNavController().popBackStack() }
    }

    /**
     * Display an error state message.
     */
    private fun displayErrorState(@StringRes text: Int) {
        title?.setText(text)
        progressBar?.visibility = View.INVISIBLE
    }

    /**
     * Display the action button and assign `onClick` behavior.
     */
    private fun displayAction(@StringRes text: Int, onClick: () -> Unit) {
        action?.run {
            setText(text)
            setOnClickListener {
                onClick()
            }
            visibility = View.VISIBLE
        }
    }
}
