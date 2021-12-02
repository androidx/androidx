package a.b;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class MainFragment$InnerFragmentArgs implements NavArgs {
    private final HashMap arguments = new HashMap();

    private MainFragment$InnerFragmentArgs() {
    }

    @SuppressWarnings("unchecked")
    private MainFragment$InnerFragmentArgs(HashMap argumentsMap) {
        this.arguments.putAll(argumentsMap);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static MainFragment$InnerFragmentArgs fromBundle(@NonNull Bundle bundle) {
        MainFragment$InnerFragmentArgs __result = new MainFragment$InnerFragmentArgs();
        bundle.setClassLoader(MainFragment$InnerFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("mainArg")) {
            String mainArg;
            mainArg = bundle.getString("mainArg");
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            __result.arguments.put("mainArg", mainArg);
        } else {
            throw new IllegalArgumentException("Required argument \"mainArg\" is missing and does not have an android:defaultValue");
        }
        return __result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static MainFragment$InnerFragmentArgs fromSavedStateHandle(
            @NonNull SavedStateHandle savedStateHandle) {
        MainFragment$InnerFragmentArgs __result = new MainFragment$InnerFragmentArgs();
        if (savedStateHandle.contains("mainArg")) {
            String mainArg;
            mainArg = savedStateHandle.get("mainArg");
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            __result.arguments.put("mainArg", mainArg);
        } else {
            throw new IllegalArgumentException("Required argument \"mainArg\" is missing and does not have an android:defaultValue");
        }
        return __result;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getMainArg() {
        return (String) arguments.get("mainArg");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle toBundle() {
        Bundle __result = new Bundle();
        if (arguments.containsKey("mainArg")) {
            String mainArg = (String) arguments.get("mainArg");
            __result.putString("mainArg", mainArg);
        }
        return __result;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public SavedStateHandle toSavedStateHandle() {
        SavedStateHandle __result = new SavedStateHandle();
        if (arguments.containsKey("mainArg")) {
            String mainArg = (String) arguments.get("mainArg");
            __result.set("mainArg", mainArg);
        }
        return __result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        MainFragment$InnerFragmentArgs that = (MainFragment$InnerFragmentArgs) object;
        if (arguments.containsKey("mainArg") != that.arguments.containsKey("mainArg")) {
            return false;
        }
        if (getMainArg() != null ? !getMainArg().equals(that.getMainArg()) : that.getMainArg() != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (getMainArg() != null ? getMainArg().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MainFragment$InnerFragmentArgs{"
                + "mainArg=" + getMainArg()
                + "}";
    }

    public static final class Builder {
        private final HashMap arguments = new HashMap();

        @SuppressWarnings("unchecked")
        public Builder(@NonNull MainFragment$InnerFragmentArgs original) {
            this.arguments.putAll(original.arguments);
        }

        @SuppressWarnings("unchecked")
        public Builder(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("mainArg", mainArg);
        }

        @NonNull
        public MainFragment$InnerFragmentArgs build() {
            MainFragment$InnerFragmentArgs result = new MainFragment$InnerFragmentArgs(arguments);
            return result;
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public Builder setMainArg(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("mainArg", mainArg);
            return this;
        }

        @SuppressWarnings({"unchecked","GetterOnBuilder"})
        @NonNull
        public String getMainArg() {
            return (String) arguments.get("mainArg");
        }
    }
}