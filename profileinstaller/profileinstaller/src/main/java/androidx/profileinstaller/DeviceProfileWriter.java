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

package androidx.profileinstaller;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * Orchestrate device-level profiler decisions.
 *
 * This class is structured such that it is fast at execution time and avoids allocating extra
 * memory, or reading files multiple times, above api simplicity.
 *
 * Usage:
 *
 * <pre>
 * if (!deviceProfileWriter.deviceAllowsProfileInstallerAotWrites()) {
 *     return; // nothing else to do here
 * }
 * deviceProfileWriter.copyProfileOrRead(skipStrategy)
 *     .transcodeIfNeeded()
 *     .writeIfNeeded(skipStrategy);
 * </pre>
 *
 * @hide
 */
@RequiresApi(19)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DeviceProfileWriter {

    @NonNull
    private final AssetManager mAssetManager;
    @NonNull
    private final ProfileInstaller.Diagnostics mDiagnostics;
    @Nullable
    private final byte[] mDesiredVersion;
    @NonNull
    private final File mCurProfile;
    @NonNull
    private final String mProfileSourceLocation;
    @NonNull
    private final File mRefProfile;
    private boolean mDeviceSupportsAotProfile = false;
    @Nullable
    private Map<String, DexProfileData> mProfile;
    @Nullable
    private byte[] mTranscodedProfile;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public DeviceProfileWriter(@NonNull AssetManager assetManager,
            @NonNull ProfileInstaller.Diagnostics diagnostics,
            @NonNull String profileSourceLocation, @NonNull File curProfile,
            @NonNull File refProfile) {
        mAssetManager = assetManager;
        mDiagnostics = diagnostics;
        mProfileSourceLocation = profileSourceLocation;
        mCurProfile = curProfile;
        mRefProfile = refProfile;
        mDesiredVersion = desiredVersion();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean deviceAllowsProfileInstallerAotWrites() {
        if (mDesiredVersion == null) {
            mDiagnostics.result(ProfileInstaller.RESULT_UNSUPPORTED_ART_VERSION,
                    Build.VERSION.SDK_INT);
            return false;
        }

        if (!mCurProfile.canWrite()) {
            // It's possible that some OEMs might not allow writing to this directory. If this is
            // the case, there's not really anything we can do, so we should quit before doing
            // any unnecessary work.
            mDiagnostics.result(ProfileInstaller.RESULT_NOT_WRITABLE, null);
            return false;
        }

        mDeviceSupportsAotProfile = true;
        return true;
    }

    private void assertDeviceAllowsProfileInstallerAotWritesCalled() {
        if (!mDeviceSupportsAotProfile) {
            throw new IllegalStateException("This device doesn't support aot. Did you call "
                    + "deviceSupportsAotProfile()?");
        }
    }

    /**
     * Attempt to copy the profile, or if it needs transcode it read it.
     *
     * Always call this with transcodeIfNeeded and writeIfNeeded()
     *
     * <pre>
     *     deviceProfileInstaller.copyProfileOrRead(skipStrategy)
     *         .transcodeIfNeeded()
     *         .writeIfNeeded()
     * </pre>
     *
     * @hide
     * @param skipStrategy decide if the profile should be written
     * @return this to chain call to transcodeIfNeeded
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public DeviceProfileWriter copyProfileOrRead(@NonNull SkipStrategy skipStrategy) {
        assertDeviceAllowsProfileInstallerAotWritesCalled();
        byte[] desiredVersion = mDesiredVersion;
        if (desiredVersion == null) {
            return this;
        }
        try (AssetFileDescriptor fd = mAssetManager.openFd(mProfileSourceLocation)) {
            try (InputStream is = fd.createInputStream()) {
                byte[] baselineVersion = ProfileTranscoder.readHeader(is);
                // TODO: this is assuming that the baseline version is the P format. We should
                //  consider whether or not we want to also check for "future" formats, and
                //  assume that if a future format ended up in this file location, that the
                //  platform probably supports it and go ahead and move it to the cur profile
                //  location without parsing anything. For now, a "future" format will just fail
                //  below in the readProfile step.
                boolean transcodingNeeded = !Arrays.equals(desiredVersion, baselineVersion);
                if (transcodingNeeded) {
                    mProfile = ProfileTranscoder.readProfile(is, baselineVersion);
                    return this;
                } else {
                    if (!skipStrategy.shouldSkip(fd.getLength(),
                            generateExistingProfileStateFromFileSystem())) {
                        // just do the copy
                        try (OutputStream os = new FileOutputStream(mCurProfile)) {
                            ProfileTranscoder.writeHeader(os, desiredVersion);
                            Encoding.writeAll(is, os);
                        }
                        mDiagnostics.result(ProfileInstaller.RESULT_INSTALL_SUCCESS, null);
                    }
                }
            }
        }  catch (FileNotFoundException e) {
            mDiagnostics.result(ProfileInstaller.RESULT_BASELINE_PROFILE_NOT_FOUND, e);
        } catch (IOException e) {
            mDiagnostics.result(ProfileInstaller.RESULT_IO_EXCEPTION, e);
        } catch (IllegalStateException e) {
            mDiagnostics.result(ProfileInstaller.RESULT_PARSE_EXCEPTION, e);
        }
        return this;
    }

    /**
     * Attempt to transcode profile, or if it needs transcode it read it.
     *
     * Always call this after copyProfileorRead
     *
     * <pre>
     *     deviceProfileInstaller.copyProfileOrRead(skipStrategy)
     *         .transcodeIfNeeded()
     *         .writeIfNeeded()
     * </pre>
     *
     * This method will always clear the profile read by copyProfileOrRead and may only be called
     * once.
     *
     * @hide
     * @return this to chain call call writeIfNeeded()
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public DeviceProfileWriter transcodeIfNeeded() {
        Map<String, DexProfileData> profile = mProfile;
        byte[] desiredVersion = mDesiredVersion;
        if (profile == null || desiredVersion == null) {
            return this;
        }
        assertDeviceAllowsProfileInstallerAotWritesCalled();
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ProfileTranscoder.writeHeader(os, desiredVersion);
            boolean success = ProfileTranscoder.transcodeAndWriteBody(
                    os,
                    desiredVersion,
                    profile
            );

            if (!success) {
                mDiagnostics.result(ProfileInstaller.RESULT_DESIRED_FORMAT_UNSUPPORTED, null);
                mProfile = null;
                return this;
            }

            mTranscodedProfile = os.toByteArray();
        } catch (IOException e) {
            mDiagnostics.result(ProfileInstaller.RESULT_IO_EXCEPTION, e);
        } catch (IllegalStateException e) {
            mDiagnostics.result(ProfileInstaller.RESULT_PARSE_EXCEPTION, e);
        }
        mProfile = null;
        return this;
    }

    /**
     * Write the transcoded profile generated by transcodeIfNeeded()
     *
     * This method will always clear the profile, and may only be called once.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void writeIfNeeded(@NonNull SkipStrategy skipStrategy) {
        byte[] transcodedProfile = mTranscodedProfile;
        if (transcodedProfile == null) {
            return;
        }
        assertDeviceAllowsProfileInstallerAotWritesCalled();
        if (!skipStrategy.shouldSkip(transcodedProfile.length,
                generateExistingProfileStateFromFileSystem())) {
            try (
                InputStream bis = new ByteArrayInputStream(transcodedProfile);
                OutputStream os = new FileOutputStream(mCurProfile)
            ) {
                Encoding.writeAll(bis, os);
                mDiagnostics.result(ProfileInstaller.RESULT_INSTALL_SUCCESS, null);
            } catch (FileNotFoundException e) {
                mDiagnostics.result(ProfileInstaller.RESULT_BASELINE_PROFILE_NOT_FOUND, e);
            } catch (IOException e) {
                mDiagnostics.result(ProfileInstaller.RESULT_IO_EXCEPTION, e);
            } finally {
                mTranscodedProfile = null;
                mProfile = null;
            }
        }
    }

    private static @Nullable byte[] desiredVersion() {
        // If SDK is pre-N, we don't want to do anything, so return null.
        if (Build.VERSION.SDK_INT < ProfileVersion.MIN_SUPPORTED_SDK) {
            return null;
        }

        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
                return ProfileVersion.V001_N;

            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
                return ProfileVersion.V005_O;

            case Build.VERSION_CODES.P:
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.R:
                return ProfileVersion.V010_P;

            default:
                return null;
        }
    }

    /**
     * This is slow, only call it right before you need to pass it to SkipStrategy
     */
    @NonNull
    private ExistingProfileState generateExistingProfileStateFromFileSystem() {
        return new ExistingProfileState(
                /* curLength */ mCurProfile.length(),
                /* refLength */ mRefProfile.length(),
                /* curExists */ mCurProfile.exists(),
                /* refExists */mRefProfile.exists()
        );
    }

    /**
     * Provide a skip strategy to DeviceProfileWriter, to avoid writing profiles basod on any
     * heuristic.
     */
    public interface SkipStrategy {

        /**
         * Return true if this profile write should be skipped.
         *
         * @param newProfileLength length of profile to write
         * @param existingProfileState current on-disk profile information
         * @return false to write profile, true to skip
         */
        boolean shouldSkip(long newProfileLength,
                @NonNull ExistingProfileState existingProfileState);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class ExistingProfileState {
        private final long mCurLength;
        private final long mRefLength;
        private final boolean mCurExists;
        private final boolean mRefExists;

        ExistingProfileState(long curLength, long refLength, boolean curExists,
                boolean refExists) {
            mCurLength = curLength;
            mRefLength = refLength;
            mCurExists = curExists;
            mRefExists = refExists;
        }

        /**
         * @return length of existing cur profile
         */
        public long getCurLength() {
            return mCurLength;
        }

        /**
         * @return length of existing ref profile
         */
        public long getRefLength() {
            return mRefLength;
        }

        /**
         * @return true if cur file exists
         */
        public boolean hasCurFile() {
            return mCurExists;
        }

        /**
         * @return true if ref file exists
         */
        public boolean hasRefFile() {
            return mRefExists;
        }
    }
}
