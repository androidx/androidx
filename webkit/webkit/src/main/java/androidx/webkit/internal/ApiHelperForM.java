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

package androidx.webkit.internal;

import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewCompat;

/**
 * Utility class to use new APIs that were added in M (API level 23).
 * These need to exist in a separate class so that Android framework can successfully verify
 * classes without encountering the new APIs.
 */
@RequiresApi(Build.VERSION_CODES.M)
public class ApiHelperForM {
    private ApiHelperForM() {
    }

    /**
     * @see WebMessagePort#postMessage(WebMessage)
     */
    public static void postMessage(@NonNull WebMessagePort webMessagePort,
            @NonNull WebMessage webMessage) {
        webMessagePort.postMessage(webMessage);
    }

    /**
     * @see WebMessagePort#close()
     */
    public static void close(@NonNull WebMessagePort webMessagePort) {
        webMessagePort.close();
    }

    /**
     * Wraps the passed callback in the framework callback type to isolate new types in this class.
     * @see WebMessagePort#setWebMessageCallback(WebMessagePort.WebMessageCallback)
     */
    public static void setWebMessageCallback(@NonNull WebMessagePort frameworksImpl,
            @NonNull WebMessagePortCompat.WebMessageCallbackCompat callback) {
        frameworksImpl.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
            @Override
            public void onMessage(WebMessagePort port, WebMessage message) {
                callback.onMessage(new WebMessagePortImpl(port),
                        WebMessagePortImpl.frameworkMessageToCompat(message));
            }
        });
    }

    /**
     * Calls
     * {@link WebMessagePort#setWebMessageCallback(WebMessagePort.WebMessageCallback, Handler)}
     * Wraps the passed callback in the framework callback type to isolate new types in this class.
     */
    public static void setWebMessageCallback(@NonNull WebMessagePort frameworksImpl,
            @NonNull WebMessagePortCompat.WebMessageCallbackCompat callback,
            @Nullable Handler handler) {
        frameworksImpl.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
            @Override
            public void onMessage(WebMessagePort port, WebMessage message) {
                callback.onMessage(new WebMessagePortImpl(port),
                        WebMessagePortImpl.frameworkMessageToCompat(message));
            }
        }, handler);
    }

    /**
     * @see WebMessage#WebMessage(String, WebMessagePort[])}  WebMessage
     */
    @NonNull
    public static WebMessage createWebMessage(@NonNull WebMessageCompat message) {
        return new WebMessage(message.getData(),
                WebMessagePortImpl.compatToPorts(message.getPorts()));
    }

    /**
     * @see WebMessageCompat#WebMessageCompat(String, WebMessagePortCompat[])
     */
    @NonNull
    public static WebMessageCompat createWebMessageCompat(@NonNull WebMessage webMessage) {
        return new WebMessageCompat(webMessage.getData(),
                WebMessagePortImpl.portsToCompat(webMessage.getPorts()));
    }

    /**
     * @see WebResourceError#getErrorCode()
     */
    public static int getErrorCode(@NonNull WebResourceError webResourceError) {
        return webResourceError.getErrorCode();
    }


    /**
     * @see WebResourceError#getDescription()
     */
    @NonNull
    public static CharSequence getDescription(@NonNull WebResourceError webResourceError) {
        return webResourceError.getDescription();
    }

    /**
     * @see WebSettings#setOffscreenPreRaster(boolean)
     */
    public static void setOffscreenPreRaster(@NonNull WebSettings webSettings, boolean b) {
        webSettings.setOffscreenPreRaster(b);
    }

    /**
     * @see WebSettings#getOffscreenPreRaster()
     */
    public static boolean getOffscreenPreRaster(@NonNull WebSettings webSettings) {
        return webSettings.getOffscreenPreRaster();
    }

    /**
     * Wraps the passed callback in the framework callback type to isolate new types in this class.
     * @see WebView#postVisualStateCallback(long, WebView.VisualStateCallback)
     */
    public static void postVisualStateCallback(@NonNull WebView webView, long requestId,
            final @NonNull WebViewCompat.VisualStateCallback callback) {
        webView.postVisualStateCallback(requestId, new WebView.VisualStateCallback() {
            @Override
            public void onComplete(long l) {
                callback.onComplete(l);
            }
        });
    }

    /**
     * @see WebView#postWebMessage(WebMessage, Uri)
     */
    public static void postWebMessage(@NonNull WebView webView, @NonNull WebMessage message,
            @NonNull Uri targetOrigin) {
        webView.postWebMessage(message, targetOrigin);
    }

    /**
     * @see WebView#createWebMessageChannel()
     */
    @NonNull
    public static WebMessagePort[] createWebMessageChannel(@NonNull WebView webView) {
        return webView.createWebMessageChannel();
    }
}
