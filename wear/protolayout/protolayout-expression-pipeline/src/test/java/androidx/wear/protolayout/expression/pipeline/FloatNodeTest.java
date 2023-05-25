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

import static androidx.wear.protolayout.expression.PlatformHealthSources.Keys.HEART_RATE_BPM;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;

import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.AnimatableFixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.ArithmeticFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.DynamicAnimatedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.FixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.Int32ToFloatNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.StateFloatSourceNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticOpType;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateFloatSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class FloatNodeTest {
    private static final AppDataKey<DynamicFloat> KEY_FOO = new AppDataKey<>("foo");
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private PlatformDataProvider mMockDataProvider;
    @Test
    public void fixedFloatNodesTest() {
        List<Float> results = new ArrayList<>();
        float testValue = 6.6f;

        FixedFloat protoNode = FixedFloat.newBuilder().setValue(testValue).build();
        FixedFloatNode node = new FixedFloatNode(protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(testValue);
    }

    @Test
    public void stateFloatSourceNodeTest() {
        List<Float> results = new ArrayList<>();
        float testValue = 6.6f;
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setFloatVal(FixedFloat.newBuilder().setValue(testValue))
                                        .build()));

        StateFloatSource protoNode = StateFloatSource.newBuilder().setSourceKey("foo").build();
        StateFloatSourceNode node =
                new StateFloatSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(testValue);
    }

    @Test
    public void stateFloatSourceNode_updatesWithStateChanges() {
        List<Float> results = new ArrayList<>();
        float oldValue = 6.5f;
        float newValue = 7.8f;

        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setFloatVal(FixedFloat.newBuilder().setValue(oldValue))
                                        .build()));

        StateFloatSource protoNode = StateFloatSource.newBuilder().setSourceKey("foo").build();
        StateFloatSourceNode node =
                new StateFloatSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();
        assertThat(results).containsExactly(oldValue);

        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(newValue))
                                .build()));

        assertThat(results).containsExactly(oldValue, newValue).inOrder();
    }

    @Test
    public void stateFloatSource_canSubscribeToHeartRateUpdates() {
        PlatformDataStore platformDataStore = new PlatformDataStore(
                Collections.singletonMap(
                        HEART_RATE_BPM,
                        mMockDataProvider));
        StateFloatSource dailyStepsSource =
                StateFloatSource.newBuilder()
                        .setSourceKey(HEART_RATE_BPM.getKey())
                        .setSourceNamespace(
                                HEART_RATE_BPM.getNamespace())
                        .build();
        List<Float> results = new ArrayList<>();
        StateFloatSourceNode dailyStepsSourceNode =
                new StateFloatSourceNode(
                        platformDataStore,
                        dailyStepsSource,
                        new AddToListCallback<>(results));

        dailyStepsSourceNode.preInit();
        dailyStepsSourceNode.init();
        ArgumentCaptor<PlatformDataReceiver> receiverCaptor =
                ArgumentCaptor.forClass(PlatformDataReceiver.class);
        verify(mMockDataProvider).setReceiver(any(), receiverCaptor.capture());

        PlatformDataReceiver receiver = receiverCaptor.getValue();
        receiver.onData(
                PlatformDataValues.of(
                        HEART_RATE_BPM, DynamicDataBuilders.DynamicDataValue.fromFloat(70.0f)));

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(70.0f);

        receiver.onData(
                PlatformDataValues.of(
                        HEART_RATE_BPM, DynamicDataBuilders.DynamicDataValue.fromFloat(80.0f)));

        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(70.0f, 80.0f);
    }

    @Test
    public void stateFloatSourceNode_noUpdatesAfterDestroy() {
        List<Float> results = new ArrayList<>();
        float oldValue = 6.5f;
        float newValue = 7.8f;

        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setFloatVal(FixedFloat.newBuilder().setValue(oldValue))
                                        .build()));

        StateFloatSource protoNode = StateFloatSource.newBuilder().setSourceKey("foo").build();
        StateFloatSourceNode node =
                new StateFloatSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();
        assertThat(results).containsExactly(oldValue);

        results.clear();
        node.destroy();

        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(newValue))
                                .build()));

        assertThat(results).isEmpty();
    }

    @Test
    public void arithmeticFloat_add() {
        List<Float> results = new ArrayList<>();
        ArithmeticFloatOp protoNode =
                ArithmeticFloatOp.newBuilder()
                        .setOperationType(ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();

        ArithmeticFloatNode node =
                new ArithmeticFloatNode(protoNode, new AddToListCallback<>(results));

        float lhsValue = 6.6f;
        FixedFloat lhsProtoNode = FixedFloat.newBuilder().setValue(lhsValue).build();
        FixedFloatNode lhsNode = new FixedFloatNode(lhsProtoNode, node.getLhsIncomingCallback());
        lhsNode.init();

        float oldRhsValue = 6.5f;
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setFloatVal(FixedFloat.newBuilder().setValue(oldRhsValue))
                                        .build()));
        StateFloatSource rhsProtoNode = StateFloatSource.newBuilder().setSourceKey("foo").build();
        StateFloatSourceNode rhsNode =
                new StateFloatSourceNode(oss, rhsProtoNode, node.getRhsIncomingCallback());

        rhsNode.preInit();
        rhsNode.init();

        assertThat(results).containsExactly(lhsValue + oldRhsValue);

        float newRhsValue = 7.8f;
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(newRhsValue))
                                .build()));
        assertThat(results)
                .containsExactly(lhsValue + oldRhsValue, lhsValue + newRhsValue)
                .inOrder();
    }

    @Test
    public void int32ToFloatTest() {
        List<Float> results = new ArrayList<>();
        Int32ToFloatNode node = new Int32ToFloatNode(new AddToListCallback<>(results));

        int oldIntValue = 65;
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setInt32Val(FixedInt32.newBuilder().setValue(oldIntValue))
                                        .build()));

        StateInt32Source protoNode = StateInt32Source.newBuilder().setSourceKey("foo").build();
        StateInt32SourceNode intNode =
                new StateInt32SourceNode(oss, protoNode, node.getIncomingCallback());

        intNode.preInit();
        intNode.init();

        assertThat(results).containsExactly((float) oldIntValue);

        int newIntValue = 12;
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(newIntValue))
                                .build()));

        assertThat(results).containsExactly((float) oldIntValue, (float) newIntValue).inOrder();
    }

    @Test
    public void animatableFixedFloat_animates() {
        float startValue = 3.0f;
        float endValue = 33.0f;
        List<Float> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        AnimatableFixedFloat protoNode =
                AnimatableFixedFloat.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedFloatNode node =
                new AnimatableFixedFloatNode(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(true);

        node.preInit();
        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(startValue);
        assertThat(Iterables.getLast(results)).isEqualTo(endValue);
    }

    @Test
    public void animatableFixedFloat_whenInvisible_skipToEnd() {
        float startValue = 3.0f;
        float endValue = 33.0f;
        List<Float> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        AnimatableFixedFloat protoNode =
                AnimatableFixedFloat.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedFloatNode node =
                new AnimatableFixedFloatNode(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(false);

        node.preInit();
        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(endValue);
    }

    @Test
    public void animatableFixedFloat_whenNoQuota_skip() {
        float startValue = 3.0f;
        float endValue = 33.0f;
        List<Float> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(0);
        AnimatableFixedFloat protoNode =
                AnimatableFixedFloat.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedFloatNode node =
                new AnimatableFixedFloatNode(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(true);

        node.preInit();
        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(endValue);
    }

    @Test
    public void dynamicAnimatedFloat_onlyAnimateWhenVisible() {
        float value1 = 3.0f;
        float value2 = 11.0f;
        float value3 = 17.0f;
        List<Float> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setFloatVal(
                                                FixedFloat.newBuilder().setValue(value1).build())
                                        .build()));
        DynamicAnimatedFloatNode floatNode =
                new DynamicAnimatedFloatNode(
                        new AddToListCallback<>(results),
                        AnimationSpec.getDefaultInstance(),
                        quotaManager);
        floatNode.setVisibility(false);
        StateFloatSourceNode stateNode =
                new StateFloatSourceNode(
                        oss,
                        StateFloatSource.newBuilder().setSourceKey("foo").build(),
                        floatNode.getInputCallback());

        stateNode.preInit();
        stateNode.init();

        results.clear();
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(value2))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Only contains last value.
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(value2);

        floatNode.setVisibility(true);
        results.clear();
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(value3))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Contains intermediate values besides the initial and last.
        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(value2);
        assertThat(Iterables.getLast(results)).isEqualTo(value3);
        assertThat(results).isInOrder();
    }
}
