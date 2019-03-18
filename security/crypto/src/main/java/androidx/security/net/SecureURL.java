/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.security.net;


import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;
import androidx.security.config.TldConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

/**
 * A URL that provides TLS, certification validity checks, and TLD verification automatically.
 */
@TargetApi(Build.VERSION_CODES.N)
public class SecureURL {

    private static final String TAG = "SecureURL";

    private URL mUrl;
    private SecureConfig mSecureConfig;
    private String mClientCertAlias;

    public SecureURL(@NonNull String spec)
            throws MalformedURLException {
        this(spec, null, SecureConfig.getDefault());
    }

    public SecureURL(@NonNull String spec, @NonNull String clientCertAlias)
            throws MalformedURLException {
        this(spec, clientCertAlias, SecureConfig.getDefault());
    }

    public SecureURL(@NonNull String spec, @NonNull String clientCertAlias,
            @NonNull SecureConfig secureConfig)
            throws MalformedURLException {
        this.mUrl = new URL(addProtocol(spec));
        this.mClientCertAlias = clientCertAlias;
        this.mSecureConfig = secureConfig;
    }


    /**
     * Gets the hostname used to construct the underlying URL.
     *
     * @return the hostname associated with the Url.
     */
    @NonNull
    public String getHostname() {
        return this.mUrl.getHost();
    }

    /**
     * Gets the port used to construct the underlying URL.
     *
     * @return the port associated with the Url.
     */
    public int getPort() {
        int port = this.mUrl.getPort();
        if (port == -1) {
            port = this.mUrl.getDefaultPort();
        }
        return port;
    }

    private String addProtocol(@NonNull String spec) {
        if (!spec.toLowerCase().startsWith("http://")
                && !spec.toLowerCase().startsWith("https://")) {
            return "https://" + spec;
        }
        return spec;
    }


    /**
     * Gets the client cert alias.
     *
     * @return The client cert alias.
     */
    @NonNull
    public String getClientCertAlias() {
        return this.mClientCertAlias;
    }


    /**
     * Opens a connection using default certs with a custom SSLSocketFactory.
     *
     * @return the UrlConnection of the newly opened connection.
     * @throws IOException
     */
    @NonNull
    public URLConnection openConnection() throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) this.mUrl.openConnection();
        urlConnection.setSSLSocketFactory(new ValidatableSSLSocketFactory(this));
        return urlConnection;
    }

    /**
     * Opens a connection using the provided trusted list of CAs.
     *
     * @param trustedCAs list of CAs to be trusted by this connection
     * @return The opened connection
     * @throws IOException
     */
    @NonNull
    public URLConnection openUserTrustedCertConnection(
            @NonNull Map<String, InputStream> trustedCAs)
            throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) this.mUrl.openConnection();
        urlConnection.setSSLSocketFactory(new ValidatableSSLSocketFactory(this,
                trustedCAs, mSecureConfig));
        return urlConnection;
    }

    /**
     * Checks the hostname against an open SSLSocket connect to the hostname for validity for certs
     * and hostname validity. Only used internally by ValidatableSSLSocket.
     * <p>
     * Example Code:
     * SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
     * SSLSocket socket = (SSLSocket) sf.createSocket("https://"+hostname, 443);
     * socket.startHandshake();
     * boolean valid = SecurityExt.isValid(hostname, socket);
     * </p>
     *
     * @param hostname The host name to check
     * @param socket   The SSLSocket that is open to the URL of the host to check
     * @return true if the SSLSocket has a valid cert and if the hostname is valid, false otherwise.
     */
    boolean isValid(@NonNull String hostname, @NonNull SSLSocket socket) {
        try {
            Log.i(TAG, "Hostname verifier: " + HttpsURLConnection
                    .getDefaultHostnameVerifier().verify(hostname, socket.getSession()));
            Log.i(TAG, "isValid Peer Certs: "
                    + isValid(Arrays.asList(socket.getSession().getPeerCertificates())));
            return HttpsURLConnection.getDefaultHostnameVerifier()
                    .verify(hostname, socket.getSession())
                    && isValid(Arrays.asList(socket.getSession().getPeerCertificates()))
                    && validTldWildcards(Arrays.asList(socket.getSession().getPeerCertificates()));
        } catch (SSLPeerUnverifiedException e) {
            Log.i(TAG, "Valid Check failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks the HttpsUrlConnection certificates for validity.
     * <p>
     * Example Code:
     * SecureURL mUrl = new SecureURL("https://" + host);
     * conn = (HttpsURLConnection) mUrl.openConnection();
     * boolean valid = SecurityExt.isValid(conn);
     * </p>
     *
     * @param conn The connection to check the certificates of
     * @return true if the certificates for the HttpsUrlConnection are valid, false otherwise
     */
    public boolean isValid(@NonNull HttpsURLConnection conn) {
        try {
            return isValid(Arrays.asList(conn.getServerCertificates()))
                    && validTldWildcards(Arrays.asList(conn.getServerCertificates()));
        } catch (SSLPeerUnverifiedException e) {
            Log.i(TAG, "Valid Check failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Internal method to check a list of certificates for validity.
     *
     * @param certs list of certs to check
     * @return true if the certs are valid, false otherwise
     */
    private boolean isValid(@NonNull List<? extends Certificate> certs) {
        try {
            List<Certificate> leafCerts = new ArrayList<>();
            for (Certificate cert : certs) {
                if (!isRootCA(cert)) {
                    leafCerts.add(cert);
                }
            }
            CertPath path = CertificateFactory.getInstance(mSecureConfig.getCertPath())
                    .generateCertPath(leafCerts);
            KeyStore ks = KeyStore.getInstance(mSecureConfig.getAndroidCAStore());
            try {
                ks.load(null, null);
            } catch (IOException e) {
                e.printStackTrace();
                throw new AssertionError(e);
            }
            CertPathValidator cpv = CertPathValidator.getInstance(mSecureConfig
                    .getCertPathValidator());
            PKIXParameters params = new PKIXParameters(ks);
            PKIXRevocationChecker checker = (PKIXRevocationChecker) cpv.getRevocationChecker();
            checker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));
            params.addCertPathChecker(checker);
            cpv.validate(path, params);
            return true;
        } catch (CertPathValidatorException e) {
            // If this message prints out "Unable to determine revocation status due to
            // network error"
            // Make sure your network security config allows for clear text access of the relevant
            // OCSP mUrl.
            e.printStackTrace();
            return false;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal method to check if a cert is a CA.
     *
     * @param cert The cert to check
     * @return true if the cert is a RootCA, false otherwise
     */
    private boolean isRootCA(@NonNull Certificate cert) {
        boolean rootCA = false;
        if (cert instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate) cert;
            if (x509Certificate.getSubjectDN().getName().equals(
                    x509Certificate.getIssuerDN().getName())) {
                rootCA = true;
            }
        }
        return rootCA;
    }


    private boolean validTldWildcards(@NonNull List<? extends Certificate> certs) {
        // For a more complete list https://publicsuffix.org/list/public_suffix_list.dat
        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate) {
                X509Certificate x509Cert = (X509Certificate) cert;
                try {
                    Collection<List<?>> subAltNames = x509Cert.getSubjectAlternativeNames();
                    if (subAltNames != null) {
                        List<String> dnsNames = new ArrayList<>();
                        for (List<?> tldList : subAltNames) {
                            if (tldList.size() >= 2) {
                                dnsNames.add(tldList.get(1).toString().toUpperCase());
                            }
                        }
                        // Populate DNS NAMES, make sure they are lower case
                        for (String dnsName : dnsNames) {
                            if (TldConstants.VALID_TLDS.contains(dnsName)) {
                                Log.i(TAG, "FAILED WILDCARD TldConstants CHECK: " + dnsName);
                                return false;
                            }
                        }
                    }
                } catch (CertificateParsingException ex) {
                    Log.i(TAG, "Cert Parsing Issue: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

}
