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

package androidx.wear.protolayout.expression.pipeline;

import android.icu.util.ULocale;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.ComparisonFloatNode;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.ComparisonInt32Node;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.FixedBoolNode;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.LogicalBoolOp;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.NotBoolOp;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.StateBoolNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.AnimatableFixedColorNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.DynamicAnimatedColorNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.FixedColorNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.StateColorSourceNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.AnimatableFixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.ArithmeticFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.DynamicAnimatedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.FixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.Int32ToFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.StateFloatNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.ArithmeticInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FloatToInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.PlatformInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.EpochTimePlatformDataSource;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.SensorGatewayPlatformDataSource;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FixedStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FloatFormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.Int32FormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StateStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StringConcatOpNode;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalStringOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Evaluates protolayout dynamic types.
 *
 * <p>Given a dynamic ProtoLayout data source, this builds up a sequence of {@link DynamicDataNode}
 * instances, which can source the required data, and transform it into its final form.
 *
 * <p>Data source can include animations which will then emit value transitions.
 *
 * <p>In order to evaluate dynamic types, the caller needs to add any number of pending dynamic
 * types with {@link #bind} methods and then call {@link #processPendingBindings()} to start
 * evaluation on those dynamic types. Starting evaluation can be done for batches of dynamic types.
 *
 * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
 * BoundDynamicType#close()}.
 *
 * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
 * BoundDynamicType#close()}.
 */
public class DynamicTypeEvaluator implements AutoCloseable {
    private static final String TAG = "DynamicTypeEvaluator";

    @Nullable private final SensorGateway mSensorGateway;
    @Nullable private final SensorGatewayPlatformDataSource mSensorGatewayDataSource;
    @NonNull private final TimeGatewayImpl mTimeGateway;
    @Nullable private final EpochTimePlatformDataSource mTimeDataSource;
    @NonNull private final ObservableStateStore mStateStore;
    private final boolean mEnableAnimations;
    @NonNull private final QuotaManager mAnimationQuotaManager;
    @NonNull private final List<DynamicDataNode<?>> mDynamicTypeNodes = new ArrayList<>();
    private final Handler mUiHandler;

    @NonNull
    private static final QuotaManager DISABLED_ANIMATIONS_QUOTA_MANAGER =
            new QuotaManager() {
                @Override
                public boolean tryAcquireQuota(int quota) {
                    return false;
                }

                @Override
                public void releaseQuota(int quota) {
                    throw new IllegalStateException(
                            "releaseQuota method is called when no quota is acquired!");
                }
            };

    /**
     * Creates a {@link DynamicTypeEvaluator} without animation support.
     *
     * @param platformDataSourcesInitiallyEnabled Whether sending updates from sensor and time
     *     sources should be allowed initially. After that, enabling updates from sensor and time
     *     sources can be done via {@link #enablePlatformDataSources()} or {@link
     *     #disablePlatformDataSources()}.
     * @param sensorGateway The gateway for sensor data.
     * @param stateStore The state store that will be used for dereferencing the state keys in the
     *     dynamic types.
     */
    public DynamicTypeEvaluator(
            boolean platformDataSourcesInitiallyEnabled,
            @Nullable SensorGateway sensorGateway,
            @NonNull ObservableStateStore stateStore) {
        // Build pipeline with quota that doesn't allow any animations.
        this(
                platformDataSourcesInitiallyEnabled,
                sensorGateway,
                stateStore,
                /* enableAnimations= */ false,
                DISABLED_ANIMATIONS_QUOTA_MANAGER);
    }

    /**
     * Creates a {@link DynamicTypeEvaluator} with animation support. Maximum number of concurrently
     * running animations is defined in the given {@link QuotaManager}. Passing in animatable data
     * source to any of the methods will emit value transitions, for example animatable float from 5
     * to 10 will emit all values between those numbers (i.e. 5, 6, 7, 8, 9, 10).
     *
     * @param platformDataSourcesInitiallyEnabled Whether sending updates from sensor and time
     *     sources should be allowed initially. After that, enabling updates from sensor and time
     *     sources can be done via {@link #enablePlatformDataSources()} or {@link
     *     #disablePlatformDataSources()}.
     * @param sensorGateway The gateway for sensor data.
     * @param stateStore The state store that will be used for dereferencing the state keys in the
     *     dynamic types.
     * @param animationQuotaManager The quota manager used for limiting the number of concurrently
     *     running animations.
     */
    public DynamicTypeEvaluator(
            boolean platformDataSourcesInitiallyEnabled,
            @Nullable SensorGateway sensorGateway,
            @NonNull ObservableStateStore stateStore,
            @NonNull QuotaManager animationQuotaManager) {
        this(
                platformDataSourcesInitiallyEnabled,
                sensorGateway,
                stateStore,
                /* enableAnimations= */ true,
                animationQuotaManager);
    }

    /**
     * Creates a {@link DynamicTypeEvaluator}.
     *
     * @param platformDataSourcesInitiallyEnabled Whether sending updates from sensor and time
     *     sources should be allowed initially. After that, enabling updates from sensor and time
     *     sources can be done via {@link #enablePlatformDataSources()} or {@link
     *     #disablePlatformDataSources()}.
     * @param sensorGateway The gateway for sensor data.
     * @param stateStore The state store that will be used for dereferencing the state keys in the
     *     dynamic types.
     * @param animationQuotaManager The quota manager used for limiting the number of concurrently
     *     running animations.
     */
    private DynamicTypeEvaluator(
            boolean platformDataSourcesInitiallyEnabled,
            @Nullable SensorGateway sensorGateway,
            @NonNull ObservableStateStore stateStore,
            boolean enableAnimations,
            @NonNull QuotaManager animationQuotaManager) {
        this.mSensorGateway = sensorGateway;
        mUiHandler = new Handler(Looper.getMainLooper());
        Executor uiExecutor = new MainThreadExecutor(mUiHandler);
        if (this.mSensorGateway != null) {
            if (platformDataSourcesInitiallyEnabled) {
                this.mSensorGateway.enableUpdates();
            } else {
                this.mSensorGateway.disableUpdates();
            }
            this.mSensorGatewayDataSource =
                    new SensorGatewayPlatformDataSource(uiExecutor, this.mSensorGateway);
        } else {
            this.mSensorGatewayDataSource = null;
        }

        this.mTimeGateway = new TimeGatewayImpl(mUiHandler, platformDataSourcesInitiallyEnabled);
        this.mTimeDataSource = new EpochTimePlatformDataSource(uiExecutor, mTimeGateway);

        this.mEnableAnimations = enableAnimations;
        this.mStateStore = stateStore;
        this.mAnimationQuotaManager = animationQuotaManager;
    }

    /**
     * Starts evaluating all stored pending dynamic types.
     *
     * <p>This needs to be called when new pending dynamic types are added via any {@code bind}
     * method, either when one or a batch is added.
     *
     * <p>Any pending dynamic type will be initialized for evaluation. All other already initialized
     * dynamic types will remain unaffected.
     *
     * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
     * BoundDynamicType#close()}.
     *
     * @hide
     */
    @UiThread
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void processPendingBindings() {
        processBindings(mDynamicTypeNodes);

        // This method empties the array with dynamic type nodes.
        clearDynamicTypesArray();
    }

    @UiThread
    private static void processBindings(List<DynamicDataNode<?>> bindings) {
        preInitNodes(bindings);
        initNodes(bindings);
    }

    /**
     * Removes any stored pending bindings by clearing the list that stores them. Note that this
     * doesn't destroy them.
     */
    @UiThread
    private void clearDynamicTypesArray() {
        mDynamicTypeNodes.clear();
    }

    /** This should be called before initNodes() */
    @UiThread
    private static void preInitNodes(List<DynamicDataNode<?>> bindings) {
        bindings.stream()
                .filter(n -> n instanceof DynamicDataSourceNode)
                .forEach(n -> ((DynamicDataSourceNode<?>) n).preInit());
    }

    @UiThread
    private static void initNodes(List<DynamicDataNode<?>> bindings) {
        bindings.stream()
                .filter(n -> n instanceof DynamicDataSourceNode)
                .forEach(n -> ((DynamicDataSourceNode<?>) n).init());
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicString} for evaluation.
     * Evaluation will start immediately.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param stringSource The given String dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @param locale The locale used for the given String source.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicString stringSource,
            @NonNull ULocale locale,
            @NonNull DynamicTypeValueReceiver<String> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(stringSource.toDynamicStringProto(), consumer, locale, resultBuilder);
        mUiHandler.post(() -> processBindings(resultBuilder));
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicString} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param stringSource The given String dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @param locale The locale used for the given String source.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicString stringSource,
            @NonNull ULocale locale,
            @NonNull DynamicTypeValueReceiver<String> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(stringSource, consumer, locale, resultBuilder);
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicInt32} for evaluation.
     * Evaluation will start immediately.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param int32Source The given integer dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicInt32 int32Source,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(int32Source.toDynamicInt32Proto(), consumer, resultBuilder);
        mUiHandler.post(() -> processBindings(resultBuilder));
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicInt32} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param int32Source The given integer dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicInt32 int32Source,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(int32Source, consumer, resultBuilder);
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicFloat} for evaluation.
     * Evaluation will start immediately.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param floatSource The given float dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicFloat floatSource,
            @NonNull DynamicTypeValueReceiver<Float> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                floatSource.toDynamicFloatProto(), consumer, resultBuilder, Optional.empty());
        mUiHandler.post(() -> processBindings(resultBuilder));
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicFloat} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param floatSource The given float dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @param animationFallbackValue The value used if the given {@link DynamicFloat} is animatable
     *     and animation are disabled.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicFloat floatSource,
            @NonNull DynamicTypeValueReceiver<Float> consumer,
            float animationFallbackValue) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(floatSource, consumer, resultBuilder, Optional.of(animationFallbackValue));
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicFloat} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param floatSource The given float dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicFloat floatSource, @NonNull DynamicTypeValueReceiver<Float> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(floatSource, consumer, resultBuilder, Optional.empty());
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicColor} for evaluation.
     * Evaluation will start immediately.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param colorSource The given color dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                colorSource.toDynamicColorProto(), consumer, resultBuilder, Optional.empty());
        mUiHandler.post(() -> processBindings(resultBuilder));
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicColor} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param colorSource The given color dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(colorSource, consumer, resultBuilder, Optional.empty());
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicColor} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param colorSource The given color dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @param animationFallbackValue The value used if the given {@link DynamicFloat} is animatable
     *     and animation are disabled.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiver<Integer> consumer,
            int animationFallbackValue) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(colorSource, consumer, resultBuilder, Optional.of(animationFallbackValue));
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicBool} for evaluation.
     * Evaluation will start immediately.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param boolSource The given boolean dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicBool boolSource,
            @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(boolSource.toDynamicBoolProto(), consumer, resultBuilder);
        mUiHandler.post(() -> processBindings(resultBuilder));
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicBool} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link #processPendingBindings} is called.
     *
     * <p>While the {@link BoundDynamicType} is not destroyed with {@link BoundDynamicType#close()}
     * by caller, results of evaluation will be sent through the given {@link
     * DynamicTypeValueReceiver}.
     *
     * @param boolSource The given boolean dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicBool boolSource, @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(boolSource, consumer, resultBuilder);
        mDynamicTypeNodes.addAll(resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Same as {@link #bind(DynamicBuilders.DynamicString, ULocale, DynamicTypeValueReceiver)}, but
     * instead of returning one {@link BoundDynamicType}, all {@link DynamicDataNode} produced by
     * evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicString stringSource,
            @NonNull DynamicTypeValueReceiver<String> consumer,
            @NonNull ULocale locale,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
        DynamicDataNode<?> node;

        switch (stringSource.getInnerCase()) {
            case FIXED:
                node = new FixedStringNode(stringSource.getFixed(), consumer);
                break;
            case INT32_FORMAT_OP:
                {
                    NumberFormatter formatter =
                            new NumberFormatter(stringSource.getInt32FormatOp(), locale);
                    Int32FormatNode int32FormatNode = new Int32FormatNode(formatter, consumer);
                    node = int32FormatNode;
                    bindRecursively(
                            stringSource.getInt32FormatOp().getInput(),
                            int32FormatNode.getIncomingCallback(),
                            resultBuilder);
                    break;
                }
            case FLOAT_FORMAT_OP:
                {
                    NumberFormatter formatter =
                            new NumberFormatter(stringSource.getFloatFormatOp(), locale);
                    FloatFormatNode floatFormatNode = new FloatFormatNode(formatter, consumer);
                    node = floatFormatNode;
                    bindRecursively(
                            stringSource.getFloatFormatOp().getInput(),
                            floatFormatNode.getIncomingCallback(),
                            resultBuilder,
                            Optional.empty());
                    break;
                }
            case STATE_SOURCE:
                {
                    node =
                            new StateStringNode(
                                    mStateStore, stringSource.getStateSource(), consumer);
                    break;
                }
            case CONDITIONAL_OP:
                {
                    ConditionalOpNode<String> conditionalNode = new ConditionalOpNode<>(consumer);

                    ConditionalStringOp op = stringSource.getConditionalOp();
                    bindRecursively(
                            op.getCondition(),
                            conditionalNode.getConditionIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            op.getValueIfTrue(),
                            conditionalNode.getTrueValueIncomingCallback(),
                            locale,
                            resultBuilder);
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            locale,
                            resultBuilder);

                    node = conditionalNode;
                    break;
                }
            case CONCAT_OP:
                {
                    StringConcatOpNode concatNode = new StringConcatOpNode(consumer);
                    node = concatNode;
                    bindRecursively(
                            stringSource.getConcatOp().getInputLhs(),
                            concatNode.getLhsIncomingCallback(),
                            locale,
                            resultBuilder);
                    bindRecursively(
                            stringSource.getConcatOp().getInputRhs(),
                            concatNode.getRhsIncomingCallback(),
                            locale,
                            resultBuilder);
                    break;
                }
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicString has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicString source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind(DynamicBuilders.DynamicInt32, DynamicTypeValueReceiver)}, but instead of
     * returning one {@link BoundDynamicType}, all {@link DynamicDataNode} produced by evaluating
     * given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicInt32 int32Source,
            @NonNull DynamicTypeValueReceiver<Integer> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
        DynamicDataNode<Integer> node;

        switch (int32Source.getInnerCase()) {
            case FIXED:
                node = new FixedInt32Node(int32Source.getFixed(), consumer);
                break;
            case PLATFORM_SOURCE:
                node =
                        new PlatformInt32SourceNode(
                                int32Source.getPlatformSource(),
                                mTimeDataSource,
                                mSensorGatewayDataSource,
                                consumer);
                break;
            case ARITHMETIC_OPERATION:
                {
                    ArithmeticInt32Node arithmeticNode =
                            new ArithmeticInt32Node(int32Source.getArithmeticOperation(), consumer);
                    node = arithmeticNode;

                    bindRecursively(
                            int32Source.getArithmeticOperation().getInputLhs(),
                            arithmeticNode.getLhsIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            int32Source.getArithmeticOperation().getInputRhs(),
                            arithmeticNode.getRhsIncomingCallback(),
                            resultBuilder);

                    break;
                }
            case STATE_SOURCE:
                {
                    node =
                            new StateInt32SourceNode(
                                    mStateStore, int32Source.getStateSource(), consumer);
                    break;
                }
            case CONDITIONAL_OP:
                {
                    ConditionalOpNode<Integer> conditionalNode = new ConditionalOpNode<>(consumer);

                    ConditionalInt32Op op = int32Source.getConditionalOp();
                    bindRecursively(
                            op.getCondition(),
                            conditionalNode.getConditionIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            op.getValueIfTrue(),
                            conditionalNode.getTrueValueIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            resultBuilder);

                    node = conditionalNode;
                    break;
                }
            case FLOAT_TO_INT:
                {
                    FloatToInt32Node conversionNode =
                            new FloatToInt32Node(int32Source.getFloatToInt(), consumer);
                    node = conversionNode;

                    bindRecursively(
                            int32Source.getFloatToInt().getInput(),
                            conversionNode.getIncomingCallback(),
                            resultBuilder,
                            Optional.empty());
                    break;
                }
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicInt32 has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicInt32 source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind(DynamicBuilders.DynamicFloat, DynamicTypeValueReceiver)}, but instead of
     * returning one {@link BoundDynamicType}, all {@link DynamicDataNode} produced by evaluating
     * given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicFloat floatSource,
            @NonNull DynamicTypeValueReceiver<Float> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @NonNull Optional<Float> animationFallbackValue) {
        DynamicDataNode<?> node;

        switch (floatSource.getInnerCase()) {
            case FIXED:
                node = new FixedFloatNode(floatSource.getFixed(), consumer);
                break;
            case STATE_SOURCE:
                node =
                        new StateFloatNode(
                                mStateStore, floatSource.getStateSource().getSourceKey(), consumer);
                break;
            case ARITHMETIC_OPERATION:
                {
                    ArithmeticFloatNode arithmeticNode =
                            new ArithmeticFloatNode(floatSource.getArithmeticOperation(), consumer);
                    node = arithmeticNode;

                    bindRecursively(
                            floatSource.getArithmeticOperation().getInputLhs(),
                            arithmeticNode.getLhsIncomingCallback(),
                            resultBuilder,
                            Optional.empty());
                    bindRecursively(
                            floatSource.getArithmeticOperation().getInputRhs(),
                            arithmeticNode.getRhsIncomingCallback(),
                            resultBuilder,
                            Optional.empty());

                    break;
                }
            case INT32_TO_FLOAT_OPERATION:
                {
                    Int32ToFloatNode toFloatNode = new Int32ToFloatNode(consumer);
                    node = toFloatNode;

                    bindRecursively(
                            floatSource.getInt32ToFloatOperation().getInput(),
                            toFloatNode.getIncomingCallback(),
                            resultBuilder);
                    break;
                }
            case CONDITIONAL_OP:
                {
                    ConditionalOpNode<Float> conditionalNode = new ConditionalOpNode<>(consumer);

                    ConditionalFloatOp op = floatSource.getConditionalOp();
                    bindRecursively(
                            op.getCondition(),
                            conditionalNode.getConditionIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            op.getValueIfTrue(),
                            conditionalNode.getTrueValueIncomingCallback(),
                            resultBuilder,
                            Optional.empty());
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            resultBuilder,
                            Optional.empty());

                    node = conditionalNode;
                    break;
                }
            case ANIMATABLE_FIXED:
                if (!mEnableAnimations && animationFallbackValue.isPresent()) {
                    // Just assign static value if animations are disabled.
                    node =
                            new FixedFloatNode(
                                    FixedFloat.newBuilder()
                                            .setValue(animationFallbackValue.get())
                                            .build(),
                                    consumer);
                } else {
                    // We don't have to check if enableAnimations is true, because if it's false and
                    // we didn't have static value set, constructor has put QuotaManager that don't
                    // have any quota, so animations won't be played and they would jump to the end
                    // value.
                    node =
                            new AnimatableFixedFloatNode(
                                    floatSource.getAnimatableFixed(),
                                    consumer,
                                    mAnimationQuotaManager);
                }
                break;
            case ANIMATABLE_DYNAMIC:
                if (!mEnableAnimations && animationFallbackValue.isPresent()) {
                    // Just assign static value if animations are disabled.
                    node =
                            new FixedFloatNode(
                                    FixedFloat.newBuilder()
                                            .setValue(animationFallbackValue.get())
                                            .build(),
                                    consumer);

                } else {
                    // We don't have to check if enableAnimations is true, because if it's false and
                    // we didn't have static value set, constructor has put QuotaManager that don't
                    // have any quota, so animations won't be played and they would jump to the end
                    // value.
                    AnimatableDynamicFloat dynamicNode = floatSource.getAnimatableDynamic();
                    DynamicAnimatedFloatNode animationNode =
                            new DynamicAnimatedFloatNode(
                                    consumer, dynamicNode.getSpec(), mAnimationQuotaManager);
                    node = animationNode;

                    bindRecursively(
                            dynamicNode.getInput(),
                            animationNode.getInputCallback(),
                            resultBuilder,
                            animationFallbackValue);
                }
                break;

            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicFloat has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicFloat source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind(DynamicBuilders.DynamicColor, DynamicTypeValueReceiver)}, but instead of
     * returning one {@link BoundDynamicType}, all {@link DynamicDataNode} produced by evaluating
     * given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiver<Integer> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @NonNull Optional<Integer> animationFallbackValue) {
        DynamicDataNode<?> node;

        switch (colorSource.getInnerCase()) {
            case FIXED:
                node = new FixedColorNode(colorSource.getFixed(), consumer);
                break;
            case STATE_SOURCE:
                node =
                        new StateColorSourceNode(
                                mStateStore, colorSource.getStateSource(), consumer);
                break;
            case ANIMATABLE_FIXED:
                if (!mEnableAnimations && animationFallbackValue.isPresent()) {
                    // Just assign static value if animations are disabled.
                    node =
                            new FixedColorNode(
                                    FixedColor.newBuilder()
                                            .setArgb(animationFallbackValue.get())
                                            .build(),
                                    consumer);

                } else {
                    // We don't have to check if enableAnimations is true, because if it's false and
                    // we didn't have static value set, constructor has put QuotaManager that don't
                    // have any quota, so animations won't be played and they would jump to the end
                    // value.
                    node =
                            new AnimatableFixedColorNode(
                                    colorSource.getAnimatableFixed(),
                                    consumer,
                                    mAnimationQuotaManager);
                }
                break;
            case ANIMATABLE_DYNAMIC:
                if (!mEnableAnimations && animationFallbackValue.isPresent()) {
                    // Just assign static value if animations are disabled.
                    node =
                            new FixedColorNode(
                                    FixedColor.newBuilder()
                                            .setArgb(animationFallbackValue.get())
                                            .build(),
                                    consumer);

                } else {
                    // We don't have to check if enableAnimations is true, because if it's false and
                    // we didn't have static value set, constructor has put QuotaManager that don't
                    // have any quota, so animations won't be played and they would jump to the end
                    // value.
                    AnimatableDynamicColor dynamicNode = colorSource.getAnimatableDynamic();
                    DynamicAnimatedColorNode animationNode =
                            new DynamicAnimatedColorNode(
                                    consumer, dynamicNode.getSpec(), mAnimationQuotaManager);
                    node = animationNode;

                    bindRecursively(
                            dynamicNode.getInput(),
                            animationNode.getInputCallback(),
                            resultBuilder,
                            animationFallbackValue);
                }
                break;
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicColor has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicColor source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind(DynamicBuilders.DynamicBool, DynamicTypeValueReceiver)}, but instead of
     * returning one {@link BoundDynamicType}, all {@link DynamicDataNode} produced by evaluating
     * given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicBool boolSource,
            @NonNull DynamicTypeValueReceiver<Boolean> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
        DynamicDataNode<?> node;

        switch (boolSource.getInnerCase()) {
            case FIXED:
                node = new FixedBoolNode(boolSource.getFixed(), consumer);
                break;
            case STATE_SOURCE:
                node = new StateBoolNode(mStateStore, boolSource.getStateSource(), consumer);
                break;
            case INT32_COMPARISON:
                {
                    ComparisonInt32Node compNode =
                            new ComparisonInt32Node(boolSource.getInt32Comparison(), consumer);
                    node = compNode;

                    bindRecursively(
                            boolSource.getInt32Comparison().getInputLhs(),
                            compNode.getLhsIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            boolSource.getInt32Comparison().getInputRhs(),
                            compNode.getRhsIncomingCallback(),
                            resultBuilder);

                    break;
                }
            case LOGICAL_OP:
                {
                    LogicalBoolOp logicalNode =
                            new LogicalBoolOp(boolSource.getLogicalOp(), consumer);
                    node = logicalNode;

                    bindRecursively(
                            boolSource.getLogicalOp().getInputLhs(),
                            logicalNode.getLhsIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            boolSource.getLogicalOp().getInputRhs(),
                            logicalNode.getRhsIncomingCallback(),
                            resultBuilder);

                    break;
                }
            case NOT_OP:
                {
                    NotBoolOp notNode = new NotBoolOp(consumer);
                    node = notNode;
                    bindRecursively(
                            boolSource.getNotOp().getInput(),
                            notNode.getIncomingCallback(),
                            resultBuilder);
                    break;
                }
            case FLOAT_COMPARISON:
                {
                    ComparisonFloatNode compNode =
                            new ComparisonFloatNode(boolSource.getFloatComparison(), consumer);
                    node = compNode;

                    bindRecursively(
                            boolSource.getFloatComparison().getInputLhs(),
                            compNode.getLhsIncomingCallback(),
                            resultBuilder,
                            Optional.empty());
                    bindRecursively(
                            boolSource.getFloatComparison().getInputRhs(),
                            compNode.getRhsIncomingCallback(),
                            resultBuilder,
                            Optional.empty());

                    break;
                }
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicBool has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicBool source type");
        }

        resultBuilder.add(node);
    }

    /** Enables sending updates on sensor and time. */
    @UiThread
    public void enablePlatformDataSources() {
        if (mSensorGateway != null) {
            mSensorGateway.enableUpdates();
        }

        mTimeGateway.enableUpdates();
    }

    /** Disables sending updates on sensor and time. */
    @UiThread
    public void disablePlatformDataSources() {
        if (mSensorGateway != null) {
            mSensorGateway.disableUpdates();
        }

        mTimeGateway.disableUpdates();
    }

    /**
     * Closes existing time gateway.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void close() {
        try {
            mTimeGateway.close();
        } catch (RuntimeException ex) {
            Log.e(TAG, "Error while cleaning up time gateway", ex);
        }
    }
}
