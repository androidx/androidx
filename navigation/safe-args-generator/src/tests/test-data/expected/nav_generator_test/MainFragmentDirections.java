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

package foo.flavor;

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class MainFragmentDirections {
    @NonNull
    public static StartLogin startLogin() {
        return new StartLogin();
    }

    public static class StartLogin implements NavDirections {
        public StartLogin() {
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            return __outBundle;
        }

        public int getActionId() {
            return foo.R.id.start_login;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            StartLogin that = (StartLogin) object;
            if (getActionId() != that.getActionId()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "StartLogin(actionId=" + getActionId() + "){"
                    + "}";
        }
    }
}