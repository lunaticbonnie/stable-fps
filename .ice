GRADLEW :: "./current/gradlew -p current"

clean:
  python change_version.py clean
change-version:
  python change_version.py $$ARGS
run:
  $$GRADLEW runClient
build:
  $$GRADLEW build