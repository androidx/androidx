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

package androidx.security.identity;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.icu.util.Calendar;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Map;

/**
 * An object that contains the result of retrieving data from a credential. This is used to return
 * data requested from a {@link IdentityCredential}.
 */
public abstract class ResultData {

    /** Value was successfully retrieved. */
    public static final int STATUS_OK = 0;

    /** Requested entry does not exist. */
    public static final int STATUS_NO_SUCH_ENTRY = 1;

    /** Requested entry was not requested. */
    public static final int STATUS_NOT_REQUESTED = 2;

    /** Requested entry wasn't in the request message. */
    public static final int STATUS_NOT_IN_REQUEST_MESSAGE = 3;

    /** The requested entry was not retrieved because user authentication wasn't performed. */
    public static final int STATUS_USER_AUTHENTICATION_FAILED = 4;

    /** The requested entry was not retrieved because reader authentication wasn't performed. */
    public static final int STATUS_READER_AUTHENTICATION_FAILED = 5;

    /**
     * The requested entry was not retrieved because it was configured without any access
     * control profile.
     */
    public static final int STATUS_NO_ACCESS_CONTROL_PROFILES = 6;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    protected ResultData() {}

    /**
     * Returns a CBOR structure containing the retrieved data.
     *
     * <p>This structure - along with the session transcript - may be cryptographically
     * authenticated to prove to the reader that the data is from a trusted credential and
     * {@link #getMessageAuthenticationCode()} can be used to get a MAC.
     *
     * <p>The CBOR structure which is cryptographically authenticated is the
     * {@code DeviceAuthenticationBytes} structure according to the following
     * <a href="https://tools.ietf.org/html/rfc8610">CDDL</a> schema:
     *
     * <pre>
     *   DeviceAuthentication = [
     *     "DeviceAuthentication",
     *     SessionTranscript,
     *     DocType,
     *     DeviceNameSpacesBytes
     *   ]
     *
     *   DocType = tstr
     *   SessionTranscript = any
     *   DeviceNameSpacesBytes = #6.24(bstr .cbor DeviceNameSpaces)
     *   DeviceAuthenticationBytes = #6.24(bstr .cbor DeviceAuthentication)
     * </pre>
     *
     * <p>where
     *
     * <pre>
     *   DeviceNameSpaces = {
     *     * NameSpace => DeviceSignedItems
     *   }
     *
     *   DeviceSignedItems = {
     *     + DataItemName => DataItemValue
     *   }
     *
     *   NameSpace = tstr
     *   DataItemName = tstr
     *   DataItemValue = any
     * </pre>
     *
     * <p>The returned data is the binary encoding of the {@code DeviceNameSpaces} structure
     * as defined above.
     *
     * @return The bytes of the {@code DeviceNameSpaces} CBOR structure.
     */
    public abstract @NonNull byte[] getAuthenticatedData();

    /**
     * Returns a message authentication code over the {@code DeviceAuthenticationBytes} CBOR
     * specified in {@link #getAuthenticatedData()}, to prove to the reader that the data
     * is from a trusted credential.
     *
     * <p>The MAC proves to the reader that the data is from a trusted credential. This code is
     * produced by using the key agreement and key derivation function from the ciphersuite
     * with the authentication private key and the reader ephemeral public key to compute a
     * shared message authentication code (MAC) key, then using the MAC function from the
     * ciphersuite to compute a MAC of the authenticated data. See section 9.2.3.5 of
     * ISO/IEC 18013-5 for details of this operation.
     *
     * <p>If the {@code sessionTranscript} parameter passed to
     * {@link IdentityCredential#getEntries(byte[], Map, byte[])}  was {@code null}
     * or the reader ephmeral public key was never set using
     * {@link IdentityCredential#setReaderEphemeralPublicKey(PublicKey)}, no message
     * authencation code will be produced and this method will return {@code null}.
     *
     * At most one of {@link #getMessageAuthenticationCode()} or {@link #getEcdsaSignature()} is
     * implemented.
     *
     * @return A COSE_Mac0 structure with the message authentication code as described above
     *         or {@code null} if the conditions specified above are not met.
     */
    public abstract @Nullable byte[] getMessageAuthenticationCode();

    /**
     * Returns a digital signature over the {@code DeviceAuthenticationBytes} CBOR
     * specified in {@link #getAuthenticatedData()}, to prove to the reader that the data
     * is from a trusted credential. The signature will be made with one of the provisioned
     * dynamic authentication keys.
     *
     * At most one of {@link #getMessageAuthenticationCode()} or {@link #getEcdsaSignature()} is
     * implemented.
     *
     * @return {@code null} if not implemented, otherwise a COSE_Sign1 structure with the payload
     *         set to the data returned by {@link #getAuthenticatedData()}.
     */
    public abstract @Nullable byte[] getEcdsaSignature();

    /**
     * Returns the static authentication data associated with the dynamic authentication
     * key used to sign or MAC the data returned by {@link #getAuthenticatedData()}.
     *
     * @return The static authentication data associated with dynamic authentication key used to
     * MAC the data.
     */
    public abstract @NonNull byte[] getStaticAuthenticationData();

    /**
     * Gets the names of namespaces with retrieved entries.
     *
     * @return collection of name of namespaces containing retrieved entries. May be empty if no
     *     data was retrieved.
     */
    public abstract @NonNull Collection<String> getNamespaces();

    /**
     * Get the names of all entries.
     *
     * This includes the name of entries that wasn't successfully retrieved.
     *
     * @param namespaceName the namespace name to get entries for.
     * @return A collection of names or {@code null} if there are no entries for the given
     *     namespace.
     */
    public abstract @Nullable Collection<String> getEntryNames(@NonNull String namespaceName);

    /**
     * Get the names of all entries that was successfully retrieved.
     *
     * This only return entries for which {@link #getStatus(String, String)} will return
     * {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name to get entries for.
     * @return A collection of names or {@code null} if there are no entries for the given
     *     namespace.
     */
    public abstract @Nullable Collection<String> getRetrievedEntryNames(
            @NonNull String namespaceName);

    /**
     * Gets the status of an entry.
     *
     * This returns {@link #STATUS_OK} if the value was retrieved, {@link #STATUS_NO_SUCH_ENTRY}
     * if the given entry wasn't retrieved, {@link #STATUS_NOT_REQUESTED} if it wasn't requested,
     * {@link #STATUS_NOT_IN_REQUEST_MESSAGE} if the request message was set but the entry wasn't
     * present in the request message,
     * {@link #STATUS_USER_AUTHENTICATION_FAILED} if the value
     * wasn't retrieved because the necessary user authentication wasn't performed,
     * {@link #STATUS_READER_AUTHENTICATION_FAILED} if the supplied reader certificate chain
     * didn't match the set of certificates the entry was provisioned with, or
     * {@link #STATUS_NO_ACCESS_CONTROL_PROFILES} if the entry was configured without any
     * access control profiles.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return the status indicating whether the value was retrieved and if not, why.
     */
    public abstract @Status int getStatus(@NonNull String namespaceName, @NonNull String name);

    /**
     * Gets the raw CBOR data for the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String, String)}
     * method returns {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return the raw CBOR data or {@code null} if no entry with the given name exists.
     */
    public abstract @Nullable byte[] getEntry(@NonNull String namespaceName, @NonNull String name);

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String, String)}
     * method returns {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return a {@code String} or {@code null} if no entry with the given name exists.
     */
    public @Nullable String getEntryString(@NonNull String namespaceName, @NonNull String name) {
        byte[] value = getEntry(namespaceName, name);
        if (value == null) {
            return null;
        }
        return Util.cborDecodeString(value);
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String, String)}
     * method returns {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return a {@code byte[]} or {@code null} if no entry with the given name exists.
     */
    public @Nullable byte[] getEntryBytestring(@NonNull String namespaceName,
            @NonNull String name) {
        byte[] value = getEntry(namespaceName, name);
        if (value == null) {
            return null;
        }
        return Util.cborDecodeByteString(value);
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String, String)}
     * method returns {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return a {@code long} or 0 if no entry with the given name exists.
     */
    public long getEntryInteger(@NonNull String namespaceName, @NonNull String name) {
        byte[] value = getEntry(namespaceName, name);
        if (value == null) {
            return 0;
        }
        return Util.cborDecodeLong(value);
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String, String)}
     * method returns {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return a {@code boolean} or {@code false} if no entry with the given name exists.
     */
    public boolean getEntryBoolean(@NonNull String namespaceName, @NonNull String name) {
        byte[] value = getEntry(namespaceName, name);
        if (value == null) {
            return false;
        }
        return Util.cborDecodeBoolean(value);
    }

    /**
     * Gets the value of an entry.
     *
     * This should only be called on an entry for which the {@link #getStatus(String, String)}
     * method returns {@link #STATUS_OK}.
     *
     * @param namespaceName the namespace name of the entry.
     * @param name the name of the entry to get the value for.
     * @return a {@code Calendar} or {@code null} if no entry with the given name exists.
     */
    public @Nullable
    Calendar getEntryCalendar(@NonNull String namespaceName,
            @NonNull String name) {
        byte[] value = getEntry(namespaceName, name);
        if (value == null) {
            return null;
        }
        return Util.cborDecodeDateTime(value);
    }

    /**
     * The type of the entry status.
     * @hide
     */
    @Retention(SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({STATUS_OK, STATUS_NO_SUCH_ENTRY, STATUS_NOT_REQUESTED, STATUS_NOT_IN_REQUEST_MESSAGE,
                        STATUS_USER_AUTHENTICATION_FAILED, STATUS_READER_AUTHENTICATION_FAILED,
                        STATUS_NO_ACCESS_CONTROL_PROFILES})
    public @interface Status {
    }
}
