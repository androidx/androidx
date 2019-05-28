/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.settings_dialog.button_cancel
import kotlinx.android.synthetic.main.settings_dialog.button_start

/**
 * DialogFragment that backs the configuration for both single tests and multiple tests
 */
internal class SettingsDialog : DialogFragment() {

    override fun onStart() {
        // If we show a dialog with a title, it doesn't take up the whole screen
        // Adjust the window to take up the full screen
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the dialog style so we get a title bar
        setStyle(DialogFragment.STYLE_NORMAL, R.style.SettingsDialogTheme)
    }

    /** Set up the dialog depending on the dialog type */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args = arguments
        val type = args?.getString(DIALOG_TYPE)
        val title = args?.getString(DIALOG_TITLE)
        val cameraNames = args?.getStringArray(CAMERA_NAMES)
        val cameraIds = args?.getStringArray(CAMERA_IDS)

        dialog?.setTitle(title)

        val dialogView = inflater.inflate(R.layout.settings_dialog, container, false)

        if (null != cameraIds && null != cameraNames) {
            when (type) {
                DIALOG_TYPE_MULTI -> {
                    val settingsFragment = MultiTestSettingsFragment()
                    val childFragmentManager = childFragmentManager
                    val fragmentTransaction = childFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.scroll_settings_dialog, settingsFragment)
                    fragmentTransaction.commit()
                }
                else -> {
                    val settingsFragment = SingleTestSettingsFragment(cameraNames, cameraIds)
                    val childFragmentManager = childFragmentManager
                    val fragmentTransaction = childFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.scroll_settings_dialog, settingsFragment)
                    fragmentTransaction.commit()
                }
            }
        }

        return dialogView
    }

    /** When view is created, set up action buttons */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        val type = args?.getString(DIALOG_TYPE)

        when (type) {
            DIALOG_TYPE_MULTI -> {
                button_start.text = getString(R.string.settings_multi_go)
                button_cancel.text = getString(R.string.settings_multi_cancel)

                button_start.setOnClickListener {
                    (activity as MainActivity).startMultiTest()
                    this.dismiss()
                }
                button_cancel.setOnClickListener { this.dismiss() }
            }
            else -> {
                button_start.text = getString(R.string.settings_single_go)
                button_cancel.text = getString(R.string.settings_single_cancel)

                button_start.setOnClickListener {
                    (activity as MainActivity).startSingleTest()
                    this.dismiss()
                }
                button_cancel.setOnClickListener { this.dismiss() }
            }
        }
    }

    companion object {
        private const val DIALOG_TYPE = "DIALOG_TYPE"
        private const val DIALOG_TITLE = "DIALOG_TITLE"
        private const val CAMERA_NAMES = "CAMERA_NAMES"
        private const val CAMERA_IDS = "CAMERA_IDS"

        internal const val DIALOG_TYPE_SINGLE = "DIALOG_TYPE_SINGLE"
        internal const val DIALOG_TYPE_MULTI = "DIALOG_TYPE_MULTI"

        /**
         * Create a new Settings dialog to configure a test run
         *
         * @param type Dialog type (DIALOG_TYPE_MULTI or DIALOG_TYPE_SINGLE)
         * @param title Dialog title
         * @param cameraNames Human readable array of camera names
         * @param cameraIds Array of camera ids
         */
        fun newInstance(
            type: String,
            title: String,
            cameraNames: Array<String>,
            cameraIds: Array<String>
        ): SettingsDialog {

            val args = Bundle()
            args.putString(DIALOG_TYPE, type)
            args.putString(DIALOG_TITLE, title)
            args.putStringArray(CAMERA_NAMES, cameraNames)
            args.putStringArray(CAMERA_IDS, cameraIds)

            val settingsDialog = SettingsDialog()
            settingsDialog.arguments = args
            return settingsDialog
        }
    }
}