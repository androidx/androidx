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

import androidx.credentials.lint.RestoreCredentialConstants.ANDROID_URI
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_RESTORE_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import androidx.credentials.lint.RestoreCredentialConstants.TAG_APPLICATION
import androidx.credentials.lint.RestoreCredentialConstants.TAG_MANIFEST
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.PartialResult
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Proactive multi-scanner adoption detector (`MissingRestoreCredentials`) that identifies Android
 * apps with user sign-up or sign-in surfaces across six detection layers and prompts adoption of
 * the Restore Credentials API (`CreateRestoreCredentialRequest` + `GetRestoreCredentialOption`) for
 * zero-tap sign-in restoration across Device-to-Device (D2D) and cloud backups:
 * - Layer 1: Jetpack Credential Manager & Google Auth APIs / dependencies
 * - Layer 2: BaaS, enterprise, and social authentication SDKs (Firebase Auth, Auth0, Okta, AppAuth,
 *   MSAL, Facebook Login, Amplify/Cognito, Supabase, Clerk, Line, Kakao)
 * - Layer 3: Jetpack Compose sign-up / sign-in UI (`SecureTextField`, `OutlinedSecureTextField`,
 *   `BasicSecureTextField`, `PasswordVisualTransformation`, `ContentType.Password` / `SmsOtpCode`)
 * - Layer 4: View XML layout sign-up / sign-in forms (`res/layout*` password & account inputs)
 * - Layer 5: Browser OAuth (`AuthTabIntent`, OAuth `<intent-filter>`), Passkey Digital Asset Links
 *   (`delegate_permission/common.get_login_creds`), `AccountManager`, and `SmsRetrieverClient`
 * - Layer 6: Exemption auto-suppression (existing Restore Credentials, legacy Block Store, Games,
 *   Wear OS / TV / Automotive, library modules, and non-account password prompts) + single-anchor
 *   module consolidation.
 */
class MissingRestoreCredentialsDetector : Detector(), SourceCodeScanner, XmlScanner, GradleScanner {

    // ===========================================================================
    // Layers 1, 2, 3, 5 & Exemption Tracking in Kotlin / Java Source (UAST)
    // ===========================================================================

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(
            UImportStatement::class.java,
            UClass::class.java,
            UMethod::class.java,
            UCallExpression::class.java,
        )

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitImportStatement(node: UImportStatement) {
                val ref = node.importReference?.asSourceString() ?: return
                if (ref.startsWith(PACKAGE_LEGACY_BLOCKSTORE)) {
                    context.getPartialResults(ISSUE).map().put(KEY_SAW_LEGACY_BLOCKSTORE, true)
                }
            }

            override fun visitClass(node: UClass) {
                if (
                    context.evaluator.inheritsFrom(
                        node.javaPsi,
                        CLASS_ABSTRACT_ACCOUNT_AUTHENTICATOR,
                        true,
                    )
                ) {
                    if (context.driver.isSuppressed(context, ISSUE, node as UElement)) {
                        return
                    }
                    recordCandidate(
                        context = context,
                        rank = RANK_OAUTH_OR_OS_AUTH,
                        location = context.getNameLocation(node),
                        signalDescription = "`AbstractAccountAuthenticator` account authenticator",
                    )
                }
            }

            override fun visitCallExpression(node: UCallExpression) {
                val resolvedMethod = node.resolve() ?: return
                val ownerClass = resolvedMethod.containingClass ?: return
                val rawQualifiedClass = ownerClass.qualifiedName ?: return
                val qualifiedClass = rawQualifiedClass.removeSuffix(".Companion")

                // Track Restore Credentials & legacy Block Store exemption signals unconditionally.
                if (qualifiedClass == CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST) {
                    context.getPartialResults(ISSUE).map().put(KEY_SAW_CREATE_RESTORE, true)
                    return
                }
                if (qualifiedClass == CLASS_GET_RESTORE_CREDENTIAL_OPTION) {
                    context.getPartialResults(ISSUE).map().put(KEY_SAW_GET_RESTORE, true)
                    return
                }
                if (qualifiedClass.startsWith(PACKAGE_LEGACY_BLOCKSTORE)) {
                    context.getPartialResults(ISSUE).map().put(KEY_SAW_LEGACY_BLOCKSTORE, true)
                    return
                }

                // Layer 1 & Layer 2: Explicit Credential Manager or 1P/3P Auth SDK calls (Rank 1)
                val authSdkLabel = matchAuthApiOrSdkClass(qualifiedClass, resolvedMethod)
                if (authSdkLabel != null) {
                    if (context.driver.isSuppressed(context, ISSUE, node as UElement)) {
                        return
                    }
                    recordCandidate(
                        context = context,
                        rank = RANK_AUTH_API,
                        location =
                            context.getCallLocation(
                                node,
                                includeReceiver = false,
                                includeArguments = false,
                            ),
                        signalDescription = authSdkLabel,
                    )
                    return
                }

                // Layer 5: Browser OAuth, AccountManager, and SMS Retriever OTP APIs (Rank 2)
                val osOrOAuthLabel =
                    matchOAuthOrOsAuthCall(context, ownerClass, qualifiedClass, resolvedMethod)
                if (osOrOAuthLabel != null) {
                    if (context.driver.isSuppressed(context, ISSUE, node as UElement)) {
                        return
                    }
                    recordCandidate(
                        context = context,
                        rank = RANK_OAUTH_OR_OS_AUTH,
                        location =
                            context.getCallLocation(
                                node,
                                includeReceiver = false,
                                includeArguments = false,
                            ),
                        signalDescription = osOrOAuthLabel,
                    )
                }
            }

            override fun visitMethod(node: UMethod) {
                // Layer 3: Custom Jetpack Compose Sign-Up / Sign-In UI (Rank 3) with Non-Account
                // Password Filter (requires co-occurring account ContentType or an auth-named
                // screen/class without non-account Wi-Fi/PDF/encryption terms).
                val body = node.uastBody ?: return
                var firstPasswordUiNode: UElement? = null
                var hasAccountContentType = false

                body.accept(
                    object : AbstractUastVisitor() {
                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            val methodName = node.methodName ?: return false
                            val resolved = node.resolve()
                            val ownerName = resolved?.containingClass?.qualifiedName ?: ""

                            if (isComposePasswordCall(methodName, ownerName)) {
                                if (firstPasswordUiNode == null) {
                                    firstPasswordUiNode = node
                                }
                            }
                            return false
                        }

                        override fun visitSimpleNameReferenceExpression(
                            node: USimpleNameReferenceExpression
                        ): Boolean {
                            val name = node.identifier
                            if (
                                name in COMPOSE_PASSWORD_CONTENT_TYPES &&
                                    isComposeContentTypeRef(node)
                            ) {
                                if (firstPasswordUiNode == null) {
                                    firstPasswordUiNode = node
                                }
                            } else if (
                                name in COMPOSE_ACCOUNT_CONTENT_TYPES &&
                                    isComposeContentTypeRef(node)
                            ) {
                                hasAccountContentType = true
                            }
                            return false
                        }
                    }
                )

                val anchorNode = firstPasswordUiNode ?: return

                val enclosingClassName = node.getParentOfType<UClass>(true)?.name ?: ""
                val combinedContextName = "${enclosingClassName}_${node.name}"
                val isAuthNamedScreen =
                    containsAuthScreenKeyword(combinedContextName) &&
                        !containsNonAccountPasswordKeyword(combinedContextName)

                if (hasAccountContentType || isAuthNamedScreen) {
                    if (
                        context.driver.isSuppressed(context, ISSUE, node as UElement) ||
                            context.driver.isSuppressed(context, ISSUE, anchorNode)
                    ) {
                        return
                    }
                    val location =
                        if (anchorNode is UCallExpression) {
                            context.getCallLocation(
                                anchorNode,
                                includeReceiver = false,
                                includeArguments = false,
                            )
                        } else {
                            context.getLocation(anchorNode)
                        }
                    recordCandidate(
                        context = context,
                        rank = RANK_LOGIN_UI,
                        location = location,
                        signalDescription = "Jetpack Compose sign-in UI",
                    )
                }
            }
        }

    // ===========================================================================
    // Layers 4, 5 & Manifest Exemptions in XML (Manifest, Layouts, Values)
    // ===========================================================================

    override fun visitDocument(context: XmlContext, document: Document) {
        val root = document.documentElement ?: return
        when {
            root.tagName == TAG_MANIFEST -> inspectManifestDocument(context, root)
            root.tagName == "resources" -> inspectValuesDocument(context, root)
            context.file.parentFile?.name?.startsWith("layout") == true -> {
                inspectLayoutDocument(context, root)
            }
        }
    }

    private fun inspectManifestDocument(context: XmlContext, manifestRoot: Element) {
        val map = context.getPartialResults(ISSUE).map()
        if (context.driver.isSuppressed(context, ISSUE, manifestRoot)) {
            map.put(KEY_EXEMPT_MANIFEST, true)
            return
        }

        // Check <uses-feature> non-handset exemptions (Wear OS, Android TV, Automotive).
        val featureNodes = manifestRoot.getElementsByTagName("uses-feature")
        for (i in 0 until featureNodes.length) {
            val featureEl = featureNodes.item(i) as? Element ?: continue
            val featureName = featureEl.getAndroidAttr("name")
            val requiredAttr = featureEl.getAndroidAttr("required")
            val isRequired = !requiredAttr.equals("false", ignoreCase = true)
            if (isRequired && featureName in NON_HANDSET_FEATURES) {
                map.put(KEY_EXEMPT_MANIFEST, true)
                return
            }
        }

        // Check <application> Games carve-out (appCategory="game" or isGame="true")
        // or tools:ignore="MissingRestoreCredentials" on <application>.
        val appNodes = manifestRoot.getElementsByTagName(TAG_APPLICATION)
        for (i in 0 until appNodes.length) {
            val appEl = appNodes.item(i) as? Element ?: continue
            if (context.driver.isSuppressed(context, ISSUE, appEl)) {
                map.put(KEY_EXEMPT_MANIFEST, true)
                return
            }
            val appCategory = appEl.getAndroidAttr("appCategory")
            val isGame = appEl.getAndroidAttr("isGame")
            if (
                appCategory.equals("game", ignoreCase = true) ||
                    isGame.equals("true", ignoreCase = true)
            ) {
                map.put(KEY_EXEMPT_MANIFEST, true)
                return
            }
        }

        // Layer 5: RedirectUriReceiverActivity or OAuth callback <intent-filter> schemes.
        val activityNodes = manifestRoot.getElementsByTagName("activity")
        for (i in 0 until activityNodes.length) {
            val activityEl = activityNodes.item(i) as? Element ?: continue
            val activityName = activityEl.getAndroidAttr("name")
            if (activityName == CLASS_APPAUTH_REDIRECT_ACTIVITY) {
                if (context.driver.isSuppressed(context, ISSUE, activityEl)) {
                    continue
                }
                recordCandidate(
                    context = context,
                    rank = RANK_OAUTH_OR_OS_AUTH,
                    location = context.getNameLocation(activityEl),
                    signalDescription = "`RedirectUriReceiverActivity` OAuth callback",
                )
            }
        }

        val intentFilterNodes = manifestRoot.getElementsByTagName("intent-filter")
        for (i in 0 until intentFilterNodes.length) {
            val filterEl = intentFilterNodes.item(i) as? Element ?: continue
            if (isOAuthRedirectIntentFilter(filterEl)) {
                if (context.driver.isSuppressed(context, ISSUE, filterEl)) {
                    continue
                }
                recordCandidate(
                    context = context,
                    rank = RANK_OAUTH_OR_OS_AUTH,
                    location = context.getNameLocation(filterEl),
                    signalDescription = "OAuth redirect `<intent-filter>`",
                )
            }
        }
    }

    private fun inspectValuesDocument(context: XmlContext, resourcesRoot: Element) {
        val stringNodes = resourcesRoot.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val stringEl = stringNodes.item(i) as? Element ?: continue
            val content = stringEl.textContent ?: continue
            if (content.contains("delegate_permission/common.get_login_creds")) {
                if (context.driver.isSuppressed(context, ISSUE, stringEl)) {
                    continue
                }
                recordCandidate(
                    context = context,
                    rank = RANK_OAUTH_OR_OS_AUTH,
                    location = context.getNameLocation(stringEl),
                    signalDescription = "Digital Asset Links `get_login_creds` declaration",
                )
            }
        }
    }

    private fun inspectLayoutDocument(context: XmlContext, layoutRoot: Element) {
        var passwordInputEl: Element? = null
        var accountInputEl: Element? = null

        fun walkElements(node: Node) {
            if (node is Element) {
                if (isTextInputViewTag(node.tagName)) {
                    val autofillTokens = tokenizeAutofillHints(node.getAndroidAttr("autofillHints"))
                    val inputTypeTokens = tokenizeInputType(node.getAndroidAttr("inputType"))

                    val isPasswordOrOtp =
                        autofillTokens.any { it in XML_PASSWORD_AUTOFILL_HINTS } ||
                            inputTypeTokens.any { it in XML_PASSWORD_INPUT_TYPES }
                    val isAccountIdentifier =
                        autofillTokens.any { it in XML_ACCOUNT_AUTOFILL_HINTS } ||
                            inputTypeTokens.any { it in XML_ACCOUNT_INPUT_TYPES }

                    if (isPasswordOrOtp && passwordInputEl == null) {
                        passwordInputEl = node
                    }
                    if (isAccountIdentifier && accountInputEl == null && node !== passwordInputEl) {
                        accountInputEl = node
                    }
                }
                var child = node.firstChild
                while (child != null) {
                    walkElements(child)
                    child = child.nextSibling
                }
            }
        }

        walkElements(layoutRoot)
        val anchorEl = passwordInputEl ?: return
        if (accountInputEl == null) return
        if (
            context.driver.isSuppressed(context, ISSUE, layoutRoot) ||
                context.driver.isSuppressed(context, ISSUE, anchorEl)
        ) {
            return
        }

        recordCandidate(
            context = context,
            rank = RANK_LOGIN_UI,
            location = context.getNameLocation(anchorEl),
            signalDescription = "XML sign-in layout (`${context.file.name}`)",
        )
    }

    // ===========================================================================
    // Layers 1 & 2: Gradle Build Script Scanner
    // ===========================================================================

    @Deprecated("Deprecated in GradleScanner")
    override fun checkDslPropertyAssignment(
        context: GradleContext,
        property: String,
        value: String,
        parent: String,
        parentParent: String?,
        valueCookie: Any,
        statementCookie: Any,
    ) {
        // Handled by 8-argument overload below.
    }

    override fun checkDslPropertyAssignment(
        context: GradleContext,
        property: String,
        value: String,
        parent: String,
        parentParent: String?,
        propertyCookie: Any,
        valueCookie: Any,
        statementCookie: Any,
    ) {
        val inDependenciesBlock =
            parent == "dependencies" ||
                parent == "declarativeDependencies" ||
                (parentParent == "dependencies" && isProductionDependencyConfig(property))
        if (!inDependenciesBlock || !isProductionDependencyConfig(property)) return

        val unquoted =
            GradleContext.getStringLiteralValue(value, valueCookie) ?: value.trim('"', '\'')
        inspectGradleDependencyValue(context, unquoted, statementCookie)
    }

    @Deprecated("Deprecated in GradleScanner")
    override fun checkMethodCall(
        context: GradleContext,
        statement: String,
        parent: String?,
        namedArguments: Map<String, String>,
        unnamedArguments: List<String>,
        cookie: Any,
    ) {
        // Handled by 7-argument overload below.
    }

    override fun checkMethodCall(
        context: GradleContext,
        statement: String,
        parent: String?,
        parentParent: String?,
        namedArguments: Map<String, String>,
        unnamedArguments: List<String>,
        cookie: Any,
    ) {
        if (parent != "dependencies" && parent != "declarativeDependencies") return
        if (!isProductionDependencyConfig(statement)) return

        val coordinate =
            if (namedArguments.containsKey("group") && namedArguments.containsKey("name")) {
                "${namedArguments["group"]}:${namedArguments["name"]}"
            } else {
                unnamedArguments.joinToString(":") { it.trim('"', '\'') }
            }
        inspectGradleDependencyValue(context, coordinate, cookie)
    }

    private fun inspectGradleDependencyValue(
        context: GradleContext,
        rawCoordinate: String,
        cookie: Any,
    ) {
        if (rawCoordinate.contains("play-services-auth-blockstore")) {
            context.getPartialResults(ISSUE).map().put(KEY_SAW_LEGACY_BLOCKSTORE, true)
            return
        }
        val matchedArtifact = matchAuthGradleDependency(rawCoordinate) ?: return
        if (context.isSuppressedWithComment(cookie, ISSUE)) {
            return
        }
        recordCandidate(
            context = context,
            rank = RANK_GRADLE_DEP,
            location = context.getLocation(cookie),
            signalDescription = "authentication dependency `$matchedArtifact`",
        )
    }

    // ===========================================================================
    // Layer 6: Exemption Gates & Single-Anchor Module Consolidation
    // ===========================================================================

    override fun checkPartialResults(context: Context, partialResults: PartialResult) {
        if (context.project.isLibrary) return

        val allMaps = partialResults.maps()
        if (allMaps.any { it.getBoolean(KEY_EXEMPT_MANIFEST) == true }) return
        if (allMaps.any { it.getBoolean(KEY_SAW_LEGACY_BLOCKSTORE) == true }) return

        val sawCreateRestore = allMaps.any { it.getBoolean(KEY_SAW_CREATE_RESTORE) == true }
        val sawGetRestore = allMaps.any { it.getBoolean(KEY_SAW_GET_RESTORE) == true }
        if (sawCreateRestore && sawGetRestore) return

        // Pick the single highest-priority candidate across the app module and its sub-modules.
        var bestRank = Int.MAX_VALUE
        var bestLocation: Location? = null
        var bestSignal: String? = null

        for (moduleMap in allMaps) {
            val rank = moduleMap.getInt(KEY_BEST_RANK, Int.MAX_VALUE) ?: Int.MAX_VALUE
            val loc = moduleMap.getLocation(KEY_BEST_LOCATION) ?: continue
            if (rank < bestRank) {
                bestRank = rank
                bestLocation = loc
                bestSignal = moduleMap.getString(KEY_BEST_SIGNAL, "sign-in flow")
            }
        }

        val anchorLocation = bestLocation ?: return
        val signalDesc = bestSignal ?: "sign-in flow"

        val message =
            when {
                sawCreateRestore && !sawGetRestore ->
                    "App registers a `RestoreCredential` (`CreateRestoreCredentialRequest`) without " +
                        "retrieving it via `GetRestoreCredentialOption` on target devices; implement " +
                        "end-to-end Restore Credentials restoration"
                !sawCreateRestore && sawGetRestore ->
                    "App queries `GetRestoreCredentialOption` without creating a restore key via " +
                        "`CreateRestoreCredentialRequest` upon sign-in; complete Restore Credentials " +
                        "registration"
                else ->
                    "Sign-in surface detected ($signalDesc) without Restore Credentials " +
                        "(`CreateRestoreCredentialRequest` and `GetRestoreCredentialOption`); integrate " +
                        "Restore Credentials to silently restore user sessions across device transfers"
            }

        val fix =
            LintFix.create()
                .name("Open Restore Credentials developer guide")
                .url(RESTORE_CREDENTIALS_GUIDE_URL)
                .build()

        context.report(Incident(ISSUE, anchorLocation, message, fix))
    }

    override fun afterCheckRootProject(context: Context) {
        if (context.isGlobalAnalysis() && !(context.driver.isIsolated() && LintClient.isStudio)) {
            checkPartialResults(context, context.getPartialResults(ISSUE))
        }
    }

    // ===========================================================================
    // Signal Matching Helpers
    // ===========================================================================

    private fun recordCandidate(
        context: Context,
        rank: Int,
        location: Location,
        signalDescription: String,
    ) {
        val map = context.getPartialResults(ISSUE).map()
        val existingRank = map.getInt(KEY_BEST_RANK, Int.MAX_VALUE) ?: Int.MAX_VALUE
        if (rank < existingRank) {
            map.put(KEY_BEST_RANK, rank)
            map.put(KEY_BEST_LOCATION, location)
            map.put(KEY_BEST_SIGNAL, signalDescription)
        }
    }

    private fun matchAuthApiOrSdkClass(qualifiedClass: String, method: PsiMethod): String? {
        if (qualifiedClass in CREDMAN_AUTH_REQUEST_CLASSES) {
            return "`${qualifiedClass.substringAfterLast('.')}`"
        }
        if (
            qualifiedClass.startsWith("com.google.android.libraries.identity.googleid.") &&
                (qualifiedClass.endsWith("GetGoogleIdOption") ||
                    qualifiedClass.endsWith("GetSignInWithGoogleOption"))
        ) {
            return "`${qualifiedClass.substringAfterLast('.')}`"
        }
        if (
            qualifiedClass in PLAY_SERVICES_GOOGLE_AUTH_CLASSES &&
                (method.isConstructor || method.name in PLAY_SERVICES_SIGN_IN_METHODS)
        ) {
            return "`${qualifiedClass.substringAfterLast('.')}`"
        }
        for ((prefix, sdkLabel) in THIRD_PARTY_AUTH_SDK_PACKAGES) {
            if (
                qualifiedClass.startsWith(prefix) &&
                    isThirdPartyAuthSignInOrSignUpMethod(qualifiedClass, method)
            ) {
                return "$sdkLabel (`${qualifiedClass.substringAfterLast('.')}.${method.name}`)"
            }
        }
        return null
    }

    private fun isThirdPartyAuthSignInOrSignUpMethod(
        qualifiedClass: String,
        method: PsiMethod,
    ): Boolean {
        val name = method.name
        if (name in NON_ACCOUNT_AUTH_SDK_METHODS || name.contains("Anonymous", ignoreCase = true)) {
            return false
        }
        if (
            qualifiedClass == "net.openid.appauth.AuthorizationRequest" ||
                qualifiedClass.startsWith("net.openid.appauth.AuthorizationRequest.")
        ) {
            return true
        }
        return name in THIRD_PARTY_SIGN_IN_EXACT_METHODS ||
            THIRD_PARTY_SIGN_IN_METHOD_PREFIXES.any { name.startsWith(it) }
    }

    private fun matchOAuthOrOsAuthCall(
        context: JavaContext,
        ownerClass: PsiClass,
        qualifiedClass: String,
        method: PsiMethod,
    ): String? {
        if (qualifiedClass.startsWith("androidx.browser.auth.AuthTabIntent")) {
            return "`AuthTabIntent`"
        }
        if (qualifiedClass == CLASS_APPAUTH_REDIRECT_ACTIVITY) {
            return "`RedirectUriReceiverActivity`"
        }
        if (
            method.name == "addAccountExplicitly" &&
                context.evaluator.inheritsFrom(ownerClass, "android.accounts.AccountManager", false)
        ) {
            return "`AccountManager.addAccountExplicitly`"
        }
        if (
            (method.name == "startSmsRetriever" || method.name == "startSmsUserConsent") &&
                qualifiedClass.startsWith("com.google.android.gms.auth.api.phone.")
        ) {
            return "`SmsRetrieverClient.${method.name}`"
        }
        return null
    }

    private fun isComposePasswordCall(methodName: String, ownerQualifiedName: String): Boolean {
        if (methodName in COMPOSE_SECURE_TEXT_FIELD_FUNCTIONS) {
            return ownerQualifiedName.isEmpty() ||
                ownerQualifiedName.startsWith("androidx.compose.")
        }
        if (methodName == "PasswordVisualTransformation") {
            return ownerQualifiedName.isEmpty() ||
                ownerQualifiedName.startsWith("androidx.compose.")
        }
        if (
            methodName == "password" &&
                ownerQualifiedName.startsWith("androidx.compose.ui.semantics")
        ) {
            return true
        }
        return false
    }

    private fun isComposeContentTypeRef(ref: USimpleNameReferenceExpression): Boolean {
        val resolved = ref.resolve() as? PsiMember
        val ownerName = resolved?.containingClass?.qualifiedName
        if (ownerName != null) {
            return ownerName.startsWith("androidx.compose.ui.autofill.ContentType")
        }
        return ref.uastParent?.asSourceString()?.contains("ContentType.") == true
    }

    private fun isOAuthRedirectIntentFilter(filterEl: Element): Boolean {
        val dataNodes = filterEl.getElementsByTagName("data")
        var sawCustomScheme = false
        var sawOAuthHostOrPath = false

        for (i in 0 until dataNodes.length) {
            val dataEl = dataNodes.item(i) as? Element ?: continue
            val scheme = dataEl.getAndroidAttr("scheme").lowercase()
            val host = dataEl.getAndroidAttr("host").lowercase()
            val path =
                listOf(
                        dataEl.getAndroidAttr("path"),
                        dataEl.getAndroidAttr("pathPrefix"),
                        dataEl.getAndroidAttr("pathPattern"),
                    )
                    .joinToString("/")
                    .lowercase()

            if (scheme.isNotEmpty() && scheme !in STANDARD_NON_OAUTH_SCHEMES) {
                sawCustomScheme = true
            }
            if (scheme.contains("oauth") || scheme.endsWith(".auth")) {
                sawCustomScheme = true
                sawOAuthHostOrPath = true
            }
            if (
                OAUTH_URI_TOKENS.any { token ->
                    matchesUriSegment(host, token) || matchesUriSegment(path, token)
                }
            ) {
                sawOAuthHostOrPath = true
            }
        }
        return sawCustomScheme && sawOAuthHostOrPath
    }

    private fun matchesUriSegment(value: String, token: String): Boolean {
        if (value.isEmpty()) return false
        return value.split('/', '.', '-', '_').any { segment -> segment == token }
    }

    private fun isTextInputViewTag(tagName: String): Boolean {
        val simpleName = tagName.substringAfterLast('.')
        return simpleName.endsWith("EditText") || simpleName.endsWith("AutoCompleteTextView")
    }

    private fun tokenizeAutofillHints(raw: String): Set<String> {
        if (raw.isEmpty()) return emptySet()
        return raw.split(',', ' ', '|')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun tokenizeInputType(raw: String): Set<String> {
        if (raw.isEmpty()) return emptySet()
        return raw.split('|', ' ').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun isProductionDependencyConfig(configName: String): Boolean {
        val lower = configName.lowercase()
        if (lower.startsWith("test") || lower.startsWith("androidtest")) return false
        return lower == "implementation" ||
            lower == "api" ||
            lower == "compileonly" ||
            lower == "runtimeonly" ||
            lower.endsWith("implementation") ||
            lower.endsWith("api")
    }

    private fun matchAuthGradleDependency(raw: String): String? {
        val normalized = raw.trim().lowercase()
        for (coordinatePrefix in AUTH_GRADLE_COORDINATE_PREFIXES) {
            if (normalized.startsWith(coordinatePrefix)) {
                return raw.trim()
            }
        }
        for (catalogToken in AUTH_VERSION_CATALOG_TOKENS) {
            if (normalized == catalogToken || normalized.endsWith(".$catalogToken")) {
                return raw.trim()
            }
        }
        return null
    }

    private fun containsAuthScreenKeyword(identifier: String): Boolean {
        val lower = identifier.lowercase()
        return AUTH_CONTEXT_KEYWORDS.any { lower.contains(it) }
    }

    private fun containsNonAccountPasswordKeyword(identifier: String): Boolean {
        val lower = identifier.lowercase()
        return NON_ACCOUNT_PASSWORD_KEYWORDS.any { lower.contains(it) }
    }

    private fun Element.getAndroidAttr(localName: String): String {
        val nsVal = getAttributeNS(ANDROID_URI, localName)
        if (nsVal.isNotEmpty()) return nsVal.trim()
        val prefixed = getAttribute("android:$localName")
        if (prefixed.isNotEmpty()) return prefixed.trim()
        return getAttribute(localName).trim()
    }

    companion object {
        private const val RANK_AUTH_API = 1
        private const val RANK_OAUTH_OR_OS_AUTH = 2
        private const val RANK_LOGIN_UI = 3
        private const val RANK_GRADLE_DEP = 4

        private const val KEY_SAW_CREATE_RESTORE = "SAW_CREATE_RESTORE"
        private const val KEY_SAW_GET_RESTORE = "SAW_GET_RESTORE"
        private const val KEY_SAW_LEGACY_BLOCKSTORE = "SAW_LEGACY_BLOCKSTORE"
        private const val KEY_EXEMPT_MANIFEST = "EXEMPT_MANIFEST"
        private const val KEY_BEST_RANK = "BEST_RANK"
        private const val KEY_BEST_LOCATION = "BEST_LOCATION"
        private const val KEY_BEST_SIGNAL = "BEST_SIGNAL"

        private const val PACKAGE_LEGACY_BLOCKSTORE = "com.google.android.gms.auth.blockstore"
        private const val CLASS_ABSTRACT_ACCOUNT_AUTHENTICATOR =
            "android.accounts.AbstractAccountAuthenticator"
        private const val CLASS_APPAUTH_REDIRECT_ACTIVITY =
            "net.openid.appauth.RedirectUriReceiverActivity"

        private val CREDMAN_AUTH_REQUEST_CLASSES =
            setOf(
                "androidx.credentials.CreatePasswordRequest",
                "androidx.credentials.CreatePublicKeyCredentialRequest",
                "androidx.credentials.GetPasswordOption",
                "androidx.credentials.GetPublicKeyCredentialOption",
            )

        private val PLAY_SERVICES_GOOGLE_AUTH_CLASSES =
            setOf(
                "com.google.android.gms.auth.api.signin.GoogleSignIn",
                "com.google.android.gms.auth.api.signin.GoogleSignInClient",
                "com.google.android.gms.auth.api.signin.GoogleSignInOptions",
                "com.google.android.gms.auth.api.identity.SignInClient",
                "com.google.android.gms.auth.api.identity.BeginSignInRequest",
            )

        private val PLAY_SERVICES_SIGN_IN_METHODS =
            setOf(
                "getClient",
                "getSignInIntent",
                "silentSignIn",
                "beginSignIn",
                "getSignInPendingIntent",
                "builder",
                "build",
                "setGoogleIdTokenRequestOptions",
                "setPasswordRequestOptions",
                "setPasskeysSignInRequestOptions",
                "setPasskeyJsonSignInRequestOptions",
            )

        private val THIRD_PARTY_AUTH_SDK_PACKAGES =
            listOf(
                "com.google.firebase.auth." to "Firebase Auth",
                "com.auth0.android." to "Auth0 SDK",
                "com.okta." to "Okta Auth SDK",
                "net.openid.appauth.AuthorizationService" to "AppAuth OAuth SDK",
                "net.openid.appauth.AuthorizationRequest" to "AppAuth OAuth SDK",
                "com.microsoft.identity.client." to "Microsoft MSAL",
                "com.facebook.login." to "Facebook Login SDK",
                "com.amplifyframework.auth." to "AWS Amplify Auth",
                "com.amazonaws.mobileconnectors.cognitoidentityprovider." to "AWS Cognito",
                "io.github.jan.supabase.gotrue." to "Supabase GoTrue Auth",
                "io.github.jan.supabase.auth." to "Supabase Auth",
                "com.clerk." to "Clerk Auth SDK",
                "com.linecorp.linesdk." to "LINE Login SDK",
                "com.kakao.sdk.user." to "Kakao Login SDK",
                "com.kakao.sdk.auth." to "Kakao Auth SDK",
            )

        private val NON_ACCOUNT_AUTH_SDK_METHODS =
            setOf(
                "signInAnonymously",
                "getCurrentUser",
                "getInstance",
                "signOut",
                "logOut",
                "logout",
                "clear",
                "revokeAccess",
                "addAuthStateListener",
                "removeAuthStateListener",
                "addIdTokenListener",
                "removeIdTokenListener",
                "useAppLanguage",
                "setLanguageCode",
            )

        private val THIRD_PARTY_SIGN_IN_EXACT_METHODS =
            setOf(
                "signIn",
                "signUp",
                "login",
                "logIn",
                "authenticate",
                "authorize",
                "register",
                "getLoginIntent",
                "performAuthorizationRequest",
                "getAuthorizationRequestIntent",
                "exchangeAuthorizationCode",
            )

        private val THIRD_PARTY_SIGN_IN_METHOD_PREFIXES =
            listOf(
                "signInWith",
                "signUpWith",
                "createUserWith",
                "startActivityForSignInWith",
                "reauthenticate",
                "linkWith",
                "loginWith",
                "logInWith",
                "acquireToken",
                "showSignIn",
            )

        private val AUTH_GRADLE_COORDINATE_PREFIXES =
            listOf(
                "com.google.android.libraries.identity.googleid:googleid",
                "com.google.android.gms:play-services-auth",
                "com.google.firebase:firebase-auth",
                "com.auth0.android:",
                "com.okta.",
                "net.openid:appauth",
                "com.microsoft.identity.client:msal",
                "com.facebook.android:facebook-login",
                "com.amplifyframework:aws-auth-cognito",
                "com.amazonaws:aws-android-sdk-cognitoidentityprovider",
                "io.github.jan-tennert.supabase:gotrue-kt",
                "io.github.jan-tennert.supabase:auth-kt",
                "com.clerk:",
                "com.linecorp.linesdk:",
                "com.kakao.sdk:v2-user",
                "com.kakao.sdk:v2-auth",
            )

        private val AUTH_VERSION_CATALOG_TOKENS =
            listOf(
                "firebase.auth",
                "play.services.auth",
                "googleid",
                "auth0",
                "appauth",
                "facebook.login",
            )

        private val COMPOSE_SECURE_TEXT_FIELD_FUNCTIONS =
            setOf("SecureTextField", "OutlinedSecureTextField", "BasicSecureTextField")

        private val COMPOSE_PASSWORD_CONTENT_TYPES = setOf("Password", "NewPassword", "SmsOtpCode")
        private val COMPOSE_ACCOUNT_CONTENT_TYPES =
            setOf("Username", "NewUsername", "EmailAddress", "PhoneNumber")

        private val XML_PASSWORD_AUTOFILL_HINTS = setOf("password", "newpassword", "smsotpcode")
        private val XML_PASSWORD_INPUT_TYPES =
            setOf("textpassword", "textvisiblepassword", "textwebpassword", "numberpassword")

        private val XML_ACCOUNT_AUTOFILL_HINTS =
            setOf("username", "newusername", "emailaddress", "phone", "phonenumber")
        private val XML_ACCOUNT_INPUT_TYPES =
            setOf("textemailaddress", "textwebemailaddress", "phone")

        private val AUTH_CONTEXT_KEYWORDS =
            listOf(
                "signin",
                "sign_in",
                "signup",
                "sign_up",
                "login",
                "log_in",
                "register",
                "registration",
                "auth",
            )

        private val NON_ACCOUNT_PASSWORD_KEYWORDS =
            listOf(
                "wifi",
                "wi_fi",
                "hotspot",
                "pdf",
                "zip",
                "archive",
                "decrypt",
                "encrypt",
                "keystore",
                "vault",
            )

        private val STANDARD_NON_OAUTH_SCHEMES =
            setOf("http", "https", "file", "content", "geo", "mailto", "tel", "sms", "about")

        private val OAUTH_URI_TOKENS =
            setOf("oauth", "oauth2", "oauth2redirect", "auth", "callback", "signin", "login")

        private val NON_HANDSET_FEATURES =
            setOf(
                "android.hardware.type.watch",
                "android.software.leanback",
                "android.hardware.type.television",
                "android.hardware.type.automotive",
            )

        private val IMPLEMENTATION =
            Implementation(
                MissingRestoreCredentialsDetector::class.java,
                EnumSet.of(
                    Scope.JAVA_FILE,
                    Scope.RESOURCE_FILE,
                    Scope.MANIFEST,
                    Scope.GRADLE_FILE,
                ),
                Scope.JAVA_FILE_SCOPE,
                Scope.RESOURCE_FILE_SCOPE,
                Scope.MANIFEST_SCOPE,
                Scope.GRADLE_SCOPE,
            )

        val ISSUE: Issue =
            Issue.create(
                id = "MissingRestoreCredentials",
                briefDescription = "Sign-in flow detected without Restore Credentials support",
                explanation =
                    """
                Apps with optional or mandatory user accounts should silently restore a user's \
                signed-in session across Device-to-Device (D2D) and cloud backups using the Jetpack \
                Restore Credentials API (`androidx.credentials:credentials:1.5.0+`):

                1. On the source device after sign-in, register a restore key via \
                `CredentialManager.createCredential(context, CreateRestoreCredentialRequest(json))` \
                (catching `E2eeUnavailableException` and retrying with `isCloudBackupEnabled = false`).
                2. On the target device after backup/restore (ideally inside \
                `BackupAgent.onRestoreFinished()` wrapped in `runBlocking`), retrieve the restored \
                credential via `GetRestoreCredentialOption` and silently re-establish the user's session.

                If silent session restoration is intentionally omitted (for example, in regulated \
                financial, healthcare, or government apps), annotate the sign-in entry point or \
                `<application>` tag with `@SuppressLint("MissingRestoreCredentials")` / \
                `tools:ignore="MissingRestoreCredentials"`.
            """,
                category = Category.COMPLIANCE,
                priority = 7,
                severity = Severity.INFORMATIONAL,
                implementation = IMPLEMENTATION,
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
