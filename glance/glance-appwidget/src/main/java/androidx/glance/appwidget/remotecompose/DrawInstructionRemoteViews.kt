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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose

import android.app.PendingIntent
import android.os.Build
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose
import androidx.glance.appwidget.toSizeF
import java.io.ByteArrayInputStream
import java.util.zip.CRC32

internal object DrawInstructionRemoteViews {
    /** Step 2 of translation: set the remote compose draw instructions on a remoteviews. */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun create(translation: GlanceToRemoteComposeTranslation): RemoteViews {
        return when (translation) {
            is GlanceToRemoteComposeTranslation.Single -> {
                drawInstructionRemoteViews(translation)
            }
            is GlanceToRemoteComposeTranslation.SizeMap -> {
                val map = mutableMapOf<SizeF, RemoteViews>()
                @Suppress("ListIterator")
                translation.results.forEach {
                    (size: DpSize, translation: GlanceToRemoteComposeTranslation.Single) ->
                    val sizeF = size.toSizeF()
                    val remoteView = drawInstructionRemoteViews(translation)
                    map.put(sizeF, remoteView)
                }

                RemoteViews(map)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun drawInstructionRemoteViews(
    translation: GlanceToRemoteComposeTranslation.Single
): RemoteViews {
    val bytes: ByteArray = getBytes(translation.remoteComposeContext)

    // The DrawInstructions api takes a list of byte arrays as a parameter, but as of
    // 2025-1, only the first array is used. The api is designed this way to allow future
    // growth.
    val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
    val remoteViews = RemoteViews(drawInstructions)

    // wire up onClick events.
    @Suppress("ListIterator")
    translation.actionMap.forEach { (actionId: Int, pendingIntent: PendingIntent) ->
        remoteViews.setOnClickPendingIntent(actionId, pendingIntent)
    }

    val checksum =
        with(CRC32()) {
            this.update(bytes)
            this.getValue()
        }
    if (DebugRemoteCompose) {
        Log.d(
            TAG,
            "drawInstructionRemoteViews: len ${bytes.size}, first byte ${bytes.first()}, lastByte ${bytes.last()}, checksum $checksum",
        )
    }

    return remoteViews
}

/** TODO: debug function. Remove. */
internal fun getBytes(rcContext: RemoteComposeContext): ByteArray {
    val buffer: WireBuffer = rcContext.buffer.buffer
    val bufferSize = buffer.size
    val bytesCopy = ByteArray(bufferSize)
    ByteArrayInputStream(buffer.buffer, 0, bufferSize).use { it.read(bytesCopy) }

    return bytesCopy
}
