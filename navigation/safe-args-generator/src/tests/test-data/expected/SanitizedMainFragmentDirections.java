package a.b;

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class SanitizedMainFragmentDirections {
    @NonNull
    public static PreviousAction previousAction(@NonNull String arg1, @NonNull String arg2) {
        return new PreviousAction(arg1, arg2);
    }

    @NonNull
    public static NextAction nextAction(@NonNull String mainArg) {
        return new NextAction(mainArg);
    }

    public static class PreviousAction implements NavDirections {
        @NonNull
        private String arg1;

        @NonNull
        private String arg2;

        public PreviousAction(@NonNull String arg1, @NonNull String arg2) {
            this.arg1 = arg1;
            if (this.arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg_1\" is marked as non-null but was passed a null value.");
            }
            this.arg2 = arg2;
            if (this.arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg.2\" is marked as non-null but was passed a null value.");
            }
        }

        @NonNull
        public PreviousAction setArg1(@NonNull String arg1) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg_1\" is marked as non-null but was passed a null value.");
            }
            this.arg1 = arg1;
            return this;
        }

        @NonNull
        public PreviousAction setArg2(@NonNull String arg2) {
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg.2\" is marked as non-null but was passed a null value.");
            }
            this.arg2 = arg2;
            return this;
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("arg_1", this.arg1);
            __outBundle.putString("arg.2", this.arg2);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.previous_action;
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
            PreviousAction that = (PreviousAction) object;
            if (arg1 != null ? !arg1.equals(that.arg1) : that.arg1 != null) {
                return false;
            }
            if (arg2 != null ? !arg2.equals(that.arg2) : that.arg2 != null) {
                return false;
            }
            if (getActionId() != that.getActionId()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (arg1 != null ? arg1.hashCode() : 0);
            result = 31 * result + (arg2 != null ? arg2.hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }
    }

    public static class NextAction implements NavDirections {
        @NonNull
        private String mainArg;

        @NonNull
        private String optionalArg = "bla";

        public NextAction(@NonNull String mainArg) {
            this.mainArg = mainArg;
            if (this.mainArg == null) {
                throw new IllegalArgumentException("Argument \"main_arg\" is marked as non-null but was passed a null value.");
            }
        }

        @NonNull
        public NextAction setMainArg(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"main_arg\" is marked as non-null but was passed a null value.");
            }
            this.mainArg = mainArg;
            return this;
        }

        @NonNull
        public NextAction setOptionalArg(@NonNull String optionalArg) {
            if (optionalArg == null) {
                throw new IllegalArgumentException("Argument \"optional.arg\" is marked as non-null but was passed a null value.");
            }
            this.optionalArg = optionalArg;
            return this;
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("main_arg", this.mainArg);
            __outBundle.putString("optional.arg", this.optionalArg);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.next_action;
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
            NextAction that = (NextAction) object;
            if (mainArg != null ? !mainArg.equals(that.mainArg) : that.mainArg != null) {
                return false;
            }
            if (optionalArg != null ? !optionalArg.equals(that.optionalArg) : that.optionalArg != null) {
                return false;
            }
            if (getActionId() != that.getActionId()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (mainArg != null ? mainArg.hashCode() : 0);
            result = 31 * result + (optionalArg != null ? optionalArg.hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }
    }
}