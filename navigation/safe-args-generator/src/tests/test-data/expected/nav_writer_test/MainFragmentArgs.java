package a.b;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.nio.file.AccessMode;

public class MainFragmentArgs {
    @NonNull
    private String main;

    private int optional = -1;

    private int reference = a.b.R.drawable.background;

    private float floatArg = 1F;

    @NonNull
    private float[] floatArrayArg;

    @NonNull
    private ActivityInfo[] objectArrayArg;

    private boolean boolArg = true;

    @Nullable
    private ActivityInfo optionalParcelable = null;

    @NonNull
    private AccessMode enumArg = AccessMode.READ;

    private MainFragmentArgs() {
    }

    @NonNull
    public static MainFragmentArgs fromBundle(Bundle bundle) {
        MainFragmentArgs result = new MainFragmentArgs();
        bundle.setClassLoader(MainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("main")) {
            result.main = bundle.getString("main");
            if (result.main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
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
        if (bundle.containsKey("floatArrayArg")) {
            result.floatArrayArg = bundle.getFloatArray("floatArrayArg");
            if (result.floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
        } else {
            throw new IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("objectArrayArg")) {
            result.objectArrayArg = (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
            if (result.objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
        } else {
            throw new IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("boolArg")) {
            result.boolArg = bundle.getBoolean("boolArg");
        }
        if (bundle.containsKey("optionalParcelable")) {
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                result.optionalParcelable = (ActivityInfo) bundle.get("optionalParcelable");
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        if (bundle.containsKey("enumArg")) {
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
                result.enumArg = (AccessMode) bundle.get("enumArg");
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            if (result.enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
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

    @NonNull
    public float[] getFloatArrayArg() {
        return floatArrayArg;
    }

    @NonNull
    public ActivityInfo[] getObjectArrayArg() {
        return objectArrayArg;
    }

    public boolean getBoolArg() {
        return boolArg;
    }

    @Nullable
    public ActivityInfo getOptionalParcelable() {
        return optionalParcelable;
    }

    @NonNull
    public AccessMode getEnumArg() {
        return enumArg;
    }

    @NonNull
    public Bundle toBundle() {
        Bundle __outBundle = new Bundle();
        __outBundle.putString("main", this.main);
        __outBundle.putInt("optional", this.optional);
        __outBundle.putInt("reference", this.reference);
        __outBundle.putFloat("floatArg", this.floatArg);
        __outBundle.putFloatArray("floatArrayArg", this.floatArrayArg);
        __outBundle.putParcelableArray("objectArrayArg", this.objectArrayArg);
        __outBundle.putBoolean("boolArg", this.boolArg);
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || this.optionalParcelable == null) {
            __outBundle.putParcelable("optionalParcelable", Parcelable.class.cast(this.optionalParcelable));
        } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            __outBundle.putSerializable("optionalParcelable", Serializable.class.cast(this.optionalParcelable));
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
        if (Parcelable.class.isAssignableFrom(AccessMode.class) || this.enumArg == null) {
            __outBundle.putParcelable("enumArg", Parcelable.class.cast(this.enumArg));
        } else if (Serializable.class.isAssignableFrom(AccessMode.class)) {
            __outBundle.putSerializable("enumArg", Serializable.class.cast(this.enumArg));
        } else {
            throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
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
        if (floatArrayArg != null ? !floatArrayArg.equals(that.floatArrayArg) : that.floatArrayArg != null) {
            return false;
        }
        if (objectArrayArg != null ? !objectArrayArg.equals(that.objectArrayArg) : that.objectArrayArg != null) {
            return false;
        }
        if (boolArg != that.boolArg) {
            return false;
        }
        if (optionalParcelable != null ? !optionalParcelable.equals(that.optionalParcelable) : that.optionalParcelable != null) {
            return false;
        }
        if (enumArg != null ? !enumArg.equals(that.enumArg) : that.enumArg != null) {
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
        result = 31 * result + java.util.Arrays.hashCode(floatArrayArg);
        result = 31 * result + java.util.Arrays.hashCode(objectArrayArg);
        result = 31 * result + (boolArg ? 1 : 0);
        result = 31 * result + (optionalParcelable != null ? optionalParcelable.hashCode() : 0);
        result = 31 * result + (enumArg != null ? enumArg.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MainFragmentArgs{"
                + "main=" + main
                + ", optional=" + optional
                + ", reference=" + reference
                + ", floatArg=" + floatArg
                + ", floatArrayArg=" + floatArrayArg
                + ", objectArrayArg=" + objectArrayArg
                + ", boolArg=" + boolArg
                + ", optionalParcelable=" + optionalParcelable
                + ", enumArg=" + enumArg
                + "}";
    }

    public static class Builder {
        @NonNull
        private String main;

        private int optional = -1;

        private int reference = a.b.R.drawable.background;

        private float floatArg = 1F;

        @NonNull
        private float[] floatArrayArg;

        @NonNull
        private ActivityInfo[] objectArrayArg;

        private boolean boolArg = true;

        @Nullable
        private ActivityInfo optionalParcelable = null;

        @NonNull
        private AccessMode enumArg = AccessMode.READ;

        public Builder(MainFragmentArgs original) {
            this.main = original.main;
            this.optional = original.optional;
            this.reference = original.reference;
            this.floatArg = original.floatArg;
            this.floatArrayArg = original.floatArrayArg;
            this.objectArrayArg = original.objectArrayArg;
            this.boolArg = original.boolArg;
            this.optionalParcelable = original.optionalParcelable;
            this.enumArg = original.enumArg;
        }

        public Builder(@NonNull String main, @NonNull float[] floatArrayArg,
                @NonNull ActivityInfo[] objectArrayArg) {
            this.main = main;
            if (this.main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.floatArrayArg = floatArrayArg;
            if (this.floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.objectArrayArg = objectArrayArg;
            if (this.objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
        }

        @NonNull
        public MainFragmentArgs build() {
            MainFragmentArgs result = new MainFragmentArgs();
            result.main = this.main;
            result.optional = this.optional;
            result.reference = this.reference;
            result.floatArg = this.floatArg;
            result.floatArrayArg = this.floatArrayArg;
            result.objectArrayArg = this.objectArrayArg;
            result.boolArg = this.boolArg;
            result.optionalParcelable = this.optionalParcelable;
            result.enumArg = this.enumArg;
            return result;
        }

        @NonNull
        public Builder setMain(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
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
        public Builder setFloatArrayArg(@NonNull float[] floatArrayArg) {
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.floatArrayArg = floatArrayArg;
            return this;
        }

        @NonNull
        public Builder setObjectArrayArg(@NonNull ActivityInfo[] objectArrayArg) {
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.objectArrayArg = objectArrayArg;
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
        public Builder setEnumArg(@NonNull AccessMode enumArg) {
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            this.enumArg = enumArg;
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

        @NonNull
        public float[] getFloatArrayArg() {
            return floatArrayArg;
        }

        @NonNull
        public ActivityInfo[] getObjectArrayArg() {
            return objectArrayArg;
        }

        public boolean getBoolArg() {
            return boolArg;
        }

        @Nullable
        public ActivityInfo getOptionalParcelable() {
            return optionalParcelable;
        }

        @NonNull
        public AccessMode getEnumArg() {
            return enumArg;
        }
    }
}