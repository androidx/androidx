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

public class SanitizedMainFragmentArgs implements NavArgs {
    private final HashMap arguments = new HashMap();

    private SanitizedMainFragmentArgs() {
    }

    @SuppressWarnings("unchecked")
    private SanitizedMainFragmentArgs(HashMap argumentsMap) {
        this.arguments.putAll(argumentsMap);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static SanitizedMainFragmentArgs fromBundle(@NonNull Bundle bundle) {
        SanitizedMainFragmentArgs __result = new SanitizedMainFragmentArgs();
        bundle.setClassLoader(SanitizedMainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("name.with.dot")) {
            int nameWithDot;
            nameWithDot = bundle.getInt("name.with.dot");
            __result.arguments.put("name.with.dot", nameWithDot);
        } else {
            throw new IllegalArgumentException("Required argument \"name.with.dot\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name_with_underscore")) {
            int nameWithUnderscore;
            nameWithUnderscore = bundle.getInt("name_with_underscore");
            __result.arguments.put("name_with_underscore", nameWithUnderscore);
        } else {
            throw new IllegalArgumentException("Required argument \"name_with_underscore\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name with spaces")) {
            int nameWithSpaces;
            nameWithSpaces = bundle.getInt("name with spaces");
            __result.arguments.put("name with spaces", nameWithSpaces);
        } else {
            throw new IllegalArgumentException("Required argument \"name with spaces\" is missing and does not have an android:defaultValue");
        }
        return __result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static SanitizedMainFragmentArgs fromSavedStateHandle(
            @NonNull SavedStateHandle savedStateHandle) {
        SanitizedMainFragmentArgs __result = new SanitizedMainFragmentArgs();
        if (savedStateHandle.contains("name.with.dot")) {
            int nameWithDot;
            nameWithDot = savedStateHandle.get("name.with.dot");
            __result.arguments.put("name.with.dot", nameWithDot);
        } else {
            throw new IllegalArgumentException("Required argument \"name.with.dot\" is missing and does not have an android:defaultValue");
        }
        if (savedStateHandle.contains("name_with_underscore")) {
            int nameWithUnderscore;
            nameWithUnderscore = savedStateHandle.get("name_with_underscore");
            __result.arguments.put("name_with_underscore", nameWithUnderscore);
        } else {
            throw new IllegalArgumentException("Required argument \"name_with_underscore\" is missing and does not have an android:defaultValue");
        }
        if (savedStateHandle.contains("name with spaces")) {
            int nameWithSpaces;
            nameWithSpaces = savedStateHandle.get("name with spaces");
            __result.arguments.put("name with spaces", nameWithSpaces);
        } else {
            throw new IllegalArgumentException("Required argument \"name with spaces\" is missing and does not have an android:defaultValue");
        }
        return __result;
    }

    @SuppressWarnings("unchecked")
    public int getNameWithDot() {
        return (int) arguments.get("name.with.dot");
    }

    @SuppressWarnings("unchecked")
    public int getNameWithUnderscore() {
        return (int) arguments.get("name_with_underscore");
    }

    @SuppressWarnings("unchecked")
    public int getNameWithSpaces() {
        return (int) arguments.get("name with spaces");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle toBundle() {
        Bundle __result = new Bundle();
        if (arguments.containsKey("name.with.dot")) {
            int nameWithDot = (int) arguments.get("name.with.dot");
            __result.putInt("name.with.dot", nameWithDot);
        }
        if (arguments.containsKey("name_with_underscore")) {
            int nameWithUnderscore = (int) arguments.get("name_with_underscore");
            __result.putInt("name_with_underscore", nameWithUnderscore);
        }
        if (arguments.containsKey("name with spaces")) {
            int nameWithSpaces = (int) arguments.get("name with spaces");
            __result.putInt("name with spaces", nameWithSpaces);
        }
        return __result;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public SavedStateHandle toSavedStateHandle() {
        SavedStateHandle __result = new SavedStateHandle();
        if (arguments.containsKey("name.with.dot")) {
            int nameWithDot = (int) arguments.get("name.with.dot");
            __result.set("name.with.dot", nameWithDot);
        }
        if (arguments.containsKey("name_with_underscore")) {
            int nameWithUnderscore = (int) arguments.get("name_with_underscore");
            __result.set("name_with_underscore", nameWithUnderscore);
        }
        if (arguments.containsKey("name with spaces")) {
            int nameWithSpaces = (int) arguments.get("name with spaces");
            __result.set("name with spaces", nameWithSpaces);
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
        SanitizedMainFragmentArgs that = (SanitizedMainFragmentArgs) object;
        if (arguments.containsKey("name.with.dot") != that.arguments.containsKey("name.with.dot")) {
            return false;
        }
        if (getNameWithDot() != that.getNameWithDot()) {
            return false;
        }
        if (arguments.containsKey("name_with_underscore") != that.arguments.containsKey("name_with_underscore")) {
            return false;
        }
        if (getNameWithUnderscore() != that.getNameWithUnderscore()) {
            return false;
        }
        if (arguments.containsKey("name with spaces") != that.arguments.containsKey("name with spaces")) {
            return false;
        }
        if (getNameWithSpaces() != that.getNameWithSpaces()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + getNameWithDot();
        result = 31 * result + getNameWithUnderscore();
        result = 31 * result + getNameWithSpaces();
        return result;
    }

    @Override
    public String toString() {
        return "SanitizedMainFragmentArgs{"
                + "nameWithDot=" + getNameWithDot()
                + ", nameWithUnderscore=" + getNameWithUnderscore()
                + ", nameWithSpaces=" + getNameWithSpaces()
                + "}";
    }

    public static final class Builder {
        private final HashMap arguments = new HashMap();

        @SuppressWarnings("unchecked")
        public Builder(@NonNull SanitizedMainFragmentArgs original) {
            this.arguments.putAll(original.arguments);
        }

        @SuppressWarnings("unchecked")
        public Builder(int nameWithDot, int nameWithUnderscore, int nameWithSpaces) {
            this.arguments.put("name.with.dot", nameWithDot);
            this.arguments.put("name_with_underscore", nameWithUnderscore);
            this.arguments.put("name with spaces", nameWithSpaces);
        }

        @NonNull
        public SanitizedMainFragmentArgs build() {
            SanitizedMainFragmentArgs result = new SanitizedMainFragmentArgs(arguments);
            return result;
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public Builder setNameWithDot(int nameWithDot) {
            this.arguments.put("name.with.dot", nameWithDot);
            return this;
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public Builder setNameWithUnderscore(int nameWithUnderscore) {
            this.arguments.put("name_with_underscore", nameWithUnderscore);
            return this;
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public Builder setNameWithSpaces(int nameWithSpaces) {
            this.arguments.put("name with spaces", nameWithSpaces);
            return this;
        }

        @SuppressWarnings({"unchecked","GetterOnBuilder"})
        public int getNameWithDot() {
            return (int) arguments.get("name.with.dot");
        }

        @SuppressWarnings({"unchecked","GetterOnBuilder"})
        public int getNameWithUnderscore() {
            return (int) arguments.get("name_with_underscore");
        }

        @SuppressWarnings({"unchecked","GetterOnBuilder"})
        public int getNameWithSpaces() {
            return (int) arguments.get("name with spaces");
        }
    }
}