from dataclasses import dataclass
import os
import re
import sys
import time
from typing import cast
from zipfile import ZipFile

env = {k.lower(): v for k, v in os.environ.items()}
def assertf(condition: bool, string: str):
  if not condition:
    print(f"\033[31m{string}\033[0m")
    exit(1)

def clean_path(path: str, expect_exists: bool):
  if len(path) <= 4: raise ValueError(f"Suspicious path: '{path}'")
  if os.path.isdir(path):
    for path_name in os.listdir(path):
      clean_path(f"{path}/{path_name}", False)
    os.rmdir(path)
  elif expect_exists:
    os.remove(path)
  elif os.path.exists(path):
    os.remove(path)
def clean_template():
  clean_path("current/.github", False)
  clean_path("current/README.md", False)

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
  def get_override_order(self):
    is_dir = os.path.isdir(self.path)
    name = self.name
    early_type_order = 1
    type_order = 0
    if self.name.endswith(".renamefrom"):
      name = name[:-len(".renamefrom")]
      early_type_order = 0
    elif self.name.endswith(".csv"):
      name = name[:-len(".csv")]
      type_order = 1
    elif self.name.endswith(".renameto"):
      name = name[:-len(".renameto")]
      type_order = 2
    elif self.name.endswith(".remove"):
      name = name[:-len(".remove")]
      type_order = 3
    elif self.name.endswith(".softremove"):
      name = name[:-len(".softremove")]
      type_order = 4
    version = parse_version(self.version)
    return [is_dir, early_type_order, name, type_order, version.modloader, version.comparison == "+", version.numbers, version.continued]

@dataclass
class Version:
  modloader: str
  comparison: str
  numbers: list[int]
  continued: str
def split_version_string(file_name: str) -> tuple[str, str]:
  match = re.search(r"(.+?)((?:-fabric|-forge|-neoforge)?(?:[+-]\d+(?:\.\d+)+(?:-\d+(?:\.\d+)+)?)?)(\.[^.]+)?$", file_name)
  file_version = ""
  if match != None and match.group(2):
    file_version = match.group(2)
    file_name = match.group(1) + (match.group(3) or "")
  return file_name, file_version
def parse_version(file_version: str) -> Version:
  # parse modloader
  match = re.match("-?(fabric|forge|neoforge)", file_version)
  modloader = ""
  if match != None:
    modloader = match.group(1)
    file_version = file_version[match.end(1):]
  # parse comparison
  comparison = ""
  if file_version.startswith("-") or file_version.startswith("+"):
    comparison = file_version[0]
    file_version = file_version[1:]
  # parse numbers
  split = file_version.split("-", 1)
  numbers = [int(x) for x in split[0].split(".")] if split[0] else []
  return Version(modloader, comparison, numbers, split[1] if len(split) > 1 else "")
def version_matches(src_version_string: str, dest_version_string: str) -> bool:
  if src_version_string == "": return True
  dest_ver = parse_version(dest_version_string)
  from_ver = parse_version(src_version_string)
  if from_ver.modloader != "" and from_ver.modloader != dest_ver.modloader: return False
  if from_ver.comparison == "-":
    if dest_ver.numbers < from_ver.numbers: return False
    if from_ver.continued == "":
      return dest_ver.numbers >= from_ver.numbers
    else:
      to_ver = parse_version(from_ver.continued)
      return dest_ver.numbers >= from_ver.numbers and dest_ver.numbers <= to_ver.numbers
  elif from_ver.comparison == "":
    return True
  else:
    raise ValueError(f"Invalid src_version: '{src_version_string}'")

late_removes: list[str] = []
def replace_variables(string: str) -> str:
  while (match := re.search(r"\$[A-Za-z0-9_]+", string)) != None:
    variable_name = match.group(0)[1:]
    try:
      value = env[variable_name]
    except KeyError:
      print(f"  '{left}' -> \033[31m${variable_name}\033[0m")
      exit(1)
    string = string[:match.start(0)] + value + string[match.end(0):]
  return string
def apply_overrides(src: PathInfo, dest: PathInfo):
  if not version_matches(src.version, dest.version):
    return
  print(f"+ {src.path}")
  if os.path.isdir(src.path):
    # make directory
    try:
      os.mkdir(dest.path)
    except:
      pass
    # recurse
    path_infos = [src.plus_versioned(name) for name in os.listdir(src.path)]
    for info in sorted(path_infos, key=lambda info: info.get_override_order()):
      apply_overrides(info, dest.plus(info.name))
  else:
    # apply file override
    src_file = open(src.path, "r")
    if src.name.endswith(".csv"):
      dest.path = dest.path[:-len(".csv")]
      content = ""
      with open(dest.path, "r", encoding="utf8") as dest_file:
        content = dest_file.read() # NOTE: Python automatically converts "\r\n" to "\n"
      splitter = ";"
      with open(src.path, "r") as src_file:
        for line in src_file.readlines():
          if not line.strip(): continue
          split = line.split(splitter, 1)
          if len(split) == 1:
            splitter = line.strip()
            continue
          else:
            assertf(len(split) == 2, f"Invalid replace(\"{splitter}\"): '{line.strip()}'")
          left, right = split
          left = left.strip()
          right = right.strip()
          right = replace_variables(right)
          right = right.replace("\\n", "\n")
          print(f"  '{left}' -> '{right}'")
          content = re.sub(left, lambda _match: right, content, 0, re.MULTILINE)
      with open(dest.path, "w") as dest_file:
        dest_file.write(content) # NOTE: Python automatically converts "\n" back to "\r\n"
    elif src.name.endswith(".renamefrom"):
      to_path = dest.path[:-len(".renamefrom")]
      dest_dir = to_path.rsplit("/", 1)[0]
      dest_name = ""
      with open(src.path, "r") as src_file:
        dest_name = src_file.read().strip()
      from_path = f"{dest_dir}/{dest_name}"
      print(f"  '{from_path}' -> '{to_path}'")
      clean_path(to_path, False)
      os.rename(from_path, to_path)
      time.sleep(1e-3) # NOTE: fix race condition with file system
    elif src.name.endswith(".renameto"):
      from_path = dest.path[:-len(".renameto")]
      dest_dir = from_path.rsplit("/", 1)[0]
      dest_name = ""
      with open(src.path, "r") as src_file:
        dest_name = replace_variables(src_file.read().strip())
      to_path = f"{dest_dir}/{dest_name}"
      print(f"  '{from_path}' -> '{to_path}'")
      clean_path(to_path, False)
      os.rename(from_path, to_path)
      time.sleep(1e-3) # NOTE: fix race condition with file system
    elif src.name.endswith(".remove"):
      dest.path = dest.path[:-len(".remove")]
      clean_path(dest.path, True)
    elif src.name.endswith(".softremove"):
      dest.path = dest.path[:-len(".softremove")]
      clean_path(dest.path, False)
    elif src.name.endswith(".lateremove"):
      dest.path = dest.path[:-len(".lateremove")]
      late_removes.append(dest.path)
    else:
      src_file.close()
      src_file = open(src.path, "rb")
      with open(dest.path, "wb+") as dest_file:
        dest_file.write(src_file.read())
    src_file.close()

if __name__ == "__main__":
  # print versions
  args = sys.argv[1:]
  if len(args) != 1:
    versions_map = cast(dict[str, list[str]], dict())
    if os.path.isdir("templates"):
      for file_name in os.listdir("templates"):
        if file_name.endswith(".zip"): file_name = file_name[:-len(".zip")]
        modloader, version, *_ = file_name.split("-")
        version_list = versions_map.get(modloader, [])
        version_list.append(version)
        versions_map[modloader] = version_list
      for modloader, version_list in versions_map.items():
        version_list = sorted(version_list, key=lambda x: parse_version(x).numbers)
        print(f"- {modloader}: {" ".join(version_list)}")
    exit()
  # parse args
  target_version = args[0]
  # find matching template
  found_templates = []
  for name in os.listdir("templates"):
    if re.match(rf"{target_version}(?:-|.zip)", name) != None:
      found_templates.append(name)
  assertf(len(found_templates) == 1, f"found_templates: {found_templates}")
  template_name = found_templates[0]
  template_path = f"templates/{template_name}"
  template_name = template_name.rsplit(".", 1)[0]
  # change to the specified version
  clean_path("current", False)
  if target_version != "clean":
    with ZipFile(template_path) as z:
      z.extractall("current")
    if target_version.startswith("forge"):
      _, minecraft_version, forge_version, *_ = template_name.split("-")
      env["minecraft_version"] = minecraft_version
      env["minecraft_version_range"] = "[0)"
      env["forge_version"] = forge_version
      env["forge_version_range"] = "[0)"
      env["loader_version_range"] = "[0)"
      with open("current/gradle.properties", "r") as f:
        for line in f.readlines():
          for key in ["minecraft_version", "forge_version", "mapping_version", "loader_version_range", "minecraft_version_range", "forge_version_range"]:
            if line.startswith(f"{key}="):
              env[key.lower()] = line[len(key)+1:-1]
    clean_template()
    for src_path in ["modloader_overrides", "mod_overrides"]:
      apply_overrides(PathInfo.from_dir_path(src_path), PathInfo.from_dir_path("current", target_version))
    for path in late_removes:
      clean_path(path, True)
