/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.wear.protolayout.expression.PlatformHealthSources.Keys.DAILY_STEPS;
import static androidx.wear.protolayout.expression.PlatformHealthSources.Keys.HEART_RATE_BPM;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_DIVIDE;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_UNDEFINED;
import static androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32RoundMode.ROUND_MODE_CEILING;
import static androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32RoundMode.ROUND_MODE_FLOOR;
import static androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32RoundMode.ROUND_MODE_ROUND;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;

import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.PlatformHealthSources;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.AnimatableFixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.ArithmeticInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.DynamicAnimatedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.GetDurationPartOpNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.LegacyPlatformInt32SourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DurationPartType;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetDurationPartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetZonedDateTimePartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32SourceType;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.ZonedDateTimePartType;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class Int32NodesTest {
    private static final PlatformDataValues STEPS_70 =
            PlatformDataValues.of(DAILY_STEPS, DynamicDataBuilders.DynamicDataValue.fromInt(70));
    private static final PlatformDataValues STEPS_80 =
            PlatformDataValues.of(DAILY_STEPS, DynamicDataBuilders.DynamicDataValue.fromInt(80));

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock private DynamicTypeValueReceiverWithPreUpdate<Integer> mMockValueReceiver;
    @Mock private PlatformDataProvider mMockDataProvider;

    private static final AppDataKey<DynamicInt32> KEY_FOO = new AppDataKey<>("foo");

    @Test
    public void testFixedInt32Node() {
        List<Integer> results = new ArrayList<>();

        FixedInt32 protoNode = FixedInt32.newBuilder().setValue(56).build();
        FixedInt32Node node = new FixedInt32Node(protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(56);
    }

    @Test
    public void testArithmeticOperation_unknownOp_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        evaluateArithmeticExpression(
                                1,
                                1,
                                ARITHMETIC_OP_TYPE_UNDEFINED.getNumber(),
                                new AddToListCallback<>(new ArrayList<>())));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        evaluateArithmeticExpression(
                                /* lhs= */ 1,
                                /* rhs= */ 1,
                                -1 /* UNRECOGNIZED */,
                                new AddToListCallback<>(new ArrayList<>())));
    }

    @Test
    public void testArithmeticOperation_validResult_invalidateNotCalled() {
        List<Integer> results = new ArrayList<>();
        List<Boolean> invalidList = new ArrayList<>();

        evaluateArithmeticExpression(
                /* lhs= */ 4,
                /* rhs= */ 3,
                ARITHMETIC_OP_TYPE_DIVIDE.getNumber(),
                new AddToListCallback<>(results, invalidList));

        assertThat(results).containsExactly(1);
        assertThat(invalidList).isEmpty();
    }

    @Test
    public void testArithmeticOperation_arithmeticExceptionThrown_invalidate() {
        List<Integer> results = new ArrayList<>();
        List<Boolean> invalidList = new ArrayList<>();

        evaluateArithmeticExpression(
                /* lhs= */ 0,
                /* rhs= */ 0,
                ARITHMETIC_OP_TYPE_DIVIDE.getNumber(),
                new AddToListCallback<>(results, invalidList));

        assertThat(results).isEmpty();
        assertThat(invalidList).containsExactly(true);
    }

    @Test
    public void testGetDurationPartOpNode_unknownPart_throws() {
        Duration duration = Duration.ofSeconds(123456);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_UNDEFINED));

        assertThrows(
                IllegalArgumentException.class,
                () -> createGetDurationPartOpNodeAndGetPart(duration, -1 /* UNRECOGNIZED */));
    }

    @Test
    public void testGetDurationPartOpNode_positiveDuration() {

        // Equivalent to 1day and 10h:17m:36s
        Duration duration = Duration.ofSeconds(123456);

        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_DAYS))
                .isEqualTo(1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_HOURS))
                .isEqualTo(10);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_MINUTES))
                .isEqualTo(17);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_SECONDS))
                .isEqualTo(36);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_DAYS))
                .isEqualTo(1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_HOURS))
                .isEqualTo(34);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_MINUTES))
                .isEqualTo(2057);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_SECONDS))
                .isEqualTo(123456);
    }

    @Test
    public void testGetDurationPartOpNode_negativeDuration() {

        // Equivalent to negative 1day and 10h:17m:36s
        Duration duration = Duration.ofSeconds(-123456);

        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_DAYS))
                .isEqualTo(1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_HOURS))
                .isEqualTo(10);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_MINUTES))
                .isEqualTo(17);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_SECONDS))
                .isEqualTo(36);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_DAYS))
                .isEqualTo(-1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_HOURS))
                .isEqualTo(-34);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_MINUTES))
                .isEqualTo(-2057);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_SECONDS))
                .isEqualTo(-123456);
    }

    private int createGetDurationPartOpNodeAndGetPart(Duration duration, DurationPartType part) {
        return createGetDurationPartOpNodeAndGetPart(duration, part.getNumber());
    }

    private int createGetDurationPartOpNodeAndGetPart(Duration duration, int part) {
        List<Integer> results = new ArrayList<>();
        GetDurationPartOpNode node =
                new GetDurationPartOpNode(
                        GetDurationPartOp.newBuilder().setDurationPartValue(part).build(),
                        new AddToListCallback<>(results));
        node.getIncomingCallback().onData(duration);
        return results.get(0);
    }

    private int createGetZonedDateTimeOpNodeAndGetPart(
            ZonedDateTime zdt, ZonedDateTimePartType partType) {
        List<Integer> results = new ArrayList<>();
        Int32Nodes.GetZonedDateTimePartOpNode node =
                new Int32Nodes.GetZonedDateTimePartOpNode(
                        GetZonedDateTimePartOp.newBuilder().setPartType(partType).build(),
                        new AddToListCallback<>(results));
        node.getIncomingCallback().onData(zdt);
        return results.get(0);
    }

    @Test
    public void stateInt32NodeTest() {
        List<Integer> results = new ArrayList<>();
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setInt32Val(FixedInt32.newBuilder().setValue(65))
                                        .build()));

        StateInt32Source protoNode = StateInt32Source.newBuilder().setSourceKey("foo").build();
        StateInt32SourceNode node =
                new StateInt32SourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(65);
    }

    @Test
    public void stateInt32UpdatesWithStateChanges() {
        List<Integer> results = new ArrayList<>();
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setInt32Val(FixedInt32.newBuilder().setValue(65))
                                        .build()));

        StateInt32Source protoNode = StateInt32Source.newBuilder().setSourceKey("foo").build();
        StateInt32SourceNode node =
                new StateInt32SourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        results.clear();

        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(12))
                                .build()));

        assertThat(results).containsExactly(12);
    }

    @Test
    public void stateInt32Source_canSubscribeToDailyStepsUpdates() {
        PlatformDataStore platformDataStore =
                new PlatformDataStore(
                        Collections.singletonMap(
                                PlatformHealthSources.Keys.DAILY_STEPS, mMockDataProvider));
        StateInt32Source dailyStepsSource =
                StateInt32Source.newBuilder()
                        .setSourceKey(PlatformHealthSources.Keys.DAILY_STEPS.getKey())
                        .setSourceNamespace(PlatformHealthSources.Keys.DAILY_STEPS.getNamespace())
                        .build();
        List<Integer> results = new ArrayList<>();
        StateInt32SourceNode dailyStepsSourceNode =
                new StateInt32SourceNode(
                        platformDataStore, dailyStepsSource, new AddToListCallback<>(results));

        dailyStepsSourceNode.preInit();
        dailyStepsSourceNode.init();
        ArgumentCaptor<PlatformDataReceiver> receiverCaptor =
                ArgumentCaptor.forClass(PlatformDataReceiver.class);
        verify(mMockDataProvider).setReceiver(any(), receiverCaptor.capture());

        PlatformDataReceiver receiver = receiverCaptor.getValue();
        receiver.onData(STEPS_70);
        assertThat(results).containsExactly(70);

        receiver.onData(STEPS_80);
        assertThat(results).containsExactly(70, 80).inOrder();
    }

    @Test
    public void stateInt32Source_init_callsOnDataOnlyOnce() {
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    ((PlatformDataReceiver) invocation.getArgument(1))
                                            .onData(STEPS_70);
                                    return null;
                                })
                .when(mMockDataProvider)
                .setReceiver(any(), any());
        PlatformDataStore platformDataStore =
                new PlatformDataStore(
                        Collections.singletonMap(
                                PlatformHealthSources.Keys.DAILY_STEPS, mMockDataProvider));
        StateInt32Source dailyStepsSource =
                StateInt32Source.newBuilder()
                        .setSourceKey(PlatformHealthSources.Keys.DAILY_STEPS.getKey())
                        .setSourceNamespace(PlatformHealthSources.Keys.DAILY_STEPS.getNamespace())
                        .build();
        List<Integer> results = new ArrayList<>();
        AddToListCallback<Integer> addToListCallback = new AddToListCallback<>(results);
        StateInt32SourceNode dailyStepsSourceNode =
                new StateInt32SourceNode(platformDataStore, dailyStepsSource, addToListCallback);

        dailyStepsSourceNode.preInit();
        dailyStepsSourceNode.init();

        assertThat(results).containsExactly(70);
        assertThat(addToListCallback.isPreUpdateAndUpdateInSync()).isTrue();
    }

    @Test
    public void animatableFixedInt32_animates() {
        int startValue = 3;
        int endValue = 33;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        AnimatableFixedInt32 protoNode =
                AnimatableFixedInt32.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AddToListCallback<Integer> addToListCallback = new AddToListCallback<>(results);
        AnimatableFixedInt32Node node =
                new AnimatableFixedInt32Node(protoNode, addToListCallback, quotaManager);
        node.setVisibility(true);

        node.preInit();
        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(startValue);
        assertThat(Iterables.getLast(results)).isEqualTo(endValue);
        assertThat(addToListCallback.isPreUpdateAndUpdateInSync()).isTrue();
    }

    @Test
    public void animatableFixedInt32_whenInvisible_skipToEnd() {
        int startValue = 3;
        int endValue = 33;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        AnimatableFixedInt32 protoNode =
                AnimatableFixedInt32.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AddToListCallback<Integer> addToListCallback = new AddToListCallback<>(results);
        AnimatableFixedInt32Node node =
                new AnimatableFixedInt32Node(protoNode, addToListCallback, quotaManager);
        node.setVisibility(false);

        node.preInit();
        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(endValue);
        assertThat(addToListCallback.isPreUpdateAndUpdateInSync()).isTrue();
    }

    @Test
    public void animatableFixedInt32_whenNoQuota_skip() {
        int startValue = 3;
        int endValue = 33;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(0);
        AnimatableFixedInt32 protoNode =
                AnimatableFixedInt32.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedInt32Node node =
                new AnimatableFixedInt32Node(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(true);

        node.preInit();
        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(endValue);
    }

    @Test
    public void dynamicAnimatedInt32_onlyAnimateWhenVisible() {
        int value1 = 3;
        int value2 = 11;
        int value3 = 17;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setInt32Val(
                                                FixedInt32.newBuilder().setValue(value1).build())
                                        .build()));
        AddToListCallback<Integer> addToListCallback = new AddToListCallback<>(results);
        DynamicAnimatedInt32Node int32Node =
                new DynamicAnimatedInt32Node(
                        addToListCallback, AnimationSpec.getDefaultInstance(), quotaManager);
        int32Node.setVisibility(false);
        StateInt32SourceNode stateNode =
                new StateInt32SourceNode(
                        oss,
                        StateInt32Source.newBuilder().setSourceKey("foo").build(),
                        int32Node.getInputCallback());

        stateNode.preInit();
        stateNode.init();

        results.clear();
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(value2))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Only contains last value.
        assertThat(results).containsExactly(value2);

        int32Node.setVisibility(true);
        results.clear();
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(value3))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Contains intermediate values besides the initial and last.
        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(value2);
        assertThat(Iterables.getLast(results)).isEqualTo(value3);
        assertThat(results).isInOrder();
        assertThat(addToListCallback.isPreUpdateAndUpdateInSync()).isTrue();
    }

    @Test
    public void platformInt32Source_canSubscribeToHeartRateUpdates() {
        PlatformDataStore platformDataStore =
                new PlatformDataStore(
                        Collections.singletonMap(
                                PlatformHealthSources.Keys.HEART_RATE_BPM, mMockDataProvider));
        PlatformInt32Source platformSource =
                PlatformInt32Source.newBuilder()
                        .setSourceType(
                                PlatformInt32SourceType
                                        .PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE)
                        .build();
        List<Integer> results = new ArrayList<>();
        LegacyPlatformInt32SourceNode platformSourceNode =
                new LegacyPlatformInt32SourceNode(
                        platformDataStore, platformSource, new AddToListCallback<>(results));

        platformSourceNode.preInit();
        platformSourceNode.init();
        ArgumentCaptor<PlatformDataReceiver> receiverCaptor =
                ArgumentCaptor.forClass(PlatformDataReceiver.class);
        verify(mMockDataProvider).setReceiver(any(), receiverCaptor.capture());

        PlatformDataReceiver receiver = receiverCaptor.getValue();
        receiver.onData(
                PlatformDataValues.of(
                        HEART_RATE_BPM, DynamicDataBuilders.DynamicDataValue.fromFloat(70.0f)));

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(70);

        receiver.onData(
                PlatformDataValues.of(
                        HEART_RATE_BPM, DynamicDataBuilders.DynamicDataValue.fromFloat(80.0f)));

        assertThat(results).containsExactly(70, 80).inOrder();
    }

    @Test
    public void platformInt32Source_canSubscribeToDailyStepsUpdates() {
        PlatformDataStore platformDataStore =
                new PlatformDataStore(
                        Collections.singletonMap(
                                PlatformHealthSources.Keys.DAILY_STEPS, mMockDataProvider));
        PlatformInt32Source platformSource =
                PlatformInt32Source.newBuilder()
                        .setSourceType(
                                PlatformInt32SourceType.PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT)
                        .build();
        List<Integer> results = new ArrayList<>();
        LegacyPlatformInt32SourceNode platformSourceNode =
                new LegacyPlatformInt32SourceNode(
                        platformDataStore, platformSource, new AddToListCallback<>(results));

        platformSourceNode.preInit();
        platformSourceNode.init();
        ArgumentCaptor<PlatformDataReceiver> receiverCaptor =
                ArgumentCaptor.forClass(PlatformDataReceiver.class);
        verify(mMockDataProvider).setReceiver(any(), receiverCaptor.capture());

        PlatformDataReceiver receiver = receiverCaptor.getValue();
        receiver.onData(STEPS_70);

        assertThat(results).containsExactly(70);

        receiver.onData(STEPS_80);

        assertThat(results).containsExactly(70, 80).inOrder();
    }

    @Test
    public void platformInt32Source_propagatesInvalidatedSignal() {
        PlatformDataStore platformDataStore =
                new PlatformDataStore(
                        Collections.singletonMap(
                                PlatformHealthSources.Keys.HEART_RATE_BPM, mMockDataProvider));
        PlatformInt32Source platformSource =
                PlatformInt32Source.newBuilder()
                        .setSourceType(
                                PlatformInt32SourceType
                                        .PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE)
                        .build();
        LegacyPlatformInt32SourceNode platformSourceNode =
                new LegacyPlatformInt32SourceNode(
                        platformDataStore, platformSource, mMockValueReceiver);

        platformSourceNode.preInit();
        verify(mMockValueReceiver).onPreUpdate();

        platformSourceNode.init();
        ArgumentCaptor<PlatformDataReceiver> receiverCaptor =
                ArgumentCaptor.forClass(PlatformDataReceiver.class);
        verify(mMockDataProvider).setReceiver(any(), receiverCaptor.capture());

        PlatformDataReceiver receiver = receiverCaptor.getValue();
        receiver.onInvalidated(ImmutableSet.of(HEART_RATE_BPM));

        verify(mMockValueReceiver).onInvalidated();
    }

    @Test
    public void testGetZonedDateTimePartOpNode() {

        // Thursday November 29, 1973 19:40:00 (pm) in time zone Europe/London (GMT)
        // Friday November 30, 1973 01:10:00 (am) in time zone Asia/Kathmandu (+0530)
        ZonedDateTime zonedDateTime =
                ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(123450000L), ZoneId.of("Asia/Katmandu"));

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime, ZonedDateTimePartType.ZONED_DATE_TIME_PART_YEAR))
                .isEqualTo(1973);

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime, ZonedDateTimePartType.ZONED_DATE_TIME_PART_MONTH))
                .isEqualTo(11);

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime,
                                ZonedDateTimePartType.ZONED_DATE_TIME_PART_DAY_OF_MONTH))
                .isEqualTo(30);

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime,
                                ZonedDateTimePartType.ZONED_DATE_TIME_PART_DAY_OF_WEEK))
                .isEqualTo(5);

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime, ZonedDateTimePartType.ZONED_DATE_TIME_PART_HOUR_24H))
                .isEqualTo(1);

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime, ZonedDateTimePartType.ZONED_DATE_TIME_PART_MINUTE))
                .isEqualTo(10);

        assertThat(
                        createGetZonedDateTimeOpNodeAndGetPart(
                                zonedDateTime, ZonedDateTimePartType.ZONED_DATE_TIME_PART_SECOND))
                .isEqualTo(0);
    }

    @Test
    public void testFloatToInt32Node() {
        assertThat(evaluateFloatToInt32Expression(12.49f, ROUND_MODE_CEILING)).isEqualTo(13);
        assertThat(evaluateFloatToInt32Expression(12.99f, ROUND_MODE_FLOOR)).isEqualTo(12);
        assertThat(evaluateFloatToInt32Expression(12.49f, ROUND_MODE_ROUND)).isEqualTo(12);
        assertThat(evaluateFloatToInt32Expression(12.50f, ROUND_MODE_ROUND)).isEqualTo(13);
    }

    @Test
    public void testFloatToInt32Node_unspecifiedRoundingMode_defaultsToFloor() {
        assertThat(evaluateFloatToInt32Expression(12.001f)).isEqualTo(12);
        assertThat(evaluateFloatToInt32Expression(12.999f)).isEqualTo(12);
        assertThat(evaluateFloatToInt32Expression(13.000f)).isEqualTo(13);
    }

    @Test
    public void testFloatToInt32Node__unrecognizedRoundType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateFloatToInt32Expression(12.34f, -1 /* UNRECOGNIZED */));
    }

    private static void evaluateArithmeticExpression(
            int lhs, int rhs, int opType, DynamicTypeValueReceiverWithPreUpdate<Integer> receiver) {
        DynamicProto.ArithmeticInt32Op protoNode =
                DynamicProto.ArithmeticInt32Op.newBuilder().setOperationTypeValue(opType).build();

        ArithmeticInt32Node node = new ArithmeticInt32Node(protoNode, receiver);

        FixedInt32 lhsProtoNode = FixedInt32.newBuilder().setValue(lhs).build();
        FixedInt32Node lhsNode = new FixedInt32Node(lhsProtoNode, node.getLhsIncomingCallback());

        FixedInt32 rhsProtoNode = FixedInt32.newBuilder().setValue(rhs).build();
        FixedInt32Node rhsNode = new FixedInt32Node(rhsProtoNode, node.getRhsIncomingCallback());
        lhsNode.preInit();
        rhsNode.preInit();

        lhsNode.init();
        rhsNode.init();
    }

    private static int evaluateFloatToInt32Expression(
            float value, DynamicProto.FloatToInt32RoundMode roundMode) {
        return evaluateFloatToInt32Expression(value, roundMode.getNumber());
    }

    private static int evaluateFloatToInt32Expression(float value, int roundMode) {
        List<Integer> results = new ArrayList<>();
        Int32Nodes.FloatToInt32Node node =
                new Int32Nodes.FloatToInt32Node(
                        DynamicProto.FloatToInt32Op.newBuilder()
                                .setRoundModeValue(roundMode)
                                .build(),
                        new AddToListCallback<>(results));
        node.getIncomingCallback().onPreUpdate();
        node.getIncomingCallback().onData(value);

        return results.get(0);
    }

    private static int evaluateFloatToInt32Expression(float value) {
        List<Integer> results = new ArrayList<>();
        Int32Nodes.FloatToInt32Node node =
                new Int32Nodes.FloatToInt32Node(
                        DynamicProto.FloatToInt32Op.newBuilder().build(),
                        new AddToListCallback<>(results));
        node.getIncomingCallback().onPreUpdate();
        node.getIncomingCallback().onData(value);

        return results.get(0);
    }
}
