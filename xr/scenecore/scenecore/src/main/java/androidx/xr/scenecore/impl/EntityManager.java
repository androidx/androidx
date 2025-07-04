/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import static java.util.stream.Collectors.toCollection;

import androidx.xr.runtime.internal.ActivityPose;
import androidx.xr.runtime.internal.Entity;

import com.android.extensions.xr.node.Node;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the mapping between {@link Node} and {@link Entity} for a given {@link
 * JxrPlatformAdapterAxr}.
 */
@SuppressWarnings("BanConcurrentHashMap")
final class EntityManager {
    private final Map<Node, Entity> mNodeEntityMap = new ConcurrentHashMap<>();
    private final List<ActivityPose> mSystemSpaces = new ArrayList<>();

    /**
     * Returns the {@link Entity} associated with the given {@link Node}.
     *
     * @param node the {@link Node} to get the associated {@link Entity} for.
     * @return the {@link Entity} associated with the given {@link Node}, or null if no such {@link
     *     Entity} exists.
     */
    @Nullable Entity getEntityForNode(@NonNull Node node) {
        return mNodeEntityMap.get(node);
    }

    /**
     * Sets the {@link Entity} associated with the given {@link Node}.
     *
     * @param node the {@link Node} to set the associated {@link Entity} for.
     * @param entity the {@link Entity} to associate with the given {@link Node}.
     */
    void setEntityForNode(@NonNull Node node, @NonNull Entity entity) {
        mNodeEntityMap.put(node, entity);
    }

    /**
     * Returns a list of all {@link Entity}s of type {@code T} (including subtypes of {@code T}).
     *
     * @param entityClass the type of {@link Entity} to return.
     * @return a list of all {@link Entity}s of type {@code T} (including subtypes of {@code T}).
     */
    <T extends Entity> List<T> getEntitiesOfType(@NonNull Class<T> entityClass) {
        return mNodeEntityMap.values().stream()
                .filter(entityClass::isInstance)
                .map(entityClass::cast)
                .collect(toCollection(ArrayList::new));
    }

    /** Returns a collection of all {@link Entity}s. */
    Collection<Entity> getAllEntities() {
        return mNodeEntityMap.values();
    }

    /** Removes the given {@link Node} from the map. */
    void removeEntityForNode(@NonNull Node node) {
        mNodeEntityMap.remove(node);
    }

    /** Adds a system space activity pose to the EntityManager. */
    void addSystemSpaceActivityPose(@NonNull ActivityPose systemSpaceActivityPose) {
        mSystemSpaces.add(systemSpaceActivityPose);
    }

    /** Returns a collection of all system space activity poses. */
    List<ActivityPose> getAllSystemSpaceActivityPoses() {
        return mSystemSpaces;
    }

    /**
     * Returns a list of all {@link ActivityPose}s of type {@code T} (including subtypes of {@code
     * T}).
     *
     * @param systemSpaceActivityPoseClass the type of {@link ActivityPose} to return.
     * @return a list of all {@link ActivityPose}s of type {@code T} (including subtypes of {@code
     *     T}).
     */
    <T extends ActivityPose> List<T> getSystemSpaceActivityPoseOfType(
            @NonNull Class<T> systemSpaceActivityPoseClass) {
        return mSystemSpaces.stream()
                .filter(systemSpaceActivityPoseClass::isInstance)
                .map(systemSpaceActivityPoseClass::cast)
                .collect(toCollection(ArrayList::new));
    }

    /** Clears the EntityManager. */
    void clear() {
        mNodeEntityMap.clear();
        mSystemSpaces.clear();
    }
}
