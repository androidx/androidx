/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.appcompat

import androidx.appcompat.res.ColorStateListAlphaDetector
import androidx.appcompat.res.ColorStateListLoadingDetector
import androidx.appcompat.res.ImageViewTintDetector
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API

class AppCompatIssueRegistry : IssueRegistry() {
    override val api = CURRENT_API
    override val issues get() = listOf(
        ColorStateListAlphaDetector.NOT_USING_ANDROID_ALPHA,
        ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING,
        ImageViewTintDetector.USING_ANDROID_TINT
    )
}
