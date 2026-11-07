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

package androidx.credentials.lint

import androidx.credentials.lint.stubs.COROUTINES_STUB
import androidx.credentials.lint.stubs.CREDENTIALS_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RestoreCredentialBackupAgentDetectorTest : LintDetectorTest() {

    override fun getDetector() = RestoreCredentialBackupAgentDetector()

    override fun getIssues() =
        listOf(
            RestoreCredentialBackupAgentDetector.ASYNC_ISSUE,
            RestoreCredentialBackupAgentDetector.WRONG_CALLBACK_ISSUE,
        )

    @Test
    fun asyncLaunchInOnRestoreFinished_reportsErrorAndConvertsToRunBlocking() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                kotlin(
                        """
                package com.example

                import android.app.backup.BackupAgent
                import android.app.backup.BackupDataInput
                import android.app.backup.BackupDataOutput
                import android.os.ParcelFileDescriptor
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.RestoreCredential
                import kotlinx.coroutines.CoroutineScope
                import kotlinx.coroutines.Dispatchers
                import kotlinx.coroutines.launch

                class AsyncBackupAgent : BackupAgent() {
                    override fun onBackup(
                        oldState: ParcelFileDescriptor?,
                        data: BackupDataOutput?,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestore(
                        data: BackupDataInput?,
                        appVersionCode: Int,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestoreFinished() {
                        super.onRestoreFinished()
                        val manager = CredentialManager.create(this)
                        val request = GetCredentialRequest(listOf(GetRestoreCredentialOption("{}")))
                        CoroutineScope(Dispatchers.IO).launch {
                            val response = manager.getCredential(this@AsyncBackupAgent, request)
                            val cred = response.credential as? RestoreCredential
                        }
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/com/example/AsyncBackupAgent.kt line 33: Replace 'CoroutineScope(Dispatchers.IO).launch' with 'runBlocking':
                @@ -32 +32 @@
                -        CoroutineScope(Dispatchers.IO).launch {
                +        kotlinx.coroutines.runBlocking {
                """
                    .trimIndent()
            )
    }

    @Test
    fun asyncDeferredAndUnawaitedJavaGetCredentialAsync_reportErrors() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                kotlin(
                        """
                package com.example

                import android.app.backup.BackupAgent
                import android.app.backup.BackupDataInput
                import android.app.backup.BackupDataOutput
                import android.os.ParcelFileDescriptor
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import kotlinx.coroutines.CoroutineScope
                import kotlinx.coroutines.Dispatchers
                import kotlinx.coroutines.async
                import kotlinx.coroutines.launch
                import kotlinx.coroutines.runBlocking

                class DeferredBackupAgent : BackupAgent() {
                    override fun onBackup(
                        oldState: ParcelFileDescriptor?,
                        data: BackupDataOutput?,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestore(
                        data: BackupDataInput?,
                        appVersionCode: Int,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestoreFinished() {
                        val manager = CredentialManager.create(this)
                        val request = GetCredentialRequest(listOf(GetRestoreCredentialOption("{}")))
                        CoroutineScope(Dispatchers.IO).async {
                            manager.getCredential(this@DeferredBackupAgent, request)
                        }
                        runBlocking {
                            CoroutineScope(Dispatchers.IO).launch {
                                manager.getCredential(this@DeferredBackupAgent, request)
                            }
                        }
                    }
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import android.app.backup.BackupAgent;
                import android.app.backup.BackupDataInput;
                import android.app.backup.BackupDataOutput;
                import android.os.CancellationSignal;
                import android.os.ParcelFileDescriptor;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetCredentialResponse;
                import androidx.credentials.GetRestoreCredentialOption;
                import androidx.credentials.exceptions.GetCredentialException;
                import java.util.Collections;
                import java.util.concurrent.Executors;

                public class JavaAsyncBackupAgent extends BackupAgent {
                    @Override
                    public void onBackup(
                            ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {}

                    @Override
                    public void onRestore(
                            BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) {}

                    @Override
                    public void onRestoreFinished() {
                        CredentialManager manager = CredentialManager.create(this);
                        GetCredentialRequest request =
                                new GetCredentialRequest(
                                        Collections.singletonList(new GetRestoreCredentialOption("{}")));
                        manager.getCredentialAsync(
                                this,
                                request,
                                new CancellationSignal(),
                                Executors.newSingleThreadExecutor(),
                                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                                    @Override
                                    public void onResult(GetCredentialResponse result) {}

                                    @Override
                                    public void onError(GetCredentialException e) {}
                                });
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectErrorCount(3)
    }

    @Test
    fun restoreInLegacyOnRestoreCallback_reportsWrongCallbackAndFixesSignature() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                kotlin(
                        """
                package com.example

                import android.app.backup.BackupAgentHelper
                import android.app.backup.BackupDataInput
                import android.os.ParcelFileDescriptor
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import kotlinx.coroutines.runBlocking

                class WrongCallbackBackupAgent : BackupAgentHelper() {
                    override fun onRestore(
                        data: BackupDataInput?,
                        appVersionCode: Int,
                        newState: ParcelFileDescriptor?,
                    ) {
                        super.onRestore(data, appVersionCode, newState)
                        val manager = CredentialManager.create(this)
                        val request = GetCredentialRequest(listOf(GetRestoreCredentialOption("{}")))
                        runBlocking { manager.getCredential(this@WrongCallbackBackupAgent, request) }
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/com/example/WrongCallbackBackupAgent.kt line 20: Move restore credential retrieval to onRestoreFinished():
                @@ -12 +12
                -     override fun onRestore(
                -         data: BackupDataInput?,
                -         appVersionCode: Int,
                -         newState: ParcelFileDescriptor?,
                -     ) {
                -         super.onRestore(data, appVersionCode, newState)
                +     override fun onRestoreFinished() {
                +         super.onRestoreFinished()
                """
                    .trimIndent()
            )
    }

    @Test
    fun runBlockingAndLatchAwaitedJavaRestore_areCompliant() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                kotlin(
                        """
                package com.example

                import android.app.backup.BackupAgent
                import android.app.backup.BackupDataInput
                import android.app.backup.BackupDataOutput
                import android.os.ParcelFileDescriptor
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.RestoreCredential
                import kotlinx.coroutines.Dispatchers
                import kotlinx.coroutines.runBlocking
                import kotlinx.coroutines.withContext

                class CompliantBackupAgent : BackupAgent() {
                    override fun onBackup(
                        oldState: ParcelFileDescriptor?,
                        data: BackupDataOutput?,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestore(
                        data: BackupDataInput?,
                        appVersionCode: Int,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestoreFinished() {
                        super.onRestoreFinished()
                        val manager = CredentialManager.create(this)
                        val request = GetCredentialRequest(listOf(GetRestoreCredentialOption("{}")))
                        runBlocking {
                            val response =
                                withContext(Dispatchers.IO) {
                                    manager.getCredential(this@CompliantBackupAgent, request)
                                }
                            val restoreCredential = response.credential as? RestoreCredential
                        }
                    }
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import android.app.backup.BackupAgent;
                import android.app.backup.BackupDataInput;
                import android.app.backup.BackupDataOutput;
                import android.os.CancellationSignal;
                import android.os.ParcelFileDescriptor;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetCredentialResponse;
                import androidx.credentials.GetRestoreCredentialOption;
                import androidx.credentials.exceptions.GetCredentialException;
                import java.util.Collections;
                import java.util.concurrent.CountDownLatch;
                import java.util.concurrent.Executors;
                import java.util.concurrent.TimeUnit;

                public class JavaLatchBackupAgent extends BackupAgent {
                    @Override
                    public void onBackup(
                            ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {}

                    @Override
                    public void onRestore(
                            BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) {}

                    @Override
                    public void onRestoreFinished() {
                        CountDownLatch latch = new CountDownLatch(1);
                        CredentialManager manager = CredentialManager.create(this);
                        GetCredentialRequest request =
                                new GetCredentialRequest(
                                        Collections.singletonList(new GetRestoreCredentialOption("{}")));
                        manager.getCredentialAsync(
                                this,
                                request,
                                new CancellationSignal(),
                                Executors.newSingleThreadExecutor(),
                                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                                    @Override
                                    public void onResult(GetCredentialResponse result) {
                                        latch.countDown();
                                    }

                                    @Override
                                    public void onError(GetCredentialException e) {
                                        latch.countDown();
                                    }
                                });
                        try {
                            latch.await(10, TimeUnit.SECONDS);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
