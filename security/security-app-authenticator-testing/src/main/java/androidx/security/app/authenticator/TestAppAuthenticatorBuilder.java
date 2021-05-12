/*
 * Copyright 2021 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Binder;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Builder class that can be used to facilitate the creation of a new {@link AppAuthenticator} which
 * can be configured to meet the requirements of the test. Similar to the {@code AppAuthenticator},
 * the static factory methods for this class require either an XML resource or {@link InputStream}
 * containing the {@code app-authenticator} configuration allowing verification of your declared
 * config as part of the test.
 *
 * <p>There are several options to configure the behavior of the resulting {@code AppAuthenticator}.
 * <ul>
 *     <li>{@link #setTestPolicy(int)} - This sets a generic test policy. {@link
 *     #POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES} will cause the {@code AppAuthenticator}
 *     to always return that a queried package has the expected signing identity as long as it is
 *     explicitly declared in your configuration; that is, the package must be declared in a
 *     {@code package} element within either an {@code expected-identity} or {@code permission}
 *     element. {@link #POLICY_DENY_ALL} will cause the {@code AppAuthenticator} to always return
 *     that a queried package does not have the expected signing identity regardless of its
 *     declaration. These two policies can be used to verify good path and error path for
 *     scenarios where the package names can be explicitly declared in the XML configuration.
 *     <p>{@code POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES} is the default policy when no
 *     other options are configured. When any of the other set methods (except for {@link
 *     #setUidForPackage(String, int)}) are invoked they will set the policy to {@link
 *     #POLICY_CUSTOM}.
 *     </li>
 *     <li>{@link #setSignatureAcceptedForPackage(String)} - This configures the {@code
 *     AppAuthenticator} to always return that the specified package has the expected signing
 *     identity. Note this still requires the {@code app-authenticator} have a path to verify
 *     the provided package; that is, the package must either be explicitly declared in a
 *     {@code package} element or fall under a {@code all-packages} element for the query being
 *     performed. This is to ensure that a package being verified during the test could also be
 *     successfully verified in production for the given query.
 *     </li>
 *     <li>{@link #setSigningIdentityForPackage(String, String)} - This sets an explicit
 *     signing identity for the provided package; the signing identity should be
 *     specified as the SHA-256 digest of the DER encoding of the signing certificate, similar
 *     to how digests are specified in the {@code app-authenticator} configuration file. While
 *     this can be used to set a signing identity to the expected value, this is more often
 *     used to set the signing identity to a value that should not be accepted. For instance, a
 *     test suite could have a test that verifies a key that is no longer trusted is never
 *     added back to the configuration file.
 *     </li>
 *     <li>{@link #setPackageNotInstalled(String)} - This configures the {@code AppAuthenticator}
 *     to treat the specified package as not installed on the device. Since a package that is not
 *     installed can result in a different return code from the {@code AppAuthenticator} methods
 *     this configuration can be used to verify an app's behavior when an expected app is not
 *     installed on the device.
 *     </li>
 *     <li>{@link #setUidForPackage(String, int)} - The {@code AppAuthenticator} will
 *     always verify the UID of the calling package matches the specified UID (or
 *     {@link Binder#getCallingUid()} if a UID is not specified). By default this test {@code
 *     AppAuthenticator} will use the result of {@code Binder#getCallingUid()} as the UID of all
 *     queried packages. This method can be used to verify the expected behavior when a calling
 *     package's UID does not match the expected UID.
 *     </li>
 * </ul>
 */
// The purpose of this class is to build a configurable AppAuthenticator for tests so the builder
// is the top level class.
@SuppressLint("TopLevelBuilder")
public final class TestAppAuthenticatorBuilder {
    private Context mContext;
    private XmlPullParser mParser;
    private @TestPolicy int mTestPolicy;
    private TestAppSignatureVerifier.Builder mAppSignatureVerifierBuilder;
    private TestAppAuthenticatorUtils.Builder mAppAuthenticatorUtilsBuilder;

    /**
     * Private constructor that should only be called by the static factory methods.
     *
     * @param context the context within which to create the {@link AppAuthenticator}
     * @param parser  an {@link XmlPullParser} containing the definitions for the
     *                permissions and expected identities based on package / expected signing
     *                certificate digests
     */
    private TestAppAuthenticatorBuilder(Context context, XmlPullParser parser) {
        mContext = context;
        mParser = parser;
        mTestPolicy = POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES;
        mAppSignatureVerifierBuilder = new TestAppSignatureVerifier.Builder(context);
        mAppAuthenticatorUtilsBuilder = new TestAppAuthenticatorUtils.Builder(mContext);
    }

    /**
     * This test policy will cause the AppAuthenticator to return a successful signing identity for
     * all packages explicitly declared in the XML configuration. This is the default policy used
     * when a new {@code AppAuthenticator} is built without calling {@link
     * #setSigningIdentityForPackage(String, String)}, {@link
     * #setSignatureAcceptedForPackage(String)}, and {@link #setPackageNotInstalled(String)}.
     */
    public static final int POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES = 1;
    /**
     * This test policy will cause the AppAuthenticator to return that the signing identity of
     * the package does that match the expect identity from the XML configuration for all queried
     * packages.
     */
    public static final int POLICY_DENY_ALL = 2;
    /**
     * This test policy indicates that the caller will specify the expected results for each
     * package individually. This is the default policy used when a new {@code TestAppAuthenticator}
     * is built after calling any of the following:
     * {@link #setSigningIdentityForPackage(String, String)}, {@link
     * #setSignatureAcceptedForPackage(String)}, and {@link #setPackageNotInstalled(String)}.
     * Once the policy has been set to this value it cannot be changed to any of the other policies.
     */
    public static final int POLICY_CUSTOM = 3;

    @IntDef(value = {
            POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES,
            POLICY_DENY_ALL,
            POLICY_CUSTOM,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface TestPolicy {
    }

    /**
     * Returns a new {@link TestAppAuthenticatorBuilder} that can be used to create a new {@link
     * AppAuthenticator} configured to behave as required for the test.
     *
     * @param context     the context within which to create the {@link AppAuthenticator}
     * @param xmlResource the ID of the XML resource containing the definitions for the
     *                    permissions and expected identities based on package / expected signing
     *                    certificate digests
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     */
    // This is not a setter for the builder but instead a static factory method to obtain a new
    // builder.
    @SuppressLint("BuilderSetStyle")
    @NonNull
    public static TestAppAuthenticatorBuilder createFromResource(@NonNull Context context,
            @XmlRes int xmlResource) {
        Resources resources = context.getResources();
        XmlPullParser parser = resources.getXml(xmlResource);
        return new TestAppAuthenticatorBuilder(context, parser);
    }

    /**
     * Returns a new {@link TestAppAuthenticatorBuilder} that can be used to create a new {@link
     * AppAuthenticator} configured to behave as required for the test.
     *
     * @param context        the context within which to create the {@link AppAuthenticator}
     * @param xmlInputStream the XML {@link InputStream} containing the definitions for the
     *                       permissions and expected identities based on packages / expected
     *                       signing certificate digests
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     */
    // This is not a setter for the builder but instead a static factory method to obtain a new
    // builder.
    @SuppressLint("BuilderSetStyle")
    @NonNull
    public static TestAppAuthenticatorBuilder createFromInputStream(
            @NonNull Context context,
            @NonNull InputStream xmlInputStream)
            throws AppAuthenticatorXmlException {
        XmlPullParser parser;
        try {
            parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(xmlInputStream, null);
        } catch (XmlPullParserException e) {
            throw new AppAuthenticatorXmlException("Unable to create parser from provided "
                    + "InputStream", e);
        }
        return new TestAppAuthenticatorBuilder(context, parser);
    }

    /**
     * Sets the policy to be used by the {@link AppAuthenticator} for the test.
     *
     * @param testPolicy the test policy to be used by the {@code AppAuthenticator{}
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     * @see #POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES
     * @see #POLICY_DENY_ALL
     * @see #POLICY_CUSTOM
     */
    // The builder allows configuring other options that are not directly controlled by the
    // AppAuthenticator.
    @SuppressLint("MissingGetterMatchingBuilder")
    public @NonNull TestAppAuthenticatorBuilder setTestPolicy(@TestPolicy int testPolicy) {
        mTestPolicy = testPolicy;
        return this;
    }

    /**
     * Configures the resulting {@link AppAuthenticator} to always return that the signing
     * identity matches the expected value when the specified {@code packageName} is queried.
     *
     * <p>Note, the specified {@code packageName} must be defined either explicitly via a
     * {@code package} element or implicitly via a {@code all-packages} element; this ensures
     * that the XML configuration is correct and that the specified package could be verified
     * on device.
     *
     * @param packageName the name of the package for which the signing identity should be
     *                    treated as matching the expected value
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     */
    // The builder allows configuring other options that are not directly controlled by the
    // AppAuthenticator.
    @SuppressLint("MissingGetterMatchingBuilder")
    @NonNull
    public TestAppAuthenticatorBuilder setSignatureAcceptedForPackage(
            @NonNull String packageName) {
        mTestPolicy = POLICY_CUSTOM;
        mAppSignatureVerifierBuilder.setSignatureAcceptedForPackage(packageName);
        return this;
    }

    /**
     * Sets the provided {@code certDigest} as the signing identity for the specified {@code
     * packageName}.
     *
     * @param packageName the name of the package that will use the provided signing identity
     * @param certDigest  the digest to be treated as the signing identity of the specified package
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     */
    // The builder allows configuring other options that are not directly controlled by the
    // AppAuthenticator.
    @SuppressLint("MissingGetterMatchingBuilder")
    @NonNull
    public TestAppAuthenticatorBuilder setSigningIdentityForPackage(
            @NonNull String packageName,
            @NonNull String certDigest) {
        mTestPolicy = POLICY_CUSTOM;
        mAppSignatureVerifierBuilder.setSigningIdentityForPackage(packageName, certDigest);
        return this;
    }

    /**
     * Sets the provided {@code uid} as the UID of the specified {@code packageName}.
     *
     * <p>This method can be used to verify the scenario where a calling package does not have the
     * expected calling UID.
     *
     * @param packageName the name of the package that will be treated as having the provided uid
     * @param uid         the uid to use for the specified package
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     */
    // The builder allows configuring other options that are not directly controlled by the
    // AppAuthenticator.
    @SuppressLint("MissingGetterMatchingBuilder")
    @NonNull
    public TestAppAuthenticatorBuilder setUidForPackage(@NonNull String packageName,
            int uid) {
        mAppAuthenticatorUtilsBuilder.setUidForPackage(packageName, uid);
        return this;
    }

    /**
     * Treats the provided {@code packageName} as not being installed by the resulting {@link
     * AppAuthenticator}.
     *
     * @param packageName the name of the package to be treated as not installed
     * @return this instance of the {@code TestAppAuthenticatorBuilder}
     */
    // The builder allows configuring other options that are not directly controlled by the
    // AppAuthenticator.
    @SuppressLint("MissingGetterMatchingBuilder")
    @NonNull
    public TestAppAuthenticatorBuilder setPackageNotInstalled(
            @NonNull String packageName) {
        mTestPolicy = POLICY_CUSTOM;
        mAppAuthenticatorUtilsBuilder.setPackageNotInstalled(packageName);
        mAppSignatureVerifierBuilder.setPackageNotInstalled(packageName);
        return this;
    }

    /**
     * Builds an {@link AppAuthenticator} with the specified config that can be injected to satisfy
     * test requirements.
     *
     * @return a new {@code AppAuthenticator} that will respond to queries as configured
     * @throws AppAuthenticatorXmlException if the provided XML config file is not in the proper
     *                                      format to create a new {@code AppAuthenticator}
     * @throws IOException                  if an IO error is encountered when attempting to read
     *                                      the XML config file
     */
    // This class is provided so that apps can inject a configurable AppAuthenticator for their
    // tests, so it needs access to the restricted test APIs.
    @SuppressLint("RestrictedApi")
    @NonNull
    public AppAuthenticator build() throws AppAuthenticatorXmlException, IOException {
        // Obtain the config from the AppAuthenticator class to ensure that the provided XML is
        // properly configured.
        AppAuthenticator.AppAuthenticatorConfig config =
                AppAuthenticator.createConfigFromParser(mParser);

        // Configure the AppSignatureVerifier that will by the test AppAuthenticator.
        mAppSignatureVerifierBuilder.setPermissionAllowMap(config.getPermissionAllowMap());
        mAppSignatureVerifierBuilder.setExpectedIdentities(config.getExpectedIdentities());
        mAppSignatureVerifierBuilder.setTestPolicy(mTestPolicy);

        // Inject the AppSignatureVerifier and AppAuthenticatorUtils into the AppAuthenticator
        // to configure it to behave as requested.
        AppAuthenticator appAuthenticator = AppAuthenticator.createFromConfig(mContext, config);
        appAuthenticator.setAppSignatureVerifier(mAppSignatureVerifierBuilder.build());
        appAuthenticator.setAppAuthenticatorUtils(mAppAuthenticatorUtilsBuilder.build());
        return appAuthenticator;
    }
}
