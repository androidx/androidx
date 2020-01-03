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

package androidx.webkit.internal;

import android.webkit.WebResourceRequest;

import org.chromium.support_lib_boundary.WebResourceRequestBoundaryInterface;

/**
 * Adapter between {@link androidx.webkit.WebResourceRequestCompat} and
 * {@link org.chromium.support_lib_boundary.WebResourceRequestBoundaryInterface}.
 */
public class WebResourceRequestAdapter {
    private final WebResourceRequestBoundaryInterface mBoundaryInterface;

    public WebResourceRequestAdapter(WebResourceRequestBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    public boolean isRedirect() {
        return mBoundaryInterface.isRedirect();
    }
}
