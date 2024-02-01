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

import static androidx.wear.protolayout.expression.DynamicBuilders.DynamicString.constant;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;

import android.graphics.Color;
import android.icu.util.ULocale;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat.FloatFormatter;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32.IntFormatter;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator.EvaluationException;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedBool;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class ParametrizedDynamicTypeEvaluatorTest {

    private static final ZoneId GMT_PLUS_TWO = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(2));
    private static final ZoneId ASIA_KATHMANDU = ZoneId.of("Asia/Kathmandu");
    private static final ZoneId EUROPE_LONDON = ZoneId.of("Europe/London");
    private static final DynamicInstant FIXED_INSTANT =
            DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123450000L));

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static ImmutableList<Object[]> params() {
        AppDataKey<DynamicInt32> int32Source = new AppDataKey<>("state_int_15");
        ParametrizedDynamicTypeEvaluatorTest.TestCase<?>[] testCases = {
            test(constant("hello"), "hello"),
            test(DynamicString.from(new AppDataKey<>("state_hello_world")), "hello_world"),
            test(DynamicInt32.constant(5).format(), "5"),
            test(DynamicInt32.constant(10), 10),
            test(DynamicInt32.from(int32Source), 15),
            test(DynamicInt32.from(int32Source).plus(DynamicInt32.constant(2)), 17),
            test(DynamicInt32.from(int32Source).minus(DynamicInt32.constant(5)), 10),
            test(DynamicInt32.from(int32Source).times(DynamicInt32.constant(2)), 30),
            test(DynamicInt32.from(int32Source).div(DynamicInt32.constant(3)), 5),
            test(DynamicInt32.from(int32Source).rem(DynamicInt32.constant(2)), 1),
            test(DynamicInt32.from(int32Source).plus(2), 17),
            test(DynamicInt32.from(int32Source).minus(5), 10),
            test(DynamicInt32.from(int32Source).times(2), 30),
            test(DynamicInt32.from(int32Source).div(3), 5),
            test(DynamicInt32.from(int32Source).rem(2), 1),
            test(DynamicInt32.from(int32Source).plus(2.5f), 17.5f),
            test(DynamicInt32.from(int32Source).minus(5.5f), 9.5f),
            test(DynamicInt32.from(int32Source).times(2.5f), 37.5f),
            test(DynamicInt32.from(int32Source).div(2.0f), 7.5f),
            test(DynamicInt32.from(int32Source).rem(4.5f), 1.5f),
            test(DynamicInt32.from(int32Source).plus(DynamicFloat.constant(2.5f)), 17.5f),
            test(DynamicInt32.from(int32Source).minus(DynamicFloat.constant(5.5f)), 9.5f),
            test(DynamicInt32.from(int32Source).times(DynamicFloat.constant(2.5f)), 37.5f),
            test(DynamicInt32.from(int32Source).div(DynamicFloat.constant(2.0f)), 7.5f),
            test(DynamicInt32.from(int32Source).rem(DynamicFloat.constant(4.5f)), 1.5f),
            test(DynamicFloat.constant(5.0f), 5.0f),
            testForInvalidValue(DynamicFloat.constant(Float.NaN)),
            testForInvalidValue(DynamicFloat.constant(Float.NaN).plus(5.0f)),
            test(DynamicFloat.from(new AppDataKey<>("state_float_1.5")), 1.5f),
            test(DynamicFloat.constant(1234.567f).asInt(), 1234),
            test(DynamicFloat.constant(0.967f).asInt(), 0),
            test(DynamicFloat.constant(-1234.967f).asInt(), -1235),
            test(DynamicFloat.constant(-0.967f).asInt(), -1),
            test(DynamicFloat.constant(Float.MIN_VALUE).asInt(), 0),
            testForInvalidValue(DynamicFloat.constant(Float.MAX_VALUE).asInt()),
            testForInvalidValue(DynamicFloat.constant(-Float.MAX_VALUE).asInt()),
            test(DynamicInt32.constant(100).asFloat(), 100.0f),
            test(
                    DynamicInt32.constant(Integer.MIN_VALUE).asFloat(),
                    Float.valueOf(Integer.MIN_VALUE)),
            test(
                    DynamicInt32.constant(Integer.MAX_VALUE).asFloat(),
                    Float.valueOf(Integer.MAX_VALUE)),
            test(DynamicFloat.constant(100f).plus(DynamicFloat.constant(2f)), 102f),
            test(DynamicFloat.constant(100f).minus(DynamicFloat.constant(5.5f)), 94.5f),
            test(DynamicFloat.constant(5.5f).times(DynamicFloat.constant(2f)), 11f),
            test(DynamicFloat.constant(5f).div(DynamicFloat.constant(2f)), 2.5f),
            test(DynamicFloat.constant(5f).rem(DynamicFloat.constant(2f)), 1f),
            test(DynamicFloat.constant(100f).plus(2f), 102f),
            test(DynamicFloat.constant(100f).minus(5.5f), 94.5f),
            test(DynamicFloat.constant(5.5f).times(2f), 11f),
            test(DynamicFloat.constant(5f).div(2f), 2.5f),
            test(DynamicFloat.constant(5f).rem(2f), 1f),
            test(DynamicFloat.constant(0.12345622f).eq(0.12345688f), true),
            test(DynamicFloat.constant(0.123455f).ne(0.123457f), true),
            test(DynamicFloat.constant(0.12345622f).ne(0.12345688f), false),
            test(DynamicFloat.constant(0.123455f).eq(0.123457f), false),
            test(DynamicFloat.constant(0.4f).lt(0.6f), true),
            test(DynamicFloat.constant(0.4f).lt(0.2f), false),
            test(DynamicFloat.constant(0.1234568f).lt(0.1234562f), false),
            test(DynamicFloat.constant(0.4f).lte(0.6f), true),
            test(DynamicFloat.constant(0.1234568f).lte(0.1234562f), true),
            test(DynamicFloat.constant(0.6f).gt(0.4f), true),
            test(DynamicFloat.constant(0.4f).gt(0.6f), false),
            test(DynamicFloat.constant(0.1234568f).gt(0.1234562f), false),
            test(DynamicFloat.constant(0.6f).gte(0.4f), true),
            test(DynamicFloat.constant(0.1234568f).gte(0.1234562f), true),
            test(DynamicBool.constant(true), true),
            test(DynamicBool.constant(true).eq(DynamicBool.constant(true)), true),
            test(DynamicBool.constant(true).eq(DynamicBool.constant(false)), false),
            test(DynamicBool.constant(true).ne(DynamicBool.constant(true)), false),
            test(DynamicBool.constant(true).ne(DynamicBool.constant(false)), true),
            test(DynamicBool.constant(true).negate(), false),
            test(DynamicBool.constant(false).negate(), true),
            test(DynamicBool.constant(true).and(DynamicBool.constant(true)), true),
            test(DynamicBool.constant(true).and(DynamicBool.constant(false)), false),
            test(DynamicBool.constant(false).and(DynamicBool.constant(true)), false),
            test(DynamicBool.constant(false).and(DynamicBool.constant(false)), false),
            test(DynamicBool.constant(true).or(DynamicBool.constant(true)), true),
            test(DynamicBool.constant(true).or(DynamicBool.constant(false)), true),
            test(DynamicBool.constant(false).or(DynamicBool.constant(true)), true),
            test(DynamicBool.constant(false).or(DynamicBool.constant(false)), false),
            test(DynamicBool.from(new AppDataKey<>("state_bool_true")), true),
            test(DynamicBool.constant(false), false),
            test(DynamicBool.from(new AppDataKey<>("state_bool_false")), false),
            test(DynamicInt32.constant(5).eq(DynamicInt32.constant(5)), true),
            test(DynamicInt32.constant(5).eq(DynamicInt32.constant(6)), false),
            test(DynamicInt32.constant(5).ne(DynamicInt32.constant(5)), false),
            test(DynamicInt32.constant(5).ne(DynamicInt32.constant(6)), true),
            test(DynamicInt32.constant(10).lt(11), true),
            test(DynamicInt32.constant(10).lt(10), false),
            test(DynamicInt32.constant(10).lt(5), false),
            test(DynamicInt32.constant(10).lte(11), true),
            test(DynamicInt32.constant(10).lte(10), true),
            test(DynamicInt32.constant(10).lte(5), false),
            test(DynamicInt32.constant(10).gt(11), false),
            test(DynamicInt32.constant(10).gt(10), false),
            test(DynamicInt32.constant(10).gt(5), true),
            test(DynamicInt32.constant(10).gte(11), false),
            test(DynamicInt32.constant(10).gte(10), true),
            test(DynamicInt32.constant(10).gte(5), true),
            // Instant maximum value
            test(
                    DynamicInstant.withSecondsPrecision(Instant.MAX),
                    Instant.MAX.truncatedTo(ChronoUnit.SECONDS)),
            // Duration Int overflow
            test(
                    DynamicInstant.withSecondsPrecision(Instant.EPOCH)
                            .durationUntil(DynamicInstant.withSecondsPrecision(Instant.MAX))
                            .toIntSeconds(),
                    (int) Instant.MAX.getEpochSecond()),
            // Positive duration
            test(dynamicDurationOfSeconds(123456L).toIntDays(), 1),
            test(dynamicDurationOfSeconds(123456L).toIntHours(), 34),
            test(dynamicDurationOfSeconds(123456L).toIntMinutes(), 2057),
            test(dynamicDurationOfSeconds(123456L).toIntSeconds(), 123456),
            test(dynamicDurationOfSeconds(123456L).getIntDaysPart(), 1),
            test(dynamicDurationOfSeconds(123456L).getHoursPart(), 10),
            test(dynamicDurationOfSeconds(123456L).getMinutesPart(), 17),
            test(dynamicDurationOfSeconds(123456L).getSecondsPart(), 36),
            // Negative duration
            test(dynamicDurationOfSeconds(-123456L).toIntDays(), -1),
            test(dynamicDurationOfSeconds(-123456L).toIntHours(), -34),
            test(dynamicDurationOfSeconds(-123456L).toIntMinutes(), -2057),
            test(dynamicDurationOfSeconds(-123456L).toIntSeconds(), -123456),
            test(dynamicDurationOfSeconds(-123456L).getIntDaysPart(), 1),
            test(dynamicDurationOfSeconds(-123456L).getHoursPart(), 10),
            test(dynamicDurationOfSeconds(-123456L).getMinutesPart(), 17),
            test(dynamicDurationOfSeconds(-123456L).getSecondsPart(), 36),
            // date-time getters.
            // Friday November 30, 1973 01:10:00 (am) in time zone Asia/Kathmandu (+0530)
            // Thursday November 29, 1973 19:40:00 (pm) in time zone Europe/London (GMT)
            // Thursday November 29, 1973 21:40:00 (pm) in time zone  GMT+2
            test(FIXED_INSTANT.getYear(ASIA_KATHMANDU), 1973),
            test(FIXED_INSTANT.getYear(EUROPE_LONDON), 1973),
            test(FIXED_INSTANT.getYear(GMT_PLUS_TWO), 1973),
            test(FIXED_INSTANT.getMonth(ASIA_KATHMANDU), 11),
            test(FIXED_INSTANT.getMonth(EUROPE_LONDON), 11),
            test(FIXED_INSTANT.getMonth(GMT_PLUS_TWO), 11),
            test(FIXED_INSTANT.getDayOfMonth(ASIA_KATHMANDU), 30),
            test(FIXED_INSTANT.getDayOfMonth(EUROPE_LONDON), 29),
            test(FIXED_INSTANT.getDayOfMonth(GMT_PLUS_TWO), 29),
            test(FIXED_INSTANT.getDayOfWeek(ASIA_KATHMANDU), 5),
            test(FIXED_INSTANT.getDayOfWeek(EUROPE_LONDON), 4),
            test(FIXED_INSTANT.getDayOfWeek(GMT_PLUS_TWO), 4),
            test(FIXED_INSTANT.getHour(ASIA_KATHMANDU), 1),
            test(FIXED_INSTANT.getHour(EUROPE_LONDON), 19),
            test(FIXED_INSTANT.getHour(GMT_PLUS_TWO), 21),
            test(FIXED_INSTANT.getMinute(ASIA_KATHMANDU), 10),
            test(FIXED_INSTANT.getMinute(EUROPE_LONDON), 40),
            test(FIXED_INSTANT.getMinute(GMT_PLUS_TWO), 40),
            test(FIXED_INSTANT.getSecond(ASIA_KATHMANDU), 0),
            test(FIXED_INSTANT.getSecond(EUROPE_LONDON), 0),
            test(FIXED_INSTANT.getSecond(GMT_PLUS_TWO), 0),
            test(
                    DynamicString.onCondition(DynamicBool.constant(true))
                            .use(constant("Hello"))
                            .elseUse(constant("World")),
                    "Hello"),
            test(
                    DynamicString.onCondition(DynamicBool.constant(false))
                            .use(constant("Hello"))
                            .elseUse(constant("World")),
                    "World"),
            test(
                    DynamicString.from(new AppDataKey<>("state_hello_world"))
                            .concat(DynamicString.constant("_test")),
                    "hello_world_test"),
            test(
                    DynamicString.constant("this ")
                            .concat(DynamicString.constant("is "))
                            .concat(DynamicString.constant("a test")),
                    "this is a test"),
            test(
                    DynamicInt32.onCondition(DynamicBool.constant(true))
                            .use(DynamicInt32.constant(1))
                            .elseUse(DynamicInt32.constant(10)),
                    1),
            test(
                    DynamicInt32.onCondition(DynamicBool.constant(false))
                            .use(DynamicInt32.constant(1))
                            .elseUse(DynamicInt32.constant(10)),
                    10),
            test(
                    DynamicInstant.onCondition(DynamicBool.constant(true))
                            .use(DynamicInstant.withSecondsPrecision(Instant.EPOCH))
                            .elseUse(Instant.ofEpochMilli(1000)),
                    Instant.EPOCH),
            test(
                    DynamicInstant.onCondition(DynamicBool.constant(false))
                            .use(DynamicInstant.withSecondsPrecision(Instant.EPOCH))
                            .elseUse(Instant.ofEpochMilli(1000)),
                    Instant.ofEpochMilli(1000)),
            test(
                    DynamicColor.onCondition(DynamicBool.constant(true))
                            .use(DynamicColor.constant(Color.BLUE))
                            .elseUse(DynamicColor.constant(Color.RED)),
                    Color.BLUE),
            test(
                    DynamicColor.onCondition(DynamicBool.constant(false))
                            .use(DynamicColor.constant(Color.BLUE))
                            .elseUse(DynamicColor.constant(Color.RED)),
                    Color.RED),
            test(
                    DynamicDuration.onCondition(DynamicBool.constant(true))
                            .use(dynamicDurationOfSeconds(10))
                            .elseUse(Duration.ofSeconds(100)),
                    Duration.ofSeconds(10)),
            test(
                    DynamicDuration.onCondition(DynamicBool.constant(false))
                            .use(dynamicDurationOfSeconds(10))
                            .elseUse(Duration.ofSeconds(100)),
                    Duration.ofSeconds(100)),
            test(
                    DynamicFloat.constant(12.345f)
                            .format(
                                    new FloatFormatter.Builder()
                                            .setMaxFractionDigits(2)
                                            .setMinIntegerDigits(4)
                                            .setGroupingUsed(true)
                                            .build()),
                    "0,012.35"),
            test(
                    DynamicFloat.constant(12.345f)
                            .format(
                                    new FloatFormatter.Builder()
                                            .setMinFractionDigits(4)
                                            .setMinIntegerDigits(4)
                                            .setGroupingUsed(false)
                                            .build()),
                    "0012.3450"),
            test(
                    DynamicFloat.constant(12.345f)
                            .format(
                                    new FloatFormatter.Builder()
                                            .setMaxFractionDigits(1)
                                            .setGroupingUsed(true)
                                            .build())
                            .concat(DynamicString.constant("°")),
                    "12.3°"),
            test(
                    DynamicFloat.constant(12.345678f)
                            .format(
                                    new FloatFormatter.Builder()
                                            .setMinFractionDigits(4)
                                            .setMaxFractionDigits(2)
                                            .setGroupingUsed(true)
                                            .build()),
                    "12.3457"),
            test(
                    DynamicFloat.constant(12.345678f)
                            .format(
                                    new FloatFormatter.Builder()
                                            .setMinFractionDigits(2)
                                            .setGroupingUsed(true)
                                            .build()),
                    "12.346"),
            test(
                    DynamicFloat.constant(12.3456f).format(new FloatFormatter.Builder().build()),
                    "12.346"),
            test(
                    DynamicInt32.constant(12)
                            .format(
                                    new IntFormatter.Builder()
                                            .setMinIntegerDigits(4)
                                            .setGroupingUsed(true)
                                            .build()),
                    "0,012"),
            test(DynamicInt32.constant(12).format(new IntFormatter.Builder().build()), "12")
        };
        ImmutableList.Builder<Object[]> immutableListBuilder = new ImmutableList.Builder<>();
        for (ParametrizedDynamicTypeEvaluatorTest.TestCase<?> testCase : testCases) {
            immutableListBuilder.add(new Object[] {testCase});
        }
        return immutableListBuilder.build();
    }

    private final ParametrizedDynamicTypeEvaluatorTest.TestCase<?> mTestCase;

    public ParametrizedDynamicTypeEvaluatorTest(
            ParametrizedDynamicTypeEvaluatorTest.TestCase<?> testCase) {
        this.mTestCase = testCase;
    }

    @Test
    public void runTest() {
        StateStore stateStore = new StateStore(generateExampleState());

        DynamicTypeEvaluator evaluator =
                new DynamicTypeEvaluator(
                        new DynamicTypeEvaluator.Config.Builder()
                                .setStateStore(stateStore)
                                .setAnimationQuotaManager(new FixedQuotaManagerImpl(MAX_VALUE))
                                .build());

        mTestCase.runTest(evaluator);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<String> test(
            DynamicString bindUnderTest, String expectedValue) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicStringProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicString(
                                                bindUnderTest,
                                                ULocale.getDefault(),
                                                new MainThreadExecutor(),
                                                cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                expectedValue);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Integer> test(
            DynamicInt32 bindUnderTest, Integer expectedValue) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicInt32Proto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicInt32(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                expectedValue);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Integer> test(
            DynamicColor bindUnderTest, Integer expectedValue) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicColorProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicColor(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                expectedValue);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Instant> test(
            DynamicInstant bindUnderTest, Instant instant) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicInstantProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicInstant(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                instant);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Duration> test(
            DynamicDuration bindUnderTest, Duration duration) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicDurationProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicDuration(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                duration);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Float> test(
            DynamicFloat bindUnderTest, Float expectedValue) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicFloatProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicFloat(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                expectedValue);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Boolean> test(
            DynamicBool bindUnderTest, Boolean expectedValue) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicBoolProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicBool(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                },
                expectedValue);
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Integer> testForInvalidValue(
            DynamicInt32 bindUnderTest) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicInt32Proto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicInt32(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static ParametrizedDynamicTypeEvaluatorTest.TestCase<Float> testForInvalidValue(
            DynamicFloat bindUnderTest) {
        return new ParametrizedDynamicTypeEvaluatorTest.TestCase<>(
                bindUnderTest.toDynamicFloatProto().toString(),
                (evaluator, cb) -> {
                    try {
                        evaluator
                                .bind(
                                        DynamicTypeBindingRequest.forDynamicFloat(
                                                bindUnderTest, new MainThreadExecutor(), cb))
                                .startEvaluation();
                    } catch (EvaluationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static class TestCase<T> {
        private final String mName;
        private final BiConsumer<DynamicTypeEvaluator, DynamicTypeValueReceiver<T>>
                mExpressionEvaluator;
        private final T mExpectedValue;

        TestCase(
                String name,
                BiConsumer<DynamicTypeEvaluator, DynamicTypeValueReceiver<T>> expressionEvaluator,
                T expectedValue) {
            this.mName = name;
            this.mExpressionEvaluator = expressionEvaluator;
            this.mExpectedValue = expectedValue;
        }

        /** Creates a test case for an expression which expects to result in invalid value. */
        TestCase(
                String name,
                BiConsumer<DynamicTypeEvaluator, DynamicTypeValueReceiver<T>> expressionEvaluator) {
            this.mName = name;
            this.mExpressionEvaluator = expressionEvaluator;
            this.mExpectedValue = null;
        }

        public void runTest(DynamicTypeEvaluator evaluator) {
            List<T> results = new ArrayList<>();
            AtomicInteger invalidatedCalls = new AtomicInteger(0);

            DynamicTypeValueReceiver<T> callback =
                    new DynamicTypeValueReceiver<T>() {
                        @Override
                        public void onData(@NonNull T newData) {
                            results.add(newData);
                        }

                        @Override
                        public void onInvalidated() {
                            invalidatedCalls.incrementAndGet();
                        }
                    };

            this.mExpressionEvaluator.accept(evaluator, callback);
            shadowOf(Looper.getMainLooper()).idle();

            if (mExpectedValue != null) {
                // Test expects an actual value.
                assertThat(results).containsExactly(mExpectedValue);
                assertThat(invalidatedCalls.get()).isEqualTo(0);
            } else {
                // Test expects an invalid value.
                assertThat(results).isEmpty();
                assertThat(invalidatedCalls.get()).isEqualTo(1);
            }
        }

        @Override
        @NonNull
        public String toString() {
            return mName + " = " + mExpectedValue;
        }
    }

    private static DynamicDuration dynamicDurationOfSeconds(long seconds) {
        return DynamicDuration.withSecondsPrecision(Duration.ofSeconds(seconds));
    }

    private static ImmutableMap<AppDataKey<?>, DynamicDataValue> generateExampleState() {
        return ImmutableMap.of(
                new AppDataKey<DynamicString>("state_hello_world"),
                DynamicDataValue.newBuilder()
                        .setStringVal(FixedString.newBuilder().setValue("hello_world"))
                        .build(),
                new AppDataKey<DynamicInt32>("state_int_15"),
                DynamicDataValue.newBuilder()
                        .setInt32Val(FixedInt32.newBuilder().setValue(15))
                        .build(),
                new AppDataKey<DynamicFloat>("state_float_1.5"),
                DynamicDataValue.newBuilder()
                        .setFloatVal(FixedFloat.newBuilder().setValue(1.5f))
                        .build(),
                new AppDataKey<DynamicBool>("state_bool_true"),
                DynamicDataValue.newBuilder()
                        .setBoolVal(FixedBool.newBuilder().setValue(true))
                        .build(),
                new AppDataKey<DynamicBool>("state_bool_false"),
                DynamicDataValue.newBuilder()
                        .setBoolVal(FixedBool.newBuilder().setValue(false))
                        .build());
    }
}
