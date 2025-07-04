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

package androidx.pdf.event

import androidx.annotation.RestrictTo

/**
 * A tracking event indicating a failure during a user request.
 *
 * This event is logged when a non-fatal error occurs while attempting to fulfill a user's action,
 * such as rendering a page or performing a search. It includes information about the specific
 * exception that caused the failure.
 *
 * @property exception The [Throwable] that represents the failure.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RequestFailureEvent(public val exception: Throwable) : PdfTrackingEvent()
