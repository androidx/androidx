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

package androidx.ui.tooling.preview

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.compose.currentComposer
import androidx.ui.core.setContent

/**
 * Activity used to run `@Composable` previews from Android Studio.
 *
 * The `@Composable` function must have no parameters, and its name should be passed to this
 * Activity through intent parameters, using `composable` as the key and the `@Composable` fully
 * qualified name as the value.
 *
 * @hide
 */
class PreviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.getStringExtra("composable")?.let {
            Log.d("PreviewActivity", "PreviewActivity has composable $it")
            val className = it.substringBeforeLast('.')
            val methodName = it.substringAfterLast('.')
            setContent {
                invokeComposableViaReflection(
                    className,
                    methodName,
                    currentComposer
                )
            }
        }
    }
}