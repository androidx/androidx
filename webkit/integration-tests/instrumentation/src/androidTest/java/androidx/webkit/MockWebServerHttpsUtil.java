/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webkit;

import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Assert;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Scanner;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

/**
 * Helper class to enable HTTPS connections on {@link okhttp3.mockwebserver.MockWebServer}
 */
public final class MockWebServerHttpsUtil {

    /**
     * Enables HTTPS on the server, using the {@code res/raw/test_certificate.pem} certificate.
     * Must be called before {@link MockWebServer#start()}
     */
    public static void enableHttps(@NonNull MockWebServer server) {
        server.useHttps(getTestCertificate().sslSocketFactory(), false);
    }

    /**
     * Loads the SSL test certificate from resources.
     */
    @NonNull
    private static HandshakeCertificates getTestCertificate() {
        HeldCertificate testCertificate = HeldCertificate.decode(readCertificateFile());
        try {
            testCertificate.certificate().checkValidity();
        } catch (CertificateExpiredException e) {
            logFreshTestCertificate();
            Assert.fail("The test_certificate.pem certificate has expired. Check logcat for a new "
                    + "certificate.");
        } catch (CertificateNotYetValidException e) {
            Log.w("ContentFilterHeaderTest", e);
            Assert.fail(e.getMessage());
        }
        return new HandshakeCertificates.Builder().heldCertificate(
                testCertificate).addTrustedCertificate(testCertificate.certificate()).build();
    }

    @NonNull
    private static String readCertificateFile() {
        try (Scanner scanner = new Scanner(getResources().openRawResource(
                androidx.webkit.instrumentation.test.R.raw.test_certificate))) {
            // The \Z delimiter is "end of string", i.e. matches the whole file.
            scanner.useDelimiter("\\Z");
            return scanner.next();
        }
    }

    private static Resources getResources() {
        return InstrumentationRegistry.getInstrumentation().getContext().getResources();
    }

    private static void logFreshTestCertificate() {
        // The test_certificate.pem file is generated with this code.
        // The certificate generated is set to expire in year 9999, so it should not expire.
        // If the certificate is lost or broken, this code can be used to recreate the certificate.
        // Requires Android O to use ZonedDateTime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // See https://datatracker.ietf.org/doc/html/rfc5280#section-4.1.2.5 for why this value.
            ZonedDateTime notAfterTimestamp = ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0,
                    ZoneId.of("Z"));
            HeldCertificate heldCertificate = new HeldCertificate.Builder().commonName(
                    "localhost").validityInterval(0,
                    notAfterTimestamp.toInstant().toEpochMilli()).addSubjectAlternativeName(
                    "localhost").build();
            Log.d("NEW_TEST_CERTIFICATE",
                    heldCertificate.certificatePem() + heldCertificate.privateKeyPkcs8Pem());
        }
    }

    private MockWebServerHttpsUtil() {
        // Do not instantiate.
    }

}
