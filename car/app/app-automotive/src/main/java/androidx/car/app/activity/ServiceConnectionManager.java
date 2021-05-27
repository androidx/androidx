/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererService;

import java.util.List;

/**
 * Manages the renderer service connection state.
 *
 * This class handles binding and unbinding to the renderer service and make sure the renderer
 * service gets initialized and terminated properly.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ServiceConnectionManager {
    @SuppressLint({"ActionValue"})
    @VisibleForTesting
    static final String ACTION_RENDER = "android.car.template.host.RendererService";

    final ErrorHandler mErrorHandler;
    private final ComponentName mServiceComponentName;
    private final Context mContext;
    private final ServiceDispatcher mServiceDispatcher;
    private int mDisplayId;
    @Nullable private Intent mIntent;
    @Nullable private ICarAppActivity mICarAppActivity;

    @Nullable IRendererService mRendererService;

    public ServiceConnectionManager(@NonNull Context context,
            @NonNull ComponentName serviceComponentName, @NonNull ErrorHandler errorHandler) {
        mContext = context;
        mErrorHandler = errorHandler;
        mServiceComponentName = serviceComponentName;
        mServiceDispatcher = new ServiceDispatcher(mErrorHandler, this::isBound);
    }

    /**
     * Returns a {@link ServiceDispatcher} that can be used to communicate with the renderer
     * service.
     */
    @NonNull ServiceDispatcher getServiceDispatcher() {
        return mServiceDispatcher;
    }

    @VisibleForTesting
    ComponentName getServiceComponentName() {
        return mServiceComponentName;
    }

    @VisibleForTesting
    ErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    @VisibleForTesting
    ServiceConnection getServiceConnection() {
        return mServiceConnectionImpl;
    }

    @VisibleForTesting
    void setServiceConnection(ServiceConnection serviceConnection) {
        mServiceConnectionImpl = serviceConnection;
    }

    @VisibleForTesting
    void setRendererService(@Nullable IRendererService rendererService) {
        mRendererService = rendererService;
    }

    /** Returns true if the service is currently bound and able to receive messages */
    boolean isBound() {
        return mRendererService != null;
    }

    /** The service connection for the renderer service. */
    private ServiceConnection mServiceConnectionImpl =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(
                        @NonNull ComponentName name, @NonNull IBinder service) {
                    requireNonNull(name);
                    requireNonNull(service);
                    Log.i(LogTags.TAG, String.format("Host service %s is connected",
                            name.flattenToShortString()));
                    IRendererService rendererService = IRendererService.Stub.asInterface(service);
                    if (rendererService == null) {
                        mErrorHandler.onError(ErrorHandler.ErrorType.HOST_INCOMPATIBLE,
                                new Exception("Failed to get IRenderService binder from host: "
                                        + name));
                        return;
                    }

                    mRendererService = rendererService;
                    initializeService();
                }

                @Override
                public void onServiceDisconnected(@NonNull ComponentName name) {
                    requireNonNull(name);

                    // Connection lost, but it might reconnect.
                    Log.w(LogTags.TAG, String.format("Host service %s is disconnected",
                            name.flattenToShortString()));
                }

                @Override
                public void onBindingDied(@NonNull ComponentName name) {
                    requireNonNull(name);

                    // Connection permanently lost
                    mErrorHandler.onError(ErrorHandler.ErrorType.HOST_CONNECTION_LOST,
                            new Exception("Host service " + name + " is permanently disconnected"));
                }

                @Override
                public void onNullBinding(@NonNull ComponentName name) {
                    requireNonNull(name);

                    // Host rejected the binding.
                    mErrorHandler.onError(ErrorHandler.ErrorType.HOST_INCOMPATIBLE,
                            new Exception("Host service " + name + " rejected the binding "
                                    + "request"));
                }
            };

    /**
     * Binds to the renderer service and initializes the service if not bound already.
     *
     * Initializes the renderer service with given properties if already bound to the renderer
     * service.
     */
    void bind(@NonNull Intent intent, @NonNull ICarAppActivity iCarAppActivity, int displayId) {
        mIntent = requireNonNull(intent);
        mICarAppActivity = requireNonNull(iCarAppActivity);
        mDisplayId = displayId;

        if (isBound()) {
            initializeService();
            return;
        }

        Intent rendererIntent = new Intent(ACTION_RENDER);
        List<ResolveInfo> resolveInfoList =
                mContext.getPackageManager()
                        .queryIntentServices(rendererIntent, PackageManager.GET_META_DATA);
        if (resolveInfoList.size() == 1) {
            rendererIntent.setPackage(resolveInfoList.get(0).serviceInfo.packageName);
            if (!mContext.bindService(
                    rendererIntent,
                    mServiceConnectionImpl,
                    Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES)) {
                mErrorHandler.onError(ErrorHandler.ErrorType.HOST_INCOMPATIBLE,
                        new Exception("Cannot bind to the renderer host with intent: "
                                + rendererIntent));
            }
        } else if (resolveInfoList.isEmpty()) {
            mErrorHandler.onError(ErrorHandler.ErrorType.HOST_NOT_FOUND, new Exception("No "
                    + "handlers found for intent: " + rendererIntent));
        } else {
            StringBuilder logMessage =
                    new StringBuilder("Multiple hosts found, only one is allowed");
            for (ResolveInfo resolveInfo : resolveInfoList) {
                logMessage.append(
                        String.format("\nFound host %s", resolveInfo.serviceInfo.packageName));
            }
            mErrorHandler.onError(ErrorHandler.ErrorType.MULTIPLE_HOSTS,
                    new Exception(logMessage.toString()));
        }
    }

    /** Closes the connection to the connected {@code rendererService} if any. */
    void unbind() {
        if (mRendererService == null) {
            return;
        }
        try {
            mRendererService.terminate(requireNonNull(mServiceComponentName));
        } catch (RemoteException e) {
            // We are already unbinding (maybe because the host has already cut the connection)
            // Let's not log more errors unnecessarily.
        }

        Log.i(LogTags.TAG, "Unbinding from " + mServiceComponentName);
        mContext.unbindService(mServiceConnectionImpl);
        mRendererService = null;
    }

    /**
     * Initializes the {@code rendererService} for the current {@code carIAppActivity},
     * {@code serviceComponentName} and {@code displayId}.
     */
    void initializeService() {
        ICarAppActivity carAppActivity = requireNonNull(mICarAppActivity);
        IRendererService rendererService = requireNonNull(mRendererService);
        ComponentName serviceComponentName = requireNonNull(mServiceComponentName);

        Boolean success = mServiceDispatcher.fetch(false,
                () -> rendererService.initialize(carAppActivity,
                        serviceComponentName, mDisplayId));
        if (success == null || !success) {
            mErrorHandler.onError(ErrorHandler.ErrorType.HOST_ERROR,
                    new Exception("Cannot create renderer for" + serviceComponentName));
            return;
        }
        updateIntent();
    }

    /**
     * Updates the activity intent for the connected {@code rendererService}.
     */
    private void updateIntent() {
        ComponentName serviceComponentName = requireNonNull(mServiceComponentName);
        Intent intent = requireNonNull(mIntent);

        IRendererService service = mRendererService;
        if (service == null) {
            mErrorHandler.onError(ErrorHandler.ErrorType.CLIENT_SIDE_ERROR,
                    new Exception("Service dispatcher is not connected"));
            return;
        }

        Boolean success = mServiceDispatcher.fetch(false, () ->
                service.onNewIntent(intent, serviceComponentName, mDisplayId));
        if (success == null || !success) {
            mErrorHandler.onError(ErrorHandler.ErrorType.HOST_ERROR, new Exception("Renderer "
                    + "cannot handle the intent: " + intent));
        }
    }
}
