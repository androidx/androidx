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

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.icu.util.ULocale;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FixedStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.Int32FormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StateStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StringConcatOpNode;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32FormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateStringSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
public class StringNodesTest {
    private static final AppDataKey<DynamicString> KEY_FOO = new AppDataKey<>("foo");
    @Test
    public void fixedStringNodeTest() {
        List<String> results = new ArrayList<>();

        FixedString protoNode = FixedString.newBuilder().setValue("Hello World").build();
        FixedStringNode node = new FixedStringNode(protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly("Hello World");
    }

    @Test
    public void fixedString_longInput_truncated() {
        List<String> results = new ArrayList<>();

        String string500chars = generateStringWithLength(500);
        FixedString protoNode = FixedString.newBuilder().setValue(string500chars).build();
        FixedStringNode node = new FixedStringNode(protoNode, new AddToListCallback<>(results));
        node.preInit();
        node.init();

        assertThat(results)
                .containsExactly(string500chars.substring(0, StringNodes.MAXIMUM_STRING_LENGTH));
    }

    @Test
    public void fixedString_shortInput_doNotTruncate() {
        List<String> results = new ArrayList<>();

        String string50chars = generateStringWithLength(50);
        FixedString protoNode = FixedString.newBuilder().setValue(string50chars).build();
        FixedStringNode node = new FixedStringNode(protoNode, new AddToListCallback<>(results));
        node.preInit();
        node.init();

        assertThat(results).containsExactly(string50chars);
    }

    @Test
    public void int32FormatNodeTest() {
        List<String> results = new ArrayList<>();
        NumberFormatter formatter =
                new NumberFormatter(
                        Int32FormatOp.newBuilder()
                                .setGroupingUsed(true)
                                .setMinIntegerDigits(4)
                                .build(),
                        ULocale.UK);
        Int32FormatNode node = new Int32FormatNode(formatter, new AddToListCallback<>(results));
        node.getIncomingCallback().onPreUpdate();
        node.getIncomingCallback().onData(32);

        assertThat(results).containsExactly("0,032");
    }

    @Test
    public void stateStringNodeTest() {
        List<String> results = new ArrayList<>();
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setStringVal(FixedString.newBuilder().setValue("bar"))
                                        .build()));

        StateStringSource protoNode = StateStringSource.newBuilder().setSourceKey("foo").build();
        StateStringNode node =
                new StateStringNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly("bar");
    }

    @Test
    public void stateStringNode_longInput_truncate() {
        String string500chars = generateStringWithLength(500);
        List<String> results = new ArrayList<>();

        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setStringVal(
                                                FixedString.newBuilder().setValue(string500chars))
                                        .build()));

        StateStringSource protoNode = StateStringSource.newBuilder().setSourceKey("foo").build();
        StateStringNode node =
                new StateStringNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results)
                .containsExactly(string500chars.substring(0, StringNodes.MAXIMUM_STRING_LENGTH));
    }

    @Test
    public void stateStringUpdatesWithStateChanges() {
        List<String> results = new ArrayList<>();
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setStringVal(FixedString.newBuilder().setValue("bar"))
                                        .build()));

        StateStringSource protoNode = StateStringSource.newBuilder().setSourceKey("foo").build();
        StateStringNode node =
                new StateStringNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        results.clear();

        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setStringVal(FixedString.newBuilder().setValue("baz"))
                                .build()));

        assertThat(results).containsExactly("baz");
    }

    @Test
    public void concatStringNode_longInput_truncate() {
        String string300chars = generateStringWithLength(300);
        List<String> results = new ArrayList<>();

        StringConcatOpNode node = new StringConcatOpNode(new AddToListCallback<>(results));
        FixedStringNode lhsNode =
                new FixedStringNode(
                        FixedString.newBuilder().setValue(string300chars).build(),
                        node.getLhsIncomingCallback());
        FixedStringNode rhsNode =
                new FixedStringNode(
                        FixedString.newBuilder().setValue(string300chars).build(),
                        node.getRhsIncomingCallback());
        lhsNode.preInit();
        lhsNode.init();
        rhsNode.preInit();
        rhsNode.init();

        assertThat(results)
                .containsExactly(
                        string300chars
                                .concat(string300chars)
                                .substring(0, StringNodes.MAXIMUM_STRING_LENGTH));
    }

    @Test
    public void stateStringNoUpdatesAfterDestroy() {
        List<String> results = new ArrayList<>();
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                KEY_FOO,
                                DynamicDataValue.newBuilder()
                                        .setStringVal(FixedString.newBuilder().setValue("bar"))
                                        .build()));

        StateStringSource protoNode = StateStringSource.newBuilder().setSourceKey("foo").build();
        StateStringNode node =
                new StateStringNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();
        assertThat(results).containsExactly("bar");

        results.clear();
        node.destroy();

        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO,
                        DynamicDataValue.newBuilder()
                                .setStringVal(FixedString.newBuilder().setValue("baz"))
                                .build()));
        assertThat(results).isEmpty();
    }

    static String generateStringWithLength(int length) {
        if (length < 1) {
            return "";
        }
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return new String(array, UTF_8);
    }
}
