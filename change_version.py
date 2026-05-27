from dataclasses import dataclass
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

@dataclass
class PathInfo:
  path: str
  name: str
  version: str
  @staticmethod
  def from_dir_path(dir_path: str, version = ""):
    return PathInfo(dir_path, "", version)
  def plus(self, name: str):
    return PathInfo(f"{self.path}/{name}", name, self.version)
  def plus_versioned(self, name: str):
    file_path = f"{self.path}/{name}"
    file_name = file_path.rsplit("/", 1)[-1]
    match = re.search(r"-(\d+(?:\.\d+)+)(\.[^.]+)?$", file_name)
    file_version = ""
    if match != None:
      file_version = match.group(1)
      file_name = (file_name[:match.start(0)] + file_name[match.start(2):])
    return PathInfo(file_path, file_name, file_version)

def apply_overrides(src: PathInfo, dest: PathInfo):
  if os.path.isdir(src.path):
    # make directory
    try:
      os.mkdir(dest.path)
    except:
      pass
    # recurse
    path_infos = [src.plus_versioned(name) for name in os.listdir(src.path)]
    for info in sorted(path_infos, key=lambda info: [info.name, info.version]):
      print(f"+ {info.path}")
      apply_overrides(info, dest.plus(info.name))
  else:
    # apply file override
    src_file = open(src.path, "r")
    print(src, dest)
    if src.version == "" or dest.version >= src.version:
      if src.name.endswith(".csv"):
        dest.path = dest.path[:-len(".csv")]
        content = ""
        with open(dest.path, "r", encoding="utf8") as dest_file:
          content = dest_file.read()
        with open(src.path, "r") as src_file:
          for line in src_file.readlines():
            if not line.strip(): continue
            left, right = line.split(";", 1)
            left = left.strip()
            right = right.strip()
            print(f"  '{left}' -> '{right}'")
            content = re.sub(left, right, content, count=1)
        with open(dest.path, "w") as dest_file:
          dest_file.write(content)
      elif src.name.endswith(".remove"):
        dest.path = dest.path[:-len(".remove")]
        try:
          os.remove(dest.path)
        except:
          pass
      else:
        with open(dest.path, "w+") as dest_file:
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
    apply_overrides(PathInfo.from_dir_path("overrides"), PathInfo.from_dir_path("current", target_version))
