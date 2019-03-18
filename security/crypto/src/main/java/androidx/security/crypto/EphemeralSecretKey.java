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


package androidx.security.crypto;

import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.GCMParameterSpec;

/**
 * A destroyable SecretKey. Can be completely evicted from memory on deletion.
 */
public class EphemeralSecretKey implements KeySpec, SecretKey {

    private static final long serialVersionUID = 7177238317307289223L;
    private static final String TAG = "EphemeralSecretKey";

    private byte[] mKey;

    private String mAlgorithm;

    private boolean mKeyDestroyed;

    private SecureConfig mSecureConfig;

    public EphemeralSecretKey(@NonNull byte[] key) {
        this(key, KeyProperties.KEY_ALGORITHM_AES, SecureConfig.getDefault());
    }

    public EphemeralSecretKey(@NonNull byte[] key, @NonNull String algorithm) {
        this(key, algorithm, SecureConfig.getDefault());
    }

    public EphemeralSecretKey(@NonNull byte[] key, @NonNull SecureConfig secureConfig) {
        this(key, KeyProperties.KEY_ALGORITHM_AES, secureConfig);
    }

    /**
     * Constructs a secret mKey from the given byte array.
     *
     * <p>This constructor does not check if the given bytes indeed specify a
     * secret mKey of the specified mAlgorithm. For example, if the mAlgorithm is
     * DES, this constructor does not check if <code>mKey</code> is 8 bytes
     * long, and also does not check for weak or semi-weak keys.
     * In order for those checks to be performed, an mAlgorithm-specific
     * <i>mKey specification</i> class (in this case:
     * {@link DESKeySpec DESKeySpec})
     * should be used.
     *
     * @param key       the mKey material of the secret mKey. The contents of
     *                  the array are copied to protect against subsequent modification.
     * @param algorithm the name of the secret-mKey mAlgorithm to be associated
     *                  with the given mKey material.
     * @throws IllegalArgumentException if <code>mAlgorithm</code>
     *                                  is null or <code>mKey</code> is null or empty.
     */
    public EphemeralSecretKey(@NonNull byte[] key, @NonNull String algorithm,
            @NonNull SecureConfig secureConfig) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty mKey");
        }
        this.mKey = key;
        this.mAlgorithm = algorithm;
        this.mKeyDestroyed = false;
        this.mSecureConfig = secureConfig;
    }

    /**
     * Constructs a secret mKey from the given byte array, using the first
     * <code>len</code> bytes of <code>mKey</code>, starting at
     * <code>offset</code> inclusive.
     *
     * <p> The bytes that constitute the secret mKey are
     * those between <code>mKey[offset]</code> and
     * <code>mKey[offset+len-1]</code> inclusive.
     *
     * <p>This constructor does not check if the given bytes indeed specify a
     * secret mKey of the specified mAlgorithm. For example, if the mAlgorithm is
     * DES, this constructor does not check if <code>mKey</code> is 8 bytes
     * long, and also does not check for weak or semi-weak keys.
     * In order for those checks to be performed, an mAlgorithm-specific mKey
     * specification class (in this case:
     * {@link DESKeySpec DESKeySpec})
     * must be used.
     *
     * @param key       the mKey material of the secret mKey. The first
     *                  <code>len</code> bytes of the array beginning at
     *                  <code>offset</code> inclusive are copied to protect
     *                  against subsequent modification.
     * @param offset    the offset in <code>mKey</code> where the mKey material
     *                  starts.
     * @param len       the length of the mKey material.
     * @param algorithm the name of the secret-mKey mAlgorithm to be associated
     *                  with the given mKey material.
     * @throws IllegalArgumentException       if <code>mAlgorithm</code>
     *                                        is null or <code>mKey</code> is null,
     *                                        empty, or too short,
     *                                        i.e. {@code mKey.length-offset<len}.
     * @throws ArrayIndexOutOfBoundsException is thrown if
     *                                        <code>offset</code> or <code>len</code>
     *                                        index bytes outside the
     *                                        <code>mKey</code>.
     */
    public EphemeralSecretKey(@NonNull byte[] key, int offset, int len,
            @NonNull String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty mKey");
        }
        if (key.length - offset < len) {
            throw new IllegalArgumentException("Invalid offset/length combination");
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        this.mKey = new byte[len];
        System.arraycopy(key, offset, this.mKey, 0, len);
        this.mAlgorithm = algorithm;
        this.mKeyDestroyed = false;
    }

    /**
     * Returns the name of the mAlgorithm associated with this secret mKey.
     *
     * @return the secret mKey mAlgorithm.
     */
    @NonNull
    public String getAlgorithm() {
        return this.mAlgorithm;
    }

    /**
     * Returns the name of the encoding format for this secret mKey.
     *
     * @return the string "RAW".
     */
    @NonNull
    public String getFormat() {
        return "RAW";
    }

    /**
     * Returns the mKey material of this secret mKey.
     *
     * @return the mKey material. Returns a new array
     * each time this method is called.
     */
    @NonNull
    public byte[] getEncoded() {
        return this.mKey;
    }

    /**
     * Calculates a hash code value for the object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode() {
        int retval = 0;
        for (int i = 1; i < this.mKey.length; i++) {
            retval += this.mKey[i] * i;
        }
        if (this.mAlgorithm.equalsIgnoreCase("TripleDES")) {
            return (retval ^= "desede".hashCode());
        } else {
            return (retval ^= this.mAlgorithm.toLowerCase(Locale.ENGLISH).hashCode());
        }
    }

    /**
     * Tests for equality between the specified object and this
     * object. Two SecretKeySpec objects are considered equal if
     * they are both SecretKey instances which have the
     * same case-insensitive mAlgorithm name and mKey encoding.
     *
     * @param obj the object to test for equality with this object.
     * @return true if the objects are considered equal, false if
     * <code>obj</code> is null or otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof SecretKey)) {
            return false;
        }

        String thatAlg = ((SecretKey) obj).getAlgorithm();
        if (!(thatAlg.equalsIgnoreCase(this.mAlgorithm))) {
            if ((!(thatAlg.equalsIgnoreCase("DESede"))
                    || !(this.mAlgorithm.equalsIgnoreCase("TripleDES")))
                    && (!(thatAlg.equalsIgnoreCase("TripleDES"))
                    || !(this.mAlgorithm.equalsIgnoreCase("DESede")))) {
                return false;
            }
        }

        byte[] thatKey = ((SecretKey) obj).getEncoded();

        return MessageDigest.isEqual(this.mKey, thatKey);
    }

    @Override
    public void destroy() {
        if (!mKeyDestroyed) {
            Arrays.fill(mKey, (byte) 0);
            this.mKey = null;
            mKeyDestroyed = true;
        }
    }


    /**
     * Manually destroy the cipher by zeroing out all instances of the mKey.
     *
     * @param cipher The Cipher used with this mKey.
     * @param opmode The opmode of the cipher.
     */
    public void destroyCipherKey(@NonNull Cipher cipher, int opmode) {
        try {
            byte[] blankKey = new byte[mSecureConfig.getSymmetricKeySize() / 8];
            byte[] iv = new byte[SecureConfig.AES_IV_SIZE_BYTES];
            Arrays.fill(blankKey, (byte) 0);
            Arrays.fill(iv, (byte) 0);
            EphemeralSecretKey blankSecretKey =
                    new EphemeralSecretKey(blankKey, mSecureConfig.getSymmetricKeyAlgorithm());
            cipher.init(opmode, blankSecretKey,
                    new GCMParameterSpec(mSecureConfig.getSymmetricGcmTagLength(), iv));
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Could not destroy mKey.");
        }
    }

    @Override
    public boolean isDestroyed() {
        return mKeyDestroyed;
    }
}

