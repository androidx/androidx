/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.integration.view

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect
import androidx.camera.effects.playservices.lowlight.LowLightBoostException

/**
 * Shared UI formatting and target parsing helper methods for [LowLightBoostEffectActivity] and
 * [LowLightBoostEffectFragment].
 */
@RequiresApi(Build.VERSION_CODES.R)
internal object LowLightBoostAppHelper {

    /**
     * Enables or disables [views]. For a [ViewGroup] such as a RadioGroup, its children are also
     * updated because disabling a ViewGroup does not disable its children.
     */
    fun setControlsEnabled(enabled: Boolean, vararg views: View) {
        for (view in views) {
            view.isEnabled = enabled
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    view.getChildAt(index).isEnabled = enabled
                }
            }
        }
    }

    internal fun parseTargetExtra(targetExtra: String): Int {
        return when (targetExtra.uppercase()) {
            "PREVIEW" -> CameraEffect.PREVIEW
            "PREV_VIDEO" -> CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE
            "ALL" ->
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
            else -> {
                Log.w(TAG, "Unknown target \"$targetExtra\"; using PREVIEW")
                CameraEffect.PREVIEW
            }
        }
    }

    internal fun targetIdToMask(checkedId: Int): Int {
        return when (checkedId) {
            R.id.radio_target_preview_video -> CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE
            R.id.radio_target_all ->
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
            else -> CameraEffect.PREVIEW
        }
    }

    internal fun checkTargetRadioButton(
        targetSetting: Int,
        radioTargetPreview: RadioButton,
        radioTargetPreviewVideo: RadioButton,
        radioTargetAll: RadioButton,
    ) {
        when (targetSetting) {
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE ->
                radioTargetPreviewVideo.isChecked = true
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE ->
                radioTargetAll.isChecked = true
            else -> radioTargetPreview.isChecked = true
        }
    }

    private fun formatRotation(rotation: Int): String {
        return when (rotation) {
            Surface.ROTATION_0 -> "0° (ROTATION_0)"
            Surface.ROTATION_90 -> "90° (ROTATION_90)"
            Surface.ROTATION_180 -> "180° (ROTATION_180)"
            Surface.ROTATION_270 -> "270° (ROTATION_270)"
            else -> "$rotation"
        }
    }

    internal fun updateStatusUi(
        context: Context,
        statusTextView: TextView,
        toggleLlbButton: Button,
        isEnabled: Boolean,
        cameraSelector: CameraSelector,
        isSupported: Boolean?,
        isCompatibleMode: Boolean,
        targetSetting: Int,
        rotation: Int,
        currentLlbStatus: Int?,
        currentLlbError: LowLightBoostException?,
        streamStateText: String,
    ) {
        val cameraFacing =
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "Back" else "Front"
        val suppText = isSupported?.toString() ?: "checking..."
        val modeText = if (isCompatibleMode) "COMPATIBLE" else "PERFORMANCE"
        val targetText =
            when (targetSetting) {
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE -> "PREV|VIDEO"
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE ->
                    "ALL"
                else -> "PREVIEW"
            }

        val stateName =
            when (currentLlbStatus) {
                LowLightBoostEffect.STATUS_OFF -> "OFF"
                LowLightBoostEffect.STATUS_INACTIVE -> "INACTIVE"
                LowLightBoostEffect.STATUS_ACTIVE -> "ACTIVE"
                LowLightBoostEffect.STATUS_PENDING_INSTALLATION -> "PENDING_INSTALLATION"
                else -> if (isEnabled) "INITIALIZING" else "OFF"
            }

        // A non-null error always comes with STATUS_OFF, so it is shown instead of the status.
        val llbStatusText =
            if (currentLlbError != null) {
                val detail = currentLlbError.message ?: currentLlbError.javaClass.simpleName
                "ERROR (code=${currentLlbError.errorCode}, $detail)"
            } else {
                stateName
            }

        val line1 =
            "Mode: $modeText | Facing: $cameraFacing | Rot: ${formatRotation(rotation)} | " +
                "Target: $targetText\n"
        val llbSegment = "LLB: $llbStatusText"
        val line2Suffix = " | Stream: $streamStateText | Supported: $suppText"
        val status = line1 + llbSegment + line2Suffix

        if (currentLlbError != null) {
            val spannable = SpannableStringBuilder(status)
            spannable.setSpan(
                ForegroundColorSpan(Color.RED),
                line1.length,
                line1.length + llbSegment.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            statusTextView.text = spannable
        } else {
            statusTextView.text = status
        }
        toggleLlbButton.text =
            if (isEnabled) context.getString(R.string.llb_disable)
            else context.getString(R.string.llb_enable)
    }

    private const val TAG = "LowLightBoostAppHelper"
}
