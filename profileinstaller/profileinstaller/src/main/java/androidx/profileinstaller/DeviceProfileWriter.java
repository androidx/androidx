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

import static androidx.profileinstaller.ProfileTranscoder.MAGIC_PROF;
import static androidx.profileinstaller.ProfileTranscoder.MAGIC_PROFM;

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
import java.util.concurrent.Executor;

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
    private final Executor mExecutor;
    @NonNull
    private final ProfileInstaller.DiagnosticsCallback mDiagnostics;
    @Nullable
    private final byte[] mDesiredVersion;
    @NonNull
    private final File mCurProfile;
    @NonNull
    private final String mApkName;
    @NonNull
    private final String mProfileSourceLocation;
    @NonNull
    private final String mProfileMetaSourceLocation;
    private boolean mDeviceSupportsAotProfile = false;
    @Nullable
    private DexProfileData[] mProfile;
    @Nullable
    private byte[] mTranscodedProfile;

    private void result(@ProfileInstaller.ResultCode int code, @Nullable Object data) {
        mExecutor.execute(() -> mDiagnostics.onResultReceived(code, data));
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public DeviceProfileWriter(
            @NonNull AssetManager assetManager,
            @NonNull Executor executor,
            @NonNull ProfileInstaller.DiagnosticsCallback diagnosticsCallback,
            @NonNull String apkName,
            @NonNull String profileSourceLocation,
            @NonNull String profileMetaSourceLocation,
            @NonNull File curProfile
    ) {
        mAssetManager = assetManager;
        mExecutor = executor;
        mDiagnostics = diagnosticsCallback;
        mApkName = apkName;
        mProfileSourceLocation = profileSourceLocation;
        mProfileMetaSourceLocation = profileMetaSourceLocation;
        mCurProfile = curProfile;
        mDesiredVersion = desiredVersion();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean deviceAllowsProfileInstallerAotWrites() {
        if (mDesiredVersion == null) {
            result(ProfileInstaller.RESULT_UNSUPPORTED_ART_VERSION, Build.VERSION.SDK_INT);
            return false;
        }

        if (!mCurProfile.canWrite()) {
            // It's possible that some OEMs might not allow writing to this directory. If this is
            // the case, there's not really anything we can do, so we should quit before doing
            // any unnecessary work.
            result(ProfileInstaller.RESULT_NOT_WRITABLE, null);
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
     *     deviceProfileInstaller.read()
     *         .transcodeIfNeeded()
     *         .write()
     * </pre>
     *
     * @hide
     * @return this to chain call to transcodeIfNeeded
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public DeviceProfileWriter read() {
        assertDeviceAllowsProfileInstallerAotWritesCalled();
        if (mDesiredVersion == null) {
            return this;
        }
        try (AssetFileDescriptor fd = mAssetManager.openFd(mProfileSourceLocation)) {
            try (InputStream is = fd.createInputStream()) {
                byte[] baselineVersion = ProfileTranscoder.readHeader(is, MAGIC_PROF);
                mProfile = ProfileTranscoder.readProfile(is, baselineVersion, mApkName);
            }
        }  catch (FileNotFoundException e) {
            mDiagnostics.onResultReceived(ProfileInstaller.RESULT_BASELINE_PROFILE_NOT_FOUND, e);
        } catch (IOException e) {
            mDiagnostics.onResultReceived(ProfileInstaller.RESULT_IO_EXCEPTION, e);
        } catch (IllegalStateException e) {
            mDiagnostics.onResultReceived(ProfileInstaller.RESULT_PARSE_EXCEPTION, e);
        }
        DexProfileData[] profile = mProfile;
        if (profile != null && requiresMetadata()) {
            try (AssetFileDescriptor fd = mAssetManager.openFd(mProfileMetaSourceLocation)) {
                try (InputStream is = fd.createInputStream()) {
                    byte[] metaVersion = ProfileTranscoder.readHeader(is, MAGIC_PROFM);
                    mProfile = ProfileTranscoder.readMeta(
                            is,
                            metaVersion,
                            profile
                    );
                    return this;
                }
            } catch (FileNotFoundException e) {
                mDiagnostics.onResultReceived(
                        ProfileInstaller.RESULT_META_FILE_REQUIRED_BUT_NOT_FOUND,
                        e
                );
            } catch (IOException e) {
                mDiagnostics.onResultReceived(ProfileInstaller.RESULT_IO_EXCEPTION, e);
            } catch (IllegalStateException e) {
                mDiagnostics.onResultReceived(ProfileInstaller.RESULT_PARSE_EXCEPTION, e);
            }
        }
        return this;
    }

    /**
     * Attempt to transcode profile, or if it needs transcode it read it.
     *
     * Always call this after read
     *
     * <pre>
     *     deviceProfileInstaller.read()
     *         .transcodeIfNeeded()
     *         .write()
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
        DexProfileData[] profile = mProfile;
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
                mDiagnostics.onResultReceived(
                        ProfileInstaller.RESULT_DESIRED_FORMAT_UNSUPPORTED,
                        null
                );
                mProfile = null;
                return this;
            }

            mTranscodedProfile = os.toByteArray();
        } catch (IOException e) {
            mDiagnostics.onResultReceived(ProfileInstaller.RESULT_IO_EXCEPTION, e);
        } catch (IllegalStateException e) {
            mDiagnostics.onResultReceived(ProfileInstaller.RESULT_PARSE_EXCEPTION, e);
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
    public boolean write() {
        byte[] transcodedProfile = mTranscodedProfile;
        if (transcodedProfile == null) {
            return false;
        }
        assertDeviceAllowsProfileInstallerAotWritesCalled();
        try (
            InputStream bis = new ByteArrayInputStream(transcodedProfile);
            OutputStream os = new FileOutputStream(mCurProfile)
        ) {
            Encoding.writeAll(bis, os);
            result(ProfileInstaller.RESULT_INSTALL_SUCCESS, null);
            return true;
        } catch (FileNotFoundException e) {
            result(ProfileInstaller.RESULT_BASELINE_PROFILE_NOT_FOUND, e);
        } catch (IOException e) {
            result(ProfileInstaller.RESULT_IO_EXCEPTION, e);
        } finally {
            mTranscodedProfile = null;
            mProfile = null;
        }
        return false;
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
                return ProfileVersion.V005_O;
            case Build.VERSION_CODES.O_MR1:
                return ProfileVersion.V009_O_MR1;

            case Build.VERSION_CODES.P:
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.R:
                return ProfileVersion.V010_P;

            default:
                return null;
        }
    }

    private static boolean requiresMetadata() {
        // If SDK is pre-N, we don't want to do anything, so return null.
        if (Build.VERSION.SDK_INT < ProfileVersion.MIN_SUPPORTED_SDK) {
            return false;
        }

        switch (Build.VERSION.SDK_INT) {
            // The profiles for N and N_MR1 used class ids to identify classes instead of type
            // ids, which is what the V0.1.0 profile encodes, so a metadata file is required in
            // order to transcode to this profile.
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
                return true;

            // for all of these versions, the data encoded in the V0.1.0 profile is enough to
            // losslessly transcode into these other formats.
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
            case Build.VERSION_CODES.P:
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.R:
            default:
                return false;
        }
    }
}
