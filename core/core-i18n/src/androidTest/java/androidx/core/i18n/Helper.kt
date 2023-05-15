/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.core.i18n

object Helper {
    /*
     * This method changes all instances of U+202F to U+0020.
     *
     * Android U takes ICU 71.1, which uses NNBSP (NARROW NO-BREAK SPACE, U+202F)
     * betwee time and day cycle (for example 9:42\u202FPM)
     *
     * The Android `java.text.DateFormat` was patched to not use nnbsp (U+202F)
     * in Android U, but ICU still returns times with U+202F.
     * So this would give different results, but it is expected.
     * In time this will probably go away (as the newer Android images propagate everywhere).
     *
     * And, since the patch happened without changing the Android version (pre-release),
     * there are some Android U images that use space and some that use NNBSP.
     * So testing the version is not enough to reliably tell if we will get.
     */
    fun normalizeNnbsp(text: String): String {
        return text.replace("\u202F", " ")
    }
}