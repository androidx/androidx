package a.b;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class MainFragmentArgs {
    @NonNull
    private String main;

    private int optional = -1;

    private int reference = a.b.R.drawable.background;

    private float floatArg = 1F;

    private boolean boolArg = true;

    @Nullable
    private ActivityInfo optionalParcelable = null;

    private MainFragmentArgs() {
    }

    @NonNull
    public static MainFragmentArgs fromBundle(Bundle bundle) {
        MainFragmentArgs result = new MainFragmentArgs();
        bundle.setClassLoader(MainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("main")) {
            result.main = bundle.getString("main");
        } else {
            throw new IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("optional")) {
            result.optional = bundle.getInt("optional");
        }
        if (bundle.containsKey("reference")) {
            result.reference = bundle.getInt("reference");
        }
        if (bundle.containsKey("floatArg")) {
            result.floatArg = bundle.getFloat("floatArg");
        }
        if (bundle.containsKey("boolArg")) {
            result.boolArg = bundle.getBoolean("boolArg");
        }
        if (bundle.containsKey("optionalParcelable")) {
            result.optionalParcelable = bundle.getParcelable("optionalParcelable");
        }
        return result;
    }

    @NonNull
    public String getMain() {
        return main;
    }

    public int getOptional() {
        return optional;
    }

    public int getReference() {
        return reference;
    }

    public float getFloatArg() {
        return floatArg;
    }

    public boolean getBoolArg() {
        return boolArg;
    }

    @Nullable
    public ActivityInfo getOptionalParcelable() {
        return optionalParcelable;
    }

    @NonNull
    public Bundle toBundle() {
        Bundle __outBundle = new Bundle();
        __outBundle.putString("main", this.main);
        __outBundle.putInt("optional", this.optional);
        __outBundle.putInt("reference", this.reference);
        __outBundle.putFloat("floatArg", this.floatArg);
        __outBundle.putBoolean("boolArg", this.boolArg);
        __outBundle.putParcelable("optionalParcelable", this.optionalParcelable);
        return __outBundle;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }
        MainFragmentArgs that = (MainFragmentArgs) object;
        if (main != null ? !main.equals(that.main) : that.main != null) {
            return false;
        }
        if (optional != that.optional) {
            return false;
        }
        if (reference != that.reference) {
            return false;
        }
        if (Float.compare(that.floatArg, floatArg) != 0) {
            return false;
        }
        if (boolArg != that.boolArg) {
            return false;
        }
        if (optionalParcelable != null ? !optionalParcelable.equals(that.optionalParcelable) : that.optionalParcelable != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (main != null ? main.hashCode() : 0);
        result = 31 * result + optional;
        result = 31 * result + reference;
        result = 31 * result + Float.floatToIntBits(floatArg);
        result = 31 * result + (boolArg ? 1 : 0);
        result = 31 * result + (optionalParcelable != null ? optionalParcelable.hashCode() : 0);
        return result;
    }

    public static class Builder {
        @NonNull
        private String main;

        private int optional = -1;

        private int reference = a.b.R.drawable.background;

        private float floatArg = 1F;

        private boolean boolArg = true;

        @Nullable
        private ActivityInfo optionalParcelable = null;

        public Builder(MainFragmentArgs original) {
            this.main = original.main;
            this.optional = original.optional;
            this.reference = original.reference;
            this.floatArg = original.floatArg;
            this.boolArg = original.boolArg;
            this.optionalParcelable = original.optionalParcelable;
        }

        public Builder(@NonNull String main) {
            this.main = main;
        }

        @NonNull
        public MainFragmentArgs build() {
            MainFragmentArgs result = new MainFragmentArgs();
            result.main = this.main;
            result.optional = this.optional;
            result.reference = this.reference;
            result.floatArg = this.floatArg;
            result.boolArg = this.boolArg;
            result.optionalParcelable = this.optionalParcelable;
            return result;
        }

        @NonNull
        public Builder setMain(@NonNull String main) {
            this.main = main;
            return this;
        }

        @NonNull
        public Builder setOptional(int optional) {
            this.optional = optional;
            return this;
        }

        @NonNull
        public Builder setReference(int reference) {
            this.reference = reference;
            return this;
        }

        @NonNull
        public Builder setFloatArg(float floatArg) {
            this.floatArg = floatArg;
            return this;
        }

        @NonNull
        public Builder setBoolArg(boolean boolArg) {
            this.boolArg = boolArg;
            return this;
        }

        @NonNull
        public Builder setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
            this.optionalParcelable = optionalParcelable;
            return this;
        }

        @NonNull
        public String getMain() {
            return main;
        }

        public int getOptional() {
            return optional;
        }

        public int getReference() {
            return reference;
        }

        public float getFloatArg() {
            return floatArg;
        }

        public boolean getBoolArg() {
            return boolArg;
        }

        @Nullable
        public ActivityInfo getOptionalParcelable() {
            return optionalParcelable;
        }
    }
}