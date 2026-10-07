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

package androidx.credentials.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CancellationSignal
import android.os.IBinder
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.exceptions.CreateCredentialException

/**
 * A base service for relying party applications to receive batch passkey creation events from
 * Credential Manager.
 *
 * This service is extended by *relying party* applications that want their users to be offered a
 * passkey during a batch passkey creation flow.
 *
 * This service is bound only for the duration of a single event, and calls to it are stateless. If
 * an implementation needs to maintain state between calls it must do so itself; the hosting process
 * may be terminated by the Android System while unbound.
 *
 * ## Service Registration
 *
 * To receive these events, a relying party must declare a subclass of this service in its manifest
 * with an intent filter for [ACTION_RELYING_PARTY_EVENTS_SERVICE]:
 * ```xml
 * <service
 *     android:name=".MyRelyingPartyEventsService"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="androidx.credentials.service.action.RELYING_PARTY_EVENTS_SERVICE" />
 *     </intent-filter>
 * </service>
 * ```
 *
 * Caller identity verification is enforced by the binder stub returned from [onBind] on every
 * incoming call, and callers that fail verification receive an error.
 *
 * ## Event Sequence
 *
 * The three callbacks correspond to the three points at which Credential Manager needs to talk to
 * the relying party during a batch creation flow:
 * 1. [onCheckPublicKeyCredentialCreationEligibility] determines whether this app should be offered
 *    to the user at all.
 * 2. [onGetPublicKeyCredentialCreationRequest] supplies the WebAuthn passkey registration options
 *    once the user has opted in.
 * 3. [onPublicKeyCredentialCreated] delivers the resulting attestation or creation failure so the
 *    relying party can register the credential with its backend or handle the error.
 *
 * All three callbacks are invoked on the main thread, so implementations must offload any network
 * or disk I/O to a background thread or coroutine. The [OutcomeReceiverCompat] `callback` passed to
 * each method is thread-safe and may be completed from any thread. Each callback should be
 * completed exactly once; only the first completion is delivered and later ones are ignored. The
 * `cancellationSignal` passed to each callback is cancelled if the caller goes away before the
 * callback is completed, in which case the result can no longer be delivered.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RelyingPartyEventsService : Service() {

    final override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action != ACTION_RELYING_PARTY_EVENTS_SERVICE) {
            return null
        }
        val classNames = getAllowedProvidersFromManifest()
        if (classNames.isEmpty()) {
            return null
        }
        val provider = instantiateStubProvider(classNames) ?: return null
        return try {
            provider.getStubImplementation(this)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to get stub implementation for RelyingPartyEventsService", e)
            null
        }
    }

    private fun instantiateStubProvider(classNames: List<String>): RelyingPartyEventsStubProvider? {
        var provider: RelyingPartyEventsStubProvider? = null
        val loader = javaClass.classLoader ?: ClassLoader.getSystemClassLoader()
        for (className in classNames) {
            try {
                val klass = Class.forName(className, /* initialize= */ false, loader)
                if (!RelyingPartyEventsStubProvider::class.java.isAssignableFrom(klass)) {
                    Log.e(TAG, "$className does not implement RelyingPartyEventsStubProvider")
                    continue
                }
                val candidate =
                    klass.getConstructor().newInstance() as RelyingPartyEventsStubProvider
                if (provider != null) {
                    Log.i(TAG, "Only one RelyingPartyEventsStubProvider allowed")
                    return null
                }
                provider = candidate
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to instantiate RelyingPartyEventsStubProvider $className", e)
            }
        }
        return provider
    }

    @Suppress("deprecation")
    @VisibleForTesting
    internal open fun getAllowedProvidersFromManifest(): List<String> {
        return try {
            val packageManager = packageManager ?: return emptyList()
            val packageInfo =
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_META_DATA or PackageManager.GET_SERVICES,
                )
            val classNames = mutableListOf<String>()
            val services = packageInfo.services
            if (services != null) {
                for (serviceInfo in services) {
                    val className =
                        serviceInfo.metaData?.getString(
                            RelyingPartyEventsStubProvider.RELYING_PARTY_EVENTS_SERVICE_PROVIDER_KEY
                        )
                    if (className != null) {
                        classNames.add(className)
                    }
                }
            }
            classNames.toList()
        } catch (e: Throwable) {
            emptyList()
        }
    }

    /**
     * Called on the main thread to determine whether this relying party has an account that is
     * ready for passkey creation: the user is signed in and in good standing, so the relying party
     * needs no further input from the user and can hand passkey creation off to Credential Manager.
     * Only eligible relying parties are offered to the user in the batch creation flow.
     *
     * Implementations should offload any network or disk I/O to a background thread and complete
     * [callback] (which is thread-safe and may be called from any thread) with an eligibility
     * response, or with a [CreateCredentialException] if eligibility could not be determined. Not
     * completing the callback will leave the caller waiting until the binding times out.
     *
     * @param request the eligibility query
     * @param cancellationSignal a signal that is cancelled if the caller goes away before
     *   [callback] is completed
     * @param callback the thread-safe callback to receive the eligibility result
     */
    @MainThread
    public abstract fun onCheckPublicKeyCredentialCreationEligibility(
        request: CheckPublicKeyCredentialCreationEligibilityRequest,
        cancellationSignal: CancellationSignal,
        callback:
            OutcomeReceiverCompat<
                CheckPublicKeyCredentialCreationEligibilityResponse,
                CreateCredentialException,
            >,
    )

    /**
     * Called on the main thread to obtain the WebAuthn passkey registration options (challenge,
     * rpId, user entity) that the credential provider should use to create the passkey.
     *
     * The credential provider identifies this relying party by its package name and signing
     * certificates, as verified by the system. Any
     * [origin][androidx.credentials.CreatePublicKeyCredentialRequest.origin] set on the returned
     * request is ignored, so the provider binds the passkey to this app rather than to a web
     * origin. Any
     * [client data hash][androidx.credentials.CreatePublicKeyCredentialRequest.clientDataHash] is
     * ignored too, since it is only meaningful together with an origin.
     *
     * Implementations must offload any network or disk I/O (such as fetching a fresh challenge from
     * the relying party backend) to a background thread or coroutine and complete [callback], which
     * is thread-safe and may be invoked from any thread.
     *
     * @param request the creation options query
     * @param cancellationSignal a signal that is cancelled if the caller goes away before
     *   [callback] is completed
     * @param callback the thread-safe callback to receive the creation request
     */
    @MainThread
    public abstract fun onGetPublicKeyCredentialCreationRequest(
        request: GetPublicKeyCredentialCreationRequest,
        cancellationSignal: CancellationSignal,
        callback:
            OutcomeReceiverCompat<
                GetPublicKeyCredentialCreationResponse,
                CreateCredentialException,
            >,
    )

    /**
     * Called on the main thread after the credential provider has attempted to create the passkey,
     * delivering either the resulting WebAuthn attestation
     * ([PublicKeyCredentialCreatedRequest.response]) so the relying party can register it with its
     * backend, or the creation failure ([PublicKeyCredentialCreatedRequest.exception]).
     *
     * When [PublicKeyCredentialCreatedRequest.response] is non-null, the relying party should
     * verify and register the attestation with its backend on a background thread, then complete
     * [callback] with `callback.onResult(PublicKeyCredentialCreatedResponse())` (or
     * `callback.onError(...)` if backend registration fails). When
     * [PublicKeyCredentialCreatedRequest.exception] is non-null, the relying party should call
     * `callback.onResult(PublicKeyCredentialCreatedResponse())` once it has handled or recorded the
     * creation failure. [callback] is thread-safe and may be completed from any thread.
     *
     * **Security note**: The relying party backend MUST independently verify the WebAuthn
     * challenge, origin, and user entity before persisting the credential; it must not trust the
     * attestation based solely on the invocation of this callback.
     *
     * @param request the request carrying either the created credential's attestation response or
     *   the creation failure exception
     * @param cancellationSignal a signal that is cancelled if the caller goes away before
     *   [callback] is completed
     * @param callback the thread-safe callback to acknowledge handling of the result or report a
     *   backend registration failure
     */
    @MainThread
    public abstract fun onPublicKeyCredentialCreated(
        request: PublicKeyCredentialCreatedRequest,
        cancellationSignal: CancellationSignal,
        callback:
            OutcomeReceiverCompat<
                PublicKeyCredentialCreatedResponse,
                CreateCredentialException,
            >,
    )

    public companion object {
        /**
         * The intent action relying party services must declare in their manifest to receive batch
         * passkey creation events.
         */
        public const val ACTION_RELYING_PARTY_EVENTS_SERVICE: String =
            "androidx.credentials.service.action.RELYING_PARTY_EVENTS_SERVICE"

        private const val TAG = "RelyingPartyEvents"
    }
}
