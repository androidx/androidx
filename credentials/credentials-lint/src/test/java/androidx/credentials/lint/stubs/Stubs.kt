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

package androidx.credentials.lint.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

private val CREDENTIAL_STUB: TestFile =
    kotlin(
            """
        package androidx.credentials

        import android.os.Bundle

        abstract class Credential(open val type: String, open val data: Bundle = Bundle())

        class RestoreCredential(
            val authenticationResponseJson: String,
            data: Bundle = Bundle(),
        ) : Credential(TYPE_RESTORE_CREDENTIAL, data) {
            companion object {
                const val TYPE_RESTORE_CREDENTIAL: String =
                    "androidx.credentials.TYPE_RESTORE_CREDENTIAL"
            }
        }

        class PublicKeyCredential(
            val authenticationResponseJson: String,
            data: Bundle = Bundle(),
        ) : Credential(TYPE_PUBLIC_KEY_CREDENTIAL, data) {
            companion object {
                const val TYPE_PUBLIC_KEY_CREDENTIAL: String =
                    "androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"
            }
        }

        class PasswordCredential(
            val id: String,
            val password: String,
        ) : Credential("android.credentials.TYPE_PASSWORD_CREDENTIAL")
        """
        )
        .indented()

private val CREDENTIAL_OPTIONS_AND_REQUESTS_STUB: TestFile =
    kotlin(
            """
        package androidx.credentials

        import android.os.Bundle

        abstract class CredentialOption(val type: String)

        class GetRestoreCredentialOption(
            val requestJson: String,
        ) : CredentialOption(RestoreCredential.TYPE_RESTORE_CREDENTIAL)

        class GetPublicKeyCredentialOption
        @JvmOverloads
        constructor(
            val requestJson: String,
            val clientDataHash: ByteArray? = null,
        ) : CredentialOption(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)

        class GetPasswordOption : CredentialOption("android.credentials.TYPE_PASSWORD_CREDENTIAL")

        class GetCredentialRequest
        @JvmOverloads
        constructor(
            val credentialOptions: List<CredentialOption>,
            val origin: String? = null,
        ) {
            class Builder {
                fun addCredentialOption(credentialOption: CredentialOption): Builder = this
                fun setCredentialOptions(credentialOptions: List<CredentialOption>): Builder = this
                fun build(): GetCredentialRequest = throw UnsupportedOperationException()
            }

            companion object {
                @JvmStatic
                @JvmOverloads
                fun createFrom(
                    credentialOptions: List<CredentialOption>,
                    origin: String? = null,
                ): GetCredentialRequest = throw UnsupportedOperationException()
            }
        }

        class GetCredentialResponse(val credential: Credential)

        abstract class CreateCredentialRequest(val type: String)

        class CreateRestoreCredentialRequest
        @JvmOverloads
        constructor(
            val requestJson: String,
            val isCloudBackupEnabled: Boolean = true,
        ) : CreateCredentialRequest(RestoreCredential.TYPE_RESTORE_CREDENTIAL)

        open class CreateCredentialResponse(val type: String, val data: Bundle = Bundle())

        class ClearCredentialStateRequest
        @JvmOverloads
        constructor(
            val requestType: String = TYPE_CLEAR_CREDENTIAL_STATE,
        ) {
            companion object {
                const val TYPE_CLEAR_CREDENTIAL_STATE: String =
                    "androidx.credentials.TYPE_CLEAR_CREDENTIAL_STATE"
                const val TYPE_CLEAR_RESTORE_CREDENTIAL: String =
                    "androidx.credentials.TYPE_CLEAR_RESTORE_CREDENTIAL"
            }
        }
        """
        )
        .indented()

private val CREDENTIAL_EXCEPTIONS_STUB: TestFile =
    kotlin(
            """
        package androidx.credentials.exceptions

        open class GetCredentialException(
            val type: String,
            val errorMessage: CharSequence? = null,
        ) : Exception(errorMessage?.toString())

        open class CreateCredentialException(
            val type: String,
            val errorMessage: CharSequence? = null,
        ) : Exception(errorMessage?.toString())

        open class ClearCredentialException(
            val type: String,
            val errorMessage: CharSequence? = null,
        ) : Exception(errorMessage?.toString())
        """
        )
        .indented()

private val E2EE_EXCEPTION_STUB: TestFile =
    kotlin(
            """
        package androidx.credentials.exceptions.restorecredential

        import androidx.credentials.exceptions.CreateCredentialException

        class E2eeUnavailableException(errorMessage: CharSequence? = null) :
            CreateCredentialException("androidx.credentials.TYPE_E2EE_UNAVAILABLE_EXCEPTION")
        """
        )
        .indented()

private val CREDENTIAL_MANAGER_STUB: TestFile =
    kotlin(
            """
        package androidx.credentials

        import android.content.Context
        import android.os.CancellationSignal
        import androidx.credentials.exceptions.ClearCredentialException
        import androidx.credentials.exceptions.CreateCredentialException
        import androidx.credentials.exceptions.GetCredentialException
        import java.util.concurrent.Executor

        interface CredentialManagerCallback<R : Any?, E : Any> {
            fun onResult(result: R)
            fun onError(e: E)
        }

        interface CredentialManager {
            suspend fun getCredential(
                context: Context,
                request: GetCredentialRequest,
            ): GetCredentialResponse

            fun getCredentialAsync(
                context: Context,
                request: GetCredentialRequest,
                cancellationSignal: CancellationSignal?,
                executor: Executor,
                callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
            )

            suspend fun createCredential(
                context: Context,
                request: CreateCredentialRequest,
            ): CreateCredentialResponse

            fun createCredentialAsync(
                context: Context,
                request: CreateCredentialRequest,
                cancellationSignal: CancellationSignal?,
                executor: Executor,
                callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
            )

            suspend fun clearCredentialState(request: ClearCredentialStateRequest)

            fun clearCredentialStateAsync(
                request: ClearCredentialStateRequest,
                cancellationSignal: CancellationSignal?,
                executor: Executor,
                callback: CredentialManagerCallback<Void?, ClearCredentialException>,
            )

            companion object {
                @JvmStatic
                fun create(context: Context): CredentialManager =
                    throw UnsupportedOperationException()
            }
        }
        """
        )
        .indented()

internal val COROUTINES_STUB: TestFile =
    kotlin(
            """
        package kotlinx.coroutines

        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext

        interface CoroutineScope {
            val coroutineContext: CoroutineContext
        }

        fun CoroutineScope(context: CoroutineContext): CoroutineScope =
            throw UnsupportedOperationException()

        object Dispatchers {
            val IO: CoroutineContext
                get() = throw UnsupportedOperationException()
            val Main: CoroutineContext
                get() = throw UnsupportedOperationException()
            val Default: CoroutineContext
                get() = throw UnsupportedOperationException()
        }

        class Job

        class Deferred<out T> {
            suspend fun await(): T = throw UnsupportedOperationException()
        }

        fun CoroutineScope.launch(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> Unit,
        ): Job = throw UnsupportedOperationException()

        fun <T> CoroutineScope.async(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> T,
        ): Deferred<T> = throw UnsupportedOperationException()

        fun <T> runBlocking(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> T,
        ): T = throw UnsupportedOperationException()

        suspend fun <T> withContext(
            context: CoroutineContext,
            block: suspend CoroutineScope.() -> T,
        ): T = throw UnsupportedOperationException()
        """
        )
        .indented()

internal val CREDENTIALS_STUBS: Array<TestFile> =
    arrayOf(
        CREDENTIAL_STUB,
        CREDENTIAL_OPTIONS_AND_REQUESTS_STUB,
        CREDENTIAL_EXCEPTIONS_STUB,
        E2EE_EXCEPTION_STUB,
        CREDENTIAL_MANAGER_STUB,
    )
