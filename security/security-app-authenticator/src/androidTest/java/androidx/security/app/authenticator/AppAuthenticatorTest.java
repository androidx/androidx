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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;

import androidx.security.app.authenticator.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public final class AppAuthenticatorTest {
    private Context mContext;
    private Resources mResources;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mResources = mContext.getResources();
    }

    @Test
    public void createFrom_singleExpectedIdentity() throws Exception {
        // An XML resource with a single expected-identity tag should parse successfully.
        AppAuthenticator.createFromResource(mContext, R.xml.single_expected_identity);
        AppAuthenticator.createFromInputStream(mContext,
                mResources.openRawResource(R.raw.single_expected_identity));
    }

    @Test
    public void createFromInputStream_noRootElement() throws Exception {
        // This test verifies if the caller is attempting to create a new AppAuthenticator with
        // an empty XML InputStream then an appropriate Exception is thrown. Note, only the
        // InputStream static factory method is used as an empty XML file is not added to the
        // resources when under the res/xml directory.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.no_root_element)));
    }

    @Test
    public void createFrom_invalidRootElement() throws Exception {
        // An XML resource with a root element other than app-authenticator should result in an
        // Exception.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext, R.xml.invalid_root_element));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.invalid_root_element)));
    }

    @Test
    public void createFrom_invalidDigestAlgorithm() throws Exception {
        // Since the platform currently requires all certificate digests use the SHA-256 digest
        // algorithm when using the knownSigner permission flag an error should be reported if
        // an attempt is made to specify any other digest algorithm.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext, R.xml.invalid_digest_algo));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.invalid_digest_algo)));
    }

    @Test
    public void createFrom_multipleUnsupportedAttributes() throws Exception {
        // The static factory methods will verify tags contain only supported attributes to notify
        // the caller if they are expecting behavior based on the values for the attributes. This
        // test verifies if multiple unsupported attributes are specified then an Exception is
        // thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.multiple_unsupported_attributes));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.multiple_unsupported_attributes)));
    }

    @Test
    public void createFrom_invalidExpectedIdentityAttribute() throws Exception {
        // The expected-identity tag does not support any attributes; this test verifies if an
        // attribute is specified then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.invalid_expected_identity_attribute));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.invalid_expected_identity_attribute)));
    }

    @Test
    public void createFrom_allPackagesTagInExpectedIdentity() throws Exception {
        // The expected-identity element does not support the all-packages element since it is
        // expected that a caller is specifying this tag to verify the identity of a specific app.
        // This test verifies if an all-packages tag is within the expected-identity element then an
        // Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.all_packages_expected_identity));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.all_packages_expected_identity)));
    }

    @Test
    public void createFrom_invalidTagWithinRootElement() throws Exception {
        // The XML should only contain permission and expected-identity tags under the root element;
        // this test verifies if an invalid tag is specified then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.invalid_tag_within_root_element));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.invalid_tag_within_root_element)));
    }

    @Test
    public void createFrom_invalidTagWithinPermissionElement() throws Exception {
        // The permission element supports both the package and all-packages tags; this test
        // verifies if another tag is specified then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.invalid_tag_within_permission_element));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.invalid_tag_within_permission_element)));
    }

    @Test
    public void createFrom_invalidTagWithinPackageElement() throws Exception {
        // The package / all-packages element require one or more cert-digest elements containing
        // the certificate digests of the signer. This test verifies if another tag is specified
        // under one of these elements then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.invalid_tag_within_package_element));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.invalid_tag_within_package_element)));
    }

    @Test
    public void createFrom_noCertDigestWithinPackageElement() throws Exception {
        // The package / all-packages element require one or more cert-digest elements containing
        // the certificate digests of the signer. This test verifies is no cert-digest is
        // specified under one of these elements then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.no_cert_digest_within_package_element));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.no_cert_digest_within_package_element)));
    }

    @Test
    public void createFrom_emptyCertDigestElement() throws Exception {
        // The package / all-packages element require one or more cert-digest elements containing
        // the certificate digests of the signer. This test verifies if the cert-digest element
        // does not contain any text then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.empty_cert_digest_element));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.empty_cert_digest_element)));
    }

    @Test
    public void createFrom_emptyPermissionElement() throws Exception {
        // A permission element could potentially be left empty during development if a developer
        // wishes to guard a resource behind a permission but does not yet know the expected
        // signing identity of the callers. This test ensures the static factory methods properly
        // handle an empty permission declaration.
        AppAuthenticator.createFromResource(mContext, R.xml.empty_permission_element);
        AppAuthenticator.createFromInputStream(mContext,
                mResources.openRawResource(R.raw.empty_permission_element));
    }

    @Test
    public void createFrom_multipleCertDigestElements() throws Exception {
        // The package / all-packages element allow multiple cert-digest elements to support key
        // rotation and multiple signers. This test verifies an AppAuthenticator can be
        // successfully instantiated when the provided XML contains multiple cert-digest elements
        // within a package element.
        AppAuthenticator.createFromResource(mContext, R.xml.multiple_cert_digest_elements);
        AppAuthenticator.createFromInputStream(mContext,
                mResources.openRawResource(R.raw.multiple_cert_digest_elements));
    }

    @Test
    public void createFrom_multiplePackagesUnderPermission() throws Exception {
        // The permission element supports one or more packages; this test verifies a new
        // AppAuthenticator can be successfully instantiated when there are multiple packages
        // within the permission element.
        AppAuthenticator.createFromResource(mContext, R.xml.multiple_packages_under_permission);
        AppAuthenticator.createFromInputStream(mContext,
                mResources.openRawResource(R.raw.multiple_packages_under_permission));
    }

    @Test
    public void createFrom_noNameAttributeInPackageTag() throws Exception {
        // The package tag requires a non-empty name attribute; this test verifies if the name
        // attribute is not specified then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.no_name_attribute_in_package_tag));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.no_name_attribute_in_package_tag)));
    }

    @Test
    public void createFrom_emptyNameAttributeInPackageTag() throws Exception {
        // The package tag requires a non-empty name attribute; this test verifies if the name
        // attribute is specified but empty then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.empty_name_attribute_in_package_tag));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.empty_name_attribute_in_package_tag)));
    }

    @Test
    public void createFrom_noNameAttributeInPermissionTag() throws Exception {
        // The permission tag requires a non-empty name attribute to identify the permission; this
        // test verifies if the name attribute is not specified then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.no_name_attribute_in_permission_tag));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.no_name_attribute_in_permission_tag)));
    }

    @Test
    public void createFrom_emptyNameAttributeInPermissionTag() throws Exception {
        // The package tag requires a non-empty name attribute; this test verifies if the name
        // attribute is specified but empty then an Exception is thrown.
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromResource(mContext,
                        R.xml.empty_name_attribute_in_permission_tag));
        assertThrows(AppAuthenticatorXmlException.class,
                () -> AppAuthenticator.createFromInputStream(mContext,
                        mResources.openRawResource(R.raw.empty_name_attribute_in_permission_tag)));
    }

    @Test
    public void createFrom_allSupportedElementsAndAttributes() throws Exception {
        // This test verifies a new AppAuthenticator instance can be instantiated when the
        // provided XML contains all of the supported elements and attributes.
        AppAuthenticator.createFromResource(mContext,
                R.xml.all_supported_elements_and_attributes);
        AppAuthenticator.createFromInputStream(mContext,
                mResources.openRawResource(R.raw.all_supported_elements_and_attributes));
    }

    @Test
    public void createConfigFromParser_allSupportedElements_returnsExpectedValues()
            throws Exception {
        // The AppAuthenticator contains a static method that can be used to obtain the
        // configuration parsed from the provided XML. This test verifies that the returned
        // config contains all of the expected configuration from an XML that uses all of the
        // supported elements and attributes.
        AppAuthenticator.AppAuthenticatorConfig config =
                AppAuthenticator.createConfigFromParser(
                        mResources.getXml(R.xml.all_supported_elements_and_attributes));
        Map<String, Set<String>> expectedIdentities = config.getExpectedIdentities();
        Map<String, Map<String, Set<String>>> permissionAllowMap = config.getPermissionAllowMap();
        Map<String, Set<String>> allowedPackageCerts = permissionAllowMap.get("androidx.security"
                + ".app.authenticator.TEST_PERMISSION");

        assertEquals(1, expectedIdentities.get("com.bank.app").size());
        assertTrue(expectedIdentities.get("com.bank.app").contains(
                "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8"));
        assertEquals(2, expectedIdentities.get("com.social.app").size());
        assertTrue(expectedIdentities.get("com.social.app").contains(
                "6a8b96e278e58f62cfe3584022cec1d0527fcb85a9e5d2e1694eb0405be5b599"));
        assertTrue(expectedIdentities.get("com.social.app").contains(
                "d78405f761ff6236cc9b570347a570aba0c62a129a3ac30c831c64d09ad95469"));
        assertEquals(1, permissionAllowMap.size());
        assertEquals(3, allowedPackageCerts.size());
        assertEquals(2, allowedPackageCerts.get(AppAuthenticator.ALL_PACKAGES_TAG).size());
        assertTrue(allowedPackageCerts.get(AppAuthenticator.ALL_PACKAGES_TAG).contains(
                "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8"));
        assertTrue(allowedPackageCerts.get(AppAuthenticator.ALL_PACKAGES_TAG).contains(
                "681b0e56a796350c08647352a4db800cc44b2adc8f4c72fa350bd05d4d50264d"));
        assertEquals(1, allowedPackageCerts.get("com.android.app1").size());
        assertTrue(allowedPackageCerts.get("com.android.app1").contains(
                "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8"));
        assertEquals(2, allowedPackageCerts.get("com.android.app2").size());
        assertTrue(allowedPackageCerts.get("com.android.app2").contains(
                "6a8b96e278e58f62cfe3584022cec1d0527fcb85a9e5d2e1694eb0405be5b599"));
        assertTrue(allowedPackageCerts.get("com.android.app2").contains(
                "d78405f761ff6236cc9b570347a570aba0c62a129a3ac30c831c64d09ad95469"));
        assertEquals("SHA-256", config.getDigestAlgorithm());
    }

    @Test
    public void createConfigFromParser_upperCaseDigestInConfig_returnsMatch() throws Exception {
        // The digest computed by the AppAuthenticatorUtils is in lower case, but the
        // AppAuthenticator supports matching digests provided in upper case as well.
        // This test does not directly verify the digest of a package's signing certificate
        // but instead uses the bytes from the package name in the identity; this test ensures
        // the AppAuthenticator properly normalizes the provided digest so that it matches the
        // digest returned by AppAuthenticatorUtils.
        final String packageName = "com.example.app";
        AppAuthenticator.AppAuthenticatorConfig config = AppAuthenticator.createConfigFromParser(
                mResources.getXml(R.xml.upper_case_digest));
        Set<String> expectedPackageIdentities = config.getExpectedIdentities().get(packageName);

        assertTrue(expectedPackageIdentities.contains(
                AppAuthenticatorUtils.computeDigest(AppAuthenticator.DEFAULT_DIGEST_ALGORITHM,
                        packageName.getBytes())));
    }
}
