package a.b;

import android.os.Bundle;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;

public class SanitizedMainFragmentArgs {
    private int nameWithDot;

    private int nameWithUnderscore;

    private int nameWithSpaces;

    private SanitizedMainFragmentArgs() {
    }

    public static SanitizedMainFragmentArgs fromBundle(Bundle bundle) {
        SanitizedMainFragmentArgs result = new SanitizedMainFragmentArgs();
        bundle.setClassLoader(SanitizedMainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("name.with.dot")) {
            result.nameWithDot = bundle.getInt("name.with.dot");
        } else {
            throw new IllegalArgumentException("Required argument \"name.with.dot\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name_with_underscore")) {
            result.nameWithUnderscore = bundle.getInt("name_with_underscore");
        } else {
            throw new IllegalArgumentException("Required argument \"name_with_underscore\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name with spaces")) {
            result.nameWithSpaces = bundle.getInt("name with spaces");
        } else {
            throw new IllegalArgumentException("Required argument \"name with spaces\" is missing and does not have an android:defaultValue");
        }
        return result;
    }

    public int getNameWithDot() {
        return nameWithDot;
    }

    public int getNameWithUnderscore() {
        return nameWithUnderscore;
    }

    public int getNameWithSpaces() {
        return nameWithSpaces;
    }

    public Bundle toBundle() {
        Bundle __outBundle = new Bundle();
        __outBundle.putInt("name.with.dot", this.nameWithDot);
        __outBundle.putInt("name_with_underscore", this.nameWithUnderscore);
        __outBundle.putInt("name with spaces", this.nameWithSpaces);
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
        SanitizedMainFragmentArgs that = (SanitizedMainFragmentArgs) object;
        if (nameWithDot != that.nameWithDot) {
            return false;
        }
        if (nameWithUnderscore != that.nameWithUnderscore) {
            return false;
        }
        if (nameWithSpaces != that.nameWithSpaces) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + nameWithDot;
        result = 31 * result + nameWithUnderscore;
        result = 31 * result + nameWithSpaces;
        return result;
    }

    public static class Builder {
        private int nameWithDot;

        private int nameWithUnderscore;

        private int nameWithSpaces;

        public Builder(SanitizedMainFragmentArgs original) {
            this.nameWithDot = original.nameWithDot;
            this.nameWithUnderscore = original.nameWithUnderscore;
            this.nameWithSpaces = original.nameWithSpaces;
        }

        public Builder(int nameWithDot, int nameWithUnderscore, int nameWithSpaces) {
            this.nameWithDot = nameWithDot;
            this.nameWithUnderscore = nameWithUnderscore;
            this.nameWithSpaces = nameWithSpaces;
        }

        public SanitizedMainFragmentArgs build() {
            SanitizedMainFragmentArgs result = new SanitizedMainFragmentArgs();
            result.nameWithDot = this.nameWithDot;
            result.nameWithUnderscore = this.nameWithUnderscore;
            result.nameWithSpaces = this.nameWithSpaces;
            return result;
        }

        public Builder setNameWithDot(int nameWithDot) {
            this.nameWithDot = nameWithDot;
            return this;
        }

        public Builder setNameWithUnderscore(int nameWithUnderscore) {
            this.nameWithUnderscore = nameWithUnderscore;
            return this;
        }

        public Builder setNameWithSpaces(int nameWithSpaces) {
            this.nameWithSpaces = nameWithSpaces;
            return this;
        }

        public int getNameWithDot() {
            return nameWithDot;
        }

        public int getNameWithUnderscore() {
            return nameWithUnderscore;
        }

        public int getNameWithSpaces() {
            return nameWithSpaces;
        }
    }
}