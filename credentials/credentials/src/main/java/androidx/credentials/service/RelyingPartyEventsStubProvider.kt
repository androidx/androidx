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

import android.os.IBinder
import androidx.annotation.RestrictTo

/**
 * Supplies the binder returned from [RelyingPartyEventsService.onBind].
 *
 * Implemented by feature-provider bridge libraries (for example the Play services bridge in
 * `:credentials:credentials-play-services-auth`), which cannot be referenced directly from this
 * module. [RelyingPartyEventsService] loads the class named by the
 * [RELYING_PARTY_EVENTS_SERVICE_PROVIDER_KEY] `<meta-data>` of a `<service>` in the relying party's
 * merged manifest (never a class named by the caller), and only instantiates it if it implements
 * this interface.
 *
 * Implementations must have a public no-argument constructor, and are responsible for verifying the
 * identity of the caller on every call into the returned binder. They must reply to a caller that
 * fails verification with an error, reply to every verified call at most once, and must not forward
 * an origin or caller identity supplied by the relying party.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RelyingPartyEventsStubProvider {
    /**
     * Returns the binder that will serve [service], or null if this provider cannot serve it.
     *
     * @param service the relying party service being bound
     */
    public fun getStubImplementation(service: RelyingPartyEventsService): IBinder?

    public companion object {
        /**
         * Metadata key naming the [RelyingPartyEventsStubProvider] implementation that serves
         * [RelyingPartyEventsService].
         *
         * Read from `<meta-data>` on a `<service>` declared in the feature-provider bridge
         * library's manifest (merged into the relying party's package). The named class must
         * implement [RelyingPartyEventsStubProvider].
         */
        public const val RELYING_PARTY_EVENTS_SERVICE_PROVIDER_KEY: String =
            "androidx.credentials.service.RELYING_PARTY_EVENTS_SERVICE_PROVIDER_KEY"
    }
}
