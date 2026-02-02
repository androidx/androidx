set -e

if [ $# -ne 1 ]; then
  echo "Usage: $0 PACKAGE"
  echo "  PACKAGE - Package name of the app"
  exit 1
fi

PACKAGE=$1

LONG_ABI=`adb shell getprop ro.product.cpu.abi`
if [[ $LONG_ABI == "arm64"* ]]; then
  ABI="arm64"
else
  ABI="arm"
fi

echo "Checking for unverified classes in package = $PACKAGE, ABI $ABI"

echo -e "\nCurrent compilation: (if not \"verify\" this will be slow)"
CURRENT_COMPILATION=`adb shell dumpsys package $PACKAGE | grep "$ABI:"`
echo "    $CURRENT_COMPILATION"

APPDIR=`adb shell pm path $PACKAGE | sed 's/^package:\(.*\)\/base\.apk$/\1/'`

DUMP_CMD="adb shell oatdump --oat-file=$APPDIR/oat/$ABI/base.odex --output=/data/local/tmp/oatdump.txt"
echo -e "\nDumping oat file with\n$DUMP_CMD"

`time $DUMP_CMD`

OUTFILE=$PACKAGE-oat.txt

echo -e "\nPulling oat file to $OUTFILE"
adb pull /data/local/tmp/oatdump.txt $OUTFILE

echo -e "\nPrinting unverified classes:"
# print unverified classes, skipping test libraries that aren't expected to ship in release builds
cat $OUTFILE | grep "will be verified" \
  | grep -v Ljunit \
  | grep -v Lorg\/hamcrest \
  | grep -v Lcom\/google\/common\/truth \
