/*
 * Copyright 2018 The Android Open Source Project
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

@file:SuppressLint("ClassVerificationFailure") // Entire file is RequiresApi(17)

package androidx.core.text

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Returns layout direction for a given locale.
 *
 * @see TextUtils.getLayoutDirectionFromLocale
 */
public inline val Locale.layoutDirection: Int
    @RequiresApi(17)
    get() = TextUtils.getLayoutDirectionFromLocale(this)
