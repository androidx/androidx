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

import android.annotation.SuppressLint;
import android.icu.util.ULocale;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.wear.protolayout.expression.PlatformDataKey;
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
import androidx.wear.protolayout.expression.pipeline.DurationNodes.StateDurationSourceNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.AnimatableFixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.ArithmeticFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.DynamicAnimatedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.FixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.Int32ToFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.StateFloatSourceNode;
import androidx.wear.protolayout.expression.pipeline.InstantNodes.FixedInstantNode;
import androidx.wear.protolayout.expression.pipeline.InstantNodes.PlatformTimeSourceNode;
import androidx.wear.protolayout.expression.pipeline.InstantNodes.StateInstantSourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.AnimatableFixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.ArithmeticInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.DynamicAnimatedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FloatToInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.GetDurationPartOpNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.GetZonedDateTimePartOpNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.LegacyPlatformInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FixedStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FloatFormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.Int32FormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StateStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StringConcatOpNode;
import androidx.wear.protolayout.expression.pipeline.ZonedDateTimeNodes.InstantToZonedDateTimeOpNode;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalColorOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalDurationOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalInstantOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalStringOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicDuration;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInstant;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicZonedDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Evaluates protolayout dynamic types.
 *
 * <p>Given a dynamic ProtoLayout data source, this builds up a sequence of {@link DynamicDataNode}
 * instances, which can source the required data, and transform it into its final form.
 *
 * <p>Data source can include animations which will then emit value transitions.
 *
 * <p>In order to evaluate dynamic types, the caller needs to create a {@link
 * DynamicTypeBindingRequest}, bind it using {@link #bind(DynamicTypeBindingRequest)} method and
 * then call {@link BoundDynamicType#startEvaluation()} on the resulted {@link BoundDynamicType} to
 * start evaluation. Starting evaluation can be done for batches of dynamic types.
 *
 * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
 * BoundDynamicType#close()}.
 */
public class DynamicTypeEvaluator {
    private static final String TAG = "DynamicTypeEvaluator";
    private static final QuotaManager NO_OP_QUOTA_MANAGER =
            new QuotaManager() {
                @Override
                public boolean tryAcquireQuota(int quota) {
                    return true;
                }

                @Override
                public void releaseQuota(int quota) {}
            };

    private static final @NonNull QuotaManager DISABLED_ANIMATIONS_QUOTA_MANAGER =
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

    /** Exception thrown when the binding of a {@link DynamicTypeBindingRequest} fails. */
    public static class EvaluationException extends Exception {
        public EvaluationException(@NonNull String message) {
            super(message);
        }
    }

    private static final @NonNull StateStore EMPTY_STATE_STORE = new StateStore(emptyMap());

    private final @NonNull StateStore mStateStore;
    private final @NonNull PlatformDataStore mPlatformDataStore;
    private final @NonNull QuotaManager mAnimationQuotaManager;
    private final @NonNull QuotaManager mDynamicTypesQuotaManager;
    private final @NonNull EpochTimePlatformDataSource mTimeDataSource;
    private final boolean mEnableExpressionDeduplication;

    /** Configuration for creating {@link DynamicTypeEvaluator}. */
    public static final class Config {
        private final @Nullable StateStore mStateStore;
        private final @Nullable QuotaManager mAnimationQuotaManager;
        private final @Nullable QuotaManager mDynamicTypesQuotaManager;
        private final boolean mEnableExpressionDeduplication;

        private final @NonNull Map<PlatformDataKey<?>, PlatformDataProvider>
                mSourceKeyToDataProviders = new ArrayMap<>();

        private final @Nullable PlatformTimeUpdateNotifier mPlatformTimeUpdateNotifier;
        private final @Nullable Supplier<Instant> mClock;

        Config(
                @Nullable StateStore stateStore,
                @Nullable QuotaManager animationQuotaManager,
                @Nullable QuotaManager dynamicTypesQuotaManager,
                boolean enableExpressionDeduplication,
                @NonNull Map<PlatformDataKey<?>, PlatformDataProvider> sourceKeyToDataProviders,
                @Nullable PlatformTimeUpdateNotifier platformTimeUpdateNotifier,
                @Nullable Supplier<Instant> clock) {
            this.mStateStore = stateStore;
            this.mAnimationQuotaManager = animationQuotaManager;
            this.mDynamicTypesQuotaManager = dynamicTypesQuotaManager;
            this.mEnableExpressionDeduplication = enableExpressionDeduplication;
            this.mSourceKeyToDataProviders.putAll(sourceKeyToDataProviders);
            this.mPlatformTimeUpdateNotifier = platformTimeUpdateNotifier;
            this.mClock = clock;
        }

        /** Builds a {@link DynamicTypeEvaluator.Config}. */
        public static final class Builder {
            private @Nullable StateStore mStateStore = null;
            private @Nullable QuotaManager mAnimationQuotaManager = null;
            private @Nullable QuotaManager mDynamicTypesQuotaManager = null;
            private boolean mEnableExpressionDeduplication = false;

            private final @NonNull Map<PlatformDataKey<?>, PlatformDataProvider>
                    mSourceKeyToDataProviders = new ArrayMap<>();

            private @Nullable PlatformTimeUpdateNotifier mPlatformTimeUpdateNotifier = null;
            private @Nullable Supplier<Instant> mClock = null;

            /**
             * Sets whether to enable caching and deduplication of dynamic types. Defaults to {@code
             * false}.
             */
            @NonNull
            @RestrictTo(Scope.LIBRARY_GROUP)
            public Builder setEnableExpressionDeduplication(boolean enableExpressionDeduplication) {
                mEnableExpressionDeduplication = enableExpressionDeduplication;
                return this;
            }

            /**
             * Sets the state store that will be used for dereferencing the state keys in the
             * dynamic types.
             *
             * <p>If not set, it's the equivalent of setting an empty state store (state bindings
             * will trigger {@link DynamicTypeValueReceiver#onInvalidated()}).
             */
            public @NonNull Builder setStateStore(@NonNull StateStore value) {
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
            public @NonNull Builder setAnimationQuotaManager(@NonNull QuotaManager value) {
                mAnimationQuotaManager = value;
                return this;
            }

            /**
             * Sets the quota manager used for limiting the total size of dynamic types in the
             * pipeline.
             *
             * <p>If not set, number of dynamic types will not be restricted.
             */
            public @NonNull Builder setDynamicTypesQuotaManager(@NonNull QuotaManager value) {
                mDynamicTypesQuotaManager = value;
                return this;
            }

            /**
             * Add a platform data provider and specify the keys it can provide dynamic data for.
             *
             * <p>The provider must support at least one key. If the provider supports multiple
             * keys, they should not be independent, as their values should always update together.
             * One data key must not have multiple providers, or an exception will be thrown.
             *
             * @throws IllegalArgumentException If a PlatformDataProvider supports an empty key set
             *     or if a key has multiple data providers.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder addPlatformDataProvider(
                    @NonNull PlatformDataProvider platformDataProvider,
                    @NonNull Set<PlatformDataKey<?>> supportedDataKeys) {
                if (supportedDataKeys.isEmpty()) {
                    throw new IllegalArgumentException(
                            "The PlatformDataProvider must support at least one key");
                }
                for (PlatformDataKey<?> dataKey : supportedDataKeys) {
                    // Throws exception when one data key has multiple providers.
                    if (mSourceKeyToDataProviders.containsKey(dataKey)) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Multiple data providers for PlatformDataKey (%s)",
                                        dataKey));
                    }
                    mSourceKeyToDataProviders.put(dataKey, platformDataProvider);
                }

                return this;
            }

            /**
             * Sets the notifier used for updating the platform time data. If not set, by default
             * platform time will be updated at 1Hz using a {@code Handler} on the main thread.
             */
            public @NonNull Builder setPlatformTimeUpdateNotifier(
                    @NonNull PlatformTimeUpdateNotifier notifier) {
                this.mPlatformTimeUpdateNotifier = notifier;
                return this;
            }

            /**
             * Sets the clock ({@link Instant} supplier) used for providing time data to bindings.
             * If not set, on every reevaluation, platform time for dynamic values will be set to
             * {@link Instant#now()}.
             */
            @VisibleForTesting
            public @NonNull Builder setClock(@NonNull Supplier<Instant> clock) {
                this.mClock = clock;
                return this;
            }

            public @NonNull Config build() {
                return new Config(
                        mStateStore,
                        mAnimationQuotaManager,
                        mDynamicTypesQuotaManager,
                        mEnableExpressionDeduplication,
                        mSourceKeyToDataProviders,
                        mPlatformTimeUpdateNotifier,
                        mClock);
            }
        }

        /**
         * Gets the state store that will be used for dereferencing the state keys in the dynamic
         * types, or {@code null} which is equivalent to an empty state store (state bindings will
         * trigger {@link DynamicTypeValueReceiver#onInvalidated()}).
         */
        public @Nullable StateStore getStateStore() {
            return mStateStore;
        }

        /**
         * Gets the quota manager used for limiting the number of concurrently running animations,
         * or {@code null} if animations are disabled, causing non-infinite animations to have to
         * the end value immediately.
         */
        public @Nullable QuotaManager getAnimationQuotaManager() {
            return mAnimationQuotaManager;
        }

        /**
         * Gets the quota manager used for limiting the total number of dynamic types in the
         * pipeline, or {@code null} if there are no restriction on the number of dynamic types. If
         * present, the quota manager is used to prevent unreasonably expensive expressions.
         */
        public @Nullable QuotaManager getDynamicTypesQuotaManager() {
            return mDynamicTypesQuotaManager;
        }

        /** Returns whether deduplication of dynamic types is enabled. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public boolean isExpressionDeduplicationEnabled() {
            return mEnableExpressionDeduplication;
        }

        /** Returns any available mapping between source key and its data provider. */
        public @NonNull Map<PlatformDataKey<?>, PlatformDataProvider> getPlatformDataProviders() {
            return new ArrayMap<>(
                    (ArrayMap<PlatformDataKey<?>, PlatformDataProvider>) mSourceKeyToDataProviders);
        }

        /**
         * Returns the clock ({@link Instant} supplier) used for providing time data to bindings, or
         * {@code null} which means on every reevaluation, platform time for dynamic values will be
         * set to {@link Instant#now()}.
         */
        @VisibleForTesting
        public @Nullable Supplier<Instant> getClock() {
            return mClock;
        }

        /** Gets the notifier used for updating the platform time data. */
        public @Nullable PlatformTimeUpdateNotifier getPlatformTimeUpdateNotifier() {
            return mPlatformTimeUpdateNotifier;
        }
    }

    /** Constructs a {@link DynamicTypeEvaluator}. */
    public DynamicTypeEvaluator(@NonNull Config config) {
        this.mStateStore =
                config.getStateStore() != null ? config.getStateStore() : EMPTY_STATE_STORE;
        this.mAnimationQuotaManager =
                config.getAnimationQuotaManager() != null
                        ? config.getAnimationQuotaManager()
                        : DISABLED_ANIMATIONS_QUOTA_MANAGER;
        this.mDynamicTypesQuotaManager =
                config.getDynamicTypesQuotaManager() != null
                        ? config.getDynamicTypesQuotaManager()
                        : NO_OP_QUOTA_MANAGER;
        this.mPlatformDataStore = new PlatformDataStore(config.getPlatformDataProviders());
        this.mEnableExpressionDeduplication = config.isExpressionDeduplicationEnabled();
        PlatformTimeUpdateNotifier notifier = config.getPlatformTimeUpdateNotifier();
        if (notifier == null) {
            notifier = new PlatformTimeUpdateNotifierImpl();
            ((PlatformTimeUpdateNotifierImpl) notifier).setUpdatesEnabled(true);
        }
        Supplier<Instant> clock = config.getClock() != null ? config.getClock() : Instant::now;
        this.mTimeDataSource = new EpochTimePlatformDataSource(clock, notifier);
    }

    /**
     * Binds a {@link DynamicTypeBindingRequest}.
     *
     * <p>Evaluation of this request will start when {@link BoundDynamicType#startEvaluation()} is
     * called on the returned object.
     *
     * @throws EvaluationException when {@link QuotaManager} fails to allocate enough quota to bind
     *     the {@link DynamicTypeBindingRequest}.
     */
    public @NonNull BoundDynamicType bind(@NonNull DynamicTypeBindingRequest request)
            throws EvaluationException {
        BoundDynamicTypeImpl boundDynamicType = request.callBindOn(this);
        int dynamicNodeCost = boundDynamicType.getDynamicNodeCost();
        if (!mDynamicTypesQuotaManager.tryAcquireQuota(dynamicNodeCost)) {
            throw new EvaluationException(
                    "Dynamic type expression limit reached, failed to acquire quota of "
                            + dynamicNodeCost
                            + ". Try making the dynamic type expression shorter or reduce the"
                            + " number of dynamic type expressions.");
        }
        return boundDynamicType;
    }

    /** A wrapper for a proto message that provides content-based equality. */
    private static class SubtreeWrapper {
        private final Object mProtoMessage;

        SubtreeWrapper(Object protoMessage) {
            this.mProtoMessage = protoMessage;
        }

        @Override
        public int hashCode() {
            return DynamicProtoHashEquals.hashCode(mProtoMessage);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SubtreeWrapper)) {
                return false;
            }
            SubtreeWrapper that = (SubtreeWrapper) obj;
            return DynamicProtoHashEquals.equals(mProtoMessage, that.mProtoMessage);
        }
    }

    private static class Port<T> implements DynamicTypeValueReceiverWithPreUpdate<T> {
        private final List<DynamicTypeValueReceiverWithPreUpdate<T>> mConsumers = new ArrayList<>();

        void addConsumer(DynamicTypeValueReceiverWithPreUpdate<T> consumer) {
            mConsumers.add(consumer);
        }

        @Override
        public void onPreUpdate() {
            for (DynamicTypeValueReceiverWithPreUpdate<T> consumer : mConsumers) {
                consumer.onPreUpdate();
            }
        }

        @Override
        public void onData(@NonNull T newData) {
            for (DynamicTypeValueReceiverWithPreUpdate<T> consumer : mConsumers) {
                consumer.onData(newData);
            }
        }

        @Override
        public void onInvalidated() {
            for (DynamicTypeValueReceiverWithPreUpdate<T> consumer : mConsumers) {
                consumer.onInvalidated();
            }
        }
    }

    /**
     * A cache for {@link DynamicDataNode} instances. This is used to avoid creating duplicate nodes
     * for the same subtree.
     */
    private static class NodesCache {
        private final Map<SubtreeWrapper, Port<?>> mCache = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Nullable <T> Port<T> get(Object protoMessage) {
            return (Port<T>) mCache.get(new SubtreeWrapper(protoMessage));
        }

        <T> void put(Object protoMessage, Port<T> port) {
            mCache.put(new SubtreeWrapper(protoMessage), port);
        }
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicString stringSource,
            @NonNull ULocale locale,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<String> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                stringSource,
                consumer,
                locale,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicInt32 int32Source,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                int32Source,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicFloat floatSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Float> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                floatSource,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicColor colorSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Integer> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                colorSource,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicDuration durationSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Duration> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                durationSource,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicInstant instantSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Instant> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                instantSource,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicZonedDateTime zdtSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<ZonedDateTime> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                zdtSource,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull BoundDynamicTypeImpl bindInternal(
            @NonNull DynamicBool boolSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<Boolean> consumer) {
        List<DynamicDataNode<?>> resultBuilder = new ArrayList<>();
        bindRecursively(
                boolSource,
                consumer,
                resultBuilder,
                mEnableExpressionDeduplication ? new NodesCache() : null);
        return new BoundDynamicTypeImpl(resultBuilder, mDynamicTypesQuotaManager);
    }

    /**
     * Same as {@link #bind}, but instead of returning one {@link BoundDynamicType}, all {@link
     * DynamicDataNode} produced by evaluating given dynamic type are added to the given list.
     */
    private void bindRecursively(
            @NonNull DynamicString stringSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<String> consumer,
            @NonNull ULocale locale,
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<String> port = cache.get(stringSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<String> downstreamConsumer = consumer;
        if (cache != null) {
            Port<String> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(stringSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (stringSource.getInnerCase()) {
            case FIXED:
                node = new FixedStringNode(stringSource.getFixed(), downstreamConsumer);
                break;
            case INT32_FORMAT_OP:
                {
                    NumberFormatter formatter =
                            new NumberFormatter(stringSource.getInt32FormatOp(), locale);
                    Int32FormatNode int32FormatNode =
                            new Int32FormatNode(formatter, downstreamConsumer);
                    node = int32FormatNode;
                    bindRecursively(
                            stringSource.getInt32FormatOp().getInput(),
                            int32FormatNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case FLOAT_FORMAT_OP:
                {
                    NumberFormatter formatter =
                            new NumberFormatter(stringSource.getFloatFormatOp(), locale);
                    FloatFormatNode floatFormatNode =
                            new FloatFormatNode(formatter, downstreamConsumer);
                    node = floatFormatNode;
                    bindRecursively(
                            stringSource.getFloatFormatOp().getInput(),
                            floatFormatNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case STATE_SOURCE:
                {
                    DynamicProto.StateStringSource stateSource = stringSource.getStateSource();
                    node =
                            new StateStringNode(
                                    stateSource.getSourceNamespace().isEmpty()
                                            ? mStateStore
                                            : mPlatformDataStore,
                                    stateSource,
                                    downstreamConsumer);
                    break;
                }
            case CONDITIONAL_OP:
                {
                    ConditionalOpNode<String> conditionalNode =
                            new ConditionalOpNode<>(downstreamConsumer);

                    ConditionalStringOp op = stringSource.getConditionalOp();
                    bindRecursively(
                            op.getCondition(),
                            conditionalNode.getConditionIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            op.getValueIfTrue(),
                            conditionalNode.getTrueValueIncomingCallback(),
                            locale,
                            resultBuilder,
                            cache);
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            locale,
                            resultBuilder,
                            cache);

                    node = conditionalNode;
                    break;
                }
            case CONCAT_OP:
                {
                    StringConcatOpNode concatNode = new StringConcatOpNode(downstreamConsumer);
                    node = concatNode;
                    bindRecursively(
                            stringSource.getConcatOp().getInputLhs(),
                            concatNode.getLhsIncomingCallback(),
                            locale,
                            resultBuilder,
                            cache);
                    bindRecursively(
                            stringSource.getConcatOp().getInputRhs(),
                            concatNode.getRhsIncomingCallback(),
                            locale,
                            resultBuilder,
                            cache);
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
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<Integer> port = cache.get(int32Source);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<Integer> downstreamConsumer = consumer;
        if (cache != null) {
            Port<Integer> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(int32Source, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<Integer> node;

        switch (int32Source.getInnerCase()) {
            case FIXED:
                node = new FixedInt32Node(int32Source.getFixed(), downstreamConsumer);
                break;
            case PLATFORM_SOURCE:
                {
                    node =
                            new LegacyPlatformInt32SourceNode(
                                    mPlatformDataStore,
                                    int32Source.getPlatformSource(),
                                    downstreamConsumer);
                    break;
                }
            case ARITHMETIC_OPERATION:
                {
                    ArithmeticInt32Node arithmeticNode =
                            new ArithmeticInt32Node(
                                    int32Source.getArithmeticOperation(), downstreamConsumer);
                    node = arithmeticNode;

                    bindRecursively(
                            int32Source.getArithmeticOperation().getInputLhs(),
                            arithmeticNode.getLhsIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            int32Source.getArithmeticOperation().getInputRhs(),
                            arithmeticNode.getRhsIncomingCallback(),
                            resultBuilder,
                            cache);

                    break;
                }
            case STATE_SOURCE:
                {
                    DynamicProto.StateInt32Source stateSource = int32Source.getStateSource();
                    node =
                            new StateInt32SourceNode(
                                    stateSource.getSourceNamespace().isEmpty()
                                            ? mStateStore
                                            : mPlatformDataStore,
                                    stateSource,
                                    downstreamConsumer);
                    break;
                }
            case CONDITIONAL_OP:
                {
                    ConditionalOpNode<Integer> conditionalNode =
                            new ConditionalOpNode<>(downstreamConsumer);

                    ConditionalInt32Op op = int32Source.getConditionalOp();
                    bindRecursively(
                            op.getCondition(),
                            conditionalNode.getConditionIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            op.getValueIfTrue(),
                            conditionalNode.getTrueValueIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            resultBuilder,
                            cache);

                    node = conditionalNode;
                    break;
                }
            case FLOAT_TO_INT:
                {
                    FloatToInt32Node conversionNode =
                            new FloatToInt32Node(int32Source.getFloatToInt(), downstreamConsumer);
                    node = conversionNode;

                    bindRecursively(
                            int32Source.getFloatToInt().getInput(),
                            conversionNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case DURATION_PART:
                {
                    GetDurationPartOpNode durationPartOpNode =
                            new GetDurationPartOpNode(
                                    int32Source.getDurationPart(), downstreamConsumer);
                    node = durationPartOpNode;

                    bindRecursively(
                            int32Source.getDurationPart().getInput(),
                            durationPartOpNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case ZONED_DATE_TIME_PART:
                {
                    GetZonedDateTimePartOpNode zdtPartOpNode =
                            new GetZonedDateTimePartOpNode(
                                    int32Source.getZonedDateTimePart(), downstreamConsumer);
                    node = zdtPartOpNode;

                    bindRecursively(
                            int32Source.getZonedDateTimePart().getInput(),
                            zdtPartOpNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case ANIMATABLE_FIXED:

                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                node =
                        new AnimatableFixedInt32Node(
                                int32Source.getAnimatableFixed(),
                                downstreamConsumer,
                                mAnimationQuotaManager);
                break;
            case ANIMATABLE_DYNAMIC:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                AnimatableDynamicInt32 dynamicNode = int32Source.getAnimatableDynamic();
                DynamicAnimatedInt32Node animationNode =
                        new DynamicAnimatedInt32Node(
                                downstreamConsumer,
                                dynamicNode.getAnimationSpec(),
                                mAnimationQuotaManager);
                node = animationNode;

                bindRecursively(
                        dynamicNode.getInput(),
                        animationNode.getInputCallback(),
                        resultBuilder,
                        cache);
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
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<Duration> port = cache.get(durationSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<Duration> downstreamConsumer = consumer;
        if (cache != null) {
            Port<Duration> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(durationSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (durationSource.getInnerCase()) {
            case BETWEEN:
                BetweenInstancesNode betweenInstancesNode =
                        new BetweenInstancesNode(downstreamConsumer);
                node = betweenInstancesNode;
                bindRecursively(
                        durationSource.getBetween().getStartInclusive(),
                        betweenInstancesNode.getLhsIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        durationSource.getBetween().getEndExclusive(),
                        betweenInstancesNode.getRhsIncomingCallback(),
                        resultBuilder,
                        cache);
                break;
            case FIXED:
                node = new FixedDurationNode(durationSource.getFixed(), downstreamConsumer);
                break;
            case CONDITIONAL_OP:
                ConditionalOpNode<Duration> conditionalNode =
                        new ConditionalOpNode<>(downstreamConsumer);

                ConditionalDurationOp op = durationSource.getConditionalOp();
                bindRecursively(
                        op.getCondition(),
                        conditionalNode.getConditionIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        op.getValueIfTrue(),
                        conditionalNode.getTrueValueIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        op.getValueIfFalse(),
                        conditionalNode.getFalseValueIncomingCallback(),
                        resultBuilder,
                        cache);

                node = conditionalNode;
                break;
            case STATE_SOURCE:
                {
                    DynamicProto.StateDurationSource stateSource = durationSource.getStateSource();
                    node =
                            new StateDurationSourceNode(
                                    stateSource.getSourceNamespace().isEmpty()
                                            ? mStateStore
                                            : mPlatformDataStore,
                                    stateSource,
                                    downstreamConsumer);
                    break;
                }
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
            @NonNull DynamicZonedDateTime zdtSource,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<ZonedDateTime> consumer,
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<ZonedDateTime> port = cache.get(zdtSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<ZonedDateTime> downstreamConsumer = consumer;
        if (cache != null) {
            Port<ZonedDateTime> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(zdtSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (zdtSource.getInnerCase()) {
            case INSTANT_TO_ZONED_DATE_TIME:
                {
                    InstantToZonedDateTimeOpNode conversionNode =
                            new InstantToZonedDateTimeOpNode(
                                    zdtSource.getInstantToZonedDateTime(), downstreamConsumer);
                    node = conversionNode;

                    bindRecursively(
                            zdtSource.getInstantToZonedDateTime().getInstant(),
                            conversionNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicZonedDateTime has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicZonedDateTime source type");
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
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<Instant> port = cache.get(instantSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<Instant> downstreamConsumer = consumer;
        if (cache != null) {
            Port<Instant> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(instantSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (instantSource.getInnerCase()) {
            case FIXED:
                node = new FixedInstantNode(instantSource.getFixed(), downstreamConsumer);
                break;
            case PLATFORM_SOURCE:
                node = new PlatformTimeSourceNode(mTimeDataSource, downstreamConsumer);
                break;
            case CONDITIONAL_OP:
                ConditionalOpNode<Instant> conditionalNode =
                        new ConditionalOpNode<>(downstreamConsumer);

                ConditionalInstantOp op = instantSource.getConditionalOp();
                bindRecursively(
                        op.getCondition(),
                        conditionalNode.getConditionIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        op.getValueIfTrue(),
                        conditionalNode.getTrueValueIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        op.getValueIfFalse(),
                        conditionalNode.getFalseValueIncomingCallback(),
                        resultBuilder,
                        cache);

                node = conditionalNode;
                break;

            case STATE_SOURCE:
                {
                    DynamicProto.StateInstantSource stateSource = instantSource.getStateSource();
                    node =
                            new StateInstantSourceNode(
                                    stateSource.getSourceNamespace().isEmpty()
                                            ? mStateStore
                                            : mPlatformDataStore,
                                    stateSource,
                                    downstreamConsumer);
                    break;
                }
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
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<Float> port = cache.get(floatSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<Float> downstreamConsumer = consumer;
        if (cache != null) {
            Port<Float> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(floatSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (floatSource.getInnerCase()) {
            case FIXED:
                node = new FixedFloatNode(floatSource.getFixed(), downstreamConsumer);
                break;
            case STATE_SOURCE:
                {
                    DynamicProto.StateFloatSource stateSource = floatSource.getStateSource();
                    node =
                            new StateFloatSourceNode(
                                    stateSource.getSourceNamespace().isEmpty()
                                            ? mStateStore
                                            : mPlatformDataStore,
                                    stateSource,
                                    downstreamConsumer);
                    break;
                }
            case ARITHMETIC_OPERATION:
                {
                    ArithmeticFloatNode arithmeticNode =
                            new ArithmeticFloatNode(
                                    floatSource.getArithmeticOperation(), downstreamConsumer);
                    node = arithmeticNode;

                    bindRecursively(
                            floatSource.getArithmeticOperation().getInputLhs(),
                            arithmeticNode.getLhsIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            floatSource.getArithmeticOperation().getInputRhs(),
                            arithmeticNode.getRhsIncomingCallback(),
                            resultBuilder,
                            cache);

                    break;
                }
            case INT32_TO_FLOAT_OPERATION:
                {
                    Int32ToFloatNode toFloatNode = new Int32ToFloatNode(downstreamConsumer);
                    node = toFloatNode;

                    bindRecursively(
                            floatSource.getInt32ToFloatOperation().getInput(),
                            toFloatNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case CONDITIONAL_OP:
                {
                    ConditionalOpNode<Float> conditionalNode =
                            new ConditionalOpNode<>(downstreamConsumer);

                    ConditionalFloatOp op = floatSource.getConditionalOp();
                    bindRecursively(
                            op.getCondition(),
                            conditionalNode.getConditionIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            op.getValueIfTrue(),
                            conditionalNode.getTrueValueIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            op.getValueIfFalse(),
                            conditionalNode.getFalseValueIncomingCallback(),
                            resultBuilder,
                            cache);

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
                                floatSource.getAnimatableFixed(),
                                downstreamConsumer,
                                mAnimationQuotaManager);
                break;
            case ANIMATABLE_DYNAMIC:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                AnimatableDynamicFloat dynamicNode = floatSource.getAnimatableDynamic();
                DynamicAnimatedFloatNode animationNode =
                        new DynamicAnimatedFloatNode(
                                downstreamConsumer,
                                dynamicNode.getAnimationSpec(),
                                mAnimationQuotaManager);
                node = animationNode;

                bindRecursively(
                        dynamicNode.getInput(),
                        animationNode.getInputCallback(),
                        resultBuilder,
                        cache);
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
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<Integer> port = cache.get(colorSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<Integer> downstreamConsumer = consumer;
        if (cache != null) {
            Port<Integer> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(colorSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (colorSource.getInnerCase()) {
            case FIXED:
                node = new FixedColorNode(colorSource.getFixed(), downstreamConsumer);
                break;
            case STATE_SOURCE:
                DynamicProto.StateColorSource stateSource = colorSource.getStateSource();
                node =
                        new StateColorSourceNode(
                                stateSource.getSourceNamespace().isEmpty()
                                        ? mStateStore
                                        : mPlatformDataStore,
                                stateSource,
                                downstreamConsumer);
                break;
            case ANIMATABLE_FIXED:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                node =
                        new AnimatableFixedColorNode(
                                colorSource.getAnimatableFixed(),
                                downstreamConsumer,
                                mAnimationQuotaManager);
                break;
            case ANIMATABLE_DYNAMIC:
                // We don't have to check if enableAnimations is true, because if it's false and
                // we didn't have static value set, constructor has put QuotaManager that don't
                // have any quota, so animations won't be played and they would jump to the end
                // value.
                AnimatableDynamicColor dynamicNode = colorSource.getAnimatableDynamic();
                DynamicAnimatedColorNode animationNode =
                        new DynamicAnimatedColorNode(
                                downstreamConsumer,
                                dynamicNode.getAnimationSpec(),
                                mAnimationQuotaManager);
                node = animationNode;

                bindRecursively(
                        dynamicNode.getInput(),
                        animationNode.getInputCallback(),
                        resultBuilder,
                        cache);
                break;
            case CONDITIONAL_OP:
                ConditionalOpNode<Integer> conditionalNode =
                        new ConditionalOpNode<>(downstreamConsumer);

                ConditionalColorOp op = colorSource.getConditionalOp();
                bindRecursively(
                        op.getCondition(),
                        conditionalNode.getConditionIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        op.getValueIfTrue(),
                        conditionalNode.getTrueValueIncomingCallback(),
                        resultBuilder,
                        cache);
                bindRecursively(
                        op.getValueIfFalse(),
                        conditionalNode.getFalseValueIncomingCallback(),
                        resultBuilder,
                        cache);

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
            @NonNull List<DynamicDataNode<?>> resultBuilder,
            @Nullable NodesCache cache) {
        if (cache != null) {
            Port<Boolean> port = cache.get(boolSource);
            if (port != null) {
                port.addConsumer(consumer);
                return;
            }
        }

        DynamicTypeValueReceiverWithPreUpdate<Boolean> downstreamConsumer = consumer;
        if (cache != null) {
            Port<Boolean> newPort = new Port<>();
            newPort.addConsumer(consumer);
            cache.put(boolSource, newPort);
            downstreamConsumer = newPort;
        }

        DynamicDataNode<?> node;

        switch (boolSource.getInnerCase()) {
            case FIXED:
                node = new FixedBoolNode(boolSource.getFixed(), downstreamConsumer);
                break;
            case STATE_SOURCE:
                {
                    DynamicProto.StateBoolSource stateSource = boolSource.getStateSource();
                    node =
                            new StateBoolNode(
                                    stateSource.getSourceNamespace().isEmpty()
                                            ? mStateStore
                                            : mPlatformDataStore,
                                    stateSource,
                                    downstreamConsumer);
                    break;
                }
            case INT32_COMPARISON:
                {
                    ComparisonInt32Node compNode =
                            new ComparisonInt32Node(
                                    boolSource.getInt32Comparison(), downstreamConsumer);
                    node = compNode;

                    bindRecursively(
                            boolSource.getInt32Comparison().getInputLhs(),
                            compNode.getLhsIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            boolSource.getInt32Comparison().getInputRhs(),
                            compNode.getRhsIncomingCallback(),
                            resultBuilder,
                            cache);

                    break;
                }
            case LOGICAL_OP:
                {
                    LogicalBoolOp logicalNode =
                            new LogicalBoolOp(boolSource.getLogicalOp(), downstreamConsumer);
                    node = logicalNode;

                    bindRecursively(
                            boolSource.getLogicalOp().getInputLhs(),
                            logicalNode.getLhsIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            boolSource.getLogicalOp().getInputRhs(),
                            logicalNode.getRhsIncomingCallback(),
                            resultBuilder,
                            cache);

                    break;
                }
            case NOT_OP:
                {
                    NotBoolOp notNode = new NotBoolOp(downstreamConsumer);
                    node = notNode;
                    bindRecursively(
                            boolSource.getNotOp().getInput(),
                            notNode.getIncomingCallback(),
                            resultBuilder,
                            cache);
                    break;
                }
            case FLOAT_COMPARISON:
                {
                    ComparisonFloatNode compNode =
                            new ComparisonFloatNode(
                                    boolSource.getFloatComparison(), downstreamConsumer);
                    node = compNode;

                    bindRecursively(
                            boolSource.getFloatComparison().getInputLhs(),
                            compNode.getLhsIncomingCallback(),
                            resultBuilder,
                            cache);
                    bindRecursively(
                            boolSource.getFloatComparison().getInputRhs(),
                            compNode.getRhsIncomingCallback(),
                            resultBuilder,
                            cache);

                    break;
                }
            case INNER_NOT_SET:
                throw new IllegalArgumentException("DynamicBool has no inner source set");
            default:
                throw new IllegalArgumentException("Unknown DynamicBool source type");
        }

        resultBuilder.add(node);
    }
}
