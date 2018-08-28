package foo.flavor;

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import foo.LoginDirections;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class LoginFragmentDirections extends LoginDirections {
    @NonNull
    public static Register register() {
        return new Register();
    }

    public static class Register implements NavDirections {
        public Register() {
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            return __outBundle;
        }

        public int getActionId() {
            return foo.R.id.register;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Register that = (Register) object;
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
            return "Register(actionId=" + getActionId() + "){"
                    + "}";
        }
    }
}