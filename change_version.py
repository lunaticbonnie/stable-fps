import os
import re
import sys
from zipfile import ZipFile

def clean_path(path: str):
  if os.path.isdir(path):
    for path_name in os.listdir(path):
      clean_path(f"{path}/{path_name}")
    os.rmdir(path)
  elif os.path.exists(path):
    os.remove(path)
def clean_template():
  clean_path("current/.github")
  clean_path("current/src/main/java/patrolin/stablefps/mixin/ExampleMixin.java")
  clean_path("current/README.md")
def apply_overrides(src: str, dest: str, target_version: str):
  if os.path.isdir(src):
    try:
      os.mkdir(dest)
    except:
      pass
    path_names = sorted(os.listdir(src))
    for path_name in path_names:
      apply_overrides(f"{src}/{path_name}", f"{dest}/{path_name}", target_version)
  else:
    src_file = open(src, "r")
    print(f"+ {src}")
    if src.endswith(".csv"):
      dest = dest[:-len(".csv")]
      content = ""
      with open(dest, "r", encoding="utf8") as dest_file:
        content = dest_file.read()
      with open(src, "r") as src_file:
        for line in src_file.readlines():
          if not line.strip(): continue
          left, right = line.split(";", 1)
          left = left.strip()
          right = right.strip()
          print(f"  '{left}' -> '{right}'")
          content = re.sub(left, right, content, count=1)
      with open(dest, "w") as dest_file:
        dest_file.write(content)
    else:
      file_version = None
      dir_path, file_name = dest.rsplit("/", 1)
      split = file_name.split("-", 1)
      if len(split) == 2:
        file_version, file_name = split
        dest = f"{dir_path}/{file_name}"
      if file_version == None or target_version >= file_version:
        with open(dest, "w+") as dest_file:
          dest_file.write(src_file.read())
    src_file.close()

if __name__ == "__main__":
  args = sys.argv[1:]
  if len(args) != 1:
    if os.path.isdir("templates"):
      versions = [v.rsplit("-", 1)[1][:-len(".zip")] for v in os.listdir("templates")]
      print("versions: " + " ".join(sorted(versions)))
    exit()
  target_version = args[0]
  clean_path("current")
  if target_version != "clean":
    with ZipFile(f"templates/stable-fps-template-{target_version}.zip") as z:
      z.extractall("current")
    clean_template()
    apply_overrides("overrides", "current", target_version)
