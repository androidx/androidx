/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

/**
 * PaintOperation interface, used for operations aimed at painting (while any operation _can_ paint,
 * this make it a little more explicit)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PaintOperation extends Operation implements Serializable {

    @Override
    public void apply(@NonNull RemoteContext context) {
        if (context.getMode() == RemoteContext.ContextMode.PAINT) {
            PaintContext paintContext = context.getPaintContext();
            if (paintContext != null) {
                paint(paintContext);
            }
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    /**
     * Paint the operation in the context
     *
     * @param context painting context
     */
    public abstract void paint(@NonNull PaintContext context);

    /**
     * Will return true if the operation is similar enough to the current one, in the context of an
     * animated transition.
     */
    public boolean suitableForTransition(@NonNull Operation op) {
        // by default expects the op to not be suitable
        return false;
    }

    /** Path or Bitmap need to be dereferenced */
    public static final int PTR_DEREFERENCE = 0x1 << 30;

    /** Valid bits in Path or Bitmap */
    public static final int VALUE_MASK = 0xFFFF;

    /**
     * Get the id from the context if needed
     *
     * @param id the id to get
     * @param context the context
     * @return the id dereferenced if needed
     */
    protected int getId(int id, @NonNull PaintContext context) {
        int returnId = id & VALUE_MASK;
        if ((id & PTR_DEREFERENCE) != 0) {
            returnId = context.getContext().getInteger(returnId);
        }
        return returnId;
    }
}
