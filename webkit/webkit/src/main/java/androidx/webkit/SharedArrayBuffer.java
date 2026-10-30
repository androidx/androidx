/*
 * Copyright 2026 The Android Open Source Project
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

import android.system.ErrnoException;

import androidx.annotation.AnyThread;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

import org.chromium.support_lib_boundary.SharedArrayBufferBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * The Java representation of a JavaScript
 * <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/SharedArrayBuffer">SharedArrayBuffer</a>.
 * <p>
 * A {@link SharedArrayBuffer} is a region of memory that is shared between the application and
 * the web content running in WebView, which means that writes performed on one side are visible
 * on the other side without copying the data or posting another message.
 * <p>
 * Use {@link #allocate(String, long)} to allocate a new buffer, and send it to web content by
 * posting a {@link WebMessageCompat} carrying it, or by replying with
 * {@link JavaScriptReplyProxy#postMessage(SharedArrayBuffer)}. Web content that is not <a
 * href="https://developer.mozilla.org/en-US/docs/Web/API/Window/crossOriginIsolated">cross-origin
 * isolated</a> cannot use a {@code SharedArrayBuffer}; see
 * {@link Profile#setCrossOriginIsolatedAllowlist(java.util.Set)}.
 * <p>
 * Shared memory provides no notification when its contents change. An application that needs to
 * observe writes made by web content has to read the buffer again, or has to be told by web
 * content through a separate message.
 * <p>
 * Any web content that holds this buffer can write arbitrary data into it at any time. Treat the
 * contents as untrusted input.
 *
 * @see android.os.SharedMemory
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SharedArrayBuffer {
    private final @NonNull SharedArrayBufferBoundaryInterface mBoundaryInterface;

    private SharedArrayBuffer(@NonNull SharedArrayBufferBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Allocates a new {@link SharedArrayBuffer} of {@code size} bytes.
     * <p>
     * The memory is allocated as an {@link android.os.SharedMemory} region in the application's
     * process, and is accounted against the application.
     *
     * @param name an optional name for the underlying shared memory region, which is only used
     *             for debugging purposes and does not need to be unique. See
     *             {@link android.os.SharedMemory#create(String, int)}.
     * @param size the size of the buffer in bytes.
     * @return the newly allocated {@link SharedArrayBuffer}.
     * @throws ErrnoException if the shared memory region could not be allocated.
     * @throws IllegalArgumentException if {@code size} is outside of the supported range.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#WEB_MESSAGE_SHARED_ARRAY_BUFFER} feature is not supported.
     *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
     */
    @AnyThread
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_SHARED_ARRAY_BUFFER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @NonNull SharedArrayBuffer allocate(@Nullable String name,
            @IntRange(from = 1, to = Integer.MAX_VALUE) long size) throws ErrnoException {
        if (size < 1 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("SharedArrayBuffer size must be in the range [1, "
                    + Integer.MAX_VALUE + "] bytes, but was " + size + ".");
        }
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.WEB_MESSAGE_SHARED_ARRAY_BUFFER;
        if (feature.isSupportedByWebView()) {
            return fromInvocationHandler(WebViewGlueCommunicator.getFactory().getStatics()
                    .createSharedArrayBuffer(name, size));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns a direct {@link ByteBuffer} that gives access to the shared memory region.
     * <p>
     * The region is mapped into the application's address space the first time this method is
     * called. Every call returns a new {@link ByteBuffer} over that same mapping, with its own
     * independent position, limit and mark. All of them, as well as the
     * {@code SharedArrayBuffer} held by web content, observe each other's writes.
     * <p>
     * The returned buffer has a capacity of {@link #getSize()} bytes, and its byte order is
     * {@link ByteOrder#nativeOrder()}, which matches the byte order of JavaScript typed arrays
     * such as {@code Int32Array}. Calling {@link ByteBuffer#duplicate()} or
     * {@link ByteBuffer#slice()} on the returned buffer resets the byte order to
     * {@link ByteOrder#BIG_ENDIAN}.
     *
     * @return a direct {@link ByteBuffer} backed by the shared memory region.
     * @throws ErrnoException if the shared memory region could not be mapped.
     */
    public @NonNull ByteBuffer mapAndCreateByteBuffer() throws ErrnoException {
        return mBoundaryInterface.mapAndCreateByteBuffer();
    }

    /**
     * Returns the size of the shared memory region in bytes.
     */
    public long getSize() {
        return mBoundaryInterface.getSize();
    }

    /**
     * Wraps a {@link SharedArrayBuffer} boundary object handed out by WebView.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull SharedArrayBuffer fromInvocationHandler(
            /* SharedArrayBuffer */ @NonNull InvocationHandler invocationHandler) {
        return new SharedArrayBuffer(Objects.requireNonNull(
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        SharedArrayBufferBoundaryInterface.class, invocationHandler)));
    }

    /**
     * Returns the boundary object this instance wraps.
     * <p>
     * WebView resolves a {@link SharedArrayBuffer} boundary object back to the buffer it
     * allocated, so the very same {@link InvocationHandler} that WebView handed out has to be
     * passed back when the buffer is sent in a message.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public /* SharedArrayBuffer */ @NonNull InvocationHandler getInvocationHandler() {
        return Proxy.getInvocationHandler(mBoundaryInterface);
    }
}
