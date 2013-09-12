package android.support.multidex;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

public class MultiDexTestRunner extends InstrumentationTestRunner {

    @Override
    public void onCreate(Bundle arguments) {
        MultiDex.install(getTargetContext());
        super.onCreate(arguments);
    }

}
