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

import static java.util.Collections.emptyMap;

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
import androidx.wear.protolayout.expression.pipeline.DurationNodes.BetweenInstancesNode;
import androidx.wear.protolayout.expression.pipeline.DurationNodes.FixedDurationNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.AnimatableFixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.ArithmeticFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.DynamicAnimatedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.FixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.Int32ToFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.StateFloatSourceNode;
import androidx.wear.protolayout.expression.pipeline.InstantNodes.FixedInstantNode;
import androidx.wear.protolayout.expression.pipeline.InstantNodes.PlatformTimeSourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.AnimatableFixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.ArithmeticInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.DynamicAnimatedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FloatToInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.GetDurationPartOpNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.PlatformInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FixedStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FloatFormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.Int32FormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StateStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StringConcatOpNode;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalColorOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalStringOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicDuration;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInstant;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 * types with {@link #bind} methods and then call {@link BoundDynamicType#startEvaluation()} on each
 * of them to start evaluation. Starting evaluation can be done for batches of dynamic types.
 *
 * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
 * BoundDynamicType#close()}.
 *
 * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
 * BoundDynamicType#close()}.
 */
public class DynamicTypeEvaluator implements AutoCloseable {
    private static final String TAG = "DynamicTypeEvaluator";

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

    @NonNull private static final StateStore EMPTY_STATE_STORE = new StateStore(emptyMap());

    @NonNull private final Config mConfig;
    @NonNull private final StateStore mStateStore;
    @NonNull private final QuotaManager mAnimationQuotaManager;
    @NonNull private final TimeGateway mTimeGateway;
    @Nullable private final EpochTimePlatformDataSource mTimeDataSource;
    @Nullable private final SensorGatewayPlatformDataSource mSensorGatewayDataSource;

    /** Configuration for {@link #DynamicTypeEvaluator(Config)}. */
    public static final class Config {
        private final boolean mPlatformDataSourcesInitiallyEnabled;
        @Nullable private final StateStore mStateStore;
        @Nullable private final QuotaManager mAnimationQuotaManager;
        @Nullable private final TimeGateway mTimeGateway;
        @Nullable private final SensorGateway mSensorGateway;

        Config(
                boolean platformDataSourcesInitiallyEnabled,
                @Nullable StateStore stateStore,
                @Nullable QuotaManager animationQuotaManager,
                @Nullable TimeGateway timeGateway,
                @Nullable SensorGateway sensorGateway) {
            this.mPlatformDataSourcesInitiallyEnabled = platformDataSourcesInitiallyEnabled;
            this.mStateStore = stateStore;
            this.mAnimationQuotaManager = animationQuotaManager;
            this.mTimeGateway = timeGateway;
            this.mSensorGateway = sensorGateway;
        }

        /** Builds a {@link DynamicTypeEvaluator.Config}. */
        public static final class Builder {
            private boolean mPlatformDataSourcesInitiallyEnabled = false;
            @Nullable private StateStore mStateStore = null;
            @Nullable private QuotaManager mAnimationQuotaManager = null;
            @Nullable private TimeGateway mTimeGateway = null;
            @Nullable private SensorGateway mSensorGateway = null;

            /**
             * Sets whether sending updates from sensor and time sources should be allowed
             * initially. After that, enabling updates from sensor and time sources can be done via
             * {@link #enablePlatformDataSources()} or {@link #disablePlatformDataSources()}.
             *
             * <p>Defaults to {@code false}.
             */
            @NonNull
            public Builder setPlatformDataSourcesInitiallyEnabled(boolean value) {
                mPlatformDataSourcesInitiallyEnabled = value;
                return this;
            }

            /**
             * Sets the state store that will be used for dereferencing the state keys in the
             * dynamic types.
             *
             * <p>If not set, it's the equivalent of setting an empty state store (state bindings
             * will trigger {@link DynamicTypeValueReceiver#onInvalidated()}).
             */
            @NonNull
            public Builder setStateStore(@NonNull StateStore value) {
                mStateStore = value;
                return this;
            }

            /**
             * Sets the quota manager used for limiting the number of concurrently running
             * animations.
             *
             * <p>If not set, animations are disabled and non-infinite animations will have the end
             * value immediately.
             */
            @NonNull
            public Builder setAnimationQuotaManager(@NonNull QuotaManager value) {
                mAnimationQuotaManager = value;
                return this;
            }

            /**
             * Sets the gateway used for time data.
             *
             * <p>If not set, a default 1hz {@link TimeGateway} implementation that utilizes a
             * main-thread {@code Handler} to trigger is used.
             */
            @NonNull
            public Builder setTimeGateway(@NonNull TimeGateway value) {
                mTimeGateway = value;
                return this;
            }

            /**
             * Sets the gateway used for sensor data.
             *
             * <p>If not set, sensor data will not be available (sensor bindings will trigger {@link
             * DynamicTypeValueReceiver#onInvalidated()}).
             */
            @NonNull
            public Builder setSensorGateway(@NonNull SensorGateway value) {
                mSensorGateway = value;
                return this;
            }

            @NonNull
            public Config build() {
                return new Config(
                        mPlatformDataSourcesInitiallyEnabled,
                        mStateStore,
                        mAnimationQuotaManager,
                        mTimeGateway,
                        mSensorGateway);
            }
        }

        /**
         * Gets whether sending updates from sensor and time sources should be allowed initially.
         * After that, enabling updates from sensor and time sources can be done via {@link
         * #enablePlatformDataSources()} or {@link #disablePlatformDataSources()}.
         */
        public boolean isPlatformDataSourcesInitiallyEnabled() {
            return mPlatformDataSourcesInitiallyEnabled;
        }

        /**
         * Gets the state store that will be used for dereferencing the state keys in the dynamic
         * types, or {@code null} which is equivalent to an empty state store (state bindings will
         * trigger {@link DynamicTypeValueReceiver#onInvalidated()}).
         */
        @Nullable
        public StateStore getStateStore() {
            return mStateStore;
        }

        /**
         * Gets the quota manager used for limiting the number of concurrently running animations,
         * or {@code null} if animations are disabled, causing non-infinite animations to have to
         * the end value immediately.
         */
        @Nullable
        public QuotaManager getAnimationQuotaManager() {
            return mAnimationQuotaManager;
        }

        /**
         * Gets the gateway used for sensor data, or {@code null} if sensor data is unavailable
         * (sensor bindings will trigger {@link DynamicTypeValueReceiver#onInvalidated()}).
         */
        @Nullable
        public SensorGateway getSensorGateway() {
            return mSensorGateway;
        }

        /**
         * Gets the gateway used for time data, or {@code null} if a default 1hz {@link TimeGateway}
         * that utilizes a main-thread {@code Handler} to trigger is used.
         */
        @Nullable
        public TimeGateway getTimeGateway() {
            return mTimeGateway;
        }
    }

    /** Constructs a {@link DynamicTypeEvaluator}. */
    public DynamicTypeEvaluator(@NonNull Config config) {
        this.mConfig = config;
        this.mStateStore =
                config.getStateStore() != null ? config.getStateStore() : EMPTY_STATE_STORE;
        this.mAnimationQuotaManager =
                config.getAnimationQuotaManager() != null
                        ? config.getAnimationQuotaManager()
                        : DISABLED_ANIMATIONS_QUOTA_MANAGER;
        Handler uiHandler = new Handler(Looper.getMainLooper());
        MainThreadExecutor uiExecutor = new MainThreadExecutor(uiHandler);
        this.mTimeGateway =
                config.getTimeGateway() != null
                        ? config.getTimeGateway()
                        : new TimeGatewayImpl(uiHandler);
        this.mTimeDataSource = new EpochTimePlatformDataSource(uiExecutor, mTimeGateway);
        if (config.isPlatformDataSourcesInitiallyEnabled()
                && this.mTimeGateway instanceof TimeGatewayImpl) {
            ((TimeGatewayImpl) this.mTimeGateway).enableUpdates();
        }
        if (config.getSensorGateway() != null) {
            if (config.isPlatformDataSourcesInitiallyEnabled()) {
                config.getSensorGateway().enableUpdates();
            } else {
                config.getSensorGateway().disableUpdates();
            }
            this.mSensorGatewayDataSource =
                    new SensorGatewayPlatformDataSource(uiExecutor, config.getSensorGateway());
        } else {
            this.mSensorGatewayDataSource = null;
        }
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicString} for evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param stringSource The given String dynamic type that should be evaluated.
     * @param locale The locale used for the given String source.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicString stringSource,
            @NonNull ULocale locale,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<String> consumer) {
        return bind(
                stringSource.toDynamicStringProto(),
                locale,
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicString} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param stringSource The given String dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     * @param locale The locale used for the given String source.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicString stringSource,
            @NonNull ULocale locale,
            @NonNull DynamicTypeValueReceiver<String> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                stringSource,
                new DynamicTypeValueReceiverOnExecutor<>(consumer),
                locale,
                resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicInt32} for evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param int32Source The given integer dynamic type that should be evaluated.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicInt32 int32Source,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        return bind(
                int32Source.toDynamicInt32Proto(),
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicInt32} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param int32Source The given integer dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicInt32 int32Source,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                int32Source, new DynamicTypeValueReceiverOnExecutor<>(consumer), resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicFloat} for evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param floatSource The given float dynamic type that should be evaluated.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicFloat floatSource,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<Float> consumer) {
        return bind(
                floatSource.toDynamicFloatProto(),
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicFloat} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param floatSource The given float dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicFloat floatSource, @NonNull DynamicTypeValueReceiver<Float> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                floatSource, new DynamicTypeValueReceiverOnExecutor<>(consumer), resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicColor} for evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param colorSource The given color dynamic type that should be evaluated.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicColor colorSource,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        return bind(
                colorSource.toDynamicColorProto(),
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicColor} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param colorSource The given color dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiver<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                colorSource, new DynamicTypeValueReceiverOnExecutor<>(consumer), resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicDuration} for evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param durationSource The given duration dynamic type that should be evaluated.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicDuration durationSource,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<Duration> consumer) {
        return bind(
                durationSource.toDynamicDurationProto(),
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicDuration} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param durationSource The given durations dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicDuration durationSource,
            @NonNull DynamicTypeValueReceiver<Duration> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                durationSource, new DynamicTypeValueReceiverOnExecutor<>(consumer), resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicInstant} for evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param instantSource The given instant dynamic type that should be evaluated.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicInstant instantSource,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<Instant> consumer) {
        return bind(
                instantSource.toDynamicInstantProto(),
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicInstant} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param instantSource The given instant dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicInstant instantSource,
            @NonNull DynamicTypeValueReceiver<Instant> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                instantSource, new DynamicTypeValueReceiverOnExecutor<>(consumer), resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Adds dynamic type from the given {@link DynamicBuilders.DynamicBool} for evaluation.
     * Evaluation will start immediately.
     *
     * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
     * the given {@link Executor}.
     *
     * @param boolSource The given boolean dynamic type that should be evaluated.
     * @param executor The Executor to run the consumer on.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    public BoundDynamicType bind(
            @NonNull DynamicBuilders.DynamicBool boolSource,
            @NonNull Executor executor,
            @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
        return bind(
                boolSource.toDynamicBoolProto(),
                new DynamicTypeValueReceiverOnExecutor<>(executor, consumer));
    }

    /**
     * Adds pending dynamic type from the given {@link DynamicBool} for future evaluation.
     *
     * <p>Evaluation of this dynamic type will start when {@link BoundDynamicType#startEvaluation()}
     * is called on the returned object.
     *
     * @param boolSource The given boolean dynamic type that should be evaluated.
     * @param consumer The registered consumer for results of the evaluation. It will be called from
     *     UI thread.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BoundDynamicType bind(
            @NonNull DynamicBool boolSource, @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                boolSource, new DynamicTypeValueReceiverOnExecutor<>(consumer), resultBuilder);
        return new BoundDynamicTypeImpl(resultBuilder);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicString stringSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<String> consumer,
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
                            resultBuilder);
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
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicInt32 int32Source,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Integer> consumer,
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
                            resultBuilder);
                    break;
                }
            case DURATION_PART:
                {
                    GetDurationPartOpNode durationPartOpNode =
                            new GetDurationPartOpNode(int32Source.getDurationPart(), consumer);
                    node = durationPartOpNode;

                    bindRecursively(
                            int32Source.getDurationPart().getInput(),
                            durationPartOpNode.getIncomingCallback(),
                            resultBuilder);
                    break;
                }
            case ANIMATABLE_FIXED:

                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                node =
                        new AnimatableFixedInt32Node(
                                int32Source.getAnimatableFixed(), consumer, mAnimationQuotaManager);
                break;
            case ANIMATABLE_DYNAMIC:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                AnimatableDynamicInt32 dynamicNode = int32Source.getAnimatableDynamic();
                DynamicAnimatedInt32Node animationNode =
                        new DynamicAnimatedInt32Node(
                                consumer, dynamicNode.getAnimationSpec(), mAnimationQuotaManager);
                node = animationNode;

                bindRecursively(
                        dynamicNode.getInput(), animationNode.getInputCallback(), resultBuilder);
                break;
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicInt32 has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicInt32 source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicDuration durationSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Duration> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
        DynamicDataNode<?> node;

        switch (durationSource.getInnerCase()) {
            case BETWEEN:
                BetweenInstancesNode betweenInstancesNode = new BetweenInstancesNode(consumer);
                node = betweenInstancesNode;
                bindRecursively(
                        durationSource.getBetween().getStartInclusive(),
                        betweenInstancesNode.getLhsIncomingCallback(),
                        resultBuilder);
                bindRecursively(
                        durationSource.getBetween().getEndExclusive(),
                        betweenInstancesNode.getRhsIncomingCallback(),
                        resultBuilder);
                break;
            case FIXED:
                node = new FixedDurationNode(durationSource.getFixed(), consumer);
                break;
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicDuration has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicDuration source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicInstant instantSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Instant> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
        DynamicDataNode<?> node;

        switch (instantSource.getInnerCase()) {
            case FIXED:
                node = new FixedInstantNode(instantSource.getFixed(), consumer);
                break;
            case PLATFORM_SOURCE:
                node = new PlatformTimeSourceNode(mTimeDataSource, consumer);
                break;

            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicInstant has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicInstant source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicFloat floatSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Float> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
        DynamicDataNode<?> node;

        switch (floatSource.getInnerCase()) {
            case FIXED:
                node = new FixedFloatNode(floatSource.getFixed(), consumer);
                break;
            case STATE_SOURCE:
                node =
                        new StateFloatSourceNode(
                                mStateStore, floatSource.getStateSource(), consumer);
                break;
            case ARITHMETIC_OPERATION:
                {
                    ArithmeticFloatNode arithmeticNode =
                            new ArithmeticFloatNode(floatSource.getArithmeticOperation(), consumer);
                    node = arithmeticNode;

                    bindRecursively(
                            floatSource.getArithmeticOperation().getInputLhs(),
                            arithmeticNode.getLhsIncomingCallback(),
                            resultBuilder);
                    bindRecursively(
                            floatSource.getArithmeticOperation().getInputRhs(),
                            arithmeticNode.getRhsIncomingCallback(),
                            resultBuilder);

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
                            resultBuilder);
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            resultBuilder);

                    node = conditionalNode;
                    break;
                }
            case ANIMATABLE_FIXED:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                node =
                        new AnimatableFixedFloatNode(
                                floatSource.getAnimatableFixed(), consumer, mAnimationQuotaManager);
                break;
            case ANIMATABLE_DYNAMIC:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                AnimatableDynamicFloat dynamicNode = floatSource.getAnimatableDynamic();
                DynamicAnimatedFloatNode animationNode =
                        new DynamicAnimatedFloatNode(
                                consumer, dynamicNode.getAnimationSpec(), mAnimationQuotaManager);
                node = animationNode;

                bindRecursively(
                        dynamicNode.getInput(), animationNode.getInputCallback(), resultBuilder);
                break;

            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicFloat has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicFloat source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Integer> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder) {
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
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                node =
                        new AnimatableFixedColorNode(
                                colorSource.getAnimatableFixed(), consumer, mAnimationQuotaManager);
                break;
            case ANIMATABLE_DYNAMIC:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                AnimatableDynamicColor dynamicNode = colorSource.getAnimatableDynamic();
                DynamicAnimatedColorNode animationNode =
                        new DynamicAnimatedColorNode(
                                consumer, dynamicNode.getAnimationSpec(), mAnimationQuotaManager);
                node = animationNode;

                bindRecursively(
                        dynamicNode.getInput(), animationNode.getInputCallback(), resultBuilder);
                break;
            case CONDITIONAL_OP:
                ConditionalOpNode<Integer> conditionalNode = new ConditionalOpNode<>(consumer);

                ConditionalColorOp op = colorSource.getConditionalOp();
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
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicColor has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicColor source type");
        }

        resultBuilder.add(node);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicBool boolSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Boolean> consumer,
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
                            resultBuilder);
                    bindRecursively(
                            boolSource.getFloatComparison().getInputRhs(),
                            compNode.getRhsIncomingCallback(),
                            resultBuilder);

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
        if (this.mTimeGateway instanceof TimeGatewayImpl) {
            ((TimeGatewayImpl) mTimeGateway).enableUpdates();
        }
        if (mConfig.getSensorGateway() != null) {
            mConfig.getSensorGateway().enableUpdates();
        }
    }

    /** Disables sending updates on sensor and time. */
    @UiThread
    public void disablePlatformDataSources() {
        if (this.mTimeGateway instanceof TimeGatewayImpl) {
            ((TimeGatewayImpl) mTimeGateway).disableUpdates();
        }
        if (mConfig.getSensorGateway() != null) {
            mConfig.getSensorGateway().disableUpdates();
        }
    }

    /**
     * Closes resources owned by this {@link DynamicTypeEvaluator}.
     *
     * <p>This will not close provided resources, like the {@link TimeGateway} or {@link
     * SensorGateway}.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void close() {
        if (mTimeGateway instanceof TimeGatewayImpl) {
            try {
                ((TimeGatewayImpl) mTimeGateway).close();
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error while cleaning up time gateway", ex);
            }
        }
    }

    /**
     * Wraps {@link DynamicTypeValueReceiver} and executes its methods on the given {@link
     * Executor}.
     */
    private static class DynamicTypeValueReceiverOnExecutor<T>
            implements DynamicTypeValueReceiverWithPreUpdate<T> {

        @NonNull private final Executor mExecutor;
        @NonNull private final DynamicTypeValueReceiver<T> mConsumer;

        DynamicTypeValueReceiverOnExecutor(@NonNull DynamicTypeValueReceiver<T> consumer) {
            this(Runnable::run, consumer);
        }

        DynamicTypeValueReceiverOnExecutor(
                @NonNull Executor executor, @NonNull DynamicTypeValueReceiver<T> consumer) {
            this.mConsumer = consumer;
            this.mExecutor = executor;
        }

        /** This method is noop in this class. */
        @Override
        @SuppressWarnings("ExecutorTaskName")
        public void onPreUpdate() {}

        @Override
        @SuppressWarnings("ExecutorTaskName")
        public void onData(@NonNull T newData) {
            mExecutor.execute(() -> mConsumer.onData(newData));
        }

        @Override
        @SuppressWarnings("ExecutorTaskName")
        public void onInvalidated() {
            mExecutor.execute(mConsumer::onInvalidated);
        }
    }
}
