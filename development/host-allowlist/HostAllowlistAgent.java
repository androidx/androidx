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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Set;
import java.util.stream.Stream;

/** Java agent that only lets the JVM resolve the host names in ALLOWED. */
public class HostAllowlistAgent extends InetAddressResolverProvider implements InetAddressResolver {
    private static final Set<String> ALLOWED =
            Set.of(
                    "localhost", // loopback, never leaves this machine
                    "storage.googleapis.com", // GCP remote build cache (Cloud Storage)
                    "oauth2.googleapis.com", // refreshing the build cache's access token
                    "www.googleapis.com", // validating that token (gcpbuildcache's TokenInfo)
                    "metadata.google.internal", // build cache credentials on GCE machines
                    "androidx.develocity.cloud", // Develocity build scans
                    "secretmanager.googleapis.com", // Develocity access key
                    "update.code.visualstudio.com"); // latest VS Code version, for ./codew
    private static InetAddressResolver builtin;
    // This machine's own name (not "localhost"), which InetAddress.getLocalHost() resolves
    private static String hostName;

    /**
     * The first time any code looks up a host name, Java picks which class answers all lookups from
     * then on. We want that class to be this one. If Gradle code made the first lookup, Java could
     * not see this jar and would pick its own class instead. So we make the first lookup here,
     * before Gradle starts. The name we look up does not matter. It only triggers the pick. This
     * class answers it with "not allowed", and we ignore that.
     */
    public static void premain(String args) {
        try {
            InetAddress.getByName("host-allowlist.invalid");
        } catch (UnknownHostException expected) {
        }
        if (builtin == null) {
            System.err.println("The AndroidX host allowlist could not be installed");
            System.exit(1);
        }
    }

    @Override
    public InetAddressResolver get(Configuration configuration) {
        builtin = configuration.builtinResolver();
        hostName = configuration.lookupLocalHostName();
        return this;
    }

    @Override
    public String name() {
        return "androidx-host-allowlist";
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy policy)
            throws UnknownHostException {
        if (!ALLOWED.contains(host) && !host.equals(hostName)) {
            throw new UnknownHostException(
                    host
                            + " is not on the AndroidX build's host allowlist."
                            + " Set ALLOW_PUBLIC_REPOS=true to allow it.");
        }
        return builtin.lookupByName(host, policy);
    }

    @Override
    public String lookupByAddress(byte[] address) throws UnknownHostException {
        return builtin.lookupByAddress(address);
    }
}
