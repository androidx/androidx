/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.content.Context;
import android.os.Handler;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main interface for accessing CameraX library.
 *
 * <p>This is a singleton class that is responsible for managing the set of camera
 * instances and {@link UseCase} instances that exist. A {@link UseCase} is bound to {@link
 * LifecycleOwner} so that the lifecycle is used to control the use case. There are 3 distinct sets
 * lifecycle states to be aware of.
 *
 * <p>When the lifecycle is in the STARTED or RESUMED states the cameras are opened asynchronously
 * and made ready for capturing. Data capture starts when triggered by the bound {@link UseCase}.
 *
 * <p>When the lifecycle is in the CREATED state any cameras with no {@link UseCase} attached
 * will be closed asynchronously.
 *
 * <p>When the lifecycle transitions to the DESTROYED state the {@link UseCase} will be unbound.
 * A {@link #bindToLifecycle(LifecycleOwner, UseCase...)} when the lifecycle is already in the
 * DESTROYED state will fail. A call to {@link #bindToLifecycle(LifecycleOwner, UseCase...)}
 * will need to be made with another lifecycle to rebind the {@link UseCase} that has been
 * unbound.
 *
 * <pre>{@code
 * public void setup() {
 *   // Initialize UseCase
 *   useCase = ...;
 *
 *   // UseCase binding event
 *   CameraX.bindToLifecycle(lifecycleOwner, useCase);
 *
 *   // Make calls on useCase
 * }
 *
 * public void operateOnUseCase() {
 *   if (CameraX.isBound(useCase)) {
 *     // Make calls on useCase
 *   }
 * }
 *
 * public void prematureTearDown() {
 *   // Not required, but only if we want to remove it before the lifecycle automatically removes
 *   // the use case
 *   CameraX.unbind(useCase);
 * }
 * }</pre>
 *
 * <p>All operations on a use case, including binding and unbinding, should be done on the main
 * thread, because lifecycle events are triggered on main thread. By only accessing the use case on
 * the main thread it is a guaranteed that the use case will not be unbound in the middle of a
 * method call.
 */
@MainThread
public final class CameraX {

    private static final CameraX INSTANCE = new CameraX();
    final CameraRepository mCameraRepository = new CameraRepository();
    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    private final UseCaseGroupRepository mUseCaseGroupRepository = new UseCaseGroupRepository();
    private final ErrorHandler mErrorHandler = new ErrorHandler();
    private CameraFactory mCameraFactory;
    private CameraDeviceSurfaceManager mSurfaceManager;
    private UseCaseConfigFactory mDefaultConfigFactory;
    private Context mContext;
    /** Prevents construction. */
    private CameraX() {
    }

    /**
     * Binds the collection of {@link UseCase} to a {@link LifecycleOwner}.
     *
     * <p>If the lifecycleOwner contains a {@link Lifecycle} that is already
     * in the STARTED state or greater than the created use cases will attach to the cameras and
     * trigger the appropriate notifications. This will generally cause a temporary glitch in the
     * camera as part of the reset process. This will also help to calculate suggested resolutions
     * depending on the use cases bound to the {@link Lifecycle}. If the use cases are bound
     * separately, it will find the supported resolution with the priority depending on the
     * binding sequence. If the use cases are bound with a single call, it will find the
     * supported resolution with the priority in sequence of ImageCapture, VideoCapture, Preview
     * and then ImageAnalysis. What resolutions can be supported will depend on the camera device
     * hardware level that there are some default guaranteed resolutions listed in
     * {@link android.hardware.camera2.CameraDevice#createCaptureSession}.
     *
     * <p> Currently up to 3 use cases may be bound at any time.  Exceeding this will throw an
     * IllegalArgumentException.
     *
     * @param lifecycleOwner The lifecycleOwner which controls the lifecycle transitions of the use
     *                       cases.
     * @param useCases       The use cases to bind to a lifecycle.
     * @throws IllegalArgumentException If the use case has already been bound to another lifecycle.
     */
    public static void bindToLifecycle(LifecycleOwner lifecycleOwner, UseCase... useCases) {
        UseCaseGroupLifecycleController useCaseGroupLifecycleController =
                INSTANCE.getOrCreateUseCaseGroup(lifecycleOwner);
        UseCaseGroup useCaseGroupToBind = useCaseGroupLifecycleController.getUseCaseGroup();

        Collection<UseCaseGroupLifecycleController> controllers =
                INSTANCE.mUseCaseGroupRepository.getUseCaseGroups();
        for (UseCase useCase : useCases) {
            for (UseCaseGroupLifecycleController controller : controllers) {
                UseCaseGroup useCaseGroup = controller.getUseCaseGroup();
                if (useCaseGroup.contains(useCase) && useCaseGroup != useCaseGroupToBind) {
                    throw new IllegalStateException(
                            String.format(
                                    "Use case %s already bound to a different lifecycle.",
                                    useCase));
                }
            }
        }

        calculateSuggestedResolutions(useCases);

        for (UseCase useCase : useCases) {
            useCaseGroupToBind.addUseCase(useCase);
            for (String cameraId : useCase.getAttachedCameraIds()) {
                attach(cameraId, useCase);
            }
        }

        useCaseGroupLifecycleController.notifyState();
    }

    /**
     * Returns true if the {@link UseCase} is bound to a lifecycle. Otherwise returns false.
     *
     * <p>It is not strictly necessary to check if a use case is bound or not. As long as the
     * lifecycle it was bound to has not entered a DESTROYED state or if it hasn't been unbound by
     * {@link #unbind(UseCase...)} or {@link #unbindAll()} then the use case will remain bound.
     * A use case will not be unbound in the middle of a method call as long as it is running on the
     * main thread. This is because a lifecycle events will only automatically triggered on the main
     * thread.
     */
    public static boolean isBound(UseCase useCase) {
        Collection<UseCaseGroupLifecycleController> controllers =
                INSTANCE.mUseCaseGroupRepository.getUseCaseGroups();

        for (UseCaseGroupLifecycleController controller : controllers) {
            UseCaseGroup useCaseGroup = controller.getUseCaseGroup();
            if (useCaseGroup.contains(useCase)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Unbinds all specified use cases from the lifecycle and removes them from CameraX.
     *
     * <p>This will initiate a close of every open camera which has zero {@link UseCase}
     * associated with it at the end of this call.
     *
     * <p>If a use case in the argument list is not bound, then then it is simply ignored.
     *
     * @param useCases The collection of use cases to remove.
     */
    public static void unbind(UseCase... useCases) {
        Collection<UseCaseGroupLifecycleController> useCaseGroups =
                INSTANCE.mUseCaseGroupRepository.getUseCaseGroups();

        Map<String, List<UseCase>> detachingUseCaseMap = new HashMap<>();

        for (UseCase useCase : useCases) {
            for (UseCaseGroupLifecycleController useCaseGroupLifecycleController : useCaseGroups) {
                UseCaseGroup useCaseGroup = useCaseGroupLifecycleController.getUseCaseGroup();
                if (useCaseGroup.removeUseCase(useCase)) {
                    // Saves all detaching use cases and detach them at once.
                    for (String cameraId : useCase.getAttachedCameraIds()) {
                        List<UseCase> useCasesOnCameraId = detachingUseCaseMap.get(cameraId);
                        if (useCasesOnCameraId == null) {
                            useCasesOnCameraId = new ArrayList<>();
                            detachingUseCaseMap.put(cameraId, useCasesOnCameraId);
                        }
                        useCasesOnCameraId.add(useCase);
                    }
                }
            }
        }

        for (String cameraId : detachingUseCaseMap.keySet()) {
            detach(cameraId, detachingUseCaseMap.get(cameraId));
        }

        for (UseCase useCase : useCases) {
            useCase.clear();
        }
    }

    /**
     * Unbinds all use cases from the lifecycle and removes them from CameraX.
     *
     * <p>This will initiate a close of every currently open camera.
     */
    public static void unbindAll() {
        Collection<UseCaseGroupLifecycleController> useCaseGroups =
                INSTANCE.mUseCaseGroupRepository.getUseCaseGroups();

        List<UseCase> useCases = new ArrayList<>();
        for (UseCaseGroupLifecycleController useCaseGroupLifecycleController : useCaseGroups) {
            UseCaseGroup useCaseGroup = useCaseGroupLifecycleController.getUseCaseGroup();
            useCases.addAll(useCaseGroup.getUseCases());
        }

        unbind(useCases.toArray(new UseCase[0]));
    }

    /**
     * Returns the camera id for a camera with the specified lens facing.
     *
     * <p>This only gives the first (primary) camera found with the specified facing.
     *
     * @param lensFacing the lens facing of the camera
     * @return the cameraId if camera exists or {@code null} if no camera with specified facing
     * exists
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due to
     *                                        insufficient permissions.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public static String getCameraWithLensFacing(LensFacing lensFacing)
            throws CameraInfoUnavailableException {
        return INSTANCE.getCameraFactory().cameraIdForLensFacing(lensFacing);
    }

    /**
     * Returns the camera info for the camera with the given camera id.
     *
     * @param cameraId the internal id of the camera
     * @return the camera info if it can be retrieved for the given id.
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due to
     *                                        insufficient permissions.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public static CameraInfo getCameraInfo(String cameraId) throws CameraInfoUnavailableException {
        return INSTANCE.getCameraRepository().getCamera(cameraId).getCameraInfo();
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} which can be used to query for valid surface
     * configurations.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static CameraDeviceSurfaceManager getSurfaceManager() {
        return INSTANCE.getCameraDeviceSurfaceManager();
    }

    /**
     * Returns the default configuration for the given use case configuration type.
     *
     * <p>The options contained in this configuration serve as fallbacks if they are not included in
     * the user-provided configuration used to create a use case.
     *
     * @param configType the configuration type
     * @param lensFacing The {@link LensFacing} that the default configuration will target to.
     * @return the default configuration for the given configuration type
     * @throws IllegalStateException if Camerax has not yet been initialized.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public static <C extends UseCaseConfig<?>> C getDefaultUseCaseConfig(
            Class<C> configType, LensFacing lensFacing) {
        return INSTANCE.getDefaultConfigFactory().getConfig(configType, lensFacing);
    }

    /**
     * Sets an {@link ErrorListener} which will get called anytime a CameraX specific error is
     * encountered.
     *
     * @param errorListener the listener which will get all the error messages. If this is set to
     *                      {@code null} then the default error listener will be set.
     * @param handler       the handler for the thread to run the error handling on. If this is
     *                      set to
     *                      {@code null} then it will default to run on the main thread.
     */
    public static void setErrorListener(ErrorListener errorListener, Handler handler) {
        INSTANCE.mErrorHandler.setErrorListener(errorListener, handler);
    }

    /**
     * Posts an error which can be handled by the {@link ErrorListener}.
     *
     * @param errorCode the type of error that occurred
     * @param message   the associated message with more details of the error
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static void postError(ErrorCode errorCode, String message) {
        INSTANCE.mErrorHandler.postError(errorCode, message);
    }

    /**
     * Initializes CameraX with the given context and application configuration.
     *
     * <p>The context enables CameraX to obtain access to necessary services, including the camera
     * service. For example, the context can be provided by the application.
     *
     * @param context   to attach
     * @param appConfig configuration options for this application session.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static void init(Context context, AppConfig appConfig) {
        INSTANCE.initInternal(context, appConfig);
    }

    /**
     * Returns the context used for CameraX.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static Context getContext() {
        return INSTANCE.mContext;
    }

    /**
     * Returns true if CameraX is initialized.
     *
     * <p>Any previous call to {@link #init(Context, AppConfig)} would have initialized
     * CameraX.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static boolean isInitialized() {
        return INSTANCE.mInitialized.get();
    }

    /**
     * Registers the callbacks for the {@link BaseCamera} to the {@link UseCase}.
     *
     * @param cameraId the id for the {@link BaseCamera}
     * @param useCase  the use case to register the callback for
     */
    private static void attach(String cameraId, UseCase useCase) {
        BaseCamera camera = INSTANCE.getCameraRepository().getCamera(cameraId);
        if (camera == null) {
            throw new IllegalArgumentException("Invalid camera: " + cameraId);
        }

        useCase.addStateChangeListener(camera);
        useCase.attachCameraControl(cameraId, camera.getCameraControl());

    }

    /**
     * Removes the callbacks registered by the {@link BaseCamera} to the {@link UseCase}.
     *
     * @param cameraId the id for the {@link BaseCamera}
     * @param useCases the list of use case to remove the callback from.
     */
    private static void detach(String cameraId, List<UseCase> useCases) {
        BaseCamera camera = INSTANCE.getCameraRepository().getCamera(cameraId);
        if (camera == null) {
            throw new IllegalArgumentException("Invalid camera: " + cameraId);
        }

        for (UseCase useCase : useCases) {
            useCase.removeStateChangeListener(camera);
            useCase.detachCameraControl(cameraId);
        }
        camera.removeOnlineUseCase(useCases);
    }

    private static void calculateSuggestedResolutions(UseCase... useCases) {
        Collection<UseCaseGroupLifecycleController> controllers =
                INSTANCE.mUseCaseGroupRepository.getUseCaseGroups();
        Map<String, List<UseCase>> originalCameraIdUseCaseMap = new HashMap<>();
        Map<String, List<UseCase>> newCameraIdUseCaseMap = new HashMap<>();

        // Collect original use cases for different camera devices
        for (UseCaseGroupLifecycleController controller : controllers) {
            UseCaseGroup useCaseGroup = controller.getUseCaseGroup();
            for (UseCase useCase : useCaseGroup.getUseCases()) {
                for (String cameraId : useCase.getAttachedCameraIds()) {
                    List<UseCase> useCaseList = originalCameraIdUseCaseMap.get(cameraId);
                    if (useCaseList == null) {
                        useCaseList = new ArrayList<>();
                        originalCameraIdUseCaseMap.put(cameraId, useCaseList);
                    }
                    useCaseList.add(useCase);
                }
            }
        }

        // Collect new use cases for different camera devices
        for (UseCase useCase : useCases) {
            String cameraId = null;
            LensFacing lensFacing =
                    useCase.getUseCaseConfig()
                            .retrieveOption(CameraDeviceConfig.OPTION_LENS_FACING);
            try {
                cameraId = getCameraWithLensFacing(lensFacing);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid camera lens facing: " + lensFacing, e);
            }

            List<UseCase> useCaseList = newCameraIdUseCaseMap.get(cameraId);
            if (useCaseList == null) {
                useCaseList = new ArrayList<>();
                newCameraIdUseCaseMap.put(cameraId, useCaseList);
            }
            useCaseList.add(useCase);
        }

        // Get suggested resolutions and update the use case session configuration
        for (String cameraId : newCameraIdUseCaseMap.keySet()) {
            Map<UseCase, Size> suggestResolutionsMap =
                    getSurfaceManager()
                            .getSuggestedResolutions(
                                    cameraId,
                                    originalCameraIdUseCaseMap.get(cameraId),
                                    newCameraIdUseCaseMap.get(cameraId));

            for (UseCase useCase : useCases) {
                Size resolution = suggestResolutionsMap.get(useCase);
                Map<String, Size> suggestedCameraSurfaceResolutionMap = new HashMap<>();
                suggestedCameraSurfaceResolutionMap.put(cameraId, resolution);
                useCase.updateSuggestedResolution(suggestedCameraSurfaceResolutionMap);
            }
        }
    }

    /**
     * Returns the {@link CameraFactory} instance.
     *
     * @throws IllegalStateException if the {@link CameraFactory} has not been set, due to being
     *                               uninitialized.
     */
    private CameraFactory getCameraFactory() {
        if (mCameraFactory == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mCameraFactory;
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} instance.
     *
     * @throws IllegalStateException if the {@link CameraDeviceSurfaceManager} has not been set, due
     *                               to being uninitialized.
     */
    private CameraDeviceSurfaceManager getCameraDeviceSurfaceManager() {
        if (mSurfaceManager == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mSurfaceManager;
    }

    private UseCaseConfigFactory getDefaultConfigFactory() {
        if (mDefaultConfigFactory == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mDefaultConfigFactory;
    }

    private void initInternal(Context context, AppConfig appConfig) {
        if (mInitialized.getAndSet(true)) {
            return;
        }

        mContext = context.getApplicationContext();
        mCameraFactory = appConfig.getCameraFactory(null);
        if (mCameraFactory == null) {
            throw new IllegalStateException(
                    "Invalid app configuration provided. Missing CameraFactory.");
        }

        mSurfaceManager = appConfig.getDeviceSurfaceManager(null);
        if (mSurfaceManager == null) {
            throw new IllegalStateException(
                    "Invalid app configuration provided. Missing CameraDeviceSurfaceManager.");
        }

        mDefaultConfigFactory = appConfig.getUseCaseConfigRepository(null);
        if (mDefaultConfigFactory == null) {
            throw new IllegalStateException(
                    "Invalid app configuration provided. Missing UseCaseConfigFactory.");
        }

        mCameraRepository.init(mCameraFactory);
    }

    private UseCaseGroupLifecycleController getOrCreateUseCaseGroup(LifecycleOwner lifecycleOwner) {
        return mUseCaseGroupRepository.getOrCreateUseCaseGroup(
                lifecycleOwner, new UseCaseGroupRepository.UseCaseGroupSetup() {
                    @Override
                    public void setup(UseCaseGroup useCaseGroup) {
                        useCaseGroup.setListener(mCameraRepository);
                    }
                });
    }

    private CameraRepository getCameraRepository() {
        return mCameraRepository;
    }

    /** The types of error states that can occur. */
    public enum ErrorCode {
        /** The camera has moved into an unexpected state from which it can not recover from. */
        CAMERA_STATE_INCONSISTENT,
        /** A {@link UseCase} has encountered an error from which it can not recover from. */
        USE_CASE_ERROR
    }

    /** The direction the camera faces relative to device screen. */
    public enum LensFacing {
        /** A camera on the device facing the same direction as the device's screen. */
        FRONT,
        /** A camera on the device facing the opposite direction as the device's screen. */
        BACK
    }

    /** Listener called whenever an error condition occurs within CameraX. */
    public interface ErrorListener {

        /**
         * Called whenever an error occurs within CameraX.
         *
         * @param error   the type of error that occurred
         * @param message detailed message of the error condition
         */
        void onError(ErrorCode error, String message);
    }
}
