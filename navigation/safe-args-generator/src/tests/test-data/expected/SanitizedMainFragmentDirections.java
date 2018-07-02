package a.b;

import android.os.Bundle;
import androidx.navigation.NavDirections;
import java.lang.String;

public class SanitizedMainFragmentDirections {
    public static PreviousAction previousAction(String arg1, String arg2) {
        return new PreviousAction(arg1, arg2);
    }

    public static NextAction nextAction(String mainArg) {
        return new NextAction(mainArg);
    }

    public static class PreviousAction implements NavDirections {
        private String arg1;

        private String arg2;

        public PreviousAction(String arg1, String arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public PreviousAction setArg1(String arg1) {
            this.arg1 = arg1;
            return this;
        }

        public PreviousAction setArg2(String arg2) {
            this.arg2 = arg2;
            return this;
        }

        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("arg_1", this.arg1);
            __outBundle.putString("arg.2", this.arg2);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.previous_action;
        }
    }

    public static class NextAction implements NavDirections {
        private String mainArg;

        private String optionalArg = "bla";

        public NextAction(String mainArg) {
            this.mainArg = mainArg;
        }

        public NextAction setMainArg(String mainArg) {
            this.mainArg = mainArg;
            return this;
        }

        public NextAction setOptionalArg(String optionalArg) {
            this.optionalArg = optionalArg;
            return this;
        }

        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("main_arg", this.mainArg);
            __outBundle.putString("optional.arg", this.optionalArg);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.next_action;
        }
    }
}