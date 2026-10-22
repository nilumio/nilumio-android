#!/usr/bin/env python3
"""List and count nilumio's changes against the upstream branch point.

Usage (from the repository root):
    python3 docs/tools/nilumio_changes.py list   [UPSTREAM_REF]   # Markdown table of changed files with dates
    python3 docs/tools/nilumio_changes.py stats  [UPSTREAM_REF]   # Markdown summary by category

UPSTREAM_REF defaults to upstream/main. The comparison base is
`git merge-base HEAD UPSTREAM_REF`, so after merging a new upstream release
only nilumio's own changes are counted.
"""
import subprocess
import sys
from collections import defaultdict


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def category(path):
    p = path.lower()
    if "/values-" in p and p.endswith(".xml") or p.endswith(".strings") or p.startswith("translations/") or "/values/strings" in p:
        return "UI texts and translations"
    if p.endswith((".png", ".webp", ".jpg", ".jpeg", ".svg", ".mp4", ".wav", ".ttf", ".otf")) or ".xcassets/" in p or "/mipmap-" in p:
        return "images and media"
    if p.endswith((".md", ".txt")) or p.startswith(("docs/", "store/")) and p.endswith((".html", ".md")):
        return "documentation and store texts"
    if p.endswith((".html", ".json")) and p.startswith(("website/", "store/website/")):
        return "website"
    if "/test/" in p or p.startswith(("swiftfintests", "tests/")):
        return "tests"
    if p.endswith((".kt", ".java", ".swift", ".py", ".sh", ".mjs")):
        return "code"
    return "build and configuration"


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "stats"
    ref = sys.argv[2] if len(sys.argv) > 2 else "upstream/main"
    base = git("merge-base", "HEAD", ref).strip()
    status = {}
    for line in git("diff", "--name-status", "-M", base, "HEAD").splitlines():
        parts = line.split("\t")
        status[parts[-1]] = (parts[0][0], parts[1] if parts[0][0] == "R" else None)
    numstat = {}
    for line in git("diff", "--numstat", "-M", base, "HEAD").splitlines():
        a, d, path = line.split("\t", 2)
        if " => " in path:  # rename shown as a/{x => y}/b
            path = [p for p in status if status[p][0] == "R" and path.split(" => ")[-1].rstrip("}") in p][0]
        numstat[path] = (None if a == "-" else int(a), None if d == "-" else int(d))

    if mode == "list":
        print(f"Base: `{base}`\n")
        print("| Status | File | Changed |")
        print("|---|---|---|")
        names = {"A": "added", "M": "modified", "D": "removed", "R": "renamed"}
        for path in sorted(status):
            kind, old = status[path]
            dates = git("log", "--format=%ad", "--date=short", f"{base}..HEAD", "--", path, *( [old] if old else [] )).split()
            when = dates[-1] if dates and dates[-1] == dates[0] else (f"{dates[-1]} to {dates[0]}" if dates else "")
            label = f"`{old}` to `{path}`" if old else f"`{path}`"
            print(f"| {names.get(kind, kind)} | {label} | {when} |")
        return

    own = defaultdict(lambda: [0, 0, 0])        # files, lines, binary files
    upstream = defaultdict(lambda: [0, 0, 0, 0])  # files, added, removed, binary files
    for path, (kind, _) in status.items():
        a, d = numstat.get(path, (None, None))
        cat = category(path)
        if kind == "A":
            own[cat][0] += 1
            if a is None:
                own[cat][2] += 1
            else:
                own[cat][1] += a
        else:
            upstream[cat][0] += 1
            if a is None:
                upstream[cat][3] += 1
            else:
                upstream[cat][1] += a
                upstream[cat][2] += d
    cats = ["code", "tests", "build and configuration", "UI texts and translations", "images and media", "website", "documentation and store texts"]
    print(f"Base: `{base}`\n")
    print("| Category | New own files | Lines in new files | Upstream files touched | Lines added there | Lines removed there |")
    print("|---|---:|---:|---:|---:|---:|")
    tot = [0] * 5
    for c in cats:
        o, u = own[c], upstream[c]
        if not any(o) and not any(u):
            continue
        row = [o[0], o[1], u[0], u[1], u[2]]
        tot = [x + y for x, y in zip(tot, row)]
        extra = []
        if o[2]:
            extra.append(f"{o[2]} binary new")
        if u[3]:
            extra.append(f"{u[3]} binary changed")
        name = c + (f" ({', '.join(extra)})" if extra else "")
        print(f"| {name} | " + " | ".join(str(x) for x in row) + " |")
    print("| **total** | " + " | ".join(f"**{x}**" for x in tot) + " |")


if __name__ == "__main__":
    main()
