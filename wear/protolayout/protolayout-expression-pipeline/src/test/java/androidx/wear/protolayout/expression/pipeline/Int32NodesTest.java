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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class Int32NodesTest {
    @Test
    public void testFixedInt32Node() {
        List<Integer> results = new ArrayList<>();

        FixedInt32 protoNode = FixedInt32.newBuilder().setValue(56).build();
        FixedInt32Node node = new FixedInt32Node(protoNode, new AddToListCallback<>(results));

        node.init();

        assertThat(results).containsExactly(56);
    }

    @Test
    public void stateInt32NodeTest() {
        List<Integer> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
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
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setInt32Val(FixedInt32.newBuilder().setValue(65))
                                        .build()));

        StateInt32Source protoNode = StateInt32Source.newBuilder().setSourceKey("foo").build();
        StateInt32SourceNode node =
                new StateInt32SourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        results.clear();

        oss.setStateEntryValues(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(12))
                                .build()));

        assertThat(results).containsExactly(12);
    }
}
