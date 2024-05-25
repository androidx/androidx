/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.collection.internal

// The function below is technically identical to Float.fromBits()
// However, since it is declared as top- level functions, it does
// not incur the cost of a static fetch through the Companion class.
// Using this top-level function, the generated arm64 code after
// dex2oat is exactly a single `fmov`

/** Returns the [Float] value corresponding to a given bit representation. */
@PublishedApi internal expect fun floatFromBits(bits: Int): Float
