# temporary script to make flatfoot repo, next to the support repo
cd lifecycle && ./gradlew uploadArchives --info
cd ../room && ./gradlew uploadArchives --info
