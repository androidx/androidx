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

package androidx.webkit;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * The Java representation of the HTML5 PostMessage event. See
 * https://html.spec.whatwg.org/multipage/comms.html#the-messageevent-interfaces
 * for definition of a MessageEvent in HTML5.
 */
public class WebMessageCompat {

    /**
     * Indicates the payload of WebMessageCompat is String.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int TYPE_STRING = 0;
    /**
     * Indicates the payload of WebMessageCompat is JavaScript ArrayBuffer.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int TYPE_ARRAY_BUFFER = 1;
    private final @Nullable WebMessagePortCompat[] mPorts;
    private final @Nullable String mString;
    private final @Nullable byte[] mArrayBuffer;
    private final @Type int mType;

    /**
     * Creates a WebMessage with String payload.
     *
     * @param data the string of the message.
     */
    public WebMessageCompat(@Nullable String data) {
        this(data, null);
    }

    /**
     * Creates a WebMessage with String payload.
     *
     * @param data  the string data of the message.
     * @param ports the ports that are sent with the message.
     */
    public WebMessageCompat(@Nullable String data, @Nullable WebMessagePortCompat[] ports) {
        mString = data;
        mArrayBuffer = null;
        mPorts = ports;
        mType = TYPE_STRING;
    }

    /**
     * Creates a WebMessage with JavaScript ArrayBuffer payload.
     *
     * @param arrayBuffer the array buffer data of the message.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public WebMessageCompat(@NonNull byte[] arrayBuffer) {
        this(arrayBuffer, null);
    }

    /**
     * Creates a WebMessage with JavaScript ArrayBuffer payload.
     *
     * @param arrayBuffer the array buffer data of the message.
     * @param ports       the ports that are sent with the message.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public WebMessageCompat(@NonNull byte[] arrayBuffer,
            @Nullable WebMessagePortCompat[] ports) {
        Objects.requireNonNull(arrayBuffer);
        mArrayBuffer = arrayBuffer;
        mString = null;
        mPorts = ports;
        mType = TYPE_ARRAY_BUFFER;
    }

    /**
     * Returns the payload type of the message.
     * @return the payload type of WebMessageCompat.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Type int getType() {
        return mType;
    }

    /**
     * Returns the ArrayBuffer data of message. A ArrayBuffer or Transferable ArrayBuffer can be
     * received from JavaScript.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Nullable byte[] getArrayBuffer() {
        return mArrayBuffer;
    }

    /**
     * Returns the String data of the message.
     */
    public @Nullable String getData() {
        return mString;
    }

    /**
     * Returns the ports that are sent with the message, or {@code null} if no port
     * is sent.
     */
    @Nullable
    public WebMessagePortCompat[] getPorts() {
        return mPorts;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({TYPE_STRING, TYPE_ARRAY_BUFFER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}
}
