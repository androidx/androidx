/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_NO_PROFILE;
import static androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Provides API to verify whether a compilation profile was installed with the app. This does not
 * make a distinction between cloud or baseline profile. The output of
 * {@link #getCompilationStatusAsync()}  allows to check if the app has been compiled with a
 * compiled profile or whether there is a profile enqueued for compilation.
 *
 * If {@link ProfileInstallerInitializer} was disabled, it's necessary to manually trigger the
 * method {@link #writeProfileVerification(Context)} or the {@link ListenableFuture} returned by
 * {@link ProfileVerifier#getCompilationStatusAsync()} will hang or timeout.
 *
 * Note that {@link ProfileVerifier} requires {@link Build.VERSION_CODES#P} due to a permission
 * issue: the reference profile folder is not accessible to pre api 28. When calling this api on
 * unsupported api, {@link #getCompilationStatusAsync()} returns
 * {@link CompilationStatus#RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION}. The same permission issue
 * exists also on {@link Build.VERSION_CODES#R} so also in that case the api returns
 * {@link CompilationStatus#RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION}.
 */
public final class ProfileVerifier {
    private static final String REF_PROFILES_BASE_DIR = "/data/misc/profiles/ref/";
    private static final String CUR_PROFILES_BASE_DIR = "/data/misc/profiles/cur/0/";
    private static final String PROFILE_FILE_NAME = "primary.prof";
    private static final String PROFILE_INSTALLED_CACHE_FILE_NAME = "profileInstalled";
    private static final ResolvableFuture<CompilationStatus> sFuture = ResolvableFuture.create();
    private static final Object SYNC_OBJ = new Object();
    private static final String TAG = "ProfileVerifier";

    @Nullable
    private static CompilationStatus sCompilationStatus = null;

    private ProfileVerifier() {
    }

    /**
     * Caches the information on whether a reference profile exists for this app. This method
     * performs IO operations and should not be executed on main thread. Note that this method
     * should be called manually a few seconds after app startup if
     * {@link  ProfileInstallerInitializer} has been disabled.
     *
     * @param context an instance of the {@link Context}.
     * @return the {@link CompilationStatus} of the app profile. Note that this is the same
     * {@link CompilationStatus} obtained through {@link #getCompilationStatusAsync()}.
     */
    @WorkerThread
    @NonNull
    public static CompilationStatus writeProfileVerification(@NonNull Context context
    ) {
        return writeProfileVerification(context, false);
    }

    /**
     * Caches the information on whether a reference profile exists for this app. This method
     * performs IO operations and should not be executed on main thread. This specific api is for
     * internal usage of this package only. The flag {@code forceVerifyCurrentProfile} should
     * be triggered only when installing from broadcast receiver to force a current profile
     * verification.
     *
     * @param context                   an instance of the {@link Context}.
     * @param forceVerifyCurrentProfile requests a force verification for current profile. This
     *                                  should be used when installing profile through
     *                                  {@link ProfileInstallReceiver}.
     * @return the {@link CompilationStatus} of the app profile. Note that this is the same
     * {@link CompilationStatus} obtained through {@link #getCompilationStatusAsync()}.
     */
    @NonNull
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static CompilationStatus writeProfileVerification(
            @NonNull Context context,
            boolean forceVerifyCurrentProfile
    ) {

        // `forceVerifyCurrentProfile` can force a verification for the current profile only.
        // Current profile can be installed at any time through the ProfileInstallerReceiver so
        // the cached result won't work.
        if (!forceVerifyCurrentProfile && sCompilationStatus != null) {
            return sCompilationStatus;
        }

        synchronized (SYNC_OBJ) {

            if (!forceVerifyCurrentProfile && sCompilationStatus != null) {
                return sCompilationStatus;
            }

            // ProfileVerifier supports only api 28 and above.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                    || Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                return setCompilationStatus(
                        RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION,
                        false,
                        false
                );
            }

            // Check reference profile file existence. Note that when updating from a version with
            // profile to a version without profile, a new reference profile of size zero is
            // created. This should be equivalent to no reference profile.
            File referenceProfileFile = new File(
                    new File(REF_PROFILES_BASE_DIR, context.getPackageName()), PROFILE_FILE_NAME);
            long referenceProfileSize = referenceProfileFile.length();
            boolean hasReferenceProfile =
                    referenceProfileFile.exists() && referenceProfileSize > 0;

            // Check current profile file existence
            File currentProfileFile = new File(
                    new File(CUR_PROFILES_BASE_DIR, context.getPackageName()), PROFILE_FILE_NAME);
            long currentProfileSize = currentProfileFile.length();
            boolean hasCurrentProfile =
                    currentProfileFile.exists() && currentProfileSize > 0;

            // Checks package last update time that will be used to determine whether the app
            // has been updated.
            long packageLastUpdateTime;
            try {
                packageLastUpdateTime = getPackageLastUpdateTime(context);
            } catch (PackageManager.NameNotFoundException e) {
                return setCompilationStatus(
                        RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST,
                        hasReferenceProfile,
                        hasCurrentProfile
                );
            }

            // Reads the current profile verification cache file
            File cacheFile = new File(context.getFilesDir(), PROFILE_INSTALLED_CACHE_FILE_NAME);
            Cache currentCache = null;
            if (cacheFile.exists()) {
                try {
                    currentCache = Cache.readFromFile(cacheFile);
                } catch (IOException e) {
                    return setCompilationStatus(
                            RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ,
                            hasReferenceProfile,
                            hasCurrentProfile
                    );
                }
            }

            // Here it's calculated the result code, initially set to either the latest saved value
            // or `no profile exists`
            int resultCode;

            // There are 2 profiles: reference and current. These 2 are handled differently.
            // The reference profile can be installed only by package manager or app Store.
            // This can be assessed only at first app start or app updates (i.e. when the package
            // info last update has changed). After the first install a reference profile can be
            // created as a result of bg dex opt.

            // Check if this is a first start or an update or the previous profile was awaiting
            // compilation.
            if (currentCache == null
                    || currentCache.mPackageLastUpdateTime != packageLastUpdateTime
                    || currentCache.mResultCode
                    == RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION) {

                // If so, reevaluate if the app has a reference profile and whether a current
                // profile has been installed (since this runs after profile installer).
                if (hasReferenceProfile) {
                    resultCode = RESULT_CODE_COMPILED_WITH_PROFILE;
                } else if (hasCurrentProfile) {
                    resultCode = RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION;
                } else {
                    resultCode = RESULT_CODE_NO_PROFILE;
                }
            } else {

                // If not, utilize the cached result since the reference profile might be the result
                // of a bg dex opt.
                resultCode = currentCache.mResultCode;
            }

            // A current profile can be installed by the profile installer also through broadcast,
            // therefore if this was a forced installation it can happen at anytime. the flag
            // `forceVerifyCurrentProfile` can request a force verification for the current
            // profile only.
            if (forceVerifyCurrentProfile && hasCurrentProfile
                    && resultCode != RESULT_CODE_COMPILED_WITH_PROFILE) {
                resultCode = RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION;
            }

            // If a profile has just been compiled, verify if the size matches between reference
            // and current matches.
            if (currentCache != null
                    && (currentCache.mResultCode == RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                    && resultCode == RESULT_CODE_COMPILED_WITH_PROFILE) {

                // If there is an issue with the profile compilation, the reference profile size
                // might be smaller than the current profile installed by profileinstaller. Note
                // that this is not 100% accurate and it may return the wrong information if the
                // portion of current profile added to the installed current profile, when the
                // user uses the app, is larger than the installed current profile itself.

                // The size of the reference profile should be at least the same in current if
                // the compilation worked. Otherwise something went wrong. Note that on some api
                // levels the reference profile file may not be visible to the app, so size
                // cannot be read.
                if (referenceProfileSize < currentCache.mInstalledCurrentProfileSize) {
                    resultCode = RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING;
                }
            }

            // We now have a new verification result.
            Cache newCache = new Cache(
                    /* schema = */ Cache.SCHEMA,
                    /* resultCode = */ resultCode,
                    /* packageLastUpdateTime = */ packageLastUpdateTime,
                    /* installedCurrentProfileSize = */ currentProfileSize
            );

            // At this point we can cache the result if there was no cache file or if the result has
            // changed (for example due to a force install).
            if (currentCache == null || !currentCache.equals(newCache)) {
                try {
                    newCache.writeOnFile(cacheFile);
                } catch (IOException e) {
                    resultCode =
                            RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE;
                }
            }

            // Set and report the calculated value
            return setCompilationStatus(resultCode, hasReferenceProfile, hasCurrentProfile);
        }
    }

    private static CompilationStatus setCompilationStatus(
            int resultCode,
            boolean hasReferenceProfile,
            boolean hasCurrentProfile
    ) {
        sCompilationStatus = new CompilationStatus(
                /* resultCode = */ resultCode,
                /* hasReferenceProfile */ hasReferenceProfile,
                /* hasCurrentProfile */ hasCurrentProfile
        );
        sFuture.set(sCompilationStatus);
        return sCompilationStatus;
    }

    @SuppressWarnings("deprecation")
    private static long getPackageLastUpdateTime(Context context)
            throws PackageManager.NameNotFoundException {

        // PackageManager#getPackageInfo(String, int) was deprecated in API 33.
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Api33Impl.getPackageInfo(packageManager, context).lastUpdateTime;
        } else {
            return packageManager.getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
        }
    }

    /**
     * Returns a future containing the {@link CompilationStatus} of the app profile. The
     * {@link CompilationStatus} can be used to determine whether a baseline or cloud profile is
     * installed either through app store or package manager (reference profile) or profile
     * installer (current profile), in order to tag performance metrics versions. In the first
     * case a reference profile is immediately installed, i.e. a the app has been compiled with a
     * profile. In the second case the profile is awaiting compilation that will happen at some
     * point later in background.
     *
     * @return A future containing the {@link CompilationStatus}.
     */
    @NonNull
    public static ListenableFuture<CompilationStatus> getCompilationStatusAsync() {
        return sFuture;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static class Cache {
        private static final int SCHEMA = 1;
        final int mSchema;
        final int mResultCode;
        final long mPackageLastUpdateTime;
        final long mInstalledCurrentProfileSize;

        Cache(
                int schema,
                int resultCode,
                long packageLastUpdateTime,
                long installedCurrentProfileSize
        ) {
            mSchema = schema;
            mResultCode = resultCode;
            mPackageLastUpdateTime = packageLastUpdateTime;
            mInstalledCurrentProfileSize = installedCurrentProfileSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof Cache)) return false;
            Cache cacheFile = (Cache) o;
            return mResultCode == cacheFile.mResultCode
                    && mPackageLastUpdateTime == cacheFile.mPackageLastUpdateTime
                    && mSchema == cacheFile.mSchema
                    && mInstalledCurrentProfileSize == cacheFile.mInstalledCurrentProfileSize;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    mResultCode,
                    mPackageLastUpdateTime,
                    mSchema,
                    mInstalledCurrentProfileSize
            );
        }

        void writeOnFile(@NonNull File file) throws IOException {
            file.delete();
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(mSchema);
                dos.writeInt(mResultCode);
                dos.writeLong(mPackageLastUpdateTime);
                dos.writeLong(mInstalledCurrentProfileSize);
            }
        }

        static Cache readFromFile(@NonNull File file) throws IOException {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                return new Cache(
                        dis.readInt(),
                        dis.readInt(),
                        dis.readLong(),
                        dis.readLong()
                );
            }
        }
    }

    /**
     * {@link CompilationStatus} contains the result of a profile verification operation. It
     * offers API to determine whether a profile was installed
     * {@link CompilationStatus#getProfileInstallResultCode()} and to check whether the app has
     * been compiled with a profile or a profile is enqueued for compilation. Note that the
     * app can be compiled with a profile also as result of background dex optimization.
     */
    public static class CompilationStatus {

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                RESULT_CODE_NO_PROFILE,
                RESULT_CODE_COMPILED_WITH_PROFILE,
                RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION,
                RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING,
                RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST,
                RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ,
                RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE,
                RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION
        })
        public @interface ResultCode {
        }

        private static final int RESULT_CODE_ERROR_CODE_BIT_SHIFT = 16;

        /**
         * Indicates that no profile was installed for this app. This means that no profile was
         * installed when installing the app through app store or package manager and profile
         * installer either didn't run ({@link ProfileInstallerInitializer} disabled) or the app
         * was packaged without a compilation profile.
         */
        public static final int RESULT_CODE_NO_PROFILE = 0;

        /**
         * Indicates that a profile is installed and the app has been compiled with it. This is the
         * result of installation through app store or package manager, or installation through
         * profile installer and subsequent compilation during background dex optimization.
         */
        public static final int RESULT_CODE_COMPILED_WITH_PROFILE = 1;

        /**
         * Indicates that a profile is installed and the app will be compiled with it later when
         * background dex optimization runs (i.e when the device is in idle
         * and connected to the power). This is the result of installation through profile
         * installer. When the profile is compiled, the result code will change to
         * {@link #RESULT_CODE_COMPILED_WITH_PROFILE}. Note that to test that the app is compiled
         * with the installed profile, the background dex optimization can be forced through the
         * following adb shell command:
         * ```
         * adb shell cmd package compile -f -m speed-profile <PACKAGE_NAME>
         * ```
         */
        public static final int RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION = 2;

        /**
         * Indicates that a profile is installed and the app has been compiled with it.
         * This is the result of installation through app store or package manager. Note that
         * this result differs from {@link #RESULT_CODE_COMPILED_WITH_PROFILE} as the profile
         * is smaller than expected and may not include all the methods initially included in the
         * baseline profile.
         */
        public static final int RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING = 3;

        /**
         * Indicates an error during the verification process: a
         * {@link PackageManager.NameNotFoundException} was launched when querying the
         * {@link PackageManager} for the app package.
         */
        public static final int RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST =
                1 << RESULT_CODE_ERROR_CODE_BIT_SHIFT;

        /**
         * Indicates that a previous verification result cache file exists but it cannot be read.
         */
        public static final int RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ =
                2 << RESULT_CODE_ERROR_CODE_BIT_SHIFT;

        /**
         * Indicates that wasn't possible to write the verification result cache file. This can
         * happen only because something is wrong with app folder permissions or if there is no
         * free disk space on the device.
         */
        public static final int
                RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE =
                3 << RESULT_CODE_ERROR_CODE_BIT_SHIFT;

        /**
         * Indicates that ProfileVerifier runs on an unsupported api version of Android.
         * Note that ProfileVerifier supports only {@link Build.VERSION_CODES#P} and above.
         * Note that when this result code is returned {@link #isCompiledWithProfile()} and
         * {@link #hasProfileEnqueuedForCompilation()} return false.
         */
        public static final int RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION =
                4 << RESULT_CODE_ERROR_CODE_BIT_SHIFT;

        final int mResultCode;
        private final boolean mHasReferenceProfile;
        private final boolean mHasCurrentProfile;

        CompilationStatus(
                int resultCode,
                boolean hasReferenceProfile,
                boolean hasCurrentProfile
        ) {
            this.mResultCode = resultCode;
            this.mHasCurrentProfile = hasCurrentProfile;
            this.mHasReferenceProfile = hasReferenceProfile;
        }

        /**
         * @return a result code that indicates whether there is a baseline profile installed and
         * whether the app has been compiled with it. This depends on the installation method: if it
         * was installed through app store or package manager the app gets compiled immediately
         * with the profile and the return code is
         * {@link CompilationStatus#RESULT_CODE_COMPILED_WITH_PROFILE},
         * otherwise it'll be in `awaiting compilation` state and it'll be compiled at some point
         * later in the future, so the return code will be
         * {@link CompilationStatus#RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION}.
         * In the case that no profile was installed, the result code will be
         * {@link CompilationStatus#RESULT_CODE_NO_PROFILE}.
         *
         * Note that even if no profile was installed it's still possible for the app to have a
         * profile and be compiled with it, as result of background dex optimization.
         * The result code does a simple size check to ensure the compilation process completed
         * without errors. If the size check fails this method will return
         * {@link CompilationStatus#RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING}. The size
         * check is
         * not 100% accurate as the actual compiled methods are not checked.
         *
         * If something fails during the verification process, this method will return one of the
         * result codes associated with an error.
         *
         * Note that only api 28 {@link Build.VERSION_CODES#P} and above is supported
         * and that {@link #RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION} is returned when calling
         * this api on pre api 28.
         */
        @ResultCode
        public int getProfileInstallResultCode() {
            return mResultCode;
        }

        /**
         * @return True whether this app has been compiled with a profile, false otherwise. An
         * app can be compiled with a profile because of profile installation through app store,
         * package manager or profileinstaller and subsequent background dex optimization. There
         * should be a performance improvement when an app has been compiled with a profile. Note
         * that if {@link #getProfileInstallResultCode()} returns
         * {@link #RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION} this method always returns
         * always false.
         */
        public boolean isCompiledWithProfile() {
            return mHasReferenceProfile;
        }

        /**
         * @return True whether this app has a profile enqueued for compilation, false otherwise. An
         * app can have a profile enqueued for compilation because of profile installation through
         * profileinstaller or simply when the user starts interacting with the app. Note that if
         * {@link #getProfileInstallResultCode()} returns
         * {@link #RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION} this method always returns false.
         */
        public boolean hasProfileEnqueuedForCompilation() {
            return mHasCurrentProfile;
        }
    }

    @RequiresApi(33)
    private static class Api33Impl {
        private Api33Impl() {
        }

        @DoNotInline
        static PackageInfo getPackageInfo(
                PackageManager packageManager,
                Context context) throws PackageManager.NameNotFoundException {
            return packageManager.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.PackageInfoFlags.of(0)
            );
        }
    }
}
