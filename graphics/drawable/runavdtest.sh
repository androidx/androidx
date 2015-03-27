. ../../../../build/envsetup.sh
mmm -j20 . && mmm -j20 ./testanimated/ && \
adb install -r $OUT/data/app/AndroidAnimatedVectorDrawableTests/AndroidAnimatedVectorDrawableTests.apk && \
adb shell am start -n android.support.test.vectordrawable/android.support.test.vectordrawable.TestAVDActivity

