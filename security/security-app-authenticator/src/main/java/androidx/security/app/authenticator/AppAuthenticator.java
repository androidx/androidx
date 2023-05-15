/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.security.app.authenticator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import com.google.auto.value.AutoValue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods to verify the signing identity of other apps on the device.
 */
// TODO(b/175503230): Add usage details to class level documentation once implementation is
//  complete.
public class AppAuthenticator {
    private static final String TAG = "AppAuthenticator";

    /**
     * This is returned by {@link #checkCallingAppIdentity(String, String)} and
     * {@link #checkCallingAppIdentity(String, String, int, int)} when the specified package name
     * has the expected signing identity for the provided permission.
     */
    public static final int PERMISSION_GRANTED = 0;

    /**
     * This is returned by {@link #checkCallingAppIdentity(String, String)} and
     * {@link #checkCallingAppIdentity(String, String, int, int)} when the specified package name
     * does not have any of the expected signing identities for the provided permission.
     *
     * @see PackageManager#SIGNATURE_NO_MATCH
     */
    public static final int PERMISSION_DENIED_NO_MATCH = -3;

    /**
     * This is returned by {@link #checkCallingAppIdentity(String, String)} and
     * {@link #checkCallingAppIdentity(String, String, int, int)} when the specified package name
     * does not belong to an app installed on the device.
     *
     * @see PackageManager#SIGNATURE_UNKNOWN_PACKAGE
     */
    public static final int PERMISSION_DENIED_UNKNOWN_PACKAGE = -4;

    /**
     * This is returned by {@link #checkCallingAppIdentity(String, String)} and
     * {@link #checkCallingAppIdentity(String, String, int, int)} when the specified package name
     * does not belong to the provided calling UID, or if the UID is not provided and the
     * specified package name does not belong to the UID of the calling process as returned by
     * {@link Binder#getCallingUid()}.
     */
    public static final int PERMISSION_DENIED_PACKAGE_UID_MISMATCH = -5;

    /**
     * Values returned when checking that a specified package has the expected signing identity
     * for a particular permission.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {
            PERMISSION_GRANTED,
            PERMISSION_DENIED_NO_MATCH,
            PERMISSION_DENIED_UNKNOWN_PACKAGE,
            PERMISSION_DENIED_PACKAGE_UID_MISMATCH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AppIdentityPermissionResult {}

    /**
     * This is returned by {@link #checkAppIdentity(String)} when the specified package name has
     * the expected signing identity.
     *
     * @see PackageManager#SIGNATURE_MATCH
     */
    public static final int SIGNATURE_MATCH = 0;

    /**
     * This is returned by {@link #checkAppIdentity(String)} when the specified package name does
     * not have the expected signing identity.
     *
     * @see PackageManager#SIGNATURE_NO_MATCH
     */
    public static final int SIGNATURE_NO_MATCH = -1;

    /**
     * Values returned when checking that a specified package has the expected signing identity
     * on the device.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {
            SIGNATURE_MATCH,
            SIGNATURE_NO_MATCH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AppIdentityResult {}

    /**
     * The root tag for an AppAuthenticator XMl config file.
     */
    private static final String ROOT_TAG = "app-authenticator";
    /**
     * The tag to declare a new permission that can be granted to enclosed packages / signing
     * identities.
     */
    private static final String PERMISSION_TAG = "permission";
    /**
     * The tag to begin declaration of the expected signing identities for the enclosed packages.
     */
    private static final String EXPECTED_IDENTITY_TAG = "expected-identity";
    /**
     * The tag to declare a new signing identity of a package within either a permission or
     * expected-identity element.
     */
    private static final String PACKAGE_TAG = "package";
    /**
     * The tag to declare all packages signed with the enclosed signing identities are to be
     * granted to the enclosing permission.
     */
    static final String ALL_PACKAGES_TAG = "all-packages";
    /**
     * The tag to declare a known signing certificate digest for the enclosing package.
     */
    private static final String CERT_DIGEST_TAG = "cert-digest";
    /**
     * The attribute to declare the name within a permission or package element.
     */
    private static final String NAME_ATTRIBUTE = "name";
    /**
     * The default digest algorithm used for all certificate digests if one is not specified in
     * the root element.
     */
    static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";

    private AppSignatureVerifier mAppSignatureVerifier;
    private AppAuthenticatorUtils mAppAuthenticatorUtils;

    /**
     * Private constructor; instances should be created through the static factory methods.
     *
     * @param appSignatureVerifier the verifier to be used to verify app signing identities
     * @param appAuthenticatorUtils the utils to be used
     */
    AppAuthenticator(AppSignatureVerifier appSignatureVerifier,
            AppAuthenticatorUtils appAuthenticatorUtils) {
        mAppSignatureVerifier = appSignatureVerifier;
        mAppAuthenticatorUtils = appAuthenticatorUtils;
    }

    /**
     * Allows injection of the {@code appSignatureVerifier} to be used during tests.
     */
    @VisibleForTesting
    void setAppSignatureVerifier(AppSignatureVerifier appSignatureVerifier) {
        mAppSignatureVerifier = appSignatureVerifier;
    }

    /**
     * Allows injection of the {@code appAuthenticatorUtils} to be used during tests.
     * @param appAuthenticatorUtils
     */
    @VisibleForTesting
    void setAppAuthenticatorUtils(AppAuthenticatorUtils appAuthenticatorUtils) {
        mAppAuthenticatorUtils = appAuthenticatorUtils;
    }

    /**
     * Enforces the specified {@code packageName} has the expected signing identity for the
     * provided {@code permission}.
     *
     * <p>This method should be used when verifying the identity of a calling process of an IPC.
     * This is the same as calling {@link #enforceCallingAppIdentity(String, String, int, int)} with
     * the pid and uid returned by {@link Binder#getCallingPid()} and
     * {@link Binder#getCallingUid()}.
     *
     * @param packageName the name of the package to be verified
     * @param permission the name of the permission as specified in the XML from which to verify the
     *                   package / signing identity
     * @throws SecurityException if the signing identity of the package does not match that defined
     * for the permission
     */
    public void enforceCallingAppIdentity(@NonNull String packageName, @NonNull String permission) {
        enforceCallingAppIdentity(packageName, permission,
                mAppAuthenticatorUtils.getCallingPid(), mAppAuthenticatorUtils.getCallingUid());
    }

    /**
     * Enforces the specified {@code packageName} belongs to the provided {@code pid} / {@code uid}
     * and has the expected signing identity for the {@code permission}.
     *
     * <p>This method should be used when verifying the identity of a calling process of an IPC.
     *
     * @param packageName the name of the package to be verified
     * @param permission the name of the permission as specified in the XML from which to verify the
     *                   package / signing identity
     * @param pid the expected pid of the process
     * @param uid the expected uid of the package
     * @throws SecurityException if the uid does not belong to the specified package, or if the
     * signing identity of the package does not match that defined for the permission
     */
    public void enforceCallingAppIdentity(@NonNull String packageName, @NonNull String permission,
            int pid, int uid) {
        AppAuthenticatorResult result = checkCallingAppIdentityInternal(packageName, permission,
                pid, uid);
        if (result.getResultCode() != PERMISSION_GRANTED) {
            throw new SecurityException(result.getResultMessage());
        }
    }

    /**
     * Checks the specified {@code packageName} has the expected signing identity for the
     * provided {@code permission}.
     *
     * <p>This method should be used when verifying the identity of a calling process of an IPC.
     * This is the same as calling {@link #checkCallingAppIdentity(String, String, int, int)} with
     * the pid and uid returned by {@link Binder#getCallingPid()} and
     * {@link Binder#getCallingUid()}.
     *
     * @param packageName the name of the package to be verified
     * @param permission the name of the permission as specified in the XML from which to verify the
     *                   package / signing identity
     * @return {@link #PERMISSION_GRANTED} if the specified {@code packageName} has the expected
     * signing identity for the provided {@code permission},<br>
     *     {@link #PERMISSION_DENIED_NO_MATCH} if the specified {@code packageName} does not have
     *     the expected signing identity for the provided {@code permission},<br>
     *     {@link #PERMISSION_DENIED_UNKNOWN_PACKAGE} if the specified {@code packageName} does not
     *     exist on the device,<br>
     *     {@link #PERMISSION_DENIED_PACKAGE_UID_MISMATCH} if the uid as returned from
     *     {@link Binder#getCallingUid()} does not match the uid assigned to the package
     */
    @AppIdentityPermissionResult
    public int checkCallingAppIdentity(@NonNull String packageName, @NonNull String permission) {
        return checkCallingAppIdentity(packageName, permission,
                mAppAuthenticatorUtils.getCallingPid(), mAppAuthenticatorUtils.getCallingUid());
    }

    /**
     * Checks the specified {@code packageName} has the expected signing identity for the
     * provided {@code permission}.
     *
     * <p>This method should be used when verifying the identity of a calling process of an IPC.
     *
     * @param packageName the name of the package to be verified
     * @param permission the name of the permission as specified in the XML from which to verify the
     *                   package / signing identity
     * @param pid the expected pid of the process
     * @param uid the expected uid of the package
     * @return {@link #PERMISSION_GRANTED} if the specified {@code packageName} has the expected
     * signing identity for the provided {@code permission},<br>
     *     {@link #PERMISSION_DENIED_NO_MATCH} if the specified {@code packageName} does not have
     *     the expected signing identity for the provided {@code permission},<br>
     *     {@link #PERMISSION_DENIED_UNKNOWN_PACKAGE} if the specified {@code packageName} does not
     *     exist on the device,<br>
     *     {@link #PERMISSION_DENIED_PACKAGE_UID_MISMATCH} if the specified {@code uid} does not
     *     match the uid assigned to the package
     */
    @AppIdentityPermissionResult
    public int checkCallingAppIdentity(@NonNull String packageName, @NonNull String permission,
            int pid, int uid) {
        AppAuthenticatorResult result = checkCallingAppIdentityInternal(packageName, permission,
                pid, uid);
        if (result.getResultCode() != PERMISSION_GRANTED) {
            Log.e(TAG, result.getResultMessage());
        }
        return result.getResultCode();
    }

    /**
     * Checks the specified {@code packageName} has the expected signing identity for the
     * provided {@code permission} under the calling {@code pid} and {@code uid}.
     */
    // The pid variable may be used in a future release for platform verification; it is
    // currently added to the public API and this method to seamlessly make use of any platform
    // features in the future.
    @SuppressWarnings("UnusedVariable")
    private AppAuthenticatorResult checkCallingAppIdentityInternal(String packageName,
            String permission,
            int pid,
            int uid) {
        // First verify that the UID of the calling package matches the specified value.
        int packageUid;
        try {
            packageUid = mAppAuthenticatorUtils.getUidForPackage(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return AppAuthenticatorResult.create(PERMISSION_DENIED_UNKNOWN_PACKAGE,
                    "The app " + packageName + " was not found on the device");
        }
        if (packageUid != uid) {
            return AppAuthenticatorResult.create(PERMISSION_DENIED_PACKAGE_UID_MISMATCH,
                    "The expected UID, " + uid + ", of the app " + packageName
                            + " does not match the actual UID, " + packageUid);
        }
        if (mAppSignatureVerifier.verifySigningIdentity(packageName, permission)) {
            return AppAuthenticatorResult.create(PERMISSION_GRANTED, null);
        }
        return AppAuthenticatorResult.create(PERMISSION_DENIED_NO_MATCH, "The signing"
                + " identity of app " + packageName + " does not match the expected identity");
    }


    /**
     * Enforces the specified {@code packageName} has the expected signing identity as declared in
     * the {@code <expected-identity>} tag.
     *
     * <p>This method should be used when an app's signing identity must be verified; for instance
     * before a client connects to an exported service this method can be used to verify that the
     * app comes from the expected developer.
     *
     * @param packageName the name of the package to be verified
     * @throws SecurityException if the signing identity of the package does not match that defined
     * in the {@code <expected-identity>} tag
     */
    public void enforceAppIdentity(@NonNull String packageName) {
        if (checkAppIdentity(packageName) != SIGNATURE_MATCH) {
            throw new SecurityException("The app " + packageName + " does not match the expected "
                    + "signing identity");
        }
    }

    /**
     * Checks the specified {@code packageName} has the expected signing identity as specified in
     * the {@code <expected-identity>} tag.
     *
     * <p>This method should be used when an app's signing identity must be verified; for instance
     * before a client connects to an exported service this method can be used to verify that the
     * app comes from the expected developer.
     *
     * @param packageName the name of the package to be verified
     * @return {@link #SIGNATURE_MATCH} if the specified package has the expected
     * signing identity
     */
    @AppIdentityResult
    public int checkAppIdentity(@NonNull String packageName) {
        if (mAppSignatureVerifier.verifyExpectedIdentity(packageName)) {
            return SIGNATURE_MATCH;
        }
        return SIGNATURE_NO_MATCH;
    }

    /**
     * Creates a new {@code AppAuthenticator} that can be used to guard resources based on
     * package name / signing identity as well as allow verification of expected signing identities
     * before interacting with other apps on a device using the configuration defined in the
     * provided {@code xmlInputStream}.
     *
     * @param context        the context within which to create the {@code AppAuthenticator}
     * @param xmlInputStream the XML {@link InputStream} containing the definitions for the
     *                       permissions and expected identities based on packages / expected
     *                       signing certificate digests
     * @return a new {@code AppAuthenticator} that can be used to enforce the signing
     * identities defined in the provided XML {@code InputStream}
     * @throws AppAuthenticatorXmlException if the provided XML {@code InputStream} is not in the
     *                                      proper format to create a new {@code AppAuthenticator}
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      the XML {@code InputStream}
     */
    public static @NonNull AppAuthenticator createFromInputStream(@NonNull Context context,
            @NonNull InputStream xmlInputStream) throws AppAuthenticatorXmlException, IOException {
        XmlPullParser parser;
        try {
            parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(xmlInputStream, null);
        } catch (XmlPullParserException e) {
            throw new AppAuthenticatorXmlException("Unable to create parser from provided "
                    + "InputStream", e);
        }
        return createFromParser(context, parser);
    }

    /**
     * Creates a new {@code AppAuthenticator} that can be used to guard resources based on
     * package name / signing identity as well as allow verification of expected signing identities
     * before interacting with other apps on a device using the configuration defined in the
     * provided XML resource.
     *
     * @param context     the context within which to create the {@code AppAuthenticator}
     * @param xmlResource the ID of the XML resource containing the definitions for the
     *                    permissions and expected identities based on package / expected signing
     *                    certificate digests
     * @return a new {@code AppAuthenticator} that can be used to enforce the signing identities
     * defined in the provided XML resource
     * @throws AppAuthenticatorXmlException if the provided XML resource is not in the proper format
     *                                      to create a new {@code AppAuthenticator}
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      the XML resource
     */
    public static @NonNull AppAuthenticator createFromResource(@NonNull Context context,
            @XmlRes int xmlResource) throws AppAuthenticatorXmlException, IOException {
        Resources resources = context.getResources();
        XmlPullParser parser = resources.getXml(xmlResource);
        return createFromParser(context, parser);
    }

    /**
     * Creates a new {@code AppAuthenticator} that can be used to guard resources based on
     * package name / signing identity as well as allow verification of expected signing identities
     * before interacting with other apps on a device using the configuration defined in the
     * provided {@code parser}.
     *
     * @param context the context within which to create the {@code AppAuthenticator}
     * @param parser  an {@link XmlPullParser} containing the definitions for the
     *                permissions and expected identities based on package / expected signing
     *                certificate digests
     * @return a new {@code AppAuthenticator} that can be used to enforce the signing identities
     * defined in the provided {@code XmlPullParser}
     * @throws AppAuthenticatorXmlException if the provided XML parsed by the {@code XmlPullParser}
     *                                      is not in the proper format to create a new
     *                                      {@code AppAuthenticator}
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      from the {@code XmlPullParser}
     */
    private static AppAuthenticator createFromParser(Context context, XmlPullParser parser)
            throws AppAuthenticatorXmlException, IOException {
        AppAuthenticatorConfig config = createConfigFromParser(parser);
        return createFromConfig(context, config);
    }

    /**
     * Creates a new {@code AppAuthenticator} that can be used to guard resources based on
     * package name / signing identity as well as allow verification of expected signing identities
     * before interacting with other apps on a device using the configuration defined in the
     * provided {@code config}.
     *
     * @param context the context within which to create the {@code AppAuthenticator}
     * @param config  an {@link AppAuthenticatorConfig} containing the definitions for the
     *                permissions and expected identities based on package / expected signing
     *                certificate digests
     * @return a new {@code AppAuthenticator} that can be used to enforce the signing identities
     * defined in the provided {@code config}
     */
    static AppAuthenticator createFromConfig(Context context,
            @NonNull AppAuthenticatorConfig config) {
        AppSignatureVerifier verifier = AppSignatureVerifier.builder(context)
                .setPermissionAllowMap(config.getPermissionAllowMap())
                .setExpectedIdentities(config.getExpectedIdentities())
                .setDigestAlgorithm(config.getDigestAlgorithm())
                .build();
        return new AppAuthenticator(verifier, new AppAuthenticatorUtils(context));
    }

    /**
     * Creates a new {@code AppAuthenticatorConfig} that can be used to instantiate a new {@code
     * AppAuthenticator} with the specified config.
     *
     * @param parser an {@link XmlPullParser} containing the definition for the permissions and
     *               expected identities based on package / expected signing certificate digests
     * @return a new {@code AppAuthenticatorConfig} based on the config declared in the {@code
     * parser} that can be used to instantiate a new {@code AppAuthenticator}.
     * @throws AppAuthenticatorXmlException if the provided XML parsed by the {@code XmlPullParser}
     *                                      is not in the proper format to create a new
     *                                      {@code AppAuthenticator}
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      from the {@code XmlPullParser}
     */
    static AppAuthenticatorConfig createConfigFromParser(XmlPullParser parser)
            throws AppAuthenticatorXmlException, IOException {
        Map<String, Map<String, Set<String>>> permissionAllowMap = new ArrayMap<>();
        Map<String, Set<String>> expectedIdentities = new ArrayMap<>();
        try {
            parseToNextStartTag(parser);
            String tag = parser.getName();
            if (TextUtils.isEmpty(tag) || !tag.equalsIgnoreCase(ROOT_TAG)) {
                throw new AppAuthenticatorXmlException(
                        "Provided XML does not contain the expected root tag: " + ROOT_TAG);
            }
            assertExpectedAttribute(parser, ROOT_TAG, null, false);
            String digestAlgorithm = DEFAULT_DIGEST_ALGORITHM;
            int eventType = parser.nextTag();
            // Each new start tag should be for a new permission / expected-identity.
            while (eventType == XmlPullParser.START_TAG) {
                tag = parser.getName();
                if (tag.equalsIgnoreCase(PERMISSION_TAG)) {
                    assertExpectedAttribute(parser, PERMISSION_TAG, NAME_ATTRIBUTE, true);
                    String permissionName = parser.getAttributeValue(null, NAME_ATTRIBUTE);
                    if (TextUtils.isEmpty(permissionName)) {
                        throw new AppAuthenticatorXmlException(
                                "The " + PERMISSION_TAG + " tag requires a non-empty value for the "
                                        + NAME_ATTRIBUTE + " attribute");
                    }
                    Map<String, Set<String>> allowedPackageCerts = parsePackages(parser, true);
                    if (permissionAllowMap.containsKey(permissionName)) {
                        permissionAllowMap.get(permissionName).putAll(allowedPackageCerts);
                    } else {
                        permissionAllowMap.put(permissionName, allowedPackageCerts);
                    }
                } else if (tag.equalsIgnoreCase(EXPECTED_IDENTITY_TAG)) {
                    assertExpectedAttribute(parser, EXPECTED_IDENTITY_TAG, null, true);
                    expectedIdentities.putAll(parsePackages(parser, false));
                } else {
                    throw new AppAuthenticatorXmlException(
                            "Expected " + PERMISSION_TAG + " or " + EXPECTED_IDENTITY_TAG
                                    + " under root tag at line " + parser.getLineNumber());
                }
                eventType = parser.nextTag();
            }
            return AppAuthenticatorConfig.create(permissionAllowMap, expectedIdentities,
                    digestAlgorithm);
        } catch (XmlPullParserException e) {
            throw new AppAuthenticatorXmlException("Caught an exception parsing the provided "
                    + "XML:", e);
        }
    }

    /**
     * Parses package tags from the provided {@code parser}, allowing the {@code all-packages}
     * tag if the {@code allPackagesAllowed} boolean is true.
     *
     * @param parser             the {@link XmlPullParser} from which to parser the packages
     * @param allPackagesAllowed boolean indicating whether the {@code all-packages} element is
     *                           allowed
     * @return a mapping from the enclosed packages to signing identities
     * @throws AppAuthenticatorXmlException if the provided XML parsed by the {@code XmlPullParser}
     *                                      is not in the proper format
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      from the {@code XmlPullParser}
     * @throws XmlPullParserException       if any errors are encountered when attempting to
     *                                      parse the provided {@code XmlPullParser}
     */
    private static Map<String, Set<String>> parsePackages(XmlPullParser parser,
            boolean allPackagesAllowed)
            throws AppAuthenticatorXmlException, IOException, XmlPullParserException {
        Map<String, Set<String>> allowedPackageCerts = new ArrayMap<>();
        int eventType = parser.nextTag();
        while (eventType == XmlPullParser.START_TAG) {
            String tag = parser.getName();
            String packageName;
            if (tag.equalsIgnoreCase(PACKAGE_TAG)) {
                assertExpectedAttribute(parser, PACKAGE_TAG, NAME_ATTRIBUTE, true);
                packageName = parser.getAttributeValue(null, NAME_ATTRIBUTE);
                if (TextUtils.isEmpty(packageName)) {
                    throw new AppAuthenticatorXmlException(
                            "The " + PACKAGE_TAG + " tag requires a non-empty value for the "
                                    + NAME_ATTRIBUTE + " attribute");
                }
            } else if (tag.equalsIgnoreCase(ALL_PACKAGES_TAG)) {
                packageName = ALL_PACKAGES_TAG;
                if (!allPackagesAllowed) {
                    throw new AppAuthenticatorXmlException("The " + ALL_PACKAGES_TAG
                            + " tag is not allowed within this element on line "
                            + parser.getLineNumber());
                }
            } else {
                throw new AppAuthenticatorXmlException(
                        "Unexpected tag " + tag + " on line " + parser.getLineNumber()
                                + "; expected " + PACKAGE_TAG + "" + (allPackagesAllowed ? " or "
                                + ALL_PACKAGES_TAG : ""));
            }
            Set<String> allowedCertDigests = parseCertDigests(parser);
            if (allowedCertDigests.isEmpty()) {
                throw new AppAuthenticatorXmlException("No " + CERT_DIGEST_TAG + " tag found "
                        + "within " + tag + " element on line " + parser.getLineNumber());
            }
            if (allowedPackageCerts.containsKey(packageName)) {
                allowedPackageCerts.get(packageName).addAll(allowedCertDigests);
            } else {
                allowedPackageCerts.put(packageName, allowedCertDigests);
            }
            eventType = parser.nextTag();
        }
        return allowedPackageCerts;
    }

    /**
     * Parses certificate digests from the provided {@code parser}, returning a {@link Set} of
     * parsed digests.
     *
     * @param parser the {@link XmlPullParser} from which to parser the digests
     * @return a {@code Set} of certificate digests
     * @throws AppAuthenticatorXmlException if the provided XML parsed by the {@code XmlPullParser}
     *                                      is not in the proper format
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      from the {@code XmlPullParser}
     * @throws XmlPullParserException       if any errors are encountered when attempting to
     *                                      parse the provided {@code XmlPullParser}
     */
    private static Set<String> parseCertDigests(XmlPullParser parser)
            throws AppAuthenticatorXmlException, IOException,
            XmlPullParserException {
        Set<String> allowedCertDigests = new ArraySet<>();
        int eventType = parser.nextTag();
        while (eventType == XmlPullParser.START_TAG) {
            String tag = parser.getName();
            if (!tag.equalsIgnoreCase(CERT_DIGEST_TAG)) {
                throw new AppAuthenticatorXmlException(
                        "Expected " + CERT_DIGEST_TAG + " on line " + parser.getLineNumber());
            }
            String digest = parser.nextText().trim();
            if (TextUtils.isEmpty(digest)) {
                throw new AppAuthenticatorXmlException("The " + CERT_DIGEST_TAG + " element "
                        + "on line " + parser.getLineNumber() + " must have non-empty text "
                        + "containing the certificate digest of the signer");
            }
            allowedCertDigests.add(normalizeCertDigest(digest));
            eventType = parser.nextTag();
        }
        return allowedCertDigests;
    }

    /**
     * Normalizes the provided {@code certDigest} to ensure it is in the proper form for {@code
     * Collection} membership checks when comparing a package's signing certificate digest against
     * those provided to the {@code AppAuthenticator}.
     *
     * @param certDigest the digest to be normalized
     * @return a normalized form of the provided digest that can be used in subsequent {@code
     * Collection} membership checks
     */
    static String normalizeCertDigest(String certDigest) {
        // The AppAuthenticatorUtils#computeDigest method uses lower case characters to compute the
        // digest.
        return certDigest.toLowerCase(Locale.US);
    }

    /**
     * Moves the provided {@code parser} to the next {@link XmlPullParser#START_TAG} or {@link
     * XmlPullParser#END_DOCUMENT} if the end of the document is reached, returning the value of
     * the event type.
     */
    private static int parseToNextStartTag(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop to reach the first start tag or end of the document.
        }
        return type;
    }

    /**
     * Asserts the current {@code tagName} contains only the specified {@code expectedAttribute},
     * or no elements if not {@code required}; a null {@code expectedAttribute} can be used to
     * assert no attributes are provided.
     *
     * <p>This method is intended to report if unsupported attributes are specified to warn the
     * caller that the provided value will not be used by this instance. Since this method is
     * checking the attributes it must only be called when the current event type is {@link
     * XmlPullParser#START_TAG}.
     */
    private static void assertExpectedAttribute(XmlPullParser parser, String tagName,
            String expectedAttribute, boolean required)
            throws AppAuthenticatorXmlException, XmlPullParserException {
        int attributeCount = parser.getAttributeCount();
        if (attributeCount == -1) {
            throw new AssertionError(
                    "parser#getAttributeCount called for event type " + parser.getEventType()
                            + " on line " + parser.getLineNumber());
        }
        if (attributeCount == 0 && expectedAttribute != null && required) {
            throw new AppAuthenticatorXmlException("The attribute " + expectedAttribute + " is "
                    + "required for tag " + tagName + " on line " + parser.getLineNumber());
        }
        StringBuilder unsupportedAttributes = null;
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = parser.getAttributeName(i);
            if (!attributeName.equalsIgnoreCase(expectedAttribute)) {
                if (unsupportedAttributes == null) {
                    unsupportedAttributes = new StringBuilder();
                } else {
                    unsupportedAttributes.append(", ");
                }
                unsupportedAttributes.append(attributeName);
            }
        }
        if (unsupportedAttributes != null) {
            String prefixMessage;
            if (expectedAttribute == null) {
                prefixMessage = "Tag " + tagName + " does not support any attributes";
            } else {
                prefixMessage = "Tag " + tagName + " only supports attribute " + expectedAttribute;
            }
            throw new AppAuthenticatorXmlException(
                    prefixMessage + "; found the following unsupported attributes on line "
                            + parser.getLineNumber() + ": " + unsupportedAttributes);
        }
    }

    /**
     * Value class containing the configuration for an {@code AppAuthenticator}.
     */
    // Suppressing the AutoValue immutable field warning as this class is only used internally
    // and is not worth bringing in the dependency for the immutable classes.
    @SuppressWarnings("AutoValueImmutableFields")
    @AutoValue
    abstract static class AppAuthenticatorConfig {
        /**
         * Returns a mapping from permission to allowed packages / signing identities.
         */
        abstract Map<String, Map<String, Set<String>>> getPermissionAllowMap();

        /**
         * Returns a mapping from package name to expected signing identities.
         */
        abstract Map<String, Set<String>> getExpectedIdentities();

        /**
         * Returns the digest algorithm to be used.
         */
        abstract String getDigestAlgorithm();

        /**
         * Creates a new instance with the provided {@code permissionAllowMap}, {@code
         * expectedIdentities}, and {@code digestAlgorithm}.
         *
         * @param permissionAllowMap the mapping from permission to allowed packages / signing
         *                           identities
         * @param expectedIdentities the mapping from package name to expected signing identities
         * @param digestAlgorithm the digest algorithm to be used when computing signing
         *                        certificate digests
         * @return a new {@code AppAuthenticatorConfig} that can be used to configure the
         * AppAuthenticator instance.
         */
        static AppAuthenticatorConfig create(
                Map<String, Map<String, Set<String>>> permissionAllowMap,
                Map<String, Set<String>> expectedIdentities, String digestAlgorithm) {
            return new AutoValue_AppAuthenticator_AppAuthenticatorConfig(permissionAllowMap,
                    expectedIdentities, digestAlgorithm);

        }
    }

    /**
     * Value class for the result of an {@code AppAuthenticator} query.
     */
    @AutoValue
    abstract static class AppAuthenticatorResult {
        /**
         * Returns the result code for the query.
         */
        abstract int getResultCode();

        /**
         * Returns the result message for the query; if the query successfully verified an app's
         * signature matches the expected signing identity this value will be {@code null}.
         */
        @Nullable
        abstract String getResultMessage();

        /**
         * Creates a new instance with the provided {@code resultCode} and {@code resultMessage}.
         */
        static AppAuthenticatorResult create(int resultCode, String resultMessage) {
            return new AutoValue_AppAuthenticator_AppAuthenticatorResult(resultCode, resultMessage);
        }
    }
}
