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

package androidx.credentials.providerevents.transfer

import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.ClearExportException

/**
 * The result of calling [ProviderEventsManager.clearExport]. This response will be returned when
 * the [ClearExportRequest] was successfully processed.
 *
 * @param isDeleted if true, the clear operation deleted some entries, otherwise indicates there was
 *   no data to delete; unexpected failures will be thrown as [ClearExportException]
 */
public class ClearExportResponse(public val isDeleted: Boolean)
