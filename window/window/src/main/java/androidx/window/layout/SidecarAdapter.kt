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
@file:Suppress("DEPRECATION")

package androidx.window.layout

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.window.core.Bounds
import androidx.window.core.SpecificationComputer.Companion.startSpecification
import androidx.window.core.SpecificationComputer.VerificationMode
import androidx.window.core.SpecificationComputer.VerificationMode.QUIET
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.FOLD
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import androidx.window.layout.SidecarWindowBackend.Companion.DEBUG
import androidx.window.sidecar.SidecarDeviceState
import androidx.window.sidecar.SidecarDisplayFeature
import androidx.window.sidecar.SidecarDisplayFeature.TYPE_FOLD
import androidx.window.sidecar.SidecarDisplayFeature.TYPE_HINGE
import androidx.window.sidecar.SidecarWindowLayoutInfo
import java.lang.reflect.InvocationTargetException

/**
 * A class for translating Sidecar data classes.
 */
// TODO(b/206055949) convert to strict validation.
internal class SidecarAdapter(private val verificationMode: VerificationMode = QUIET) {

    fun translate(
        sidecarDisplayFeatures: List<SidecarDisplayFeature>,
        deviceState: SidecarDeviceState
    ): List<DisplayFeature> {
        return sidecarDisplayFeatures.mapNotNull { sidecarFeature ->
            translate(sidecarFeature, deviceState)
        }
    }

    fun translate(
        extensionInfo: SidecarWindowLayoutInfo?,
        state: SidecarDeviceState
    ): WindowLayoutInfo {
        if (extensionInfo == null) {
            return WindowLayoutInfo(emptyList())
        }
        val deviceState = SidecarDeviceState()
        val devicePosture = getSidecarDevicePosture(state)
        setSidecarDevicePosture(deviceState, devicePosture)
        val sidecarDisplayFeatures = getSidecarDisplayFeatures(extensionInfo)
        val displayFeatures = translate(sidecarDisplayFeatures, deviceState)
        return WindowLayoutInfo(displayFeatures)
    }

    fun isEqualSidecarDeviceState(
        first: SidecarDeviceState?,
        second: SidecarDeviceState?
    ): Boolean {
        if (first == second) {
            return true
        }
        if (first == null) {
            return false
        }
        if (second == null) {
            return false
        }
        val firstPosture = getSidecarDevicePosture(first)
        val secondPosture = getSidecarDevicePosture(second)
        return firstPosture == secondPosture
    }

    fun isEqualSidecarWindowLayoutInfo(
        first: SidecarWindowLayoutInfo?,
        second: SidecarWindowLayoutInfo?
    ): Boolean {
        if (first == second) {
            return true
        }
        if (first == null) {
            return false
        }
        if (second == null) {
            return false
        }
        val firstDisplayFeatures = getSidecarDisplayFeatures(first)
        val secondDisplayFeatures = getSidecarDisplayFeatures(second)
        return isEqualSidecarDisplayFeatures(firstDisplayFeatures, secondDisplayFeatures)
    }

    private fun isEqualSidecarDisplayFeatures(
        first: List<SidecarDisplayFeature>?,
        second: List<SidecarDisplayFeature>?
    ): Boolean {
        if (first === second) {
            return true
        }
        if (first == null) {
            return false
        }
        if (second == null) {
            return false
        }
        if (first.size != second.size) {
            return false
        }
        for (i in first.indices) {
            val firstFeature = first[i]
            val secondFeature = second[i]
            if (!isEqualSidecarDisplayFeature(firstFeature, secondFeature)) {
                return false
            }
        }
        return true
    }

    private fun isEqualSidecarDisplayFeature(
        first: SidecarDisplayFeature?,
        second: SidecarDisplayFeature?
    ): Boolean {
        if (first == second) {
            return true
        }
        if (first == null) {
            return false
        }
        if (second == null) {
            return false
        }
        if (first.type != second.type) {
            return false
        }
        return if (first.rect != second.rect) {
            false
        } else {
            true
        }
    }

    /**
     * Converts the display feature from extension. Can return `null` if there is an issue
     * with the value passed from extension.
     */
    internal fun translate(
        feature: SidecarDisplayFeature,
        deviceState: SidecarDeviceState
    ): DisplayFeature? {
        val checkedFeature = feature.startSpecification(TAG, verificationMode)
            .require("Type must be either TYPE_FOLD or TYPE_HINGE") {
                type == TYPE_FOLD || type == TYPE_HINGE
            }
            .require("Feature bounds must not be 0") { rect.width() != 0 || rect.height() != 0 }
            .require("TYPE_FOLD must have 0 area") {
                if (type == TYPE_FOLD) {
                    rect.width() == 0 || rect.height() == 0
                } else {
                    true
                }
            }
            .require("Feature be pinned to either left or top") {
                rect.left == 0 || rect.top == 0
            }
            .compute() ?: return null
        val type = when (checkedFeature.type) {
            TYPE_FOLD -> FOLD
            TYPE_HINGE -> HINGE
            else -> {
                return null
            }
        }
        val state = when (getSidecarDevicePosture(deviceState)) {
            SidecarDeviceState.POSTURE_CLOSED,
            SidecarDeviceState.POSTURE_UNKNOWN,
            SidecarDeviceState.POSTURE_FLIPPED -> return null
            SidecarDeviceState.POSTURE_HALF_OPENED -> FoldingFeature.State.HALF_OPENED
            SidecarDeviceState.POSTURE_OPENED -> FoldingFeature.State.FLAT
            else -> FoldingFeature.State.FLAT
        }
        return HardwareFoldingFeature(Bounds(feature.rect), type, state)
    }

    companion object {
        private val TAG = SidecarAdapter::class.java.simpleName

        // TODO(b/172620880): Workaround for Sidecar API implementation issue.
        @SuppressLint("BanUncheckedReflection")
        @VisibleForTesting
        fun getSidecarDisplayFeatures(info: SidecarWindowLayoutInfo): List<SidecarDisplayFeature> {
            try {
                return info.displayFeatures ?: emptyList()
            } catch (error: NoSuchFieldError) {
                try {
                    val methodGetFeatures = SidecarWindowLayoutInfo::class.java.getMethod(
                        "getDisplayFeatures"
                    )
                    @Suppress("UNCHECKED_CAST")
                    return methodGetFeatures.invoke(info) as List<SidecarDisplayFeature>
                } catch (e: NoSuchMethodException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: IllegalAccessException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: InvocationTargetException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
            return emptyList()
        }

        // TODO(b/172620880): Workaround for Sidecar API implementation issue.
        @SuppressLint("BanUncheckedReflection")
        @VisibleForTesting
        fun setSidecarDisplayFeatures(
            info: SidecarWindowLayoutInfo,
            displayFeatures: List<SidecarDisplayFeature?>
        ) {
            try {
                info.displayFeatures = displayFeatures
            } catch (error: NoSuchFieldError) {
                try {
                    val methodSetFeatures = SidecarWindowLayoutInfo::class.java.getMethod(
                        "setDisplayFeatures", MutableList::class.java
                    )
                    methodSetFeatures.invoke(info, displayFeatures)
                } catch (e: NoSuchMethodException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: IllegalAccessException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: InvocationTargetException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
        }

        internal fun getSidecarDevicePosture(sidecarDeviceState: SidecarDeviceState): Int {
            val rawPosture = getRawSidecarDevicePosture(sidecarDeviceState)
            return if (rawPosture < SidecarDeviceState.POSTURE_UNKNOWN ||
                rawPosture > SidecarDeviceState.POSTURE_FLIPPED
            ) {
                SidecarDeviceState.POSTURE_UNKNOWN
            } else {
                rawPosture
            }
        }

        // TODO(b/172620880): Workaround for Sidecar API implementation issue.
        @SuppressLint("BanUncheckedReflection")
        @VisibleForTesting
        fun getRawSidecarDevicePosture(sidecarDeviceState: SidecarDeviceState): Int {
            try {
                return sidecarDeviceState.posture
            } catch (error: NoSuchFieldError) {
                try {
                    val methodGetPosture = SidecarDeviceState::class.java.getMethod("getPosture")
                    return methodGetPosture.invoke(sidecarDeviceState) as Int
                } catch (e: NoSuchMethodException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: IllegalAccessException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: InvocationTargetException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
            return SidecarDeviceState.POSTURE_UNKNOWN
        }

        // TODO(b/172620880): Workaround for Sidecar API implementation issue.
        @SuppressLint("BanUncheckedReflection")
        @VisibleForTesting
        fun setSidecarDevicePosture(sidecarDeviceState: SidecarDeviceState, posture: Int) {
            try {
                sidecarDeviceState.posture = posture
            } catch (error: NoSuchFieldError) {
                try {
                    val methodSetPosture = SidecarDeviceState::class.java.getMethod(
                        "setPosture",
                        Int::class.javaPrimitiveType
                    )
                    methodSetPosture.invoke(sidecarDeviceState, posture)
                } catch (e: NoSuchMethodException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: IllegalAccessException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                } catch (e: InvocationTargetException) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}