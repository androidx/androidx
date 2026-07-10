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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.creation.RemoteComposeContext
import java.util.zip.CRC32
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val arbitrarySize = 555 // TODO: remove this value

/** TODO: this file is to support testing. Remove. */
internal fun makeCoreDocumentForDebug(wireBuffer: WireBuffer): CoreDocument {
    //    val rcState = RemoteComposeState()
    //    val b2 = RemoteComposeWriter.obtain()
    val remoteComposeBuffer =
        RemoteComposeBuffer(RemoteComposeConstants.RemoteComposeVersion).apply { // TODO: hardcoded
            //            setPlatform(platform) // TODO where'd this go?

            buffer = wireBuffer
        }
    val coreDocument =
        CoreDocument().apply {
            height = arbitrarySize
            width = arbitrarySize
            initFromBuffer(remoteComposeBuffer)
        }
    return coreDocument
}

/**
 * Debug function that converts the remote compose context's document to base64 and then prints it
 * and copies it to the clipboard.
 *
 * TODO: remove this once documents are sent via remote views.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun printAndCopyDoc(rcContext: RemoteComposeContext, androidContext: Context) {

    val bytes: ByteArray = getBytes(rcContext)
    val base64 = Base64.encode(bytes)
    val clipboard: ClipboardManager? =
        androidContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText("Origami", base64)
    clipboard!!.setPrimaryClip(clip)
    Log.d(TAG, "new doc copied to clipboard vvvvv")
    val checksum =
        with(CRC32()) {
            this.update(bytes)
            this.getValue()
        }
    Log.d(
        TAG,
        "printAndCopyDoc: len ${bytes.size}, first byte ${bytes.first()}, lastByte ${bytes.last()}, checksum $checksum",
    )
    Log.d(TAG, base64)
    Log.d(TAG, "^^^^^^")

    val doc = makeCoreDocumentForDebug(wireBuffer = rcContext.buffer.buffer)

    val docStr = doc.toNestedString()
    Log.d(TAG, "doc contents: $docStr")
}
