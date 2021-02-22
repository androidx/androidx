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

import androidx.annotation.NonNull;
import androidx.webkit.WebMessagePortCompat.WebMessageCallbackCompat;

import org.chromium.support_lib_boundary.WebMessageBoundaryInterface;
import org.chromium.support_lib_boundary.WebMessageCallbackBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;

import java.lang.reflect.InvocationHandler;

/**
 * Adapter between {@link WebMessageCallbackCompat} and {@link WebMessageCallbackBoundaryInterface}.
 */
public class WebMessageCallbackAdapter implements WebMessageCallbackBoundaryInterface {
    WebMessageCallbackCompat mImpl;

    public WebMessageCallbackAdapter(@NonNull WebMessageCallbackCompat impl) {
        mImpl = impl;
    }

    @Override
    public void onMessage(InvocationHandler port, InvocationHandler message) {
        mImpl.onMessage(new WebMessagePortImpl(port),
                WebMessageAdapter.webMessageCompatFromBoundaryInterface(
                        BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                                WebMessageBoundaryInterface.class, message)));
    }

    // This method declares which APIs the support library side supports - so that the Chromium-side
    // doesn't crash if we remove APIs from the support library side.
    @Override
    @NonNull
    public String[] getSupportedFeatures() {
        return new String[] { Features.WEB_MESSAGE_CALLBACK_ON_MESSAGE };
    }
}
