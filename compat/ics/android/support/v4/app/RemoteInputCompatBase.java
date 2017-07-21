/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.app;

import android.os.Bundle;

import java.util.Set;

/**
 * @deprecated This class was not meant to be made public.
 */
@Deprecated
class RemoteInputCompatBase {

    /**
     * @deprecated This class was not meant to be made public.
     */
    @Deprecated
    public abstract static class RemoteInput {
        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        public RemoteInput() {}

        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        protected abstract String getResultKey();

        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        protected abstract CharSequence getLabel();

        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        protected abstract CharSequence[] getChoices();

        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        protected abstract boolean getAllowFreeFormInput();

        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        protected abstract Bundle getExtras();

        /**
         * @deprecated This method was not meant to be made public.
         */
        @Deprecated
        protected abstract Set<String> getAllowedDataTypes();

        /**
         * @deprecated This class was not meant to be made public.
         */
        @Deprecated
        public interface Factory {
            RemoteInput build(String resultKey, CharSequence label,
                    CharSequence[] choices, boolean allowFreeFormInput, Bundle extras,
                    Set<String> allowedDataTypes);
            RemoteInput[] newArray(int length);
        }
    }
}
