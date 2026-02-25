/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.testapp.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.testapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.reflect.KMutableProperty0

internal typealias BooleanProperty = KMutableProperty0<Boolean>

/**
 * A dialog that allows the user to toggle feature flags.
 *
 * This class encapsulates the logic for inflating the dialog, configuring the switches based on the
 * current feature flag states, and handling user interactions to update those flags.
 *
 * The list of switches is configured using Kotlin property references ([KMutableProperty0]) for a
 * more concise and maintainable API. The performance impact of reflection is not the priority for
 * this sample app.
 *
 * @param context The context used to create the dialog and access resources.
 */
internal class FeaturePreferencesDialog(
    private val context: Context,
    private val listener: FeatureFlagListener? = null,
) {

    private val featureSwitches: List<FeatureFlagConfig> by lazy {
        listOf(
            FeatureFlagConfig(
                R.id.custom_link_handling_switch,
                PdfFeatureFlags::isCustomLinkHandlingEnabled,
                FeatureFlagNames.CUSTOM_LINK_HANDLING,
                context.getString(R.string.custom_link),
            ),
            FeatureFlagConfig(
                R.id.thumbnail_preview_switch,
                PdfFeatureFlags::isThumbnailPreviewEnabled,
                FeatureFlagNames.THUMBNAIL_PREVIEW,
                context.getString(R.string.thumbnail_preview),
            ),
            FeatureFlagConfig(
                R.id.form_filling_switch,
                PdfFeatureFlags::isFormFillingEnabled,
                FeatureFlagNames.FORM_FILLING,
                context.getString(R.string.enable_form_filling),
            ),
        )
    }

    private val settingsDialogView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.feature_flag_preference, null)
    }

    private val dialog: AlertDialog by lazy {
        // One-time setup: Find views and set change listeners.
        featureSwitches.forEach { config ->
            config.switchView =
                settingsDialogView.findViewById<SwitchCompat>(config.viewId).apply {
                    setOnCheckedChangeListener { _, isChecked ->
                        config.property.set(isChecked)
                        listener?.onFeatureFlagUpdated(config.flagName, isChecked)
                    }
                    text = config.text
                }
        }

        MaterialAlertDialogBuilder(context)
            .setView(settingsDialogView)
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                // This listener runs every time the dialog is shown.
                setOnShowListener {
                    // Update the state of the switches from the feature flags.
                    featureSwitches.forEach { config ->
                        config.switchView.isChecked = config.property.get()
                    }
                }
            }
    }

    fun show() {
        dialog.show()
    }
}

/** Data class to hold the configuration for a single feature switch in the settings dialog. */
private data class FeatureFlagConfig(
    @IdRes val viewId: Int,
    val property: BooleanProperty,
    val flagName: String,
    val text: String,
) {
    lateinit var switchView: SwitchCompat
}

interface FeatureFlagListener {
    fun onFeatureFlagUpdated(flagName: String, enabled: Boolean)
}

object FeatureFlagNames {
    const val CUSTOM_LINK_HANDLING: String = "CUSTOM_LINK_HANDLING"
    const val THUMBNAIL_PREVIEW: String = "THUMBNAIL_PREVIEW"
    const val FORM_FILLING: String = "FORM_FILLING"
}
