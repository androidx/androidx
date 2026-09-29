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

package androidx.javascriptengine;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.AnyThread;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * ServiceConnection for a JavaScriptSandbox.
 *
 * <p>There is a 1:1 mapping between a JavaScriptSandbox and a SandboxServiceConnection, but there
 * can be multiple SandboxServiceConnection instances using a common {@link SandboxServiceInstance}.
 *
 * <p>Unless otherwise stated as thread safe, methods should only be invoked on the associated
 * SandboxServiceInstance's executor.
 */
final class SandboxServiceConnection implements ServiceConnection {
    // This is the context for the JavaScriptSandbox, which may not necessarily be the context
    // for the SandboxServiceInstance, particularly for shared sandboxes where the application
    // context is used for the SandboxServiceInstance, but the JavaScriptSandbox might use a
    // ContextWrapper or Activity with subtly different behaviors.
    @NonNull
    private final Context mContext;
    @NonNull
    private final SandboxServiceInstance mSandboxServiceInstance;

    private CallbackToFutureAdapter.@Nullable Completer<JavaScriptSandbox> mCompleter;
    @Nullable
    private WeakReference<JavaScriptSandbox> mJsSandbox;

    /**
     * Construct a SandboxServiceConnection.
     */
    @AnyThread
    SandboxServiceConnection(@NonNull Context context,
            @NonNull SandboxServiceInstance sandboxServiceInstance,
            CallbackToFutureAdapter.@NonNull Completer<JavaScriptSandbox> completer) {
        mContext = context;
        mSandboxServiceInstance = sandboxServiceInstance;
        mCompleter = completer;
    }

    @Override
    @SuppressWarnings("NullAway")
    public void onServiceConnected(ComponentName name, IBinder service) {
        // It's possible for the service to die and already have been restarted before
        // we've actually observed the original death (b/267864650). If that happens,
        // onServiceConnected will be called a second time immediately after
        // onServiceDisconnected even though we already unbound. Just do nothing.
        if (mCompleter == null) {
            return;
        }
        IJsSandboxService jsSandboxService = IJsSandboxService.Stub.asInterface(service);
        JavaScriptSandbox jsSandbox;
        try {
            // If creation of the JavaScriptSandbox is successful, responsibility for closing the
            // SandboxServiceConnection is transferred to the JavaScriptSandbox.
            jsSandbox = new JavaScriptSandbox(mContext, this, jsSandboxService);
        } catch (RemoteException e) {
            handleServiceFailure(e);
            return;
        } catch (RuntimeException e) {
            handleServiceFailure(e);
            throw e;
        }
        setJsSandboxAndComplete(jsSandbox);
    }

    // TODO(crbug.com/1297672): We may want an explicit way to signal to the client that the
    // process crashed (like onRenderProcessGone in WebView), without them having to first call
    // one of the methods and have it fail.
    @Override
    public void onServiceDisconnected(ComponentName name) {
        handleServiceFailure(new RuntimeException(
                "JavaScriptSandbox internal error: onServiceDisconnected()"));
    }

    @Override
    public void onBindingDied(ComponentName name) {
        handleServiceFailure(new RuntimeException(
                "JavaScriptSandbox internal error: onBindingDied()"));
    }

    @Override
    public void onNullBinding(ComponentName name) {
        handleServiceFailure(new RuntimeException(
                "JavaScriptSandbox internal error: onNullBinding()"));
    }

    private void handleServiceFailure(@NonNull Exception e) {
        killService(e);
        completeWithException(e);
        // If the JavaScriptSandbox exists, it's responsible for calling close.
        if (getJsSandbox() == null) {
            close();
        }
    }

    /**
     * Sets the completer for the sandbox.
     */
    private void setJsSandboxAndComplete(@NonNull JavaScriptSandbox sandbox) {
        if (mCompleter == null) {
            throw new IllegalStateException("Completer already used");
        }
        if (mJsSandbox != null) {
            throw new IllegalStateException("mJsSandbox already set");
        }
        mJsSandbox = new WeakReference<>(sandbox);
        boolean success = mCompleter.set(sandbox);
        mCompleter = null;
        if (!success) {
            // Was probably canceled (rather than failed).
            // Closing the sandbox closes the connection.
            sandbox.close();
        }
    }

    private void completeWithException(@NonNull Exception e) {
        if (mCompleter == null) {
            return;
        }
        mCompleter.setException(e);
        mCompleter = null;
    }

    @Nullable
    private JavaScriptSandbox getJsSandbox() {
        return mJsSandbox != null ? mJsSandbox.get() : null;
    }

    /**
     * Unbinds from the service instance and triggers cleanup.
     *
     * <p>A connection should eventually be closed regardless of whether it ever became useful or a
     * JavaScriptSandbox instance created. It can safely be called multiple times.
     *
     * <p>The completer should ideally be used explicitly before being closed.
     *
     * <p>This does not close the JavaScriptSandbox. If a JavaScriptSandbox exists, it should be the
     * one to call close.
     */
    @AnyThread
    public void close() {
        mSandboxServiceInstance.closeConnection(this);
    }

    /**
     * Kills the service and unbinds.
     *
     * <p>Close must still be called at some point.
     *
     * <p>It is safe to call this method multiple times, even if the SandboxServiceConnection is
     * closed. The kill is still effective even if the SandboxServiceConnection is already closed.
     * Note that this means a closed JavaScriptSandbox for a shared service instance can still kill
     * that shared service instance, even if the JavaScriptSandbox was closed long ago.
     */
    @AnyThread
    public void killService(@NonNull Exception e) {
        mSandboxServiceInstance.kill(e);
    }

    /**
     * Cleans up after a close.
     *
     * <p>This should only be called by SandboxServiceInstance.
     */
    public void cleanup() {
        // Conditional to avoid unnecessary exception construction.
        if (mCompleter != null) {
            completeWithException(
                    new RuntimeException("Service connection cleaned up before completion."));
        }
    }

    /**
     * Notifies that the SandboxServiceInstance was killed (non-gracefully).
     *
     * <p>This should only be called by SandboxServiceInstance.
     */
    public void onKilled(@NonNull Exception e) {
        JavaScriptSandbox jsSandbox = getJsSandbox();
        if (jsSandbox != null) {
            jsSandbox.onServiceKilled();
        }
        completeWithException(e);
    }

    @NonNull
    public SandboxServiceInstance getSandboxServiceInstance() {
        return mSandboxServiceInstance;
    }
}
