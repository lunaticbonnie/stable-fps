GRADLEW :: "./current/gradlew -p current"

stop:
  $$GRADLEW --stop
clean:
  $$GRADLEW --stop
  python change_version.py clean
list:
  python change_version.py
fabric:
  python change_version.py fabric-$$ARGS
  $$GRADLEW genSources
forge:
  python change_version.py forge-$$ARGS
neoforge:
  python change_version.py neoforge-$$ARGS
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
  $$GRADLEW runClient
  $$GRADLEW build
  cp current/build/libs/$modid-$version.jar "dist/$modid-$version+$$ARGS.jar"