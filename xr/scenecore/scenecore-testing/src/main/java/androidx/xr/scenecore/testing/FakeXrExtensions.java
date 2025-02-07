/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.testing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.view.AttachedSurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.extensions.Config;
import androidx.xr.extensions.Consumer;
import androidx.xr.extensions.XrExtensionResult;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.media.AudioTrackExtensions;
import androidx.xr.extensions.media.MediaPlayerExtensions;
import androidx.xr.extensions.media.PointSourceAttributes;
import androidx.xr.extensions.media.SoundFieldAttributes;
import androidx.xr.extensions.media.SoundPoolExtensions;
import androidx.xr.extensions.media.SpatializerExtensions;
import androidx.xr.extensions.media.XrSpatialAudioExtensions;
import androidx.xr.extensions.node.InputEvent;
import androidx.xr.extensions.node.Mat4f;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.NodeTransform;
import androidx.xr.extensions.node.Quatf;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.extensions.passthrough.PassthroughState;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.extensions.space.ActivityPanelLaunchParameters;
import androidx.xr.extensions.space.Bounds;
import androidx.xr.extensions.space.HitTestResult;
import androidx.xr.extensions.space.SpatialCapabilities;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.extensions.splitengine.SplitEngineBridge;
import androidx.xr.extensions.subspace.Subspace;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A fake for the XrExtensions.
 *
 * <p>This has fake implementations for a subset of the XrExtension capability that is used by the
 * JXRCore runtime for AndroidXR.
 */
@SuppressWarnings("deprecation")
public class FakeXrExtensions implements XrExtensions {
    private static final String NOT_IMPLEMENTED_IN_FAKE =
            "This function is not implemented yet in FakeXrExtensions.  Please add an"
                    + " implementation if support is desired for testing.";

    @NonNull public final List<FakeNode> createdNodes = new ArrayList<>();

    @NonNull public final List<FakeGltfModelToken> createdGltfModelTokens = new ArrayList<>();

    @NonNull public final List<FakeEnvironmentToken> createdEnvironmentTokens = new ArrayList<>();

    @NonNull public final Map<Activity, FakeActivityPanel> activityPanelMap = new HashMap<>();

    FakeNode mFakeTaskNode = null;
    FakeNode mFakeEnvironmentNode = null;
    FakeNode mFakeNodeForMainWindow = null;

    // TODO: b/370033054 - fakeSpatialState should be updated according to some fake extensions
    // calls
    // like requestFullSpaceMode after migration to SpatialState API
    @NonNull public final FakeSpatialState fakeSpatialState = new FakeSpatialState();

    // Technically this could be set per-activity, but we're assuming that there's a single activity
    // associated with each JXRCore session, so we're only tracking it once for now.
    SpaceMode mSpaceMode = SpaceMode.NONE;

    int mMainWindowWidth = 0;
    int mMainWindowHeight = 0;

    Consumer<SpatialState> mSpatialStateCallback = null;

    float mPreferredAspectRatioHsm = 0.0f;
    int mOpenXrWorldSpaceType = 0;
    FakeNodeTransaction mLastFakeNodeTransaction = null;

    @NonNull
    public final FakeSpatialAudioExtensions fakeSpatialAudioExtensions =
            new FakeSpatialAudioExtensions();

    @Nullable
    public FakeNode getFakeEnvironmentNode() {
        return mFakeEnvironmentNode;
    }

    @Nullable
    public FakeNode getFakeNodeForMainWindow() {
        return mFakeNodeForMainWindow;
    }

    @Override
    public int getApiVersion() {
        // The API surface is aligned with the initial XRU release.
        return 1;
    }

    @Override
    @NonNull
    public Node createNode() {
        FakeNode node = new FakeNode();
        createdNodes.add(node);
        return node;
    }

    @Override
    @NonNull
    public NodeTransaction createNodeTransaction() {
        mLastFakeNodeTransaction = new FakeNodeTransaction();
        return mLastFakeNodeTransaction;
    }

    @Override
    @NonNull
    public Subspace createSubspace(@NonNull SplitEngineBridge splitEngineBridge, int subspaceId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @NonNull
    @Deprecated
    public Bundle setMainPanelCurvatureRadius(@NonNull Bundle bundle, float panelCurvatureRadius) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @NonNull
    public Config getConfig() {
        return new FakeConfig();
    }

    @NonNull
    public SpaceMode getSpaceMode() {
        return mSpaceMode;
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setMainWindowSize(@NonNull Activity activity, int width, int height) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void setMainWindowSize(
            @NonNull Activity activity,
            int width,
            int height,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mMainWindowWidth = width;
        mMainWindowHeight = height;
        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setMainWindowCurvatureRadius(@NonNull Activity activity, float curvatureRadius) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    public int getMainWindowWidth() {
        return mMainWindowWidth;
    }

    public int getMainWindowHeight() {
        return mMainWindowHeight;
    }

    @Override
    public void getBounds(
            @NonNull Activity activity,
            @NonNull Consumer<Bounds> callback,
            @NonNull Executor executor) {
        callback.accept(
                (mSpaceMode == SpaceMode.FULL_SPACE
                        ? new Bounds(
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY)
                        : new Bounds(1f, 1f, 1f)));
    }

    private SpatialCapabilities getCapabilities(boolean allowAll) {
        return new SpatialCapabilities() {
            @Override
            public boolean get(int capQuery) {
                return allowAll;
            }
        };
    }

    @Override
    public void getSpatialCapabilities(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialCapabilities> callback,
            @NonNull Executor executor) {
        callback.accept(fakeSpatialState.getSpatialCapabilities());
    }

    @Override
    @NonNull
    public SpatialState getSpatialState(@NonNull Activity activity) {
        return fakeSpatialState;
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public boolean canEmbedActivityPanel(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public boolean requestFullSpaceMode(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void requestFullSpaceMode(
            @NonNull Activity activity,
            boolean requestEnter,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.mBounds =
                requestEnter
                        ? new Bounds(
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY)
                        : new Bounds(10f, 10f, 10f);
        spatialState.mCapabilities = getCapabilities(requestEnter);
        sendSpatialState(spatialState);
        mSpaceMode = requestEnter ? SpaceMode.FULL_SPACE : SpaceMode.HOME_SPACE;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public boolean requestHomeSpaceMode(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<androidx.xr.extensions.space.SpatialStateEvent> callback,
            @NonNull Executor executor) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @SuppressLint("PairedRegistration")
    public void registerSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialState> callback,
            @NonNull Executor executor) {
        // note that we assume this is only called for the (single) primary activity associated with
        // the JXRCore session and we also don't honor the executor here
        mSpatialStateCallback = callback;
    }

    @Override
    public void clearSpatialStateCallback(@NonNull Activity activity) {
        mSpatialStateCallback = null;
    }

    /**
     * Tests can use this method to trigger the spatial state callback. It is invoked on the calling
     * thread.
     */
    public void sendSpatialState(@NonNull SpatialState spatialState) {
        if (mSpatialStateCallback != null) {
            mSpatialStateCallback.accept(spatialState);
        }
    }

    @Nullable
    public Consumer<SpatialState> getSpatialStateCallback() {
        return mSpatialStateCallback;
    }

    private XrExtensionResult createAsyncResult() {
        return new XrExtensionResult() {
            @Override
            public int getResult() {
                return XrExtensionResult.XR_RESULT_SUCCESS;
            }
        };
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void attachSpatialScene(
            @NonNull Activity activity, @NonNull Node sceneNode, @NonNull Node windowNode) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void attachSpatialScene(
            @NonNull Activity activity,
            @NonNull Node sceneNode,
            @NonNull Node windowNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mFakeTaskNode = (FakeNode) sceneNode;
        mFakeTaskNode.mName = "taskNode";

        mFakeNodeForMainWindow = (FakeNode) windowNode;
        mFakeNodeForMainWindow.mName = "nodeForMainWindow";

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void detachSpatialScene(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void detachSpatialScene(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mFakeTaskNode = null;
        mFakeNodeForMainWindow = null;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    @Nullable
    public FakeNode getFakeTaskNode() {
        return mFakeTaskNode;
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void attachSpatialEnvironment(
            @NonNull Activity activity, @NonNull Node environmentNode) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void attachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Node environmentNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mFakeEnvironmentNode = (FakeNode) environmentNode;
        mFakeEnvironmentNode.mName = "environmentNode";

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void detachSpatialEnvironment(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void detachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mFakeEnvironmentNode = null;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings({"AndroidJdkLibsChecker", "BadFuture"})
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.GltfModelToken>
            loadGltfModel(
                    @Nullable InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    @Nullable String url) {
        FakeGltfModelToken modelToken = new FakeGltfModelToken(url);
        createdGltfModelTokens.add(modelToken);
        return CompletableFuture.completedFuture(modelToken);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ SceneViewerResult> displayGltfModel(
            Activity activity, androidx.xr.extensions.asset.GltfModelToken gltfModel) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings({"AndroidJdkLibsChecker", "BadFuture"})
    @Override
    @NonNull
    @Deprecated
    //  public ListenableFuture</* @Nullable */ EnvironmentToken>
    // loadEnvironment(
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
            loadEnvironment(
                    @Nullable InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    @Nullable String url) {
        FakeEnvironmentToken imageToken = new FakeEnvironmentToken(url);
        createdEnvironmentTokens.add(imageToken);
        //    return immediateFuture(imageToken);
        return CompletableFuture.completedFuture(imageToken);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
            loadEnvironment(
                    InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    String url,
                    int textureWidth,
                    int textureHeight) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @Deprecated
    @NonNull
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.SceneToken>
            loadImpressScene(InputStream asset, int regionSizeBytes, int regionOffsetBytes) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @NonNull
    public SplitEngineBridge createSplitEngineBridge() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Returns a FakeNode with corresponding gltfModelToken if it was created and found
     *
     * @deprecated This method is no longer supported.
     */
    @NonNull
    @Deprecated
    public FakeNode testGetNodeWithGltfToken(
            @NonNull androidx.xr.extensions.asset.GltfModelToken token) {
        for (FakeNode node : createdNodes) {
            if (node.mGltfModel != null && node.mGltfModel.equals(token)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns a FakeNode with corresponding environmentToken if it was created and found
     *
     * @deprecated This method is no longer supported.
     */
    @NonNull
    @Deprecated
    public FakeNode testGetNodeWithEnvironmentToken(
            @NonNull androidx.xr.extensions.asset.EnvironmentToken token) {
        for (FakeNode node : createdNodes) {
            if (node.mEnvironment != null && node.mEnvironment.equals(token)) {
                return node;
            }
        }
        return null;
    }

    @Override
    @NonNull
    public ActivityPanel createActivityPanel(
            @NonNull Activity host, @NonNull ActivityPanelLaunchParameters launchParameters) {
        FakeActivityPanel fakeActivityPanel = new FakeActivityPanel(createNode());
        activityPanelMap.put(host, fakeActivityPanel);
        return fakeActivityPanel;
    }

    /** Returns the FakeActivityPanel for the given Activity. */
    @NonNull
    public FakeActivityPanel getActivityPanelForHost(@NonNull Activity host) {
        return activityPanelMap.get(host);
    }

    @Override
    @NonNull
    public ReformOptions createReformOptions(
            @NonNull Consumer<ReformEvent> callback, @NonNull Executor executor) {
        return new FakeReformOptions(callback, executor);
    }

    @Override
    public void addFindableView(@NonNull View view, @NonNull ViewGroup group) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void removeFindableView(@NonNull View view, @NonNull ViewGroup group) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @Nullable
    public Node getSurfaceTrackingNode(@NonNull View view) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void hitTest(
            @NonNull Activity activity,
            @NonNull Vec3 origin,
            @NonNull Vec3 direction,
            @NonNull Consumer<HitTestResult> callback,
            @NonNull Executor executor) {
        HitTestResult fakeHitTestResult = new HitTestResult();
        executor.execute(() -> callback.accept(fakeHitTestResult));
    }

    @Override
    public int getOpenXrWorldSpaceType() {
        return mOpenXrWorldSpaceType;
    }

    public void setOpenXrWorldSpaceType(int openXrWorldSpaceType) {
        mOpenXrWorldSpaceType = openXrWorldSpaceType;
    }

    @Override
    @NonNull
    public Bundle setFullSpaceMode(@NonNull Bundle bundle) {
        return bundle;
    }

    @Override
    @NonNull
    public Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle) {
        return bundle;
    }

    @Override
    public void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void setPreferredAspectRatio(
            @NonNull Activity activity,
            float preferredRatio,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mPreferredAspectRatioHsm = preferredRatio;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    public float getPreferredAspectRatio() {
        return mPreferredAspectRatioHsm;
    }

    @NonNull
    @Override
    public XrSpatialAudioExtensions getXrSpatialAudioExtensions() {
        return fakeSpatialAudioExtensions;
    }

    /** Tracks whether an Activity has requested a mode for when it's focused. */
    public enum SpaceMode {
        NONE,
        HOME_SPACE,
        FULL_SPACE
    }

    /** Fake implementation of Extensions Config. */
    public static class FakeConfig implements Config {
        public static final float DEFAULT_PIXELS_PER_METER = 1f;

        @Override
        public float defaultPixelsPerMeter(float density) {
            return DEFAULT_PIXELS_PER_METER;
        }
    }

    /** A fake implementation of Closeable. */
    @SuppressWarnings("NotCloseable")
    public static class FakeCloseable implements Closeable {
        boolean mClosed = false;

        @Override
        public void close() {
            mClosed = true;
        }

        public boolean isClosed() {
            return mClosed;
        }
    }

    /** A fake implementation of the XR extensions Node. */
    @SuppressWarnings("ParcelCreator")
    public static final class FakeNode implements Node {
        FakeNode mParent = null;
        float mXPosition = 0.0f;
        float mYPosition = 0.0f;
        float mZPosition = 0.0f;
        float mXOrientation = 0.0f;
        float mYOrientation = 0.0f;
        float mZOrientation = 0.0f;
        float mWOrientation = 1.0f;
        float mXScale = 1.0f;
        float mYScale = 1.0f;
        float mZScale = 1.0f;
        float mCornerRadius = 0.0f;
        boolean mIsVisible = false;
        float mAlpha = 1.0f;
        androidx.xr.extensions.asset.GltfModelToken mGltfModel = null;
        IBinder mAnchorId = null;
        String mName = null;
        float mPassthroughOpacity = 1.0f;
        @PassthroughState.Mode int mPassthroughMode = 0;
        SurfaceControlViewHost.SurfacePackage mSurfacePackage = null;
        androidx.xr.extensions.asset.EnvironmentToken mEnvironment = null;
        Consumer<InputEvent> mListener = null;
        Consumer<NodeTransform> mTransformListener = null;
        Consumer<Integer> mPointerCaptureStateCallback = null;

        Executor mExecutor = null;
        ReformOptions mReformOptions;
        Executor mTransformExecutor = null;

        private FakeNode() {}

        @Override
        public void listenForInput(
                @NonNull Consumer<InputEvent> listener, @NonNull Executor executor) {
            mListener = listener;
            mExecutor = executor;
        }

        @Override
        public void stopListeningForInput() {
            mListener = null;
            mExecutor = null;
        }

        @Override
        public void setNonPointerFocusTarget(@NonNull AttachedSurfaceControl focusTarget) {}

        @Override
        public void requestPointerCapture(
                @NonNull Consumer<Integer> stateCallback, @NonNull Executor executor) {
            mPointerCaptureStateCallback = stateCallback;
        }

        @Override
        public void stopPointerCapture() {
            mPointerCaptureStateCallback = null;
        }

        /**
         * Fires the InputEvent callback with the given event. It is invoked on the executor
         * provided in listenForInput.
         */
        public void sendInputEvent(@NonNull InputEvent event) {
            mExecutor.execute(() -> mListener.accept(event));
        }

        /**
         * Fires the nodeTransform callback with the given transform. It is invoked on the executor
         * provided in listenForInput.
         */
        public void sendTransformEvent(@NonNull FakeNodeTransform nodeTransform) {
            mTransformExecutor.execute(() -> mTransformListener.accept(nodeTransform));
        }

        @Override
        @NonNull
        public Closeable subscribeToTransform(
                @NonNull Consumer<NodeTransform> transformCallback, @NonNull Executor executor) {
            mTransformListener = transformCallback;
            mTransformExecutor = executor;
            return new FakeCloseable();
        }

        @Nullable
        public Consumer<NodeTransform> getTransformListener() {
            return mTransformListener;
        }

        @Nullable
        public Executor getTransformExecutor() {
            return mTransformExecutor;
        }

        @Override
        public void writeToParcel(@NonNull Parcel in, int flags) {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Nullable
        public FakeNode getParent() {
            return mParent;
        }

        public float getXPosition() {
            return mXPosition;
        }

        public float getYPosition() {
            return mYPosition;
        }

        public float getZPosition() {
            return mZPosition;
        }

        public float getXOrientation() {
            return mXOrientation;
        }

        public float getYOrientation() {
            return mYOrientation;
        }

        public float getZOrientation() {
            return mZOrientation;
        }

        public float getWOrientation() {
            return mWOrientation;
        }

        public float getCornerRadius() {
            return mCornerRadius;
        }

        public boolean isVisible() {
            return mIsVisible;
        }

        public float getAlpha() {
            return mAlpha;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Nullable
        @Deprecated
        public androidx.xr.extensions.asset.GltfModelToken getGltfModel() {
            return mGltfModel;
        }

        @Nullable
        public IBinder getAnchorId() {
            return mAnchorId;
        }

        @Nullable
        public String getName() {
            return mName;
        }

        @Nullable
        public SurfaceControlViewHost.SurfacePackage getSurfacePackage() {
            return mSurfacePackage;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Nullable
        @Deprecated
        public androidx.xr.extensions.asset.EnvironmentToken getEnvironment() {
            return mEnvironment;
        }

        @Nullable
        public Consumer<InputEvent> getListener() {
            return mListener;
        }

        @Nullable
        public Consumer<Integer> getPointerCaptureStateCallback() {
            return mPointerCaptureStateCallback;
        }

        @Nullable
        public Executor getExecutor() {
            return mExecutor;
        }

        @Nullable
        public ReformOptions getReformOptions() {
            return mReformOptions;
        }
    }

    /**
     * A fake implementation of the XR extensions Node transaction.
     *
     * <p>All modifications happen immediately and not when the transaction is applied.
     */
    @SuppressWarnings("NotCloseable")
    public static class FakeNodeTransaction implements NodeTransaction {
        FakeNode mLastFakeNode = null;
        boolean mApplied = false;

        private FakeNodeTransaction() {}

        @Override
        @NonNull
        public NodeTransaction setParent(@NonNull Node node, @Nullable Node parent) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mParent = (FakeNode) parent;
            return this;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Override
        @NonNull
        @Deprecated
        public NodeTransaction setEnvironment(
                @NonNull Node node, @Nullable androidx.xr.extensions.asset.EnvironmentToken token) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mEnvironment = token;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setPosition(@NonNull Node node, float x, float y, float z) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mXPosition = x;
            ((FakeNode) node).mYPosition = y;
            ((FakeNode) node).mZPosition = z;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setOrientation(
                @NonNull Node node, float x, float y, float z, float w) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mXOrientation = x;
            ((FakeNode) node).mYOrientation = y;
            ((FakeNode) node).mZOrientation = z;
            ((FakeNode) node).mWOrientation = w;
            return this;
        }

        @Override
        @NonNull
        // TODO(b/354731545): Cover this with an AndroidXREntity test
        public NodeTransaction setScale(@NonNull Node node, float sx, float sy, float sz) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mXScale = sx;
            ((FakeNode) node).mYScale = sy;
            ((FakeNode) node).mZScale = sz;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setVisibility(@NonNull Node node, boolean isVisible) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mIsVisible = isVisible;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setAlpha(@NonNull Node node, float value) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mAlpha = value;
            return this;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Override
        @NonNull
        @Deprecated
        public NodeTransaction setGltfModel(
                @NonNull Node node,
                @NonNull androidx.xr.extensions.asset.GltfModelToken gltfModelToken) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mGltfModel = gltfModelToken;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setName(@NonNull Node node, @NonNull String name) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mName = name;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setPassthroughState(
                @NonNull Node node,
                float passthroughOpacity,
                @PassthroughState.Mode int passthroughMode) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mPassthroughOpacity = passthroughOpacity;
            ((FakeNode) node).mPassthroughMode = passthroughMode;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setSurfacePackage(
                @Nullable Node node,
                @NonNull SurfaceControlViewHost.SurfacePackage surfacePackage) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mSurfacePackage = surfacePackage;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setWindowBounds(
                @NonNull SurfaceControlViewHost.SurfacePackage surfacePackage,
                int widthPx,
                int heightPx) {
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setAnchorId(@NonNull Node node, @Nullable IBinder anchorId) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mAnchorId = anchorId;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction enableReform(@NonNull Node node, @NonNull ReformOptions options) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mReformOptions = options;
            ((FakeReformOptions) options).mOptionsApplied = true;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setReformSize(@NonNull Node node, @NonNull Vec3 reformSize) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mReformOptions.setCurrentSize(reformSize);
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction disableReform(@NonNull Node node) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mReformOptions = new FakeReformOptions(null, null);
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setCornerRadius(@NonNull Node node, float cornerRadius) {
            mLastFakeNode = (FakeNode) node;
            ((FakeNode) node).mCornerRadius = cornerRadius;
            return this;
        }

        @Override
        public void apply() {
            mApplied = true;
        }

        @Override
        public void close() {}
    }

    /** A fake implementation of the XR extensions NodeTransform. */
    public static class FakeNodeTransform implements NodeTransform {
        Mat4f mTransform;

        public FakeNodeTransform(@NonNull Mat4f transform) {
            mTransform = transform;
        }

        @Override
        @NonNull
        public Mat4f getTransform() {
            return mTransform;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }

    /** A fake implementation of the XR extensions GltfModelToken. */
    public static class FakeGltfModelToken implements androidx.xr.extensions.asset.GltfModelToken {
        String mUrl;

        public FakeGltfModelToken(@NonNull String url) {
            mUrl = url;
        }

        @NonNull
        public String getUrl() {
            return mUrl;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentToken. */
    public static class FakeEnvironmentToken
            implements androidx.xr.extensions.asset.EnvironmentToken {
        String mUrl;

        public FakeEnvironmentToken(@NonNull String url) {
            this.mUrl = url;
        }

        @NonNull
        public String getUrl() {
            return mUrl;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentVisibilityState. */
    public static class FakeEnvironmentVisibilityState implements EnvironmentVisibilityState {
        @EnvironmentVisibilityState.State int mState;

        public FakeEnvironmentVisibilityState(@EnvironmentVisibilityState.State int state) {
            this.mState = state;
        }

        @Override
        @EnvironmentVisibilityState.State
        public int getCurrentState() {
            return mState;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentVisibilityState. */
    public static class FakePassthroughVisibilityState implements PassthroughVisibilityState {
        @PassthroughVisibilityState.State int mState;
        float mOpacity;

        public FakePassthroughVisibilityState(
                @PassthroughVisibilityState.State int state, float opacity) {
            this.mState = state;
            this.mOpacity = opacity;
        }

        @Override
        @PassthroughVisibilityState.State
        public int getCurrentState() {
            return mState;
        }

        @Override
        public float getOpacity() {
            return mOpacity;
        }
    }

    /** Creates fake activity panel. */
    public static class FakeActivityPanel implements ActivityPanel {
        Intent mLaunchIntent;
        Bundle mBundle;
        Activity mActivity;
        Rect mBounds;
        boolean mIsDeleted = false;
        Node mNode;

        FakeActivityPanel(@NonNull Node node) {
            mNode = node;
        }

        @Override
        public void launchActivity(@NonNull Intent intent, @Nullable Bundle options) {
            mLaunchIntent = intent;
            mBundle = options;
        }

        @Nullable
        public Intent getLaunchIntent() {
            return mLaunchIntent;
        }

        @NonNull
        public Bundle getBundle() {
            return mBundle;
        }

        @Override
        public void moveActivity(@NonNull Activity activity) {
            mActivity = activity;
        }

        @Nullable
        public Activity getActivity() {
            return mActivity;
        }

        @NonNull
        @Override
        public Node getNode() {
            return mNode;
        }

        @Override
        public void setWindowBounds(@NonNull Rect windowBounds) {
            mBounds = windowBounds;
        }

        @Nullable
        public Rect getBounds() {
            return mBounds;
        }

        @Override
        public void delete() {
            mIsDeleted = true;
        }

        public boolean isDeleted() {
            return mIsDeleted;
        }
    }

    /** Fake input event. */
    public static class FakeInputEvent implements InputEvent {
        int mSource;
        int mPointerType;
        long mTimestamp;
        Vec3 mOrigin;
        Vec3 mDirection;
        FakeHitInfo mHitInfo;
        FakeHitInfo mSecondaryHitInfo;
        int mDispatchFlags;
        int mAction;

        @Override
        public int getSource() {
            return mSource;
        }

        @Override
        public int getPointerType() {
            return mPointerType;
        }

        @Override
        public long getTimestamp() {
            return mTimestamp;
        }

        @Override
        @NonNull
        public Vec3 getOrigin() {
            return mOrigin;
        }

        @Override
        @NonNull
        public Vec3 getDirection() {
            return mDirection;
        }

        @Override
        @Nullable
        public HitInfo getHitInfo() {
            return mHitInfo;
        }

        @Override
        @Nullable
        public HitInfo getSecondaryHitInfo() {
            return mSecondaryHitInfo;
        }

        @Override
        public int getDispatchFlags() {
            return mDispatchFlags;
        }

        @Override
        public int getAction() {
            return mAction;
        }

        public void setDispatchFlags(int dispatchFlags) {
            mDispatchFlags = dispatchFlags;
        }

        public void setOrigin(@NonNull Vec3 origin) {
            mOrigin = origin;
        }

        public void setDirection(@NonNull Vec3 direction) {
            mDirection = direction;
        }

        public void setFakeHitInfo(@NonNull FakeHitInfo hitInfo) {
            mHitInfo = hitInfo;
        }

        public void setTimestamp(long timestamp) {
            mTimestamp = timestamp;
        }

        /** Fake hit info. */
        public static class FakeHitInfo implements InputEvent.HitInfo {
            int mSubspaceImpressNodeId;
            Node mInputNode;
            Vec3 mHitPosition;
            Mat4f mTransform;

            @Override
            public int getSubspaceImpressNodeId() {
                return mSubspaceImpressNodeId;
            }

            @Override
            @NonNull
            public Node getInputNode() {
                return mInputNode;
            }

            @Override
            @Nullable
            public Vec3 getHitPosition() {
                return mHitPosition;
            }

            @Override
            @NonNull
            public Mat4f getTransform() {
                return mTransform;
            }

            public void setSubspaceImpressNodeId(int subspaceImpressNodeId) {
                mSubspaceImpressNodeId = subspaceImpressNodeId;
            }

            public void setInputNode(@NonNull Node inputNode) {
                mInputNode = inputNode;
            }

            public void setHitPosition(@Nullable Vec3 hitPosition) {
                mHitPosition = hitPosition;
            }

            public void setTransform(@NonNull Mat4f transform) {
                mTransform = transform;
            }
        }
    }

    /** Fake ReformOptions. */
    public static class FakeReformOptions implements ReformOptions {

        int mEnabledReforms;
        int mReformFlags;
        int mScaleWithDistanceMode = SCALE_WITH_DISTANCE_MODE_DEFAULT;
        Vec3 mCurrentSize;
        Vec3 mMinimumSize;
        Vec3 mMaximumSize;
        float mFixedAspectRatio;
        boolean mForceShowResizeOverlay;
        Consumer<ReformEvent> mConsumer;
        Executor mExecutor;

        boolean mOptionsApplied = true;

        FakeReformOptions(Consumer<ReformEvent> consumer, Executor executor) {
            this.mConsumer = consumer;
            this.mExecutor = executor;
        }

        @Override
        public int getEnabledReform() {
            return mEnabledReforms;
        }

        @Override
        @NonNull
        public ReformOptions setEnabledReform(int i) {
            mEnabledReforms = i;
            return this;
        }

        @Override
        public int getFlags() {
            return mReformFlags;
        }

        @Override
        @NonNull
        public ReformOptions setFlags(int i) {
            mOptionsApplied = false;
            mReformFlags = i;
            return this;
        }

        @NonNull
        @Override
        public Vec3 getCurrentSize() {
            return mCurrentSize;
        }

        @Override
        @NonNull
        public ReformOptions setCurrentSize(@NonNull Vec3 vec3) {
            mOptionsApplied = false;
            mCurrentSize = vec3;
            return this;
        }

        @NonNull
        @Override
        public Vec3 getMinimumSize() {
            return mMinimumSize;
        }

        @Override
        @NonNull
        public ReformOptions setMinimumSize(@NonNull Vec3 vec3) {
            mOptionsApplied = false;
            mMinimumSize = vec3;
            return this;
        }

        @NonNull
        @Override
        public Vec3 getMaximumSize() {
            return mMaximumSize;
        }

        @Override
        @NonNull
        public ReformOptions setMaximumSize(@NonNull Vec3 vec3) {
            mOptionsApplied = false;
            mMaximumSize = vec3;
            return this;
        }

        @Override
        public float getFixedAspectRatio() {
            return mFixedAspectRatio;
        }

        @Override
        @NonNull
        public ReformOptions setFixedAspectRatio(float fixedAspectRatio) {
            mFixedAspectRatio = fixedAspectRatio;
            return this;
        }

        @Override
        @SuppressWarnings("GetterSetterNames")
        public boolean getForceShowResizeOverlay() {
            return mForceShowResizeOverlay;
        }

        @Override
        @NonNull
        public ReformOptions setForceShowResizeOverlay(boolean show) {
            mForceShowResizeOverlay = show;
            return this;
        }

        @NonNull
        @Override
        public Consumer<ReformEvent> getEventCallback() {
            return mConsumer;
        }

        @Override
        @NonNull
        @SuppressLint("InvalidNullabilityOverride")
        public ReformOptions setEventCallback(@NonNull Consumer<ReformEvent> consumer) {
            mConsumer = consumer;
            return this;
        }

        @NonNull
        @Override
        public Executor getEventExecutor() {
            return mExecutor;
        }

        @Override
        @NonNull
        public ReformOptions setEventExecutor(@NonNull Executor executor) {
            mExecutor = executor;
            return this;
        }

        @Override
        public int getScaleWithDistanceMode() {
            return mScaleWithDistanceMode;
        }

        @Override
        @NonNull
        public ReformOptions setScaleWithDistanceMode(int scaleWithDistanceMode) {
            mScaleWithDistanceMode = scaleWithDistanceMode;
            return this;
        }
    }

    /** Fake ReformEvent. */
    public static class FakeReformEvent implements ReformEvent {

        int mType;
        int mState;
        int mId;
        Vec3 mOrigin = new Vec3(0f, 0f, 0f);
        Vec3 mInitialRayOrigin = mOrigin;
        Vec3 mProposedPosition = mOrigin;
        Vec3 mOnes = new Vec3(1f, 1f, 1f);
        Vec3 mInitialRayDirection = mOnes;
        Vec3 mProposedScale = mOnes;
        Vec3 mProposedSize = mOnes;
        Vec3 mTwos = new Vec3(2f, 2f, 2f);
        Vec3 mCurrentRayOrigin = mTwos;
        Vec3 mThrees = new Vec3(3f, 3f, 3f);
        Vec3 mCurrentRayDirection = mThrees;
        Quatf mIdentity = new Quatf(0f, 0f, 0f, 1f);
        Quatf mProposedOrientation = mIdentity;

        public void setType(int type) {
            mType = type;
        }

        public void setState(int state) {
            mState = state;
        }

        public void setProposedPosition(@NonNull Vec3 proposedPosition) {
            mProposedPosition = proposedPosition;
        }

        public void setProposedScale(@NonNull Vec3 proposedScale) {
            mProposedScale = proposedScale;
        }

        public void setProposedOrientation(@NonNull Quatf proposedOrientation) {
            mProposedOrientation = proposedOrientation;
        }

        public void setProposedSize(@NonNull Vec3 proposedSize) {
            mProposedSize = proposedSize;
        }

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public int getState() {
            return mState;
        }

        @Override
        public int getId() {
            return mId;
        }

        @NonNull
        @Override
        public Vec3 getInitialRayOrigin() {
            return mInitialRayOrigin;
        }

        @NonNull
        @Override
        public Vec3 getInitialRayDirection() {
            return mInitialRayDirection;
        }

        @NonNull
        @Override
        public Vec3 getCurrentRayOrigin() {
            return mCurrentRayOrigin;
        }

        @NonNull
        @Override
        public Vec3 getCurrentRayDirection() {
            return mCurrentRayDirection;
        }

        @NonNull
        @Override
        public Vec3 getProposedPosition() {
            return mProposedPosition;
        }

        @NonNull
        @Override
        public Quatf getProposedOrientation() {
            return mProposedOrientation;
        }

        @NonNull
        @Override
        public Vec3 getProposedScale() {
            return mProposedScale;
        }

        @NonNull
        @Override
        public Vec3 getProposedSize() {
            return mProposedSize;
        }
    }

    /** A fake implementation of the XR extensions SpatialState. */
    public static class FakeSpatialState implements SpatialState {
        Bounds mBounds;
        SpatialCapabilities mCapabilities;
        EnvironmentVisibilityState mEnvironmentVisibilityState;
        PassthroughVisibilityState mPassthroughVisibilityState;

        public FakeSpatialState() {
            // Initialize params to any non-null values
            // TODO: b/370033054 - Revisit the default values for the bounds and capabilities.
            mBounds =
                    new Bounds(
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY);
            this.setAllSpatialCapabilities(true);
            mEnvironmentVisibilityState =
                    new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.INVISIBLE);
            mPassthroughVisibilityState =
                    new FakePassthroughVisibilityState(PassthroughVisibilityState.DISABLED, 0.0f);
        }

        @Override
        @NonNull
        public Bounds getBounds() {
            return mBounds;
        }

        public void setBounds(@NonNull Bounds bounds) {
            mBounds = bounds;
        }

        @Override
        @NonNull
        public SpatialCapabilities getSpatialCapabilities() {
            return mCapabilities;
        }

        @Override
        @NonNull
        public EnvironmentVisibilityState getEnvironmentVisibility() {
            return mEnvironmentVisibilityState;
        }

        public void setEnvironmentVisibility(
                @NonNull EnvironmentVisibilityState environmentVisibilityState) {
            mEnvironmentVisibilityState = environmentVisibilityState;
        }

        @Override
        @NonNull
        public PassthroughVisibilityState getPassthroughVisibility() {
            return mPassthroughVisibilityState;
        }

        public void setPassthroughVisibility(
                @NonNull PassthroughVisibilityState passthroughVisibilityState) {
            mPassthroughVisibilityState = passthroughVisibilityState;
        }

        // Methods for tests to set the capabilities.
        public void setSpatialCapabilities(@NonNull SpatialCapabilities capabilities) {
            mCapabilities = capabilities;
        }

        public void setAllSpatialCapabilities(boolean allowAll) {
            mCapabilities =
                    new SpatialCapabilities() {
                        @Override
                        public boolean get(int capQuery) {
                            return allowAll;
                        }
                    };
        }
    }

    /** Fake XrSpatialAudioExtensions. */
    public static class FakeSpatialAudioExtensions implements XrSpatialAudioExtensions {

        @NonNull
        public final FakeSoundPoolExtensions soundPoolExtensions = new FakeSoundPoolExtensions();

        FakeAudioTrackExtensions mAudioTrackExtensions = new FakeAudioTrackExtensions();

        @NonNull
        public final FakeMediaPlayerExtensions mediaPlayerExtensions =
                new FakeMediaPlayerExtensions();

        @NonNull
        @Override
        public SoundPoolExtensions getSoundPoolExtensions() {
            return soundPoolExtensions;
        }

        @NonNull
        @Override
        public AudioTrackExtensions getAudioTrackExtensions() {
            return mAudioTrackExtensions;
        }

        public void setFakeAudioTrackExtensions(
                @NonNull FakeAudioTrackExtensions audioTrackExtensions) {
            mAudioTrackExtensions = audioTrackExtensions;
        }

        @NonNull
        @Override
        public MediaPlayerExtensions getMediaPlayerExtensions() {
            return mediaPlayerExtensions;
        }
    }

    /** Fake SoundPoolExtensions. */
    public static class FakeSoundPoolExtensions implements SoundPoolExtensions {

        int mPlayAsPointSourceResult = 0;
        int mPlayAsSoundFieldResult = 0;
        int mSourceType = SpatializerExtensions.SOURCE_TYPE_BYPASS;

        public void setPlayAsPointSourceResult(int result) {
            mPlayAsPointSourceResult = result;
        }

        public void setPlayAsSoundFieldResult(int result) {
            mPlayAsSoundFieldResult = result;
        }

        public void setSourceType(@SpatializerExtensions.SourceType int sourceType) {
            mSourceType = sourceType;
        }

        @Override
        public int playAsPointSource(
                @NonNull SoundPool soundPool,
                int soundID,
                @NonNull PointSourceAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate) {
            return mPlayAsPointSourceResult;
        }

        @Override
        public int playAsSoundField(
                @NonNull SoundPool soundPool,
                int soundID,
                @NonNull SoundFieldAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate) {
            return mPlayAsSoundFieldResult;
        }

        @Override
        @SpatializerExtensions.SourceType
        public int getSpatialSourceType(@NonNull SoundPool soundPool, int streamID) {
            return mSourceType;
        }
    }

    /** Fake AudioTrackExtensions. */
    public static class FakeAudioTrackExtensions implements AudioTrackExtensions {

        PointSourceAttributes mPointSourceAttributes;

        SoundFieldAttributes mSoundFieldAttributes;

        @SpatializerExtensions.SourceType int mSourceType;

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public AudioTrack.Builder setPointSourceAttributes(
                @NonNull AudioTrack.Builder builder, @Nullable PointSourceAttributes attributes) {
            mPointSourceAttributes = attributes;
            return builder;
        }

        public void setPointSourceAttributes(
                @Nullable PointSourceAttributes pointSourceAttributes) {
            mPointSourceAttributes = pointSourceAttributes;
        }

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public AudioTrack.Builder setSoundFieldAttributes(
                @NonNull AudioTrack.Builder builder, @Nullable SoundFieldAttributes attributes) {
            mSoundFieldAttributes = attributes;
            return builder;
        }

        public void setSoundFieldAttributes(@Nullable SoundFieldAttributes soundFieldAttributes) {
            mSoundFieldAttributes = soundFieldAttributes;
        }

        @Override
        @Nullable
        public PointSourceAttributes getPointSourceAttributes(@NonNull AudioTrack track) {
            return mPointSourceAttributes;
        }

        @Nullable
        public PointSourceAttributes getPointSourceAttributes() {
            return mPointSourceAttributes;
        }

        @Override
        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack track) {
            return mSoundFieldAttributes;
        }

        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes() {
            return mSoundFieldAttributes;
        }

        @Override
        public int getSpatialSourceType(@NonNull AudioTrack track) {
            return mSourceType;
        }

        public void setSourceType(@SpatializerExtensions.SourceType int sourceType) {
            mSourceType = sourceType;
        }
    }

    /** Fake MediaPlayerExtensions. */
    public static class FakeMediaPlayerExtensions implements MediaPlayerExtensions {

        PointSourceAttributes mPointSourceAttributes;

        SoundFieldAttributes mSoundFieldAttributes;

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public MediaPlayer setPointSourceAttributes(
                @NonNull MediaPlayer mediaPlayer, @Nullable PointSourceAttributes attributes) {
            mPointSourceAttributes = attributes;
            return mediaPlayer;
        }

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public MediaPlayer setSoundFieldAttributes(
                @NonNull MediaPlayer mediaPlayer, @Nullable SoundFieldAttributes attributes) {
            mSoundFieldAttributes = attributes;
            return mediaPlayer;
        }

        @Nullable
        public PointSourceAttributes getPointSourceAttributes() {
            return mPointSourceAttributes;
        }

        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes() {
            return mSoundFieldAttributes;
        }
    }
}
