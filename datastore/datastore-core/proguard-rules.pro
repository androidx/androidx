# make sure r8 can remove the fake counter that is only in the code to handle the
# robolectric tests.
-assumenosideeffects class androidx.datastore.core.SharedCounter$Factory {
    private boolean isDalvik() return true;
}
