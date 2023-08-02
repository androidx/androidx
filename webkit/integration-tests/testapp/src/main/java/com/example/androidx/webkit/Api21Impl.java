/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.net.Uri;
import android.webkit.WebResourceRequest;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper class to avoid class verification failures for APIs introduced in version 21.
 */
@RequiresApi(21)
class Api21Impl {
    private Api21Impl() {
        // This class is not instantiable.
    }

    @NonNull
    @DoNotInline
    static Uri getUrl(@NonNull WebResourceRequest webResourceRequest) {
        return webResourceRequest.getUrl();
    }

}
