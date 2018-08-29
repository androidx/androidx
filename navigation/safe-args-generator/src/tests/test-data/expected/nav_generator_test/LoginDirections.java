package foo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class LoginDirections {
    @NonNull
    public static ActionDone actionDone() {
        return new ActionDone();
    }

    public static class ActionDone implements NavDirections {
        public ActionDone() {
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            return __outBundle;
        }

        public int getActionId() {
            return foo.R.id.action_done;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            ActionDone that = (ActionDone) object;
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
            return "ActionDone(actionId=" + getActionId() + "){"
                    + "}";
        }
    }
}