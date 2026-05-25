clean:
  python change_version.py clean
change-version:
  python change_version.py $$ARGS
run:
  ./current/gradlew -p current runClient