import collections, json, pathlib, sys

data = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8-sig"))
events = data["recording"]["events"]
top = collections.Counter()
project = collections.Counter()
for event in events:
    stack = event["values"].get("stackTrace")
    if not stack:
        continue
    frames = stack["frames"]
    names = [frame["method"]["type"]["name"] + "." + frame["method"]["name"] for frame in frames]
    if names:
        top[names[0]] += 1
    project.update(set(name for name in names if name.startswith("dev/searchforge")))
result = {
    "sampleCount": len(events),
    "topFrames": top.most_common(20),
    "projectFrames": project.most_common(20),
    "interpretation": "Sampled CPU stacks across whole process, including build/query/concurrency/storage; inclusive frame counts overlap; not wall-clock percentages.",
}
pathlib.Path(sys.argv[2]).write_text(json.dumps(result, indent=2) + "\n")
