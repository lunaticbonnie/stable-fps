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
  def plus(self, path_name: str):
    return PathInfo(f"{self.path}/{path_name}", path_name, self.version)
  def plus_versioned(self, path_name: str):
    path = f"{self.path}/{path_name}"
    path_name, file_version = split_version_string(path_name)
    return PathInfo(path, path_name, file_version)

@dataclass
class Version:
  modloader: str
  comparison: str
  numbers: list[int]
  continued: str
def split_version_string(file_name: str) -> tuple[str, str]:
  match = re.search(r"(.+?)((?:-fabric|-forge|-neoforge)?[+-]\d+(?:\.\d+)+(?:-\d+(?:\.\d+)+)?)(\.[^.]+)?$", file_name)
  file_version = ""
  if match != None:
    file_version = match.group(2)
    file_name = match.group(1) + (match.group(3) or "")
  return file_name, file_version
def parse_version(file_version: str) -> Version:
  # parse modloader
  match = re.match("-fabric|-forge|-neoforge", file_version)
  modloader = ""
  if match != None:
    modloader = match.group(0)
    file_version = file_version[len(modloader):]
  # parse comparison
  comparison = ""
  if file_version[0] == "-" or file_version[0] == "+":
    comparison = file_version[0]
    file_version = file_version[1:]
  # parse numbers
  split = file_version.split("-", 1)
  numbers = [int(x) for x in split[0].split(".")]
  return Version(modloader, comparison, numbers, split[1] if len(split) > 1 else "")
def version_matches(src_version_string: str, dest_version_string: str) -> bool:
  if src_version_string == "": return True
  dest_ver = parse_version(dest_version_string)
  from_ver = parse_version(src_version_string)
  if from_ver.comparison:
    if from_ver.modloader != "" and from_ver.modloader != dest_ver.modloader: return False
    if dest_ver.numbers < from_ver.numbers: return False
    if from_ver.continued == "":
      return dest_ver.numbers >= from_ver.numbers
    else:
      to_ver = parse_version(from_ver.continued)
      return dest_ver.numbers >= from_ver.numbers and dest_ver.numbers <= to_ver.numbers
  else:
    raise ValueError(f"Invalid src_version: '{src_version_string}'")

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
    if version_matches(src.version, dest.version):
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
      print("versions: " + " ".join(sorted(versions, key=parse_version)))
    exit()
  target_version = args[0]
  clean_path("current")
  if target_version != "clean":
    with ZipFile(f"templates/stable-fps-template-{target_version}.zip") as z:
      z.extractall("current")
    clean_template()
    apply_overrides(PathInfo.from_dir_path("overrides"), PathInfo.from_dir_path("current", target_version))
