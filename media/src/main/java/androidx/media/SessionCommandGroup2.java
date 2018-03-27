/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Set;

/**
 * @hide
 * Represent set of {@link SessionCommand2}.
 */
@RestrictTo(LIBRARY_GROUP)
public final class SessionCommandGroup2 {
    //private final CommandGroupProvider mProvider;

    /**
     * TODO: javadoc
     */
    public SessionCommandGroup2() {
//            mProvider = ApiLoader.getProvider().createMediaSession2CommandGroup(
//                    context, this, null);
    }

    /**
     * TODO: javadoc
     */
    public SessionCommandGroup2(@Nullable SessionCommandGroup2 others) {
//            mProvider = ApiLoader.getProvider().createMediaSession2CommandGroup(
//                    context, this, others);
    }

//        /**
//         * @hide
//         */
//        public CommandGroup(@NonNull CommandGroupProvider provider) {
//            mProvider = provider;
//        }

    /**
     * TODO: javadoc
     */
    public void addCommand(@NonNull SessionCommand2 command) {
        //mProvider.addCommand_impl(command);
    }

    /**
     * TODO: javadoc
     */
    public void addCommand(int commandCode) {
        // TODO: Implement
    }

    /**
     * TODO: javadoc
     */
    public void addAllPredefinedCommands() {
        //mProvider.addAllPredefinedCommands_impl();
    }

    /**
     * TODO: javadoc
     */
    public void removeCommand(@NonNull SessionCommand2 command) {
        //mProvider.removeCommand_impl(command);
    }

    /**
     * TODO: javadoc
     */
    public void removeCommand(int commandCode) {
        // TODO(jaewan): Implement.
    }

    /**
     * TODO: javadoc
     */
    public boolean hasCommand(@NonNull SessionCommand2 command) {
        //return mProvider.hasCommand_impl(command);
        return false;
    }

    /**
     * TODO: javadoc
     */
    public boolean hasCommand(int code) {
        //return mProvider.hasCommand_impl(code);
        return false;
    }

    /**
     * TODO: javadoc
     */
    public /*@NonNull*/ Set<SessionCommand2> getCommands() {
        //return mProvider.getCommands_impl();
        return null;
    }

//        /**
//         * @hide
//         */
//        public @NonNull CommandGroupProvider getProvider() {
//            return mProvider;
//        }

    /**
     * @return new bundle from the CommandGroup
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public /*@NonNull*/ Bundle toBundle() {
        //return mProvider.toBundle_impl();
        return null;
    }

    /**
     * @return new instance of CommandGroup from the bundle
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static @Nullable SessionCommandGroup2 fromBundle(Context context, Bundle commands) {
        //return ApiLoader.getProvider()
        //        .fromBundle_MediaSession2CommandGroup(context, commands);
        return null;
    }
}
