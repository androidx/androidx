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

import androidx.wear.protolayout.expression.proto.DynamicProto.InstantToZonedDateTimeOp;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Dynamic data nodes which yield {@link ZonedDateTime}. */
class ZonedDateTimeNodes {
    private ZonedDateTimeNodes() {}

    /** Dynamic instant node that has a fixed value. */
    static class InstantToZonedDateTimeOpNode
            extends DynamicDataTransformNode<Instant, ZonedDateTime> {
        InstantToZonedDateTimeOpNode(
                InstantToZonedDateTimeOp protoNode,
                DynamicTypeValueReceiverWithPreUpdate<ZonedDateTime> downstream) {
            super(
                    downstream,
                    t -> {
                        try {
                            return ZonedDateTime.ofInstant(t, ZoneId.of(protoNode.getZoneId()));
                        } catch (DateTimeException e) {
                            // Converting the exception to clarify that the error is happening
                            // because of a bad input.
                            throw new IllegalArgumentException(
                                    "Invalid zone ID: " + protoNode.getZoneId(), e);
                        }
                    });
        }
    }
}
