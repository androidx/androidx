#!/usr/bin/python

import csv, subprocess

class StringBuilder(object):
  def __init__(self, item=None):
    self.items = []
    if item is not None:
      self.add(item)

  def add(self, item):
    self.items.append(str(item))
    return self

  def __str__(self):
    return "".join(self.items)

target_map = """android-slices-builders,androidx.slice_slice-builders
android-slices-core,androidx.slice_slice-core
android-slices-view,androidx.slice_slice-view
android-support-animatedvectordrawable,androidx.vectordrawable_vectordrawable-animated
android-support-annotations,androidx.annotation_annotation
android-support-asynclayoutinflater,androidx.asynclayoutinflater_asynclayoutinflater
android-support-car,androidx.car_car
android-support-collections,androidx.collection_collection
android-support-compat,androidx.core_core
android-support-constraint-layout,androidx-constraintlayout_constraintlayout
android-support-constraint-layout-solver,androidx-constraintlayout_constraintlayout-solver
android-support-contentpaging,androidx.contentpager_contentpager
android-support-coordinatorlayout,androidx.coordinatorlayout_coordinatorlayout
android-support-core-ui,androidx.legacy_legacy-support-core-ui
android-support-core-utils,androidx.legacy_legacy-support-core-utils
android-support-cursoradapter,androidx.cursoradapter_cursoradapter
android-support-customtabs,androidx.browser_browser
android-support-customview,androidx.customview_customview
android-support-documentfile,androidx.documentfile_documentfile
android-support-drawerlayout,androidx.drawerlayout_drawerlayout
android-support-dynamic-animation,androidx.dynamicanimation_dynamicanimation
android-support-emoji,androidx.emoji_emoji
android-support-emoji-appcompat,androidx.emoji_emoji-appcompat
android-support-emoji-bundled,androidx.emoji_emoji-bundled
android-support-exifinterface,androidx.exifinterface_exifinterface
android-support-fragment,androidx.fragment_fragment
android-support-heifwriter,androidx.heifwriter_heifwriter
android-support-interpolator,androidx.interpolator_interpolator
android-support-loader,androidx.loader_loader
android-support-localbroadcastmanager,androidx.localbroadcastmanager_localbroadcastmanager
android-support-media-compat,androidx.media_media
android-support-percent,androidx.percentlayout_percentlayout
android-support-print,androidx.print_print
android-support-recommendation,androidx.recommendation_recommendation
android-support-recyclerview-selection,androidx.recyclerview_recyclerview-selection
android-support-slidingpanelayout,androidx.slidingpanelayout_slidingpanelayout
android-support-swiperefreshlayout,androidx.swiperefreshlayout_swiperefreshlayout
android-support-textclassifier,androidx.textclassifier_textclassifier
android-support-transition,androidx.transition_transition
android-support-tv-provider,androidx.tvprovider_tvprovider
android-support-v13,androidx.legacy_legacy-support-v13
android-support-v14-preference,androidx.legacy_legacy-preference-v14
android-support-v17-leanback,androidx.leanback_leanback
android-support-v17-preference-leanback,androidx.leanback_leanback-preference
android-support-v4,androidx.legacy_legacy-support-v4
android-support-v7-appcompat,androidx.appcompat_appcompat
android-support-v7-cardview,androidx.cardview_cardview
android-support-v7-gridlayout,androidx.gridlayout_gridlayout
android-support-v7-mediarouter,androidx.mediarouter_mediarouter
android-support-v7-palette,androidx.palette_palette
android-support-v7-preference,androidx.preference_preference
android-support-v7-recyclerview,androidx.recyclerview_recyclerview
android-support-vectordrawable,androidx.vectordrawable_vectordrawable
android-support-viewpager,androidx.viewpager_viewpager
android-support-wear,androidx.wear_wear
android-support-webkit,androidx.webkit_webkit
android-arch-core-common,androidx.arch.core_core-common
android-arch-core-runtime,androidx.arch.core_core-runtime
android-arch-lifecycle-common,androidx.lifecycle_lifecycle-common
android-arch-lifecycle-common-java8,androidx.lifecycle_lifecycle-common-java8
android-arch-lifecycle-extensions,androidx.lifecycle_lifecycle-extensions
android-arch-lifecycle-livedata,androidx.lifecycle_lifecycle-livedata
android-arch-lifecycle-livedata-core,androidx.lifecycle_lifecycle-livedata-core
android-arch-lifecycle-runtime,androidx.lifecycle_lifecycle-runtime
android-arch-lifecycle-viewmodel,androidx.lifecycle_lifecycle-viewmodel
android-arch-paging-common,androidx.paging_paging-common
android-arch-paging-runtime,androidx.paging_paging-runtime
android-arch-persistence-db,androidx.sqlite_sqlite
android-arch-persistence-db-framework,androidx.sqlite_sqlite-framework
android-arch-room-common,androidx.room_room-common
android-arch-room-migration,androidx.room_room-migration
android-arch-room-runtime,androidx.room_room-runtime
android-arch-room-testing,androidx.room_room-testing
android-support-design,com.google.android.material_material
$(ANDROID_SUPPORT_DESIGN_TARGETS),com.google.android.material_material"""

reader = csv.reader(target_map.split('\n'), delimiter=',')

rewriterTextBuilder = StringBuilder()
for row in reader:
  rewriterTextBuilder.add("s|").add(row[0]).add("|").add(row[1]).add("|g\n")
scriptPath = "/tmp/jetifier-make-sed-script.txt"
print("Writing " + scriptPath)
with open(scriptPath, 'w') as scriptFile:
  scriptFile.write(str(rewriterTextBuilder))

rewriteCommand = "time find . -name out -prune -o -name .git -prune -o -name .repo -prune -o -iregex '.*\.mk\|.*\.bp' -print | xargs -n 1 --no-run-if-empty -P 64 sed -i -f /tmp/jetifier-make-sed-script.txt"

print("""
Will run command:

""" + rewriteCommand + """

""")
response = raw_input("Ok? [y/n]")
if response == "y":
  subprocess.check_output(rewriteCommand, shell=True)

