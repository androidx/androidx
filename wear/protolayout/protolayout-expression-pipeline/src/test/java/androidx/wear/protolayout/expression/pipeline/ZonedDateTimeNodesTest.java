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
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.pipeline.ZonedDateTimeNodes.InstantToZonedDateTimeOpNode;
import androidx.wear.protolayout.expression.proto.DynamicProto.InstantToZonedDateTimeOp;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ZonedDateTimeNodesTest {

    @Test
    public void testInstantWithZone() {
        List<ZonedDateTime> results = new ArrayList<>();
        Instant instant = Instant.now();
        String zoneId = "Europe/Paris";
        InstantToZonedDateTimeOp proto =
                InstantToZonedDateTimeOp.newBuilder()
                        .setInstant(
                                DynamicBuilders.DynamicInstant.withSecondsPrecision(instant)
                                        .toDynamicInstantProto())
                        .setZoneId(zoneId)
                        .build();
        InstantToZonedDateTimeOpNode node =
                new InstantToZonedDateTimeOpNode(proto, new AddToListCallback<>(results));
        node.getIncomingCallback().onData(instant);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getZone()).isEqualTo(ZoneId.of(zoneId));
        assertThat(results.get(0).toInstant()).isEqualTo(instant);
    }
}
