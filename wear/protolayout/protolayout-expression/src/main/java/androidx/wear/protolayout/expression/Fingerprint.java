/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a virtually unique fingerprint for a proto message.
 *
 * <p>Note that this actually represents the way a message was built and not necessarily its
 * contents. In other words, 2 messages with the same contents may have different fingerprints if
 * their setters were called in a different order.
 *
 * <p>A value of -1 for {@code selfPropsValue} means the self part should be considered different
 * when compared with other instances of this class. A value of -1 for {@code childNodesValue} means
 * the children part should be considered different when compared with other instances of this
 * class.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class Fingerprint {
    private static final int DEFAULT_VALUE = 0;
    private static final int DISCARDED_VALUE = -1;
    private final int selfTypeValue;
    private int selfPropsValue;
    private int childNodesValue;
    private @Nullable List<Fingerprint> childNodes;

    public Fingerprint(int selfTypeValue) {
        this.selfTypeValue = selfTypeValue;
        this.selfPropsValue = DEFAULT_VALUE;
        this.childNodesValue = DEFAULT_VALUE;
        this.childNodes = null;
    }

    /**
     * Get the aggregate numeric fingerprint, representing the message itself as well as all its
     * child nodes. Returns -1 if the fingerprint is discarded.
     */
    public int aggregateValueAsInt() {
        if (selfPropsValue == DISCARDED_VALUE) {
            return DISCARDED_VALUE;
        }
        int aggregateValue = selfTypeValue;
        aggregateValue = (31 * aggregateValue) + selfPropsValue;
        aggregateValue = (31 * aggregateValue) + childNodesValue;
        return aggregateValue;
    }

    /** Get the numeric fingerprint for the message's type. */
    public int selfTypeValue() {
        return selfTypeValue;
    }

    /**
     * Get the numeric fingerprint for the message's properties only, excluding its type and child
     * nodes. Returns -1 if the fingerprint is discarded.
     */
    public int selfPropsValue() {
        return selfPropsValue;
    }

    /**
     * Get the numeric fingerprint for the child nodes. Returns -1 if the fingerprint for children
     * is discarded.
     *
     * <p>Note: If {@link #childNodes()} is empty, the children should be considered fully discarded
     * at this level. Otherwise, at least one of the children is discarded (self discard) and the
     * fingerprint of each children should be checked individually.
     */
    public int childNodesValue() {
        return childNodesValue;
    }

    /**
     * Get the child nodes. Returns empty list if the node has no children, or if the child
     * fingerprints are discarded.
     */
    public @NonNull List<Fingerprint> childNodes() {
        return childNodes == null ? Collections.emptyList() : childNodes;
    }

    /** Add a child node to this fingerprint. */
    public void addChildNode(@NonNull Fingerprint childNode) {
        // Even if the children are not discarded directly through discardValued(true), if one of
        // them is individually discarded, we need to propagate that so that the differ knows it
        // has to go down one more level. That's why childNodesValue == DISCARDED_VALUE doesn't
        // necessarily mean all of the children are discarded. childNodes is used to
        // differentiate these two cases.
        if (selfPropsValue == DISCARDED_VALUE
                && childNodesValue == DISCARDED_VALUE
                && childNodes == null) {
            return;
        }
        if (childNodes == null) {
            childNodes = new ArrayList<>();
        }
        childNodes.add(childNode);
        if (childNode.selfPropsValue == DISCARDED_VALUE) {
            childNodesValue = DISCARDED_VALUE;
        } else if (childNodesValue != DISCARDED_VALUE) {
            childNodesValue = (31 * childNodesValue) + childNode.aggregateValueAsInt();
        }
    }

    /**
     * Discard values of this fingerprint.
     *
     * @param includeChildren if True, discards children values of this fingerprints too.
     */
    public void discardValues(boolean includeChildren) {
        if (selfPropsValue == DISCARDED_VALUE
                && childNodesValue == DISCARDED_VALUE
                && !includeChildren) {
            throw new IllegalStateException(
                    "Container is in discarded state. Children can't be reinstated.");
        }
        selfPropsValue = DISCARDED_VALUE;
        if (includeChildren) {
            childNodesValue = DISCARDED_VALUE;
            childNodes = null;
        }
    }

    /** Record a property value being updated. */
    public void recordPropertyUpdate(int fieldNumber, int valueHash) {
        recordEntry(fieldNumber);
        recordEntry(valueHash);
    }

    private void recordEntry(int entry) {
        selfPropsValue = (31 * selfPropsValue) + entry;
    }
}
