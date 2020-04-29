package foo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class InnerSettingsDirections {
    private InnerSettingsDirections() {
    }

    @NonNull
    public static Exit exit(int exitReason) {
        return new Exit(exitReason);
    }

    @NonNull
    public static SettingsDirections.Main main() {
        return SettingsDirections.main();
    }

    public static class Exit implements NavDirections {
        private final HashMap arguments = new HashMap();

        private Exit(int exitReason) {
            this.arguments.put("exitReason", exitReason);
        }

        @NonNull
        public Exit setExitReason(int exitReason) {
            this.arguments.put("exitReason", exitReason);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        @NonNull
        public Bundle getArguments() {
            Bundle __result = new Bundle();
            if (arguments.containsKey("exitReason")) {
                int exitReason = (int) arguments.get("exitReason");
                __result.putInt("exitReason", exitReason);
            }
            return __result;
        }

        @Override
        public int getActionId() {
            return R.id.exit;
        }

        @SuppressWarnings("unchecked")
        public int getExitReason() {
            return (int) arguments.get("exitReason");
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
            if (getExitReason() != that.getExitReason()) {
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
            result = 31 * result + getExitReason();
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