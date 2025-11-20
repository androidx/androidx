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

package androidx.pdf.view.fastscroll

import android.content.Context
import android.util.Range

/**
 * Calculates the length of an integer range.
 *
 * @return The length of the range (inclusive).
 */
internal fun Range<Int>.length(): Int = 1 + this.upper - this.lower

/**
 * Retrieves a dimension value for a particular resource ID.
 *
 * @param id The resource ID of the dimension to retrieve.
 * @return The dimension resource value. This is a float.
 */
internal fun Context.getDimensions(id: Int): Float = this.resources.getDimension(id)
