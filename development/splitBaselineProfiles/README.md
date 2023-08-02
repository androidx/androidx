## A Utility to Split Baseline Profiles into constituent modules

* To build the fat jar `./gradlew shadowJar`.

* Run using `java -jar split-baseline-profiles-all.jar`

* To run the splitter with the `runShadow` task add
[something like](https://imperceptiblethoughts.com/shadow/application-plugin/)

```gradle
// Configuring the runShadow Task
runShadow {
  args 'foo'
}
```
