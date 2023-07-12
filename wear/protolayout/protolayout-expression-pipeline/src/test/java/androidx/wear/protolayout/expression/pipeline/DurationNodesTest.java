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
import androidx.wear.protolayout.expression.pipeline.DurationNodes.BetweenInstancesNode;
import androidx.wear.protolayout.expression.pipeline.DurationNodes.FixedDurationNode;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedDuration;

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
        node.getLhsIncomingCallback().onData(firstInstant);
        node.getRhsIncomingCallback().onData(secondInstant);

        assertThat(results).containsExactly(Duration.between(firstInstant, secondInstant));
    }
}
