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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;

import androidx.annotation.RequiresApi;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;

/**
 * Implementation of {@link WebMessagePortCompat}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link WebMessagePortCompat} functionality.
 */
public class WebMessagePortImpl extends WebMessagePortCompat {
    private final WebMessagePort mFrameworksImpl;
    // TODO(gsennton) add WebMessagePortBoundaryInterface variable

    public WebMessagePortImpl(WebMessagePort frameworksImpl) {
        mFrameworksImpl = frameworksImpl;
    }

    @SuppressLint("NewApi")
    @Override
    public void postMessage(WebMessageCompat message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFrameworksImpl.postMessage(compatToFrameworkMessage(message));
        } else { // TODO(gsennton) add reflection-based implementation
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFrameworksImpl.close();
        } else { // TODO(gsennton) add reflection-based implementation
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setWebMessageCallback(final WebMessageCallbackCompat callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFrameworksImpl.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                @SuppressWarnings("NewApi")
                public void onMessage(WebMessagePort port, WebMessage message) {
                    callback.onMessage(new WebMessagePortImpl(port),
                            frameworkMessageToCompat(message));
                }
            });
        } else { // TODO(gsennton) add reflection-based implementation
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setWebMessageCallback(Handler handler, final WebMessageCallbackCompat callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFrameworksImpl.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                @SuppressWarnings("NewApi")
                public void onMessage(WebMessagePort port, WebMessage message) {
                    callback.onMessage(new WebMessagePortImpl(port),
                            frameworkMessageToCompat(message));
                }
            }, handler);
        } else { // TODO(gsennton) add reflection-based implementation
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public WebMessagePort getFrameworkPort() {
        return mFrameworksImpl;
    }

    /**
     * Convert an array of {@link WebMessagePort} objects into an array containing objects of the
     * corresponding support library class {@link WebMessagePortCompat}.
     */
    public static WebMessagePortCompat[] portsToCompat(WebMessagePort[] ports) {
        if (ports == null) return null;
        WebMessagePortCompat[] compatPorts = new WebMessagePortCompat[ports.length];
        for (int n = 0; n < ports.length; n++) {
            compatPorts[n] = new WebMessagePortImpl(ports[n]);
        }
        return compatPorts;
    }

    /**
     * Convert an array of {@link WebMessagePortCompat} objects into an array containing objects of
     * the corresponding framework class {@link WebMessagePort}.
     */
    public static WebMessagePort[] compatToPorts(WebMessagePortCompat[] compatPorts) {
        if (compatPorts == null) return null;
        WebMessagePort[] ports = new WebMessagePort[compatPorts.length];
        for (int n = 0; n < ports.length; n++) {
            ports[n] = compatPorts[n].getFrameworkPort();
        }
        return ports;
    }

    /**
     * Convert a {@link WebMessageCompat} into the corresponding framework class {@link WebMessage}.
     */
    @RequiresApi(23)
    public static WebMessage compatToFrameworkMessage(WebMessageCompat message) {
        return new WebMessage(
                message.getData(),
                compatToPorts(message.getPorts()));
    }

    /**
     * Convert a {@link WebMessage} into the corresponding support library class
     * {@link WebMessageCompat}.
     */
    @RequiresApi(23)
    public static WebMessageCompat frameworkMessageToCompat(WebMessage message) {
        return new WebMessageCompat(
                message.getData(),
                portsToCompat(message.getPorts()));
    }
}
