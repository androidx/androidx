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

import static org.chromium.support_lib_boundary.WebMessagePayloadBoundaryInterface.WebMessagePayloadType;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;

import org.chromium.support_lib_boundary.WebMessageBoundaryInterface;
import org.chromium.support_lib_boundary.WebMessagePayloadBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * Adapter between {@link WebMessageCompat} and
 * {@link org.chromium.support_lib_boundary.WebMessageBoundaryInterface}.
 * This class is used to pass a PostMessage from the app to Chromium.
 */
public class WebMessageAdapter implements WebMessageBoundaryInterface {
    private WebMessageCompat mWebMessageCompat;

    private static final String[] sFeatures = {Features.WEB_MESSAGE_ARRAY_BUFFER};

    public WebMessageAdapter(@NonNull WebMessageCompat webMessage) {
        this.mWebMessageCompat = webMessage;
    }

    /**
     * @deprecated  Keep backwards compatibility with old version of WebView. This method is
     * equivalent to {@link WebMessagePayloadBoundaryInterface#getAsString()}.
     */
    @Deprecated
    @Override
    @Nullable
    public String getData() {
        return mWebMessageCompat.getData();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    @Nullable
    public InvocationHandler getMessagePayload() {
        final WebMessagePayloadAdapter adapter;
        switch (mWebMessageCompat.getType()) {
            case WebMessageCompat.TYPE_STRING:
                adapter = new WebMessagePayloadAdapter(mWebMessageCompat.getData());
                break;
            case WebMessageCompat.TYPE_ARRAY_BUFFER:
                adapter = new WebMessagePayloadAdapter(
                        Objects.requireNonNull(mWebMessageCompat.getArrayBuffer()));
                break;
            default:
                throw new IllegalStateException(
                        "Unknown web message payload type: " + mWebMessageCompat.getType());
        }
        return BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(adapter);
    }

    @Override
    @Nullable
    public InvocationHandler[] getPorts() {
        WebMessagePortCompat[] ports = mWebMessageCompat.getPorts();
        if (ports == null) return null;

        InvocationHandler[] invocationHandlers = new InvocationHandler[ports.length];
        for (int n = 0; n < ports.length; n++) {
            invocationHandlers[n] = ports[n].getInvocationHandler();
        }
        return invocationHandlers;
    }

    @Override
    @NonNull
    public String[] getSupportedFeatures() {
        // getData() and getPorts() are not covered by feature flags.
        return sFeatures;
    }

    /**
     * Utility method to check if the WebMessageCompat payload type is supported by WebView.
     */
    public static boolean isMessagePayloadTypeSupportedByWebView(
            @WebMessageCompat.Type final int type) {
        return type == WebMessageCompat.TYPE_STRING
                || (type == WebMessageCompat.TYPE_ARRAY_BUFFER
                && WebViewFeatureInternal.WEB_MESSAGE_ARRAY_BUFFER.isSupportedByWebView());
    }

    // ====================================================================================
    // Methods related to converting a WebMessageBoundaryInterface into a WebMessageCompat.
    // ====================================================================================

    /**
     * Utility method used to convert PostMessages from the Chromium side to
     * {@link WebMessageCompat} objects - a class apps recognize.
     * Return null when the WebMessageCompat payload type is not supported by AndroidX now.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static WebMessageCompat webMessageCompatFromBoundaryInterface(
            @NonNull WebMessageBoundaryInterface boundaryInterface) {
        final WebMessagePortCompat[] ports = toWebMessagePortCompats(
                boundaryInterface.getPorts());
        if (WebViewFeatureInternal.WEB_MESSAGE_ARRAY_BUFFER.isSupportedByWebView()) {
            WebMessagePayloadBoundaryInterface payloadInterface =
                    BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                            WebMessagePayloadBoundaryInterface.class,
                            boundaryInterface.getMessagePayload());
            final @WebMessagePayloadType int type = payloadInterface.getType();
            switch (type) {
                case WebMessagePayloadType.TYPE_STRING:
                    return new WebMessageCompat(payloadInterface.getAsString(), ports);
                case WebMessagePayloadType.TYPE_ARRAY_BUFFER:
                    return new WebMessageCompat(payloadInterface.getAsArrayBuffer(), ports);
            }
            // Unsupported message type.
            return null;
        }
        // MessagePayload not supported by WebView, fallback.
        return new WebMessageCompat(boundaryInterface.getData(), ports);
    }

    @NonNull
    private static WebMessagePortCompat[] toWebMessagePortCompats(InvocationHandler[] ports) {
        WebMessagePortCompat[] compatPorts = new WebMessagePortCompat[ports.length];
        for (int n = 0; n < ports.length; n++) {
            compatPorts[n] = new WebMessagePortImpl(ports[n]);
        }
        return compatPorts;
    }
}
