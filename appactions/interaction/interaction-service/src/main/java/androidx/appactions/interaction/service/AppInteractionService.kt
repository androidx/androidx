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

package androidx.appactions.interaction.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.CallSuper
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.entity.EntityProvider
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc
import io.grpc.Server
import io.grpc.binder.AndroidComponentAddress
import io.grpc.binder.BinderServerBuilder
import io.grpc.binder.IBinderReceiver
import io.grpc.binder.SecurityPolicies
import io.grpc.binder.SecurityPolicy
import io.grpc.binder.ServerSecurityPolicy
import java.io.IOException

/**
 * Base service class for the AppInteractionService SDK. This sets up the GRPC on-device server for
 * communication with Assistant.
 */
abstract class AppInteractionService : Service() {
    private var binderSupplier: ServerLifecycle? = null

    /**
     * Called by the system once after the Assistant binds to the service.
     *
     * @return the list of capabilities that this service supports.
     */
    abstract val registeredCapabilities: List<Capability>

    /**
     * Called by the system once after the Assistant binds to the service.
     *
     * @return the list of EntityProvider that this service supports.
     */
    // TODO(b/284057017): Open up Grounding API (remove internal).
    internal open val registeredEntityProviders: List<EntityProvider<*>> = listOf()

    /**
     * A list of [AppVerificationInfo] which define who is allowed to interact with the app's bound
     * service. This gives control over which clients are allowed to communicate with the service.
     *
     * This is the default method for enforcing security and must be overridden. Developers should
     * return an empty list should they choose to define their own security by way of overriding
     * [.getSecurityPolicyFromAllowedList].
     */
    protected abstract val allowedApps: List<AppVerificationInfo>

    /**
     * Sets a custom [SecurityPolicy] for the gRPC service. This gives control over which clients
     * are allowed to bind to your service.
     *
     * Overriding this property is **not** the preferred method for security enforcement. We
     * recommend developers override [allowedApps] for security needs. Implementing your own
     * security policy requires significant care, and an understanding of the details and pitfalls
     * of Android security. If you choose to do so, we **strongly** recommend you get such a change
     * reviewed by Android security experts.
     */
    protected open val securityPolicy: SecurityPolicy
        get() =
            SecurityPolicies.anyOf(*getSecurityPolicyFromAllowedList(allowedApps).toTypedArray())

    /**
     * Sets a custom [SecurityPolicy] for the gRPC service given the client's allowed pairs of
     * package names with corresponding Sha256 signatures. This gives control over which clients are
     * allowed to bind to your service.
     *
     * A SecurityPolicy is returned per supported Assistant. Such as "Google Assistant", "Bixby",
     * etc.
     */
    private fun getSecurityPolicyFromAllowedList(
        verificationInfoList: List<AppVerificationInfo>
    ): List<SecurityPolicy> {
        val policies: MutableList<SecurityPolicy> = ArrayList()
        if (verificationInfoList.isEmpty()) {
            policies.add(SecurityPolicies.internalOnly())
            return policies
        }
        for (verificationInfo in verificationInfoList) {
            policies.add(
                SecurityPolicies.oneOfSignatureSha256Hash(
                    this.packageManager,
                    verificationInfo.packageName,
                    verificationInfo.signatures
                )
            )
        }
        return policies
    }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        val binderReceiver = IBinderReceiver()
        val serverSecurityPolicy =
            ServerSecurityPolicy.newBuilder()
                .servicePolicy(AppInteractionServiceGrpc.SERVICE_NAME, securityPolicy)
                .build()
        val server =
            BinderServerBuilder.forAddress(AndroidComponentAddress.forContext(this), binderReceiver)
                .securityPolicy(serverSecurityPolicy)
                .intercept(RemoteViewsOverMetadataInterceptor())
                .addService(AppInteractionServiceFactory.create(this))
                .build()
        binderSupplier = ServerLifecycle(server, binderReceiver)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binderSupplier!!.get()!!
    }

    @CallSuper
    override fun onDestroy() {
        if (binderSupplier != null) {
            binderSupplier!!.shutdown()
        }
        super.onDestroy()
    }

    internal class ServerLifecycle(
        private val server: Server,
        private val receiver: IBinderReceiver
    ) {
        private var isServerStarted = false

        fun get(): IBinder? {
            synchronized(this) {
                if (!isServerStarted) {
                    try {
                        isServerStarted = true
                        server.start()
                    } catch (ioe: IOException) {
                        Log.e(TAG, "Unable to start server $server", ioe)
                    }
                }
                return receiver.get()
            }
        }

        fun shutdown() {
            synchronized(this) {
                if (isServerStarted) {
                    server.shutdownNow()
                    isServerStarted = false
                }
            }
        }
    }

    internal companion object {
        private const val TAG = "AppInteractionService"
    }
}
