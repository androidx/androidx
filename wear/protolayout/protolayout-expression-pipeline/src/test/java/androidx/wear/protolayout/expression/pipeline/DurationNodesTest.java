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
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration;
import androidx.wear.protolayout.expression.pipeline.DurationNodes.BetweenInstancesNode;
import androidx.wear.protolayout.expression.pipeline.DurationNodes.FixedDurationNode;
import androidx.wear.protolayout.expression.pipeline.DurationNodes.StateDurationSourceNode;
import androidx.wear.protolayout.expression.proto.DynamicDataProto;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.FixedProto;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedDuration;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DurationNodesTest {
    @Test
    public void testFixedDuration() {
        long seconds = 1234567L;
        List<Duration> results = new ArrayList<>();
        FixedDurationNode node =
                new FixedDurationNode(
                        FixedDuration.newBuilder().setSeconds(seconds).build(),
                        new AddToListCallback<>(results));
        node.preInit();
        node.init();
        assertThat(results).containsExactly(Duration.ofSeconds(seconds));
    }

    @Test
    public void testBetweenDuration() {
        List<Duration> results = new ArrayList<>();

        Instant firstInstant = Instant.ofEpochSecond(10000L);
        Instant secondInstant = Instant.ofEpochSecond(12345L);

        BetweenInstancesNode node = new BetweenInstancesNode(new AddToListCallback<>(results));
        node.getLhsUpstreamCallback().onData(firstInstant);
        node.getRhsUpstreamCallback().onData(secondInstant);

        assertThat(results).containsExactly(Duration.between(firstInstant, secondInstant));
    }

    @Test
    public void testStateDuration() {
        long seconds = 1234567L;
        String KEY_FOO = "foo";
        List<Duration> results = new ArrayList<>();
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                new AppDataKey<DynamicDuration>(KEY_FOO),
                                DynamicDataProto.DynamicDataValue.newBuilder()
                                        .setDurationVal(
                                                FixedProto.FixedDuration.newBuilder()
                                                        .setSeconds(seconds))
                                        .build()));
        DynamicProto.StateDurationSource protoNode =
                DynamicProto.StateDurationSource.newBuilder().setSourceKey(KEY_FOO).build();
        StateDurationSourceNode node =
                new StateDurationSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(Duration.ofSeconds(seconds));
    }
}
