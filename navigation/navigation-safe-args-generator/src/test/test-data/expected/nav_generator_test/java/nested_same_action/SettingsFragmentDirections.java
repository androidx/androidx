package foo.flavor;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import foo.R;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class SettingsFragmentDirections {
    private SettingsFragmentDirections() {
    }

    @NonNull
    public static Exit exit() {
        return new Exit();
    }

    public static class Exit implements NavDirections {
        private final HashMap arguments = new HashMap();

        private Exit() {
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public Exit setExitReason(@NonNull String exitReason) {
            if (exitReason == null) {
                throw new IllegalArgumentException("Argument \"exitReason\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("exitReason", exitReason);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        @NonNull
        public Bundle getArguments() {
            Bundle __result = new Bundle();
            if (arguments.containsKey("exitReason")) {
                String exitReason = (String) arguments.get("exitReason");
                __result.putString("exitReason", exitReason);
            } else {
                __result.putString("exitReason", "DIFFERENT");
            }
            return __result;
        }

        @Override
        public int getActionId() {
            return R.id.exit;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getExitReason() {
            return (String) arguments.get("exitReason");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Exit that = (Exit) object;
            if (arguments.containsKey("exitReason") != that.arguments.containsKey("exitReason")) {
                return false;
            }
            if (getExitReason() != null ? !getExitReason().equals(that.getExitReason()) : that.getExitReason() != null) {
                return false;
            }
            if (getActionId() != that.getActionId()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + (getExitReason() != null ? getExitReason().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "Exit(actionId=" + getActionId() + "){"
                    + "exitReason=" + getExitReason()
                    + "}";
        }
    }
}