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

package androidx.wear.protolayout.renderer.dynamicdata;

import static android.os.Looper.getMainLooper;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.FIRST_CHILD_INDEX;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.ROOT_NODE_ID;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.createNodePosId;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl;
import androidx.wear.protolayout.expression.pipeline.QuotaManager;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationParameters;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.RepeatMode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Repeatable;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticOpType;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32FormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.NotBoolOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateBoolSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateFloatSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedBool;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.DimensionProto.DpProp;
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AnimatedImageFormat;
import androidx.wear.protolayout.proto.TriggerProto.OnConditionMetTrigger;
import androidx.wear.protolayout.proto.TriggerProto.OnLoadTrigger;
import androidx.wear.protolayout.proto.TriggerProto.OnVisibleOnceTrigger;
import androidx.wear.protolayout.proto.TriggerProto.OnVisibleTrigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger.InnerCase;
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline.PipelineMaker;
import androidx.wear.protolayout.renderer.inflater.DefaultAndroidSeekableAnimatedImageResourceByResIdResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;
import androidx.wear.protolayout.renderer.test.R;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.common.truth.Expect;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLooper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// Note: Most of the functionality of DynamicDataPipeline should be tested using //
// DynamicDataPipelineProtoTest instead. This test class only exists for the cases that cannot be //
// trivially tested there (e.g. throwing exceptions in response to feature flags or handling //
// animations).
@RunWith(AndroidJUnit4.class)
public class ProtoLayoutDynamicDataPipelineTest {
    private static final String NODE_1_1 = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX);
    private static final String NODE_1_2 = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX + 1);
    private static final String NODE_1_11 = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX + 10);
    private static final String NODE_1_1_1 = createNodePosId(NODE_1_1, FIRST_CHILD_INDEX);
    private static final String NODE_1_1_1_1 = createNodePosId(NODE_1_1_1, FIRST_CHILD_INDEX);
    private final StateStore mStateStore = new StateStore(ImmutableMap.of());
    public static final String TEST_POS_ID = ROOT_NODE_ID;
    @Rule public final Expect expect = Expect.create();

    FrameLayout mRootContainer;

    @Before
    public void setUp() {
        mRootContainer = new FrameLayout(getApplicationContext());
        // This needs to be an attached view to test animations in data pipeline.
        Robolectric.buildActivity(Activity.class).setup().get().setContentView(mRootContainer);
    }

    @Test
    public void
            buildPipeline_animatableFixedFloat_animationsDisabled_noStaticValueSet_assignsEndValue() {
        List<Float> results = new ArrayList<>();
        float startValue = 5.0f;
        float endValue = 10.0f;
        DynamicFloat dynamicFloat = animatableFixedFloat(startValue, endValue);

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, dynamicFloat);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void
            buildPipeline_animatableFixedColor_animationsDisabled_noStaticValueSet_assignsEndValue() {
        List<Integer> results = new ArrayList<>();
        int startValue = 0;
        int endValue = 1;
        DynamicColor dynamicColor = animatableFixedColor(startValue, endValue);

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, dynamicColor);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void buildPipeline_dpProp_animatable_animationsDisabled_hasStaticValue_assignsEndVal() {
        List<Float> results = new ArrayList<>();
        float endValue = 10.0f;
        DynamicFloat dynamicFloat = animatableFixedFloat(5.0f, endValue);
        DpProp dpProp = DpProp.newBuilder().setDynamicValue(dynamicFloat).setValue(-5f).build();

        ProtoLayoutDynamicDataPipeline pipeline = initPipelineAnimationsDisabled(results, dpProp);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void
            buildPipeline_degreesProp_animatable_animationsDisabled_hasStaticValue_assignsEndVal() {
        List<Float> results = new ArrayList<>();
        float endValue = 10.0f;
        DynamicFloat dynamicFloat = animatableFixedFloat(5.0f, endValue);
        DegreesProp degreesProp =
                DegreesProp.newBuilder().setDynamicValue(dynamicFloat).setValue(-5f).build();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, degreesProp);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void
            buildPipeline_colorProp_animatable_animationsDisabled_hasStaticValue_assignsEndValue() {
        List<Integer> results = new ArrayList<>();
        int endValue = 1;
        DynamicColor dynamicColor = animatableFixedColor(0, endValue);
        ColorProp colorProp =
                ColorProp.newBuilder().setDynamicValue(dynamicColor).setArgb(0x12345678).build();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, colorProp);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void
            buildPipeline_colorProp_animatable_animationsDisabled_noStaticValueSet_assignsEndVal() {
        List<Integer> results = new ArrayList<>();
        DynamicColor dynamicColor = animatableFixedColor(0, 1);
        ColorProp colorProp = ColorProp.newBuilder().setDynamicValue(dynamicColor).build();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, colorProp);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(1);
    }

    @Test
    public void buildPipeline_animatableFixedFloat_animationsEnabled_builds() {
        List<Float> results = new ArrayList<>();
        DynamicFloat dynamicFloat = animatableFixedFloat(5.0f, 10.0f);

        // Leave this as-is to assert it doesn't throw...
        initPipeline(results, /* enableAnimations= */ true, dynamicFloat, /* animationsNum= */ 1);
    }

    @Test
    public void animatableFixedFloat_emitsAnimatedValuesOnStart() {
        List<Float> results = new ArrayList<>();
        DynamicFloat dynamicFloat = animatableFixedFloat(5.0f, 10.0f);

        // Leave this as-is to assert it doesn't throw...
        initPipeline(results, /* enableAnimations= */ true, dynamicFloat, /* animationsNum= */ 1);

        assertAnimation(results, 5.0f, 10.0f);
    }

    @Test
    public void
            buildPipeline_animatableDynamicFloat_animationsDisabled_noStaticValueSet_assignsEndValue() {
        List<Float> results = new ArrayList<>();
        float endValue = 1.0f;
        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setFixed(
                                                                FixedFloat.newBuilder()
                                                                        .setValue(endValue))))
                        .build();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, dynamicFloat);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void
            buildPipeline_animatableDynamicColor_animationsDisabled_noStaticValueSet_assignsEndValue() {
        List<Integer> results = new ArrayList<>();
        int endValue = 1;
        DynamicColor dynamicColor =
                DynamicColor.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicColor.newBuilder()
                                        .setInput(
                                                DynamicColor.newBuilder()
                                                        .setFixed(
                                                                FixedColor.newBuilder()
                                                                        .setArgb(endValue))))
                        .build();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineAnimationsDisabled(results, dynamicColor);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void buildPipeline_animatableDynamicFloat_animationsEnabled_builds() {
        List<Float> results = new ArrayList<>();
        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setFixed(
                                                                FixedFloat.newBuilder()
                                                                        .setValue(1.0f))))
                        .build();

        // Leave this as-is to assert it doesn't throw...
        initPipeline(results, /* enableAnimations= */ true, dynamicFloat, /* animationsNum= */ 0);
    }

    @Test
    public void buildPipeline_animatableDynamicFloat_animationsEnabled_withAnimations_builds() {
        List<Float> results = new ArrayList<>();
        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setAnimatableFixed(
                                                                AnimatableFixedFloat.newBuilder()
                                                                        .setFromValue(1)
                                                                        .setToValue(2))))
                        .build();

        // Leave this as-is to assert it doesn't throw...
        initPipeline(results, /* enableAnimations= */ true, dynamicFloat, /* animationsNum= */ 1);
    }

    @Test
    public void buildPipeline_animatableDynamicFloat_emitsAnimatedValues() {
        List<Float> results = new ArrayList<>();
        setFloatStateVal("anim_val", 5.0f);

        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setStateSource(
                                                                StateFloatSource.newBuilder()
                                                                        .setSourceKey("anim_val"))))
                        .build();
        initPipeline(results, /* enableAnimations= */ true, dynamicFloat, /* animationsNum= */ 0);

        assertThat(results).containsExactly(5.0f);
        results.clear();

        setFloatStateVal("anim_val", 15.0f);
        assertAnimation(results, 5.0f, 15.0f);
    }

    @Test
    public void buildPipeline_animatableDynamicFloat_skipsToEndIfInvisible() {
        List<Float> results = new ArrayList<>();
        AddToListCallback<Float> receiver = new AddToListCallback<>(results);
        setFloatStateVal("anim_val", 5.0f);

        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setStateSource(
                                                                StateFloatSource.newBuilder()
                                                                        .setSourceKey("anim_val"))))
                        .build();
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        pipeline.setFullyVisible(false);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicFloat, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(2);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(5.0f);
        results.clear();

        setFloatStateVal("anim_val", 15.0f);
        assertThat(results).containsExactly(15.0f);
    }

    @Test
    public void buildPipeline_animatableDynamicFloat_noInitialValueEmitsInvalid() {
        List<Float> results = new ArrayList<>();
        List<Boolean> invalidResults = new ArrayList<>();

        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setStateSource(
                                                                StateFloatSource.newBuilder()
                                                                        .setSourceKey("anim_val"))))
                        .build();
        initPipeline(
                results,
                invalidResults,
                /* enableAnimations= */ true,
                dynamicFloat,
                /* animationsNum= */ 0);

        assertThat(results).isEmpty();
        assertThat(invalidResults).contains(true);
        invalidResults.clear();

        // First value is not animated.
        setFloatStateVal("anim_val", 1.0f);

        assertThat(results).containsExactly(1.0f);
        assertThat(invalidResults).isEmpty();
        results.clear();

        // Second update is animated...
        setFloatStateVal("anim_val", 10.0f);
        assertAnimation(results, 1.0f, 10.0f);
        assertThat(invalidResults).isEmpty();
    }

    @Test
    public void buildPipeline_noCommit() {
        List<Integer> results = new ArrayList<>();
        List<Boolean> invalidResults = new ArrayList<>();
        AddToListCallback<Integer> receiver = new AddToListCallback<>(results, invalidResults);

        DynamicInt32 dynamicInt = fixedDynamicInt32(1);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        // Add pipeline to PipelineMaker but do not commit nodes.
        ProtoLayoutDynamicDataPipeline.PipelineMaker unusedPipelineMaker =
                pipeline.newPipelineMaker().addPipelineFor(dynamicInt, TEST_POS_ID, receiver);
        shadowOf(getMainLooper()).idle();

        assertThat(results).isEmpty();
        assertThat(invalidResults).isEmpty();
    }

    @Test
    public void buildPipeline_multipleCommits() {
        List<Integer> results = new ArrayList<>();
        AddToListCallback<Integer> receiver = new AddToListCallback<>(results);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        DynamicInt32 dynamicInt1 = fixedDynamicInt32(1);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt1, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(1);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(1);

        // A second commit with the same PosId resets the content for that PosId.
        DynamicInt32 dynamicInt2 = fixedDynamicInt32(2);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt2, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(1);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(1, 2).inOrder();
    }

    @Test
    public void buildPipeline_multipleCommitFromSameMaker() {
        List<Integer> results = new ArrayList<>();
        AddToListCallback<Integer> receiver = new AddToListCallback<>(results);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        DynamicInt32 dynamicInt1 = fixedDynamicInt32(1);
        ProtoLayoutDynamicDataPipeline.PipelineMaker pMaker =
                pipeline.newPipelineMaker().addPipelineFor(dynamicInt1, TEST_POS_ID, receiver);

        pMaker.commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(1);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(1);

        // Successive commit should be a no-op.
        pMaker.commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(1);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(1);
    }

    @Test
    public void buildPipeline_removeNodesRecursively() {
        List<Integer> results = new ArrayList<>();
        AddToListCallback<Integer> receiver = new AddToListCallback<>(results);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        DynamicInt32 dynamicInt1 =
                DynamicInt32.newBuilder()
                        .setArithmeticOperation(
                                ArithmeticInt32Op.newBuilder()
                                        .setOperationType(ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                                        .setInputLhs(fixedDynamicInt32(1))
                                        .setInputRhs(fixedDynamicInt32(2)))
                        .build();
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt1, NODE_1_1, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(3);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(3);

        DynamicInt32 dynamicInt2 = fixedDynamicInt32(4);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt2, NODE_1_1_1, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(4);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(3, 4).inOrder();

        DynamicInt32 dynamicInt3 = fixedDynamicInt32(5);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt3, NODE_1_11, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(5);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(3, 4, 5).inOrder();

        DynamicInt32 dynamicInt4 = fixedDynamicInt32(6);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt4, NODE_1_2, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(6);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(3, 4, 5, 6).inOrder();

        // Remove node NODE_1_1_1. NODE_1_1, NODE_1_11 and NODE_1_2 should remain in the pipeline.
        pipeline.removeChildNodesFor(NODE_1_1);
        assertThat(pipeline.size()).isEqualTo(5);
    }

    @Test
    public void buildPipeline_clearPipeline() {
        List<Integer> results = new ArrayList<>();
        AddToListCallback<Integer> receiver = new AddToListCallback<>(results);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        DynamicInt32 dynamicInt1 =
                DynamicInt32.newBuilder()
                        .setArithmeticOperation(
                                ArithmeticInt32Op.newBuilder()
                                        .setOperationType(ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                                        .setInputLhs(fixedDynamicInt32(1))
                                        .setInputRhs(fixedDynamicInt32(2)))
                        .build();
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt1, NODE_1_1, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(3);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(3);

        DynamicInt32 dynamicInt2 = fixedDynamicInt32(4);
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicInt2, NODE_1_1_1, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        assertThat(pipeline.size()).isEqualTo(4);
        shadowOf(getMainLooper()).idle();
        assertThat(results).containsExactly(3, 4).inOrder();

        // Clear all nodes.
        pipeline.clear();
        assertThat(pipeline.size()).isEqualTo(0);
    }

    @Test
    public void getNodesAffectedBy_checksInTreeHierarchy() {
        List<String> expected = Arrays.asList(NODE_1_1, NODE_1_1_1);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        PipelineMaker pipelineMaker = pipeline.newPipelineMaker();
        expected.forEach(pipelineMaker::rememberNode);
        pipelineMaker.rememberNode(NODE_1_1_1_1);
        pipelineMaker.rememberNode(NODE_1_2);
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);

        assertThat(
                        pipeline.getNodesAffectedBy(NODE_1_1_1, nodeInfo -> true).stream()
                                .map(NodeInfo::getPosId)
                                .collect(Collectors.toList()))
                .containsExactlyElementsIn(expected);
    }

    @Test
    public void resolvedAnimatedImage_canStorePlayAndResetOnVisible() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        Trigger triggerTileVisible =
                Trigger.newBuilder()
                        .setOnVisibleTrigger(OnVisibleTrigger.getDefaultInstance())
                        .build();

        pipeline.newPipelineMaker()
                .addResolvedAnimatedImage(drawableAvd, triggerTileVisible, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);

        pipeline.playAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd.started).isFalse();

        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        expect.that(drawableAvd.started).isTrue();

        pipeline.resetAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        expect.that(drawableAvd.started).isFalse();
        expect.that(drawableAvd.reset).isTrue();

        // Animate the drawable again.
        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        expect.that(drawableAvd.started).isTrue();
    }

    @Test
    public void resolvedAnimatedImage_canStoreAndPlayOnVisibleOnce() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        Trigger triggerTileVisibleOnce =
                Trigger.newBuilder()
                        .setOnVisibleOnceTrigger(OnVisibleOnceTrigger.getDefaultInstance())
                        .build();

        pipeline.newPipelineMaker()
                .addResolvedAnimatedImage(drawableAvd, triggerTileVisibleOnce, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);

        pipeline.playAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd.started).isFalse();

        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_ONCE_TRIGGER);
        expect.that(drawableAvd.started).isTrue();

        drawableAvd.stop();

        // Animation could be started only once.
        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_ONCE_TRIGGER);
        expect.that(drawableAvd.started).isFalse();
    }

    @Test
    public void resolvedAnimatedImage_canStorePlayAndResetOnLoad() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        Trigger triggerTileLoad =
                Trigger.newBuilder().setOnLoadTrigger(OnLoadTrigger.getDefaultInstance()).build();

        pipeline.newPipelineMaker()
                .addResolvedAnimatedImage(drawableAvd, triggerTileLoad, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);

        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        expect.that(drawableAvd.started).isFalse();

        pipeline.playAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd.started).isTrue();

        pipeline.resetAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd.started).isFalse();
        expect.that(drawableAvd.reset).isTrue();
    }

    @Test
    public void conditionTriggerCallback_boolInitiallyFalse_playWhenTurnsTrue() {
        String boolStateKey = "KEY";
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        DynamicBool dynamicBool = dynamicBool(boolStateKey);
        Trigger trigger = conditionTrigger(dynamicBool);
        makePipelineForConditionalBoolTrigger(pipeline, drawableAvd, dynamicBool, trigger);

        // First value is false. Shouldn't Trigger animation
        setBoolStateVal(boolStateKey, false);
        expect.that(drawableAvd.started).isFalse();

        // Should Trigger animation
        setBoolStateVal(boolStateKey, true);
        expect.that(drawableAvd.started).isTrue();

        // Shouldn't Trigger animation
        drawableAvd.started = false;
        setBoolStateVal(boolStateKey, true);
        expect.that(drawableAvd.started).isFalse();

        // Shouldn't Trigger animation
        drawableAvd.started = false;
        setBoolStateVal(boolStateKey, false);
        expect.that(drawableAvd.started).isFalse();

        // Should Trigger animation
        setBoolStateVal(boolStateKey, true);
        expect.that(drawableAvd.started).isTrue();
    }

    @Test
    public void conditionTriggerCallback_boolInitiallyTrue_playOnLoad() {
        String boolStateKey = "KEY";
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        DynamicBool dynamicBool = dynamicBool(boolStateKey);
        Trigger trigger = conditionTrigger(dynamicBool);
        makePipelineForConditionalBoolTrigger(pipeline, drawableAvd, dynamicBool, trigger);

        // Should trigger animation onLoad.
        setBoolStateVal(boolStateKey, true);
        expect.that(drawableAvd.started).isTrue();
    }

    @Test
    public void
            conditionTriggerCallback_boolInitiallyFalse_onLoadAndOnVisibilityChangeDoNotTriggerAnimation() {
        String boolStateKey = "KEY";
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        DynamicBool dynamicBool = dynamicBool(boolStateKey);
        Trigger trigger = conditionTrigger(dynamicBool);
        makePipelineForConditionalBoolTrigger(pipeline, drawableAvd, dynamicBool, trigger);

        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        expect.that(drawableAvd.started).isFalse();

        pipeline.playAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd.started).isFalse();
    }

    @Test
    public void conditionTriggerCallback_playWhenTurnsTrue_quotaIsReleased() {
        String boolStateKey = "KEY";
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        DynamicBool dynamicBool = dynamicBool(boolStateKey);
        Trigger trigger = conditionTrigger(dynamicBool);
        makePipelineForConditionalBoolTrigger(pipeline, drawableAvd, dynamicBool, trigger);

        // Should Trigger animation
        setBoolStateVal(boolStateKey, true);
        expect.that(drawableAvd.started).isTrue();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        expect.that(quotaManager.isAllQuotaReleased()).isFalse();

        // Animation is stopped, quota should be released.
        pipeline.stopAvdAnimations(InnerCase.ON_CONDITION_MET_TRIGGER);
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void newLayout_enoughDynamicNodesQuota_useDynamicData() {

        float input = 123.456f;
        String expectedOutput = "123";
        String staticValue = "static";

        AtomicReference<String> currentValue = new AtomicReference<>();
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(/* quotaCap= */ 3);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        quotaManager);

        // Building expression equivalent to: DynamicFloat.constant(input).asInt().format().
        DynamicFloat dynamicFloat =
                DynamicFloat.newBuilder()
                        .setFixed(FixedFloat.newBuilder().setValue(input).build())
                        .build();
        DynamicInt32 dynamicInt32 =
                DynamicInt32.newBuilder()
                        .setFloatToInt(FloatToInt32Op.newBuilder().setInput(dynamicFloat).build())
                        .build();
        DynamicString dynamicString =
                DynamicString.newBuilder()
                        .setInt32FormatOp(Int32FormatOp.newBuilder().setInput(dynamicInt32).build())
                        .build();

        makePipelineForDynamicString(
                pipeline, dynamicString, staticValue, "posId", currentValue::set);
        pipeline.initNewLayout();
        expect.that(pipeline.getDynamicExpressionsNodesCount()).isEqualTo(3);
        // No quota left
        expect.that(quotaManager.getRemainingQuota()).isEqualTo(0);
        expect.that(currentValue.get()).isEqualTo(expectedOutput);
    }

    @Test
    public void newLayout_noExpressionNodesQuota_useStaticData() {

        String dynamicValue = "dynamic";
        String staticValue = "static";
        AtomicReference<String> currentValue = new AtomicReference<>();
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(/* quotaCap= */ 0);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        quotaManager);

        DynamicString dynamicString =
                DynamicString.newBuilder()
                        .setFixed(FixedString.newBuilder().setValue(dynamicValue).build())
                        .build();

        makePipelineForDynamicString(
                pipeline, dynamicString, staticValue, "posId", currentValue::set);
        pipeline.initNewLayout();
        expect.that(pipeline.mPositionIdTree.get("posId").getFailedBindingRequest().size())
                .isEqualTo(1);
        expect.that(currentValue.get()).isEqualTo(staticValue);
    }

    @Test
    public void newLayout_removeNodeInfo_releaseQuota() {

        int quota = 8;
        DynamicBool expressionWith4Nodes = buildBoolExpressionWithFixedNumberOfNodes(4);
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(quota);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        quotaManager);

        makePipelineForDynamicBool(pipeline, expressionWith4Nodes, "posId1.1");
        makePipelineForDynamicBool(pipeline, expressionWith4Nodes, "posId1.1.1");

        pipeline.initNewLayout();
        // Remaining quota should be 0
        expect.that(quotaManager.getRemainingQuota()).isEqualTo(0);

        pipeline.removeChildNodesFor("posId1.1");
        pipeline.initNewLayout();
        // Reminding quota should be 4
        expect.that(quotaManager.getRemainingQuota()).isEqualTo(4);

        pipeline.removeChildNodesFor("posId1");
        pipeline.initNewLayout();
        // The entire quota should be released
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void nodeNotFullyBound_quotaReleased_nodeRetryBound() {

        String parentOfNode1 = "posId1";
        String nodeInfo1 = parentOfNode1.concat(".1");
        String nodeInfo2 = "posId2.1";
        String nodeInfo3 = "posId3.1";
        int quota = 8;
        DynamicBool expressionWith5Nodes = buildBoolExpressionWithFixedNumberOfNodes(5);
        DynamicBool expressionWith1Nodes = buildBoolExpressionWithFixedNumberOfNodes(1);
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(quota);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        quotaManager);

        // Adding an expressions with 5 dynamic nodes to nodeInfo1.
        makePipelineForDynamicBool(pipeline, expressionWith5Nodes, nodeInfo1);
        pipeline.initNewLayout();

        // nodeInfo1 expression did bound successfully. Remaining quota is 3 = 8 - 5
        expect.that(quotaManager.getRemainingQuota()).isEqualTo(3);

        // Adding an expressions with 5 dynamic nodes to nodeInfo2.
        makePipelineForDynamicBool(pipeline, expressionWith5Nodes, nodeInfo2);
        pipeline.initNewLayout();

        // Remaining quota not enough for nodeInfo2 expression to bound.
        expect.that(pipeline.mPositionIdTree.get(nodeInfo1).getFailedBindingRequest().size())
                .isEqualTo(0);
        expect.that(pipeline.mPositionIdTree.get(nodeInfo2).getFailedBindingRequest().size())
                .isEqualTo(1);

        // Remove nodeInfo1 and add nodeInfo3. nodeInfo2 still in the pipeline.
        pipeline.mPositionIdTree.removeChildNodesFor(parentOfNode1);
        // Adding an expressions with 1 dynamic node to nodeInfo3.
        makePipelineForDynamicBool(pipeline, expressionWith1Nodes, nodeInfo3);

        pipeline.initNewLayout();
        // Now the pipeline will have a total expressionNodesCount of 6 = 5 + 1
        // nodeInfo2 (failed to bound previously) and nodeInfo3(new) should be able to bound
        expect.that(quotaManager.getRemainingQuota()).isEqualTo(2);
        expect.that(pipeline.mPositionIdTree.get(nodeInfo3).getFailedBindingRequest().size())
                .isEqualTo(0);
        expect.that(pipeline.mPositionIdTree.get(nodeInfo2).getFailedBindingRequest().size())
                .isEqualTo(0);
    }

    @Test
    public void newLayout_multipleBound_noEnoughDynamicNodesQuota_satisfyOnlyFewBounds() {

        int quota = 11;
        DynamicBool expressionWith12Nodes = buildBoolExpressionWithFixedNumberOfNodes(12);
        DynamicBool expressionWith4Nodes = buildBoolExpressionWithFixedNumberOfNodes(4);
        DynamicBool expressionWith1Nodes = buildBoolExpressionWithFixedNumberOfNodes(1);

        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(quota);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        quotaManager);

        makePipelineForDynamicBool(pipeline, expressionWith12Nodes, "posId1.0");
        makePipelineForDynamicBool(pipeline, expressionWith4Nodes, "posId1.1");
        makePipelineForDynamicBool(pipeline, expressionWith4Nodes, "posId1.2");
        makePipelineForDynamicBool(pipeline, expressionWith4Nodes, "posId1.3");
        makePipelineForDynamicBool(pipeline, expressionWith1Nodes, "posId1.4");

        pipeline.initNewLayout();

        // expressionWith12Nodes related BoundType should file to bind.
        expect.that(
                        pipeline.mPositionIdTree
                                .findFirst((node) -> node.getPosId().equals("posId1.0"))
                                .getFailedBindingRequest()
                                .size())
                .isEqualTo(1);

        // Remaining quota should be exactly 2.
        expect.that(quotaManager.getRemainingQuota()).isEqualTo(2);
    }

    @Test
    public void conditionTriggerCallback_noQuota_notPlayed() {
        String boolStateKey = "KEY";
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(/* quotaCap= */ 0);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        DynamicBool dynamicBool = dynamicBool(boolStateKey);
        Trigger trigger = conditionTrigger(dynamicBool);
        makePipelineForConditionalBoolTrigger(pipeline, drawableAvd, dynamicBool, trigger);

        // Should Trigger animation, but animation shouldn't be played.
        setBoolStateVal(boolStateKey, true);
        expect.that(drawableAvd.started).isFalse();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @NonNull
    private static Trigger conditionTrigger(DynamicBool dynamicBool) {
        return Trigger.newBuilder()
                .setOnConditionMetTrigger(
                        OnConditionMetTrigger.newBuilder().setCondition(dynamicBool).build())
                .build();
    }

    @NonNull
    private static DynamicBool dynamicBool(String boolStateKey) {
        return DynamicBool.newBuilder()
                .setStateSource(StateBoolSource.newBuilder().setSourceKey(boolStateKey).build())
                .build();
    }

    private static DynamicBool buildBoolExpressionWithFixedNumberOfNodes(int count) {
        if (count < 1) {
            throw new IllegalArgumentException();
        }
        DynamicBool constant =
                DynamicBool.newBuilder()
                        .setFixed(FixedBool.newBuilder().setValue(true).build())
                        .build();

        if (count == 1) {
            return constant;
        }
        return DynamicBool.newBuilder()
                .setNotOp(
                        NotBoolOp.newBuilder()
                                .setInput(buildBoolExpressionWithFixedNumberOfNodes(count - 1))
                                .build())
                .build();
    }

    private void makePipelineForConditionalBoolTrigger(
            ProtoLayoutDynamicDataPipeline pipeline,
            TestAnimatedVectorDrawable drawableAvd,
            DynamicBool dynamicBool,
            Trigger trigger) {
        pipeline.newPipelineMaker()
                .addResolvedAnimatedImageWithBoolTrigger(
                        drawableAvd, trigger, TEST_POS_ID, dynamicBool)
                .commit(mRootContainer, /* isReattaching= */ false);
    }

    private void makePipelineForDynamicString(
            ProtoLayoutDynamicDataPipeline pipeline,
            DynamicString dynamicString,
            String invalidData,
            String posId,
            Consumer<String> consumer) {
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicString, invalidData, Locale.UK, posId, consumer)
                .commit(mRootContainer, /* isReattaching= */ false);
    }

    private void makePipelineForDynamicBool(
            ProtoLayoutDynamicDataPipeline pipeline, DynamicBool dynamicBool, String posId) {
        pipeline.newPipelineMaker()
                .addPipelineFor(dynamicBool, false, posId, (value) -> {})
                .commit(mRootContainer, /* isReattaching= */ false);
    }

    @Test
    public void resolvedSeekableAnimatedImage_canStoreAndRegisterWithAnimatableFixedFloat() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        DynamicFloat boundProgress =
                DynamicFloat.newBuilder()
                        .setAnimatableFixed(
                                AnimatableFixedFloat.newBuilder()
                                        .setFromValue(0)
                                        .setToValue(0.66f)
                                        .build())
                        .build();
        shadowOf(getMainLooper()).idle();

        SeekableAnimatedVectorDrawable drawable =
                (SeekableAnimatedVectorDrawable) createAvdSeekable(boundProgress);
        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addResolvedSeekableAnimatedImage(drawable, boundProgress, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        assertThat(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        shadowOf(getMainLooper()).idle();
        assertThat(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(drawable.getCurrentPlayTime()).isEqualTo(660L);
    }

    @Test
    public void resolvedSeekableAnimatedImage_canStoreAndRegisterWithAnimatableDynamicFloat() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        DynamicFloat boundProgress =
                DynamicFloat.newBuilder()
                        .setAnimatableDynamic(
                                AnimatableDynamicFloat.newBuilder()
                                        .setInput(
                                                DynamicFloat.newBuilder()
                                                        .setStateSource(
                                                                StateFloatSource.newBuilder()
                                                                        .setSourceKey("anim_val"))))
                        .build();
        SeekableAnimatedVectorDrawable drawable =
                (SeekableAnimatedVectorDrawable) createAvdSeekable(boundProgress);
        shadowOf(getMainLooper()).idle();

        pipeline.newPipelineMaker()
                .addResolvedSeekableAnimatedImage(drawable, boundProgress, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        assertThat(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        assertThat(pipeline.getRunningAnimationsCount()).isEqualTo(0);

        setFloatStateVal("anim_val", 0.33f);
        expect.that(drawable.getCurrentPlayTime()).isEqualTo(330L);
        setFloatStateVal("anim_val", 0.66f);
        expect.that(drawable.getCurrentPlayTime()).isEqualTo(660L);
        setFloatStateVal("anim_val", 0.99f);
        expect.that(drawable.getCurrentPlayTime()).isEqualTo(990L);

        // bound progress is clamped to [0.0, 1.0]
        setFloatStateVal("anim_val", 1.2f);
        expect.that(drawable.getCurrentPlayTime()).isEqualTo(1000L);
        setFloatStateVal("anim_val", -0.2f);
        expect.that(drawable.getCurrentPlayTime()).isEqualTo(0L);
    }

    @Test
    public void resolvedSeekableAnimatedImage_getSeekableAnimationTotalDurationMillis() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        DynamicFloat boundProgress =
                DynamicFloat.newBuilder()
                        .setStateSource(StateFloatSource.newBuilder().setSourceKey("anim_val"))
                        .build();
        SeekableAnimatedVectorDrawable drawable =
                (SeekableAnimatedVectorDrawable) createAvdSeekable(boundProgress);

        pipeline.newPipelineMaker()
                .addResolvedSeekableAnimatedImage(drawable, boundProgress, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);

        expect.that(pipeline.getSeekableAnimationTotalDurationMillis("anim_val")).isEqualTo(1000);
    }

    @Test
    public void whenInvisible_pausesAvds() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));
        DynamicFloat boundProgress =
                DynamicFloat.newBuilder()
                        .setStateSource(StateFloatSource.newBuilder().setSourceKey("anim_val"))
                        .build();
        SeekableAnimatedVectorDrawable seekableDrawable =
                (SeekableAnimatedVectorDrawable) createAvdSeekable(boundProgress);

        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        Trigger triggerTileVisible =
                Trigger.newBuilder()
                        .setOnVisibleTrigger(OnVisibleTrigger.getDefaultInstance())
                        .build();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addResolvedSeekableAnimatedImage(seekableDrawable, boundProgress, TEST_POS_ID)
                .addResolvedAnimatedImage(drawableAvd, triggerTileVisible, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);
        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        seekableDrawable.start();

        pipeline.setFullyVisible(false);

        expect.that(drawableAvd.isVisible()).isFalse();
        expect.that(seekableDrawable.isVisible()).isFalse();
    }

    @Test
    public void visibilityChange_avdsStatusChange() {
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        new FixedQuotaManagerImpl(MAX_VALUE),
                        new FixedQuotaManagerImpl(MAX_VALUE));

        TestAnimatedVectorDrawable drawableAvd1 = new TestAnimatedVectorDrawable();
        TestAnimatedVectorDrawable drawableAvd2 = new TestAnimatedVectorDrawable();
        TestAnimatedVectorDrawable drawableAvd3 = new TestAnimatedVectorDrawable();
        Trigger triggerTileVisible =
                Trigger.newBuilder()
                        .setOnVisibleTrigger(OnVisibleTrigger.getDefaultInstance())
                        .build();
        Trigger triggerTileVisibleOnce =
                Trigger.newBuilder()
                        .setOnVisibleOnceTrigger(OnVisibleOnceTrigger.getDefaultInstance())
                        .build();
        Trigger triggerTileLoad =
                Trigger.newBuilder().setOnLoadTrigger(OnLoadTrigger.getDefaultInstance()).build();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addResolvedAnimatedImage(drawableAvd1, triggerTileVisible, TEST_POS_ID)
                .addResolvedAnimatedImage(drawableAvd2, triggerTileVisibleOnce, TEST_POS_ID)
                .addResolvedAnimatedImage(drawableAvd3, triggerTileLoad, TEST_POS_ID)
                .commit(mRootContainer, /* isReattaching= */ false);

        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_TRIGGER);
        pipeline.playAvdAnimations(InnerCase.ON_VISIBLE_ONCE_TRIGGER);
        pipeline.playAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd1.started).isTrue();
        expect.that(drawableAvd2.started).isTrue();
        expect.that(drawableAvd3.started).isTrue();

        // When tile is swiped away, AVD with trigger onVisible is stopped, For other AVDs, no
        // animation status change is expected.
        pipeline.setFullyVisible(false);

        expect.that(drawableAvd1.isVisible()).isFalse();
        expect.that(drawableAvd2.isVisible()).isFalse();
        expect.that(drawableAvd3.isVisible()).isFalse();

        expect.that(drawableAvd1.started).isFalse();
        expect.that(drawableAvd2.started).isTrue();
        expect.that(drawableAvd3.started).isTrue();

        // Simulate the situation that drawableAVD2 reaches its end.
        drawableAvd2.stop();

        // When tile is back to be fully visible, re-start AVDs with trigger onVisible. For other
        // AVDs, no animation status change is expected.
        pipeline.setFullyVisible(true);

        expect.that(drawableAvd1.isVisible()).isTrue();
        expect.that(drawableAvd2.isVisible()).isTrue();
        expect.that(drawableAvd3.isVisible()).isTrue();

        expect.that(drawableAvd1.started).isTrue();
        expect.that(drawableAvd2.started).isFalse();
        expect.that(drawableAvd3.started).isTrue();
    }

    @Test
    public void allAnimations_freeQuota_played() {
        DynamicFloat dynamicFloat = animatableFixedFloat(1.0f, 10.0f);
        DynamicFloat boundProgress = animatableFixedFloat(2.0f, 10.0f);
        SeekableAnimatedVectorDrawable seekableDrawable =
                (SeekableAnimatedVectorDrawable) createAvdSeekable(boundProgress);
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineWithAllAnimations(
                        Arrays.asList(dynamicFloat),
                        boundProgress,
                        seekableDrawable,
                        drawableAvd,
                        quotaManager);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(3);
        shadowOf(getMainLooper()).idle();
        pipeline.resetAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        expect.that(drawableAvd.started).isFalse();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        shadowOf(getMainLooper()).idle();
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void delayedAnimations_notConsumeQuota_beforePlaying_freeQuota() {
        float start1 = 1.0f;
        float start2 = 100.0f;
        float end1 = 10.0f;
        float end2 = 1000.0f;

        DynamicFloat dynamicFloat1 =
                animatableFixedFloat(start1, end1, /* duration= */ 100, /* delay= */ 0);
        List<Float> results1 = new ArrayList<>();
        AddToListCallback<Float> receiver1 =
                new AddToListCallback<>(results1, /* invalidList= */ null);

        DynamicFloat dynamicFloat2 =
                animatableFixedFloat(start2, end2, /* duration= */ 200, /* delay= */ 600);
        List<Float> results2 = new ArrayList<>();
        AddToListCallback<Float> receiver2 =
                new AddToListCallback<>(results2, /* invalidList= */ null);

        // Quota allows 1 animation at the time, but the given floats are starting in different
        // time, so both should be played.
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(1);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        PipelineMaker pipelineMaker = pipeline.newPipelineMaker();

        pipelineMaker.addPipelineFor(dynamicFloat1, TEST_POS_ID, receiver1);
        pipelineMaker.addPipelineFor(dynamicFloat2, TEST_POS_ID, receiver2);
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        // one running, delayed animation is not started yet.
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        assertThat(results2).hasSize(0);

        shadowOf(getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS);
        assertAnimation(results1, start1, end1);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        shadowOf(getMainLooper()).idle();
        assertAnimation(results2, start2, end2);

        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void delayedAnimations_notConsumeQuota_beforePlaying_notEnoughQuota() {
        float start1 = 1.0f;
        float start2 = 100.0f;
        float end1 = 10.0f;
        float end2 = 1000.0f;

        DynamicFloat dynamicFloat1 =
                animatableFixedFloat(start1, end1, /* duration= */ 600, /* delay= */ 0);
        List<Float> results1 = new ArrayList<>();
        AddToListCallback<Float> receiver1 =
                new AddToListCallback<>(results1, /* invalidList= */ null);

        DynamicFloat dynamicFloat2 =
                animatableFixedFloat(start2, end2, /* duration= */ 200, /* delay= */ 300);
        List<Float> results2 = new ArrayList<>();
        AddToListCallback<Float> receiver2 =
                new AddToListCallback<>(results2, /* invalidList= */ null);

        // Quota allows 1 animation at the time, but the given floats are starting in different
        // time, so both should be played.
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(1);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        PipelineMaker pipelineMaker = pipeline.newPipelineMaker();

        pipelineMaker.addPipelineFor(dynamicFloat1, TEST_POS_ID, receiver1);
        pipelineMaker.addPipelineFor(dynamicFloat2, TEST_POS_ID, receiver2);
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        // one running, delayed animation is not started yet.
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        assertThat(results2).hasSize(0);

        shadowOf(getMainLooper()).idle();
        assertAnimation(results1, start1, end1);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        shadowOf(getMainLooper()).idle();
        assertThat(results2).hasSize(1);
        expect.that(results2).containsExactly(end2);

        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void repeatDelayedAnimations_notFreeQuota_startDelayedAnimations_notPlayed() {
        float start1 = 1.0f;
        float start2 = 5.0f;
        float end1 = 10.0f;
        float end2 = 50.0f;

        // 0~100: forward animation 100~300: reverse delay 300~400: reverse animation
        DynamicFloat dynamicFloat1 =
                animatableFixedFloat(start1, end1, /* duration= */ 100, /* delay= */ 0, 200, 2);
        List<Float> results1 = new ArrayList<>();
        AddToListCallback<Float> receiver1 =
                new AddToListCallback<>(results1, /* invalidList= */ null);

        // Try to start animation during the reverse delay of dynamicFloat1
        DynamicFloat dynamicFloat2 =
                animatableFixedFloat(start2, end2, /* duration= */ 100, /* delay= */ 200);
        List<Float> results2 = new ArrayList<>();
        AddToListCallback<Float> receiver2 =
                new AddToListCallback<>(results2, /* invalidList= */ null);

        // Quota allows 1 animation at the time, but the given floats are starting in different
        // time, so both should be played.
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(1);

        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        PipelineMaker pipelineMaker = pipeline.newPipelineMaker();

        pipelineMaker.addPipelineFor(dynamicFloat1, TEST_POS_ID, receiver1);
        pipelineMaker.addPipelineFor(dynamicFloat2, TEST_POS_ID, receiver2);
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        // one running, delayed animation is not started yet.
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        assertThat(results2).isEmpty();

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(200));
        // dynamicFloat1: forward animation played
        assertAnimation(results1, start1, end1);
        results1.clear();

        ShadowLooper.runUiThreadTasks();
        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        assertThat(results2).hasSize(1);
        expect.that(results2).containsExactly(end2);

        ShadowLooper.runUiThreadTasks();
        // dynamicFloat1: reverse animation played
        assertAnimation(results1, end1, start1);

        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void pause_notResumed_animationsWithRepeatDelay() {
        float start = 1.0f;
        float end = 10.0f;

        // 0~100: forward animation 100~300: reverse delay 300~400: reverse animation
        DynamicFloat dynamicFloat =
                animatableFixedFloat(
                        start,
                        end,
                        /* duration= */ 100,
                        /* delay= */ 0,
                        /* repeat delay = */ 200,
                        /* iterations= */ 0 // infinite animations
                        );
        List<Float> results = new ArrayList<>();
        AddToListCallback<Float> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);

        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(1);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        PipelineMaker pipelineMaker = pipeline.newPipelineMaker();
        pipelineMaker.addPipelineFor(dynamicFloat, TEST_POS_ID, receiver);
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);

        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        int resultSize = results.size();
        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        // During repeat delay, the animation is counted as running, but no value change.
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        expect.that(results.size()).isEqualTo(resultSize);
        // visibility change pauses the animation during repeat delay
        pipeline.setFullyVisible(false);
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        // check that animation should Not be resumed after the repeat delay period.
        shadowOf(getMainLooper()).idleFor(Duration.ofMillis(100));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void allAnimations_noQuota_notPlayed() {
        DynamicFloat dynamicFloat = animatableFixedFloat(5.0f, 10.0f);
        DynamicFloat boundProgress = animatableFixedFloat(5.0f, 10.0f);
        SeekableAnimatedVectorDrawable seekableDrawable =
                (SeekableAnimatedVectorDrawable) createAvdSeekable(boundProgress);
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineWithAllAnimations(
                        Arrays.asList(dynamicFloat),
                        boundProgress,
                        seekableDrawable,
                        drawableAvd,
                        new FixedQuotaManagerImpl(/* quotaCap= */ 0));

        expect.that(drawableAvd.started).isFalse();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
    }

    @Test
    public void floatAnimation_noQuota_notPlayed_assignsEndValue() {
        float endValue = 10.0f;
        float startValue = 5.0f;
        DynamicFloat dynamicFloat = animatableFixedFloat(startValue, endValue);
        ArrayList<Float> results = new ArrayList<>();

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineWithAllAnimations(
                        Arrays.asList(dynamicFloat),
                        /* boundProgress= */ null,
                        /* seekableDrawable= */ null,
                        /* drawableAvd= */ null,
                        new FixedQuotaManagerImpl(/* quotaCap= */ 0),
                        results);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(results).hasSize(1);
        expect.that(results).containsExactly(endValue);
    }

    @Test
    public void whenOutOfQuota_animationsNotPlayed() {
        List<DynamicFloat> dynamicFloats = new ArrayList<>();
        int allowedAnimations = 3;
        for (int i = 0; i < allowedAnimations + 2; i++) {
            dynamicFloats.add(animatableFixedFloat(5.0f, 10.0f));
        }
        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(allowedAnimations);

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineWithAllAnimations(
                        dynamicFloats,
                        /* boundProgress= */ null,
                        /* seekableDrawable= */ null,
                        /* drawableAvd= */ null,
                        quotaManager);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(allowedAnimations);
        shadowOf(getMainLooper()).idle();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void whenInvisible_quotaIsReleased() {
        List<DynamicFloat> dynamicFloats = new ArrayList<>();
        int allowedAnimations = 3;
        for (int i = 0; i < allowedAnimations; i++) {
            dynamicFloats.add(animatableFixedFloat(5.0f, 10.0f));
        }

        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(allowedAnimations);
        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineWithAllAnimations(
                        dynamicFloats,
                        /* boundProgress= */ null,
                        /* seekableDrawable= */ null,
                        /* drawableAvd= */ null,
                        quotaManager);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(allowedAnimations);

        pipeline.setFullyVisible(false);
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @Test
    public void whenOutOfQuotaIsReleased_newAnimationsArePlayed() {
        List<DynamicFloat> dynamicFloats = new ArrayList<>();
        int allowedAnimations = 3;
        for (int i = 0; i < allowedAnimations; i++) {
            dynamicFloats.add(animatableFixedFloat(i, 10.0f));
        }

        FixedQuotaManagerImpl quotaManager = new FixedQuotaManagerImpl(allowedAnimations);

        ProtoLayoutDynamicDataPipeline pipeline =
                initPipelineWithAllAnimations(
                        dynamicFloats,
                        /* boundProgress= */ null,
                        /* seekableDrawable= */ null,
                        /* drawableAvd= */ null,
                        quotaManager);

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(allowedAnimations);
        shadowOf(getMainLooper()).idle();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();

        AddToListCallback<Float> receiver =
                new AddToListCallback<>(new ArrayList<>(), /* invalidList= */ null);
        pipeline.setFullyVisible(true);
        ProtoLayoutDynamicDataPipeline.PipelineMaker pipelineMaker = pipeline.newPipelineMaker();
        pipelineMaker.addPipelineFor(animatableFixedFloat(5.0f, 10.f), TEST_POS_ID, receiver);
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(1);
        shadowOf(getMainLooper()).idle();
        expect.that(pipeline.getRunningAnimationsCount()).isEqualTo(0);
        expect.that(quotaManager.isAllQuotaReleased()).isTrue();
    }

    @NonNull
    private DynamicFloat animatableFixedFloat(float from, float to) {
        return DynamicFloat.newBuilder()
                .setAnimatableFixed(
                        AnimatableFixedFloat.newBuilder().setFromValue(from).setToValue(to))
                .build();
    }

    @NonNull
    private DynamicFloat animatableFixedFloat(float from, float to, int duration, int delay) {
        return DynamicFloat.newBuilder()
                .setAnimatableFixed(
                        AnimatableFixedFloat.newBuilder()
                                .setFromValue(from)
                                .setToValue(to)
                                .setAnimationSpec(
                                        AnimationSpec.newBuilder()
                                                .setAnimationParameters(
                                                        AnimationParameters.newBuilder()
                                                                .setDurationMillis(duration)
                                                                .setDelayMillis(delay)
                                                                .build())
                                                .build()))
                .build();
    }

    @NonNull
    private DynamicFloat animatableFixedFloat(
            float from, float to, int duration, int delay, int repeatDelay, int iterations) {
        AnimationParameters alternateParameters =
                AnimationParameters.newBuilder().setDelayMillis(repeatDelay).build();
        return DynamicFloat.newBuilder()
                .setAnimatableFixed(
                        AnimatableFixedFloat.newBuilder()
                                .setFromValue(from)
                                .setToValue(to)
                                .setAnimationSpec(
                                        AnimationSpec.newBuilder()
                                                .setAnimationParameters(
                                                        AnimationParameters.newBuilder()
                                                                .setDurationMillis(duration)
                                                                .setDelayMillis(delay)
                                                                .build())
                                                .setRepeatable(
                                                        Repeatable.newBuilder()
                                                                .setRepeatMode(
                                                                        RepeatMode
                                                                                .REPEAT_MODE_REVERSE
                                                                )
                                                                .setIterations(iterations)
                                                                .setForwardRepeatOverride(
                                                                        alternateParameters)
                                                                .setReverseRepeatOverride(
                                                                        alternateParameters)
                                                                .build())
                                                .build()))
                .build();
    }

    @NonNull
    private DynamicColor animatableFixedColor(int from, int to) {
        return DynamicColor.newBuilder()
                .setAnimatableFixed(
                        AnimatableFixedColor.newBuilder().setFromArgb(from).setToArgb(to).build())
                .build();
    }

    @NonNull
    private ProtoLayoutDynamicDataPipeline initPipelineWithAllAnimations(
            List<DynamicFloat> dynamicFloats,
            @Nullable DynamicFloat boundProgress,
            @Nullable SeekableAnimatedVectorDrawable seekableDrawable,
            @Nullable AnimatedVectorDrawable drawableAvd,
            QuotaManager quotaManager) {
        return initPipelineWithAllAnimations(
                dynamicFloats,
                boundProgress,
                seekableDrawable,
                drawableAvd,
                quotaManager,
                /* results= */ new ArrayList<>());
    }

    @NonNull
    private ProtoLayoutDynamicDataPipeline initPipelineWithAllAnimations(
            @NonNull List<DynamicFloat> dynamicFloats,
            @Nullable DynamicFloat boundProgress,
            @Nullable SeekableAnimatedVectorDrawable seekableDrawable,
            @Nullable AnimatedVectorDrawable drawableAvd,
            @NonNull QuotaManager quotaManager,
            @NonNull List<Float> results) {
        AddToListCallback<Float> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);
        Trigger trigger =
                Trigger.newBuilder().setOnLoadTrigger(OnLoadTrigger.getDefaultInstance()).build();
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                        mStateStore,
                        quotaManager,
                        new FixedQuotaManagerImpl(MAX_VALUE));
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        PipelineMaker pipelineMaker = pipeline.newPipelineMaker();

        for (DynamicFloat proto : dynamicFloats) {
            pipelineMaker.addPipelineFor(proto, TEST_POS_ID, receiver);
        }
        if (seekableDrawable != null && boundProgress != null) {
            pipelineMaker.addResolvedSeekableAnimatedImage(
                    seekableDrawable, boundProgress, TEST_POS_ID);
        }
        if (drawableAvd != null) {
            pipelineMaker.addResolvedAnimatedImage(drawableAvd, trigger, TEST_POS_ID);
        }
        pipelineMaker.commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();
        return pipeline;
    }

    private DynamicInt32 fixedDynamicInt32(int value) {
        return DynamicInt32.newBuilder().setFixed(FixedInt32.newBuilder().setValue(value)).build();
    }

    private ProtoLayoutDynamicDataPipeline initPipeline(
            List<Float> results, boolean enableAnimations, DynamicFloat proto, int animationsNum) {
        return initPipeline(
                results, /* invalidResults= */ null, enableAnimations, proto, animationsNum);
    }

    private ProtoLayoutDynamicDataPipeline initPipeline(
            List<Float> results,
            @Nullable List<Boolean> invalidResults,
            boolean enableAnimations,
            DynamicFloat proto,
            int animationsNum) {
        AddToListCallback<Float> receiver = new AddToListCallback<>(results, invalidResults);
        ProtoLayoutDynamicDataPipeline pipeline =
                enableAnimations
                        ? new ProtoLayoutDynamicDataPipeline(
                        /* sensorGateway= */ null,
                                mStateStore,
                                new FixedQuotaManagerImpl(MAX_VALUE),
                                new FixedQuotaManagerImpl(MAX_VALUE))
                        : new ProtoLayoutDynamicDataPipeline(
                                /* sensorGateway= */ null,
                                mStateStore);
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addPipelineFor(proto, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();
        if (enableAnimations) {
            assertThat(pipeline.getRunningAnimationsCount()).isEqualTo(animationsNum);
        }
        shadowOf(getMainLooper()).idle();
        assertThat(pipeline.getRunningAnimationsCount()).isEqualTo(0);

        return pipeline;
    }

    /** Runs one task */
    private ProtoLayoutDynamicDataPipeline initPipelineAnimationsDisabled(
            List<Float> results, DynamicFloat proto) {
        AddToListCallback<Float> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(  /* sensorGateway= */ null, mStateStore);
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addPipelineFor(proto, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        return pipeline;
    }

    /** Runs one task */
    private ProtoLayoutDynamicDataPipeline initPipelineAnimationsDisabled(
            List<Integer> results, DynamicColor proto) {
        AddToListCallback<Integer> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(  /* sensorGateway= */ null, mStateStore);
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addPipelineFor(proto, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        return pipeline;
    }

    /** Runs one task */
    private ProtoLayoutDynamicDataPipeline initPipelineAnimationsDisabled(
            List<Float> results, DpProp proto) {
        AddToListCallback<Float> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(  /* sensorGateway= */ null, mStateStore);
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addPipelineFor(proto, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        return pipeline;
    }

    /** Runs one task */
    private ProtoLayoutDynamicDataPipeline initPipelineAnimationsDisabled(
            List<Float> results, DegreesProp proto) {
        AddToListCallback<Float> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(  /* sensorGateway= */ null, mStateStore);
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addPipelineFor(proto, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        return pipeline;
    }

    /** Runs one task */
    private ProtoLayoutDynamicDataPipeline initPipelineAnimationsDisabled(
            List<Integer> results, ColorProp proto) {
        AddToListCallback<Integer> receiver =
                new AddToListCallback<>(results, /* invalidList= */ null);
        ProtoLayoutDynamicDataPipeline pipeline =
                new ProtoLayoutDynamicDataPipeline(  /* sensorGateway= */ null, mStateStore);
        shadowOf(getMainLooper()).idle();

        pipeline.setFullyVisible(true);
        pipeline.newPipelineMaker()
                .addPipelineFor(proto, TEST_POS_ID, receiver)
                .commit(mRootContainer, /* isReattaching= */ false);
        shadowOf(getMainLooper()).runOneTask();

        return pipeline;
    }

    private void setFloatStateVal(String key, float val) {
        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        key,
                        StateEntryValue.newBuilder()
                                .setFloatVal(FixedFloat.newBuilder().setValue(val))
                                .build()));
        shadowOf(getMainLooper()).idle();
    }

    private void setBoolStateVal(String key, boolean val) {
        mStateStore.setStateEntryValuesProto(
                ImmutableMap.of(
                        key,
                        StateEntryValue.newBuilder()
                                .setBoolVal(FixedBool.newBuilder().setValue(val))
                                .build()));
        shadowOf(getMainLooper()).idle();
    }

    private void assertAnimation(List<Float> results, float startVal, float endVal) {
        for (Float val : results) {
            if (startVal < endVal) {
                assertThat(val).isIn(Range.closed(startVal, endVal));
            } else {
                assertThat(val).isIn(Range.closed(endVal, startVal));
            }
        }

        assertThat(results.size()).isGreaterThan(1);

        if (startVal < endVal) {
            assertThat(results).isInOrder();
        } else {
            assertThat(results).isInOrder(Comparator.reverseOrder());
        }

        assertThat(results).contains(endVal);
    }

    private Drawable createAvdSeekable(DynamicFloat boundProgress) {
        Context context = ApplicationProvider.getApplicationContext();
        Resources resources = context.getResources();
        DefaultAndroidSeekableAnimatedImageResourceByResIdResolver resolver =
                new DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(resources);
        try {
            return resolver.getDrawableOrThrow(
                    AndroidSeekableAnimatedImageResourceByResId.newBuilder()
                            .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                            .setResourceId(R.drawable.android_animated_24dp)
                            .setProgress(boundProgress)
                            .build());
        } catch (NotFoundException | ResourceAccessException ex) {
            return null;
        }
    }

    private static class TestAnimatedVectorDrawable extends AnimatedVectorDrawable {
        public boolean started = false;
        public boolean reset = false;

        // We need to intercept callbacks and save it in this test class as shadow drawable doesn't
        // seem to call onEnd listener, meaning that quota won't be freed and we would get failing
        // test.
        private final List<AnimationCallback> mAnimationCallbacks = new ArrayList<>();

        @Override
        public void start() {
            super.start();
            started = true;
            reset = false;
        }

        @Override
        public void registerAnimationCallback(@NonNull AnimationCallback callback) {
            super.registerAnimationCallback(callback);
            mAnimationCallbacks.add(callback);
        }

        @Override
        public boolean unregisterAnimationCallback(@NonNull AnimationCallback callback) {
            mAnimationCallbacks.remove(callback);
            return super.unregisterAnimationCallback(callback);
        }

        @Override
        public void stop() {
            super.stop();
            started = false;
            mAnimationCallbacks.forEach(c -> c.onAnimationEnd(this));
        }

        @Override
        public void reset() {
            super.reset();
            started = false;
            reset = true;
            mAnimationCallbacks.forEach(c -> c.onAnimationEnd(this));
        }

        @Override
        public boolean isRunning() {
            super.isRunning();
            return started;
        }
    }
}
