package foo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class SettingsDirections {
    private SettingsDirections() {
    }

    @NonNull
    public static Main main() {
        return new Main();
    }

    @NonNull
    public static Exit exit() {
        return new Exit();
    }

    public static class Main implements NavDirections {
        private final HashMap arguments = new HashMap();

        private Main() {
        }

        @NonNull
        public Main setEnterReason(@NonNull String enterReason) {
            if (enterReason == null) {
                throw new IllegalArgumentException("Argument \"enterReason\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("enterReason", enterReason);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        @NonNull
        public Bundle getArguments() {
            Bundle __result = new Bundle();
            if (arguments.containsKey("enterReason")) {
                String enterReason = (String) arguments.get("enterReason");
                __result.putString("enterReason", enterReason);
            }
            return __result;
        }

        @Override
        public int getActionId() {
            return R.id.main;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getEnterReason() {
            return (String) arguments.get("enterReason");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Main that = (Main) object;
            if (arguments.containsKey("enterReason") != that.arguments.containsKey("enterReason")) {
                return false;
            }
            if (getEnterReason() != null ? !getEnterReason().equals(that.getEnterReason()) : that.getEnterReason() != null) {
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
            result = 31 * result + (getEnterReason() != null ? getEnterReason().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "Main(actionId=" + getActionId() + "){"
                    + "enterReason=" + getEnterReason()
                    + "}";
        }
    }

    public static class Exit implements NavDirections {
        private final HashMap arguments = new HashMap();

        private Exit() {
        }

        @NonNull
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