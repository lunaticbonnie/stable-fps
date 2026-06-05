GRADLEW :: "./current/gradlew -p current"

clean:
  $$GRADLEW --stop
  python change_version.py clean
change-version:
  python change_version.py $$ARGS
run:
  $$GRADLEW runClient
build:
  $$GRADLEW build
run-version:
  python change_version.py $$ARGS
  $$GRADLEW runClient
build-version:
  $$GRADLEW --stop
  $$GRADLEW --stop
  python change_version.py $$ARGS
  $$GRADLEW --refresh-dependencies
  $$GRADLEW runClient
  $$GRADLEW build
  cp current/build/libs/$modid-$version.jar "dist/$modid-$version+$$ARGS.jar"