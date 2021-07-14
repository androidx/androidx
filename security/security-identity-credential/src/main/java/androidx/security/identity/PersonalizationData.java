/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.security.identity;

import android.annotation.SuppressLint;
import android.icu.util.Calendar;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * An object that holds personalization data.
 *
 * This data includes access control profiles and a set of data entries and values, grouped by
 * namespace.
 *
 * This is used to provision data into a {@link WritableIdentityCredential}.
 *
 * @see WritableIdentityCredential#personalize
 */
public class PersonalizationData {

    PersonalizationData() {
    }

    @NonNull ArrayList<AccessControlProfile> mProfiles = new ArrayList<>();

    @NonNull LinkedHashMap<String, NamespaceData> mNamespaces = new LinkedHashMap<>();

    Collection<AccessControlProfile> getAccessControlProfiles() {
        return Collections.unmodifiableCollection(mProfiles);
    }

    Collection<String> getNamespaces() {
        return Collections.unmodifiableCollection(mNamespaces.keySet());
    }

    Collection<NamespaceData> getNamespaceDatas() {
        return Collections.unmodifiableCollection(mNamespaces.values());
    }

    NamespaceData getNamespaceData(String namespace) {
        return mNamespaces.get(namespace);
    }

    static class NamespaceData {

        @NonNull protected String mNamespace;
        @NonNull protected LinkedHashMap<String, EntryData> mEntries = new LinkedHashMap<>();

        protected NamespaceData(@NonNull String namespace) {
            this.mNamespace = namespace;
        }

        String getNamespaceName() {
            return mNamespace;
        }

        Collection<String> getEntryNames() {
            return Collections.unmodifiableCollection(mEntries.keySet());
        }

        Collection<AccessControlProfileId> getAccessControlProfileIds(String name) {
            EntryData value = mEntries.get(name);
            if (value != null) {
                return value.mAccessControlProfileIds;
            }
            return null;
        }

        byte[] getEntryValue(String name) {
            EntryData value = mEntries.get(name);
            if (value != null) {
                return value.mValue;
            }
            return null;
        }
    }

    static class EntryData {
        byte[] mValue;
        Collection<AccessControlProfileId> mAccessControlProfileIds;

        EntryData(byte[] value, Collection<AccessControlProfileId> accessControlProfileIds) {
            this.mValue = value;
            this.mAccessControlProfileIds = accessControlProfileIds;
        }
    }

    /**
     * A builder for {@link PersonalizationData}.
     */
    public static final class Builder {
        private PersonalizationData mData;

        /**
         * Creates a new builder for a given namespace.
         */
        public Builder() {
            this.mData = new PersonalizationData();
        }

        /**
         * Adds a new entry to the builder.
         *
         * <p>This is a low-level method which expects the data to be the bytes of a
         * <a href="https://tools.ietf.org/html/rfc7049">CBOR</a>
         * value. When possible, applications should use methods such as
         * {@link #putEntryString(String, String, Collection, String)} or
         * {@link #putEntryInteger(String, String, Collection, long)} which
         * accept normal Java data types.</p>
         *
         * @param namespace               The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add, in CBOR encoding.
         * @return The builder.
         */
        @SuppressLint("BuilderSetStyle")
        public @NonNull Builder putEntry(@NonNull String namespace, @NonNull String name,
                @NonNull Collection<AccessControlProfileId> accessControlProfileIds,
                @NonNull byte[] value) {
            NamespaceData namespaceData = mData.mNamespaces.get(namespace);
            if (namespaceData == null) {
                namespaceData = new NamespaceData(namespace);
                mData.mNamespaces.put(namespace, namespaceData);
            }
            // TODO: validate/verify that value is proper CBOR.
            namespaceData.mEntries.put(name, new EntryData(value, accessControlProfileIds));
            return this;
        }

        /**
         * Adds a new entry to the builder.
         *
         * <p>This is a convenience method which encodes {@code value} as CBOR and adds the
         * resulting bytes using {@link #putEntry(String, String, Collection, byte[])}. The
         * resulting CBOR will be major type 3 (text string).</p>
         *
         * @param namespace               The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        @SuppressLint("BuilderSetStyle")
        public @NonNull Builder putEntryString(@NonNull String namespace, @NonNull String name,
                @NonNull Collection<AccessControlProfileId> accessControlProfileIds,
                @NonNull String value) {
            return putEntry(namespace, name, accessControlProfileIds, Util.cborEncodeString(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * <p>This is a convenience method which encodes {@code value} as CBOR and adds the
         * resulting bytes using {@link #putEntry(String, String, Collection, byte[])}. The
         * resulting CBOR will be major type 2 (bytestring).</p>
         *
         * @param namespace               The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        @SuppressLint("BuilderSetStyle")
        public @NonNull Builder putEntryBytestring(@NonNull String namespace, @NonNull String name,
                @NonNull Collection<AccessControlProfileId> accessControlProfileIds,
                @NonNull byte[] value) {
            return putEntry(namespace, name, accessControlProfileIds,
                    Util.cborEncodeBytestring(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * <p>This is a convenience method which encodes {@code value} as CBOR and adds the
         * resulting bytes using {@link #putEntry(String, String, Collection, byte[])}. The
         * resulting CBOR will be major type 0 (unsigned integer) if non-negative, otherwise
         * major type 1 (negative integer).</p>
         *
         * @param namespace               The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        @SuppressLint("BuilderSetStyle")
        public @NonNull Builder putEntryInteger(@NonNull String namespace, @NonNull String name,
                @NonNull Collection<AccessControlProfileId> accessControlProfileIds,
                long value) {
            return putEntry(namespace, name, accessControlProfileIds, Util.cborEncodeNumber(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * <p>This is a convenience method which encodes {@code value} as CBOR and adds the
         * resulting bytes using {@link #putEntry(String, String, Collection, byte[])}. The
         * resulting CBOR will be major type 7 (simple value) with additional information 20 (for
         * the value {@code false}) or 21 (for the value {@code true}).</p>
         *
         * @param namespace               The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        @SuppressLint("BuilderSetStyle")
        public @NonNull Builder putEntryBoolean(@NonNull String namespace, @NonNull String name,
                @NonNull Collection<AccessControlProfileId> accessControlProfileIds,
                boolean value) {
            return putEntry(namespace, name, accessControlProfileIds,
                    Util.cborEncodeBoolean(value));
        }

        /**
         * Adds a new entry to the builder.
         *
         * <p>This is a convenience method which encodes {@code value} as CBOR and adds the
         * resulting bytes using {@link #putEntry(String, String, Collection, byte[])}. The
         * resulting CBOR will be a tagged string with tag 0 as per
         * <a href="https://tools.ietf.org/html/rfc7049#section-2.4.1">Section 2.4.1 of RFC
         * 7049</a>. This means that the tagged string follows the standard format described in
         * <a href="https://tools.ietf.org/html/rfc3339">RFC 3339</a>,
         * as refined by Section 3.3 of
         * <a href="https://tools.ietf.org/html/rfc4287">RFC 4287></a>.</p>
         *
         * @param namespace               The namespace to use, e.g. {@code org.iso.18013-5.2019}.
         * @param name                    The name of the entry, e.g. {@code height}.
         * @param accessControlProfileIds A set of access control profiles to use.
         * @param value                   The value to add.
         * @return The builder.
         */
        @SuppressLint("BuilderSetStyle")
        public @NonNull Builder putEntryCalendar(@NonNull String namespace, @NonNull String name,
                @NonNull Collection<AccessControlProfileId> accessControlProfileIds,
                @NonNull Calendar value) {
            return putEntry(namespace, name, accessControlProfileIds,
                    Util.cborEncode(Util.cborBuildDateTime(value)));
        }

        /**
         * Adds a new access control profile to the builder.
         *
         * @param profile The access control profile.
         * @return The builder.
         */
        public @NonNull Builder addAccessControlProfile(@NonNull AccessControlProfile profile) {
            mData.mProfiles.add(profile);
            return this;
        }

        /**
         * Creates a new {@link PersonalizationData} with all the entries added to the builder.
         *
         * @return A new {@link PersonalizationData} instance.
         */
        public @NonNull PersonalizationData build() {
            return mData;
        }
    }

}
