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

import androidx.credentials.lint.stubs.CREDENTIALS_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MissingRestoreCredentialsDetectorTest : LintDetectorTest() {

    override fun getDetector() = MissingRestoreCredentialsDetector()

    override fun getIssues() = listOf(MissingRestoreCredentialsDetector.ISSUE)

    private val firebaseAuthStub: TestFile =
        kotlin(
                """
            package com.google.firebase.auth

            abstract class FirebaseAuth {
                abstract fun getCurrentUser(): Any?
                abstract fun signInAnonymously()
                abstract fun signInWithEmailAndPassword(email: String, password: String)
            }
            """
            )
            .indented()

    private val composeAuthStubs: Array<TestFile> =
        arrayOf(
            kotlin(
                    """
                package androidx.compose.material3

                fun OutlinedSecureTextField(value: String = "") {}
                """
                )
                .indented(),
            kotlin(
                    """
                package androidx.compose.ui.autofill

                interface ContentType {
                    companion object {
                        val Username: ContentType = object : ContentType {}
                        val EmailAddress: ContentType = object : ContentType {}
                        val Password: ContentType = object : ContentType {}
                    }
                }
                """
                )
                .indented(),
        )

    private val smsRetrieverStub: TestFile =
        kotlin(
                """
            package com.google.android.gms.auth.api.phone

            abstract class SmsRetrieverClient {
                abstract fun startSmsRetriever()
            }
            """
            )
            .indented()

    private val legacyBlockstoreStub: TestFile =
        kotlin(
                """
            package com.google.android.gms.auth.blockstore

            interface BlockstoreClient {
                fun retrieveBytes()
            }
            """
            )
            .indented()

    @Test
    fun layer1_credManAuthWithoutRestoreCredentials_reportsSingleInformationalFinding() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        "src/com/example/SignInRepository.kt",
                        """
                package com.example

                import androidx.credentials.CreatePasswordRequest
                import androidx.credentials.GetPasswordOption

                class SignInRepository {
                    fun buildSignInOptions(): GetPasswordOption {
                        val createReq = CreatePasswordRequest("user@example.com", "secret")
                        return GetPasswordOption()
                    }
                }
                """,
                    )
                    .indented(),
            )
            .skipTestModes(TestMode.SUPPRESSIBLE)
            .run()
            .expect(
                """
                src/com/example/SignInRepository.kt:8: Hint: Sign-in surface detected (CreatePasswordRequest) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                        val createReq = CreatePasswordRequest("user@example.com", "secret")
                                        ~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )
    }

    @Test
    fun layer2_thirdPartyAuthSdksInSourceAndGradle_reportFindingsAndIgnoreTestDeps() {
        lint()
            .files(
                firebaseAuthStub,
                kotlin(
                        "src/com/example/FirebaseLoginManager.kt",
                        """
                package com.example

                import com.google.firebase.auth.FirebaseAuth

                class FirebaseLoginManager {
                    fun login(auth: FirebaseAuth, email: String, password: String) {
                        auth.signInWithEmailAndPassword(email, password)
                    }
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/FirebaseLoginManager.kt:7: Hint: Sign-in surface detected (Firebase Auth (FirebaseAuth.signInWithEmailAndPassword)) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                        auth.signInWithEmailAndPassword(email, password)
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )

        lint()
            .files(
                kts(
                        """
                dependencies {
                    implementation("com.auth0.android:auth0:2.11.0")
                }
                """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                build.gradle.kts:2: Hint: Sign-in surface detected (authentication dependency com.auth0.android:auth0:2.11.0) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    implementation("com.auth0.android:auth0:2.11.0")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )

        lint()
            .files(
                kts(
                        """
                dependencies {
                    testImplementation("com.auth0.android:auth0:2.11.0")
                    androidTestImplementation("com.google.firebase:firebase-auth:23.0.0")
                }
                """
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun layer3_composeSignInUiReportsFinding_nonAccountWifiPasswordPromptSuppressed() {
        lint()
            .files(
                *composeAuthStubs,
                kotlin(
                        "src/com/example/SignInScreen.kt",
                        """
                package com.example

                import androidx.compose.material3.OutlinedSecureTextField
                import androidx.compose.ui.autofill.ContentType

                fun SignInScreen(username: String, password: String) {
                    val userType = ContentType.Username
                    OutlinedSecureTextField(password)
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/SignInScreen.kt:8: Hint: Sign-in surface detected (Jetpack Compose sign-in UI) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    OutlinedSecureTextField(password)
                    ~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )

        lint()
            .files(
                *composeAuthStubs,
                kotlin(
                        "src/com/example/WifiPasswordDialog.kt",
                        """
                package com.example

                import androidx.compose.material3.OutlinedSecureTextField

                fun WifiPasswordDialog(passphrase: String) {
                    OutlinedSecureTextField(passphrase)
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun layer4_xmlLayoutSignInFormReportsFinding_standaloneUnlockLayoutSuppressed() {
        lint()
            .files(
                xml(
                        "res/layout/activity_login.xml",
                        """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical">
                    <EditText
                        android:id="@+id/email"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:autofillHints="emailAddress"
                        android:inputType="textEmailAddress" />
                    <EditText
                        android:id="@+id/password"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:autofillHints="password"
                        android:inputType="textPassword" />
                </LinearLayout>
                """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                res/layout/activity_login.xml:11: Hint: Sign-in surface detected (XML sign-in layout (activity_login.xml)) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    <EditText
                     ~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )

        lint()
            .files(
                xml(
                        "res/layout/dialog_unlock.xml",
                        """
                <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content">
                    <EditText
                        android:id="@+id/passcode"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="numberPassword" />
                </FrameLayout>
                """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun layer5_oauthRedirectManifestAndAssetStatementsAndSmsRetriever_reportFindings() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application>
                        <activity android:name=".OAuthRedirectActivity" android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.VIEW" />
                                <category android:name="android.intent.category.DEFAULT" />
                                <category android:name="android.intent.category.BROWSABLE" />
                                <data android:scheme="myapp" android:host="oauth" android:path="/callback" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
                """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:5: Hint: Sign-in surface detected (OAuth redirect <intent-filter>) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                            <intent-filter>
                             ~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )

        lint()
            .files(
                xml(
                        "res/values/strings.xml",
                        """
                <resources>
                    <string name="asset_statements">[{"relation": ["delegate_permission/common.get_login_creds"]}]</string>
                </resources>
                """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                res/values/strings.xml:2: Hint: Sign-in surface detected (Digital Asset Links get_login_creds declaration) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    <string name="asset_statements">[{"relation": ["delegate_permission/common.get_login_creds"]}]</string>
                     ~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )

        lint()
            .files(
                smsRetrieverStub,
                kotlin(
                        "src/com/example/OtpVerifier.kt",
                        """
                package com.example

                import com.google.android.gms.auth.api.phone.SmsRetrieverClient

                fun startOtpFlow(client: SmsRetrieverClient) {
                    client.startSmsRetriever()
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/OtpVerifier.kt:6: Hint: Sign-in surface detected (SmsRetrieverClient.startSmsRetriever) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    client.startSmsRetriever()
                           ~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )
    }

    @Test
    fun layer6_multiSignalAppConsolidatesToSingleHighestPriorityAnchor() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application>
                        <activity android:name=".OAuthCallbackActivity" android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.VIEW" />
                                <data android:scheme="myapp" android:host="oauth" android:path="/callback" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
                """
                    )
                    .indented(),
                xml(
                        "src/main/res/layout/activity_login.xml",
                        """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent">
                    <EditText android:autofillHints="username" />
                    <EditText android:autofillHints="password" />
                </LinearLayout>
                """,
                    )
                    .indented(),
                kts(
                        """
                dependencies {
                    implementation("com.google.firebase:firebase-auth:23.0.0")
                }
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                kotlin(
                        "src/main/java/com/example/PasskeySignInManager.kt",
                        """
                package com.example

                import androidx.credentials.GetPublicKeyCredentialOption

                class PasskeySignInManager {
                    fun createOption() = GetPublicKeyCredentialOption("{}")
                }
                """,
                    )
                    .indented(),
            )
            .skipTestModes(TestMode.SUPPRESSIBLE)
            .run()
            .expect(
                """
                src/main/java/com/example/PasskeySignInManager.kt:6: Hint: Sign-in surface detected (GetPublicKeyCredentialOption) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    fun createOption() = GetPublicKeyCredentialOption("{}")
                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )
    }

    @Test
    fun partialAdoption_onlyCreateWithoutGetRestoreCredential_reportsIncompleteAdoption() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        "src/com/example/PartialRestoreManager.kt",
                        """
                package com.example

                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.GetPasswordOption

                class PartialRestoreManager {
                    fun buildLoginOption() = GetPasswordOption()

                    fun buildCreateRestore(json: String) =
                        CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false)
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/PartialRestoreManager.kt:7: Hint: App registers a RestoreCredential (CreateRestoreCredentialRequest) without retrieving it via GetRestoreCredentialOption on target devices; implement end-to-end Restore Credentials restoration [MissingRestoreCredentials]
                    fun buildLoginOption() = GetPasswordOption()
                                             ~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )
    }

    @Test
    fun layer6_exemptions_compliantAppGamesWearLegacyBlockStoreAndLibrary_reportZeroFindings() {
        // 1. Compliant app with both CreateRestoreCredentialRequest and GetRestoreCredentialOption
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                class CompliantAuthManager {
                    fun passwordOption() = GetPasswordOption()
                    fun restoreOption(json: String) = GetRestoreCredentialOption(json)
                    fun createRestoreRequest(json: String) =
                        CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()

        // 2. Game app carve-out (android:appCategory="game")
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example.game">
                    <application android:appCategory="game" />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example.game

                import androidx.credentials.GetPasswordOption

                fun signIn() = GetPasswordOption()
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()

        // 3. Wear OS non-handset carve-out
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example.wear">
                    <uses-feature android:name="android.hardware.type.watch" />
                    <application />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example.wear

                import androidx.credentials.GetPasswordOption

                fun signIn() = GetPasswordOption()
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()

        // 4. Legacy Block Store app
        lint()
            .files(
                *CREDENTIALS_STUBS,
                legacyBlockstoreStub,
                kotlin(
                        """
                package com.example

                import androidx.credentials.GetPasswordOption
                import com.google.android.gms.auth.blockstore.BlockstoreClient

                fun restoreFromBlockstore(client: BlockstoreClient): GetPasswordOption {
                    client.retrieveBytes()
                    return GetPasswordOption()
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()

        // 5. Library module
        val libProject =
            ProjectDescription(
                    *CREDENTIALS_STUBS,
                    kotlin(
                            """
                    package com.example.lib

                    import androidx.credentials.GetPasswordOption

                    fun sharedOption() = GetPasswordOption()
                    """
                        )
                        .indented(),
                )
                .type(ProjectDescription.Type.LIBRARY)
        lint().projects(libProject).run().expectClean()
    }

    @Test
    fun anonymousAuthIsClean_vaultLoginWithContentTypeAndScopedSuppressionAreReported() {
        // 1. Anonymous Firebase sign-in and getCurrentUser() do not trigger
        // MissingRestoreCredentials
        lint()
            .files(
                firebaseAuthStub,
                kotlin(
                        """
                package com.example

                import com.google.firebase.auth.FirebaseAuth

                fun ensureAnonymousSession(auth: FirebaseAuth) {
                    if (auth.getCurrentUser() == null) {
                        auth.signInAnonymously()
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()

        // 2. VaultLoginScreen with explicit ContentType.EmailAddress is reported, and
        // @Suppress("MissingRestoreCredentials") on DebugLoginScreen drops only that site.
        lint()
            .files(
                *composeAuthStubs,
                kotlin(
                        "src/com/example/VaultScreens.kt",
                        """
                package com.example

                import androidx.compose.material3.OutlinedSecureTextField
                import androidx.compose.ui.autofill.ContentType

                @Suppress("MissingRestoreCredentials")
                fun DebugLoginScreen(email: String, password: String) {
                    val emailType = ContentType.EmailAddress
                    OutlinedSecureTextField(password)
                }

                fun VaultLoginScreen(email: String, password: String) {
                    val emailType = ContentType.EmailAddress
                    OutlinedSecureTextField(password)
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/VaultScreens.kt:14: Hint: Sign-in surface detected (Jetpack Compose sign-in UI) without Restore Credentials (CreateRestoreCredentialRequest and GetRestoreCredentialOption); integrate Restore Credentials to silently restore user sessions across device transfers [MissingRestoreCredentials]
                    OutlinedSecureTextField(password)
                    ~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
                    .trimIndent()
            )
    }
}
