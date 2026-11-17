/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people.playservices

import android.app.PendingIntent
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.people.peopleprovider.CreateInteractionPendingIntentRequest
import com.google.android.gms.people.peopleprovider.CreateInteractionPendingIntentResponse
import com.google.android.gms.people.peopleprovider.GetDirectoriesRequest
import com.google.android.gms.people.peopleprovider.GetDirectoriesResponse
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedRequest
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedResponse
import com.google.android.gms.people.peopleprovider.PeopleProviderCompatClient
import com.google.android.gms.people.peopleprovider.PeopleProviderCompatClient.OnDataChangedListener
import com.google.android.gms.people.peopleprovider.PeopleProviderDirectory
import com.google.android.gms.people.peopleprovider.PeopleProviderGroup
import com.google.android.gms.people.peopleprovider.PeopleProviderPerson
import com.google.android.gms.people.peopleprovider.QueryGroupsBufferResponse
import com.google.android.gms.people.peopleprovider.QueryGroupsRequest
import com.google.android.gms.people.peopleprovider.QueryPersonsBufferResponse
import com.google.android.gms.people.peopleprovider.QueryPersonsRequest
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks

/**
 * [PeopleProviderCompatClient] that records its calls and returns the results it was given,
 * standing in for Google Play services.
 *
 * Results are mutable so that a test can change them between a change notification and the re-query
 * it triggers.
 */
internal class FakePeopleProviderCompatClient(
    /** The backend type that [isPeopleProviderSupported] reports. */
    var supportedBackendType: Int = IsPeopleProviderSupportedResponse.BACKEND_GMS_CORE,
    var supportCheckFailure: Exception? = null,
    var directories: List<PeopleProviderDirectory> = emptyList(),
    var persons: List<PeopleProviderPerson> = emptyList(),
    var groups: List<PeopleProviderGroup> = emptyList(),
    /** The People Directory applications that queries report as unreachable. */
    var unreachablePackages: List<String> = emptyList(),
    var interactionPendingIntent: PendingIntent? = null,
    /** When set, every call except [isPeopleProviderSupported] fails with it. */
    var failure: Exception? = null,
    /**
     * When set, [queryPersons] and [observePersons] return tasks that complete only on
     * [completeHeldCalls], so that a test can cancel the caller while a result is in flight.
     */
    var holdCalls: Boolean = false,
) : PeopleProviderCompatClient {

    private val heldCalls = mutableListOf<() -> Unit>()
    val personRequests = mutableListOf<QueryPersonsRequest>()
    val groupRequests = mutableListOf<QueryGroupsRequest>()
    val interactionRequests = mutableListOf<CreateInteractionPendingIntentRequest>()
    val personBuffers = mutableListOf<QueryPersonsBufferResponse>()
    val groupBuffers = mutableListOf<QueryGroupsBufferResponse>()

    /** Listeners currently registered, with the request each observes. */
    val listeners = mutableMapOf<OnDataChangedListener, Any>()

    /** Listeners passed to [stopObserving], in order. */
    val stoppedListeners = mutableListOf<OnDataChangedListener>()

    var supportCheckCount: Int = 0
        private set

    /** Calls every registered listener, as Google Play services does when its cache changes. */
    fun fireDataChanged() {
        listeners.keys.toList().forEach { it.onDataChanged() }
    }

    /** Completes the calls held since [holdCalls] was set, in order. */
    fun completeHeldCalls() {
        val calls = heldCalls.toList()
        heldCalls.clear()
        calls.forEach { it() }
    }

    /**
     * Returns a task with [block]'s result, which completes at once or, when [holdCalls] is set,
     * once [completeHeldCalls] runs. [block] runs at once either way, as Google Play services acts
     * on a call before its result reaches the caller. Fails at once with [failure] when it is set.
     */
    private fun <T> resultOrHeld(block: () -> T?): Task<T> {
        failure?.let {
            return Tasks.forException(it)
        }
        val value = block()
        if (!holdCalls) return Tasks.forResult(value)
        val source = TaskCompletionSource<T>()
        heldCalls += { source.setResult(value) }
        return source.task
    }

    override fun isPeopleProviderSupported(
        request: IsPeopleProviderSupportedRequest
    ): Task<IsPeopleProviderSupportedResponse> {
        supportCheckCount++
        supportCheckFailure?.let {
            return Tasks.forException(it)
        }
        return Tasks.forResult(IsPeopleProviderSupportedResponse(supportedBackendType))
    }

    override fun getDirectories(request: GetDirectoriesRequest): Task<GetDirectoriesResponse> =
        result {
            GetDirectoriesResponse.builder().setDirectories(directories).build()
        }

    override fun queryPersons(request: QueryPersonsRequest): Task<QueryPersonsBufferResponse> =
        resultOrHeld {
            personRequests += request
            QueryPersonsBufferResponse.builder()
                .setPersons(persons)
                .setUnreachablePackages(unreachablePackages)
                .build()
                .also {
                    personBuffers += it
                }
        }

    override fun queryGroups(request: QueryGroupsRequest): Task<QueryGroupsBufferResponse> =
        result {
            groupRequests += request
            QueryGroupsBufferResponse.builder()
                .setGroups(groups)
                .setUnreachablePackages(unreachablePackages)
                .build()
                .also {
                    groupBuffers += it
                }
        }

    override fun observePersons(
        request: QueryPersonsRequest,
        listener: OnDataChangedListener,
    ): Task<Void> = resultOrHeld {
        listeners[listener] = request
        null
    }

    override fun observeGroups(
        request: QueryGroupsRequest,
        listener: OnDataChangedListener,
    ): Task<Void> {
        failure?.let {
            return Tasks.forException(it)
        }
        listeners[listener] = request
        return Tasks.forResult<Void>(null)
    }

    override fun stopObserving(listener: OnDataChangedListener): Task<Boolean> {
        stoppedListeners += listener
        return Tasks.forResult(listeners.remove(listener) != null)
    }

    override fun createInteractionPendingIntent(
        request: CreateInteractionPendingIntentRequest
    ): Task<CreateInteractionPendingIntentResponse> = result {
        interactionRequests += request
        CreateInteractionPendingIntentResponse.builder()
            .setPendingIntent(interactionPendingIntent)
            .build()
    }

    override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> =
        throw UnsupportedOperationException()

    private inline fun <T> result(block: () -> T): Task<T> {
        failure?.let {
            return Tasks.forException(it)
        }
        return Tasks.forResult(block())
    }

    companion object {
        /** An [ApiException] carrying [statusCode], as Google Play services reports failures. */
        fun apiException(statusCode: Int): ApiException = ApiException(Status(statusCode))
    }
}
