. ../../../../build/envsetup.sh
mmm -j20 . && mmm -j20 ./teststatic/ && \
adb install -r $OUT/data/app/AndroidVectorDrawableTests/AndroidVectorDrawableTests.apk && \
adb shell am start -n android.support.test.vectordrawable/android.support.test.vectordrawable.TestActivity

