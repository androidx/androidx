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

package androidx.pdf.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocumentRemote

@RestrictTo(RestrictTo.Scope.LIBRARY)
/**
 * A [Service] that provides functionality to interact with PDF document.
 *
 * This service runs in the background. It exposes a [PdfDocumentRemote] interface for clients to
 * interact with, enabling them to access and render PDF documents. The service uses [PdfRenderer]
 * and [PdfRendererPreV] internally to process the documents based on the Android OS version.
 *
 * Clients can bind to this service to perform operations such as rendering pages, extracting text,
 * and accessing document metadata.
 */
public class PdfDocumentServiceImpl : Service() {
    override fun onBind(intent: Intent?): IBinder = PdfDocumentRemoteImpl()
}
