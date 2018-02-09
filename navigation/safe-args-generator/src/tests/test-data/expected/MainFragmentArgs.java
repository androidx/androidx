package a.b;

import android.os.Bundle;
import java.lang.IllegalArgumentException;
import java.lang.String;

public class MainFragmentArgs {
    private String main;

    private int optional = -1;

    private int reference = a.b.R.drawable.background;

    private MainFragmentArgs() {
    }

    public static MainFragmentArgs fromBundle(Bundle bundle) {
        MainFragmentArgs result = new MainFragmentArgs();
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
        return result;
    }

    public String getMain() {
        return main;
    }

    public int getOptional() {
        return optional;
    }

    public int getReference() {
        return reference;
    }

    public Bundle toBundle() {
        Bundle __outBundle = new Bundle();
        __outBundle.putString("main", main);
        __outBundle.putInt("optional", optional);
        __outBundle.putInt("reference", reference);
        return __outBundle;
    }

    public static class Builder {
        private String main;

        private int optional = -1;

        private int reference = a.b.R.drawable.background;

        public Builder(MainFragmentArgs original) {
            this.main = original.main;
            this.optional = original.optional;
            this.reference = original.reference;
        }

        public Builder(String main) {
            this.main = main;
        }

        MainFragmentArgs build() {
            MainFragmentArgs result = new MainFragmentArgs();
            result.main = this.main;
            result.optional = this.optional;
            result.reference = this.reference;
            return result;
        }

        public Builder setMain(String main) {
            this.main = main;
            return this;
        }

        public Builder setOptional(int optional) {
            this.optional = optional;
            return this;
        }

        public Builder setReference(int reference) {
            this.reference = reference;
            return this;
        }

        public String getMain() {
            return main;
        }

        public int getOptional() {
            return optional;
        }

        public int getReference() {
            return reference;
        }
    }
}