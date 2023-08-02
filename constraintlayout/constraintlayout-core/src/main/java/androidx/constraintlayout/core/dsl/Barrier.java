/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import java.util.ArrayList;

public class Barrier extends Helper {
    private Constraint.Side mDirection = null;
    private int mMargin = Integer.MIN_VALUE;
    private ArrayList<Ref> references = new ArrayList<>();

    public Barrier(String name) {
        super(name, new HelperType(typeMap.get(Type.BARRIER)));
    }

    public Barrier(String name, String config) {
        super(name, new HelperType(typeMap.get(Type.BARRIER)), config);
        configMap = convertConfigToMap();
        if (configMap.containsKey("contains")) {
            Ref.addStringToReferences(configMap.get("contains"), references);
        }
    }

    /**
     * Get the direction of the Barrier
     *
     * @return direction
     */
    public Constraint.Side getDirection() {
        return mDirection;
    }

    /**
     * Set the direction of the Barrier
     *
     * @param direction
     */
    public void setDirection(Constraint.Side direction) {
        mDirection = direction;
        configMap.put("direction", sideMap.get(direction));
    }

    /**
     * Get the margin of the Barrier
     *
     * @return margin
     */
    public int getMargin() {
        return mMargin;
    }

    /**
     * Set the margin of the Barrier
     *
     * @param margin
     */
    public void setMargin(int margin) {
        mMargin = margin;
        configMap.put("margin", String.valueOf(margin));
    }

    /**
     * Convert references into a String representation
     *
     * @return a String representation of references
     */
    public String referencesToString() {
        if (references.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("[");
        for (Ref ref : references) {
            builder.append(ref.toString());
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Add a new reference
     *
     * @param ref reference
     * @return Barrier
     */
    public Barrier addReference(Ref ref) {
        references.add(ref);
        configMap.put("contains", referencesToString());
        return this;
    }

    /**
     * Add a new reference
     *
     * @param ref reference in a String representation
     * @return Chain
     */
    public Barrier addReference(String ref) {
        return addReference(Ref.parseStringToRef(ref));
    }
}