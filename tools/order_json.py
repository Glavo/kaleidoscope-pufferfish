#!/usr/bin/env python3
# Copyright (c) 2026 Glavo
# SPDX-License-Identifier: MPL-2.0

"""Order resource and template object fields without rewriting values or arrays."""

import argparse
from dataclasses import dataclass, field
import json
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
TOKEN = re.compile(r'"(?:\\.|[^"\\])*"|\$\{[^{}]+\}|[{}\[\],:]|true|false|null|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?')
MODEL = "parent render_type ambientocclusion gui_light textures display elements overrides".split()
RECIPE = ("neoforge:conditions type category group template base addition pattern key ingredient ingredients input tool "
          "soup_base carrier time base_time cook_tick cookingtime experience cut_count model_id result output").split()
ADVANCEMENT = "neoforge:conditions parent display criteria requirements rewards sends_telemetry".split()
FACES = "down up north south west east".split()
DISPLAY = "thirdperson_righthand thirdperson_lefthand firstperson_righthand firstperson_lefthand head gui ground fixed".split()


@dataclass
class Node:
    start: int
    end: int
    kind: str
    children: list = field(default_factory=list)


class Document:
    def __init__(self, text, path):
        self.text = text
        self.path = path
        self.tokens = list(TOKEN.finditer(text))
        last = 0
        for token in self.tokens:
            if text[last:token.start()].strip():
                raise ValueError(f"Unexpected text at {path}:{last}")
            last = token.end()
        if text[last:].strip():
            raise ValueError(f"Unexpected trailing text in {path}")
        self.index = 0
        self.root = self.parse()
        if self.index != len(self.tokens):
            raise ValueError(f"Multiple root values in {path}")

    def consume(self, expected=None):
        token = self.tokens[self.index]
        self.index += 1
        if expected is not None and token.group() != expected:
            raise ValueError(f"Expected {expected} in {self.path} at {token.start()}")
        return token

    def parse(self):
        token = self.consume()
        kind = token.group()
        node = Node(token.start(), token.end(), kind)
        if kind not in ("{", "["):
            return node
        closing = "}" if kind == "{" else "]"
        keys = set()
        while self.tokens[self.index].group() != closing:
            if node.children:
                self.consume(",")
            if kind == "{":
                key = self.consume()
                name = json.loads(key.group())
                if not isinstance(name, str) or name in keys:
                    raise ValueError(f"Invalid or duplicate key {name!r} in {self.path}")
                keys.add(name)
                self.consume(":")
                node.children.append((name, key.start(), self.parse()))
            else:
                node.children.append(self.parse())
        node.end = self.consume(closing).end()
        return node

    def order(self, keys, trail):
        parent = trail[-1] if trail else None
        path = self.path.as_posix()
        if "/lang/" in path:
            return []
        if parent == "faces":
            return FACES
        if parent == "display":
            return "icon title description background frame show_toast announce_to_chat hidden".split() if "icon" in keys else DISPLAY
        if parent == "textures":
            return "particle all top bottom side front back layer0 layer1 layer2".split()
        if parent in ("key", "criteria", "variants", "components", "spawn_overrides"):
            return []
        if not trail:
            if "/advancement/" in path or path.endswith("/advancement.json"):
                return ADVANCEMENT
            if "/recipe/" in path or path.endswith(("/recipe.json", "/chopping.json", "/cutting.json", "/pressing.json")):
                return RECIPE
            if "/models/" in path or "textures" in keys or "parent" in keys:
                return MODEL
            if path.endswith(".mixins.json"):
                return "required minVersion package compatibilityLevel plugin refmap mixins client server injectors overwrites".split()
            if "/worldgen/structure/" in path:
                return "type biomes step spawn_overrides terrain_adaptation ancient".split()
            if "/worldgen/structure_set/" in path:
                return "structures placement".split()
            if "/damage_type/" in path:
                return "message_id scaling exhaustion effects death_message_type".split()
        if "from" in keys and "to" in keys:
            return "name from to rotation shade light_emission faces".split()
        if parent in FACES:
            return "uv texture cullface rotation tintindex".split()
        if parent == "rotation" and "axis" in keys:
            return "origin axis angle rescale".split()
        if parent in DISPLAY:
            return "rotation translation scale".split()
        if "pools" in keys:
            return "neoforge:conditions type random_sequence pools functions".split()
        if "rolls" in keys:
            return "conditions rolls bonus_rolls entries functions".split()
        if parent in ("entries", "children"):
            return "type name weight quality conditions children functions".split()
        if parent == "structures":
            return "structure weight".split()
        if parent == "placement":
            return "type spacing separation spread_type salt frequency frequency_reduction_method locate_offset exclusion_zone".split()
        if "replace" in keys or "values" in keys:
            return "replace values".split()
        if "predicate" in keys and "model" in keys:
            return "predicate model".split()
        return ("neoforge:conditions type condition function trigger id name item tag items modid action color "
                "count chance weight quality min max minCount maxCount components conditions functions").split()

    def render(self, node, trail=()):
        if not node.children:
            return self.text[node.start:node.end]
        if node.kind == "[":
            result = self.text[node.start:node.children[0].start]
            for index, child in enumerate(node.children):
                result += self.render(child, trail)
                end = node.children[index + 1].start if index + 1 < len(node.children) else node.end
                result += self.text[child.end:end]
            return result
        preferred = self.order({entry[0] for entry in node.children}, trail)
        rank = {key: index for index, key in enumerate(preferred)}
        ordered = sorted(node.children, key=lambda entry: (rank.get(entry[0], len(rank)), entry[0]))
        result = self.text[node.start:node.children[0][1]]
        for index, (key, start, child) in enumerate(ordered):
            result += self.text[start:child.start] + self.render(child, trail + (key,))
            original = node.children[index][2]
            end = node.children[index + 1][1] if index + 1 < len(ordered) else node.end
            separator = self.text[original.end:end]
            if "/lang/" in self.path.as_posix() and index + 1 < len(ordered):
                separator = ",\n  "
            result += separator
        return result

    def fingerprint(self, node=None):
        node = self.root if node is None else node
        if node.kind == "{":
            return {key: self.fingerprint(child) for key, _, child in node.children}
        if node.kind == "[":
            return [self.fingerprint(child) for child in node.children]
        return self.text[node.start:node.end]

    def formatted(self):
        result = self.text[:self.root.start] + self.render(self.root) + self.text[self.root.end:]
        if self.fingerprint() != Document(result, self.path).fingerprint():
            raise ValueError(f"Reordering changed values in {self.path}")
        return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Report unordered files without writing them")
    args = parser.parse_args()
    changed = 0
    paths = sorted(path for directory in (ROOT / "src/main/resources", ROOT / "src/main/templates")
                   for path in directory.rglob("*.json"))
    for path in paths:
        original = path.read_text(encoding="utf-8")
        result = Document(original, path).formatted()
        if result != original:
            changed += 1
            print(path.relative_to(ROOT).as_posix())
            if not args.check:
                path.write_text(result, encoding="utf-8")
    print(f"{len(paths)} files checked; {changed} {'unordered' if args.check else 'updated'}.")
    return int(args.check and changed > 0)


if __name__ == "__main__":
    raise SystemExit(main())
