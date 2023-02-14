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

import android.icu.util.ULocale;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.StringNodes.FixedStringNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.Int32FormatNode;
import androidx.wear.protolayout.expression.pipeline.StringNodes.StateStringNode;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32FormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateStringSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class StringNodesTest {
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
    public void int32FormatNodeTest() {
        List<String> results = new ArrayList<>();
        NumberFormatter formatter =
                new NumberFormatter(
                        Int32FormatOp.newBuilder()
                                .setGroupingUsed(true)
                                .setMinIntegerDigits(4)
                                .build(),
                        ULocale.UK
                );
        Int32FormatNode node = new Int32FormatNode(formatter, new AddToListCallback<>(results));
        node.getIncomingCallback().onPreUpdate();
        node.getIncomingCallback().onData(32);

        assertThat(results).containsExactly("0,032");
    }

    @Test
    public void stateStringNodeTest() {
        List<String> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
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
    public void stateStringUpdatesWithStateChanges() {
        List<String> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setStringVal(FixedString.newBuilder().setValue("bar"))
                                        .build()));

        StateStringSource protoNode = StateStringSource.newBuilder().setSourceKey("foo").build();
        StateStringNode node =
                new StateStringNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        results.clear();

        oss.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setStringVal(FixedString.newBuilder().setValue("baz"))
                                .build()));

        assertThat(results).containsExactly("baz");
    }

    @Test
    public void stateStringNoUpdatesAfterDestroy() {
        List<String> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
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
        oss.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setStringVal(FixedString.newBuilder().setValue("baz"))
                                .build()));
        assertThat(results).isEmpty();
    }
}
