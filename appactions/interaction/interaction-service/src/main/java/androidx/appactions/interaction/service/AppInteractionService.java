/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.ActionCapability;
import androidx.appactions.interaction.service.proto.AppActionsServiceGrpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.grpc.Server;
import io.grpc.binder.AndroidComponentAddress;
import io.grpc.binder.BinderServerBuilder;
import io.grpc.binder.IBinderReceiver;
import io.grpc.binder.SecurityPolicies;
import io.grpc.binder.SecurityPolicy;
import io.grpc.binder.ServerSecurityPolicy;

/**
 * Base service class for the AppInteractionService SDK. This sets up the GRPC on-device server for
 * communication with Assistant.
 */
// TODO(b/267772921): Rewrite public facing class to Kotlin
public abstract class AppInteractionService extends Service {

    private static final String TAG = "AppInteractionService";
    private ServerLifecycle mBinderSupplier;

    public AppInteractionService() {}

    /**
     * Called by the system once after the Assistant binds to the service.
     *
     * @return the list of capabilities that this service supports.
     */
    @NonNull
    protected abstract List<ActionCapability> registerCapabilities();

    /**
     * Sets a custom {@link SecurityPolicy} for the gRPC service. This gives control over which
     * clients are allowed to bind to your service.
     *
     * <p>Overriding this method is <b>not</b> the preferred method for security enforcement. We
     * recommend developers override {@link #getAllowedApps()} for security needs. Implementing your
     * own security policy requires significant care, and an understanding of the details and
     * pitfalls of Android security. If you choose to do so, we <b>strongly</b> recommend you get
     * such a change reviewed by Android security experts.
     */
    @NonNull
    protected SecurityPolicy getSecurityPolicy() {
        return SecurityPolicies.anyOf(
                getSecurityPolicyFromAllowedList(getAllowedApps()).toArray(new SecurityPolicy[0]));
    }

    /**
     * Returns a list of the apps {@link AppVerificationInfo} that are allowed to interact with the
     * app's bound service. This gives control over which clients are allowed to communicate with
     * the service.
     *
     * <p>This is the default method for enforcing security and must be overridden. Developers
     * should return an empty list should they choose to define their own security by way of
     * overriding {@link #getSecurityPolicyFromAllowedList}.
     */
    @NonNull
    protected abstract List<AppVerificationInfo> getAllowedApps();

    /**
     * Sets a custom {@link SecurityPolicy} for the gRPC service given the client's allowed pairs of
     * package names with corresponding Sha256 signatures. This gives control over which clients are
     * allowed to bind to your service.
     *
     * <p>A SecurityPolicy is returned per supported Assistant. Such as "Google Assistant", "Bixby",
     * etc.
     */
    @NonNull
    private List<SecurityPolicy> getSecurityPolicyFromAllowedList(
            List<AppVerificationInfo> verificationInfoList) {

        List<SecurityPolicy> policies = new ArrayList<>();
        if (verificationInfoList == null || verificationInfoList.isEmpty()) {
            policies.add(SecurityPolicies.internalOnly());
            return policies;
        }
        for (AppVerificationInfo verificationInfo : verificationInfoList) {
            policies.add(
                    SecurityPolicies.oneOfSignatureSha256Hash(
                            this.getPackageManager(),
                            verificationInfo.getPackageName(),
                            verificationInfo.getSignatures()));
        }
        return policies;
    }

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        IBinderReceiver binderReceiver = new IBinderReceiver();
        ServerSecurityPolicy serverSecurityPolicy =
                ServerSecurityPolicy.newBuilder()
                        .servicePolicy(AppActionsServiceGrpc.SERVICE_NAME, getSecurityPolicy())
                        .build();
        Server server =
                BinderServerBuilder.forAddress(
                                AndroidComponentAddress.forContext(this), binderReceiver)
                        .securityPolicy(serverSecurityPolicy)
                        .intercept(new RemoteViewsOverMetadataInterceptor())
                        .addService(AppInteractionServiceFactory.create(this))
                        .build();

        mBinderSupplier = new ServerLifecycle(server, binderReceiver);
    }

    @Override
    @NonNull
    public IBinder onBind(@Nullable Intent intent) {
        return mBinderSupplier.get();
    }

    @Override
    @CallSuper
    public void onDestroy() {
        if (mBinderSupplier != null) {
            mBinderSupplier.shutdown();
        }
        super.onDestroy();
    }

    static final class ServerLifecycle {
        private final Server mServer;
        private final IBinderReceiver mReceiver;
        private boolean mStarted;

        ServerLifecycle(Server server, IBinderReceiver receiver) {
            this.mServer = server;
            this.mReceiver = receiver;
        }

        public IBinder get() {
            synchronized (this) {
                if (!mStarted) {
                    try {
                        mStarted = true;
                        mServer.start();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Unable to start server " + mServer, ioe);
                    }
                }
                return mReceiver.get();
            }
        }

        public void shutdown() {
            if (mStarted) {
                mServer.shutdownNow();
                mStarted = false;
            }
        }
    }
}
