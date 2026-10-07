#!/usr/bin/env python3
from pathlib import Path
import re

def read(name):
    return [x for x in Path("app/src/main/assets", name).read_text(encoding="utf-8").splitlines() if x.strip()]

apps = read("apps.tsv")
actions = read("actions.tsv")
synonyms = read("synonyms.tsv")
responses = read("responses.tsv")

assert len(apps) == 4000, ("apps.tsv must contain exactly 4,000 rows", len(apps))
assert len({x.split("\t")[0] for x in apps}) == len(apps)

for r in apps:
    p = r.split("\t")
    assert len(p) >= 4 and p[1].strip() and p[2].strip() and p[3].strip(), ("bad app row", r)
    aliases = [x.strip() for x in p[3].split("|") if x.strip()]
    assert len(aliases) == len(set(aliases)), ("duplicate app alias", r)

allowed_codes = {"SETTINGS","VOL_UP","VOL_DOWN","MUTE","UNMUTE","MEDIA_NEXT","MEDIA_PREV","MEDIA_PLAY","MEDIA_PAUSE","MEDIA_STOP"}
action_keys = set()
for r in actions:
    p = r.split("\t")
    assert len(p) >= 6 and p[1].strip() and p[2].strip() and p[4].strip(), ("bad action row", r)
    assert p[4] in allowed_codes, ("unknown action code", p[4])
    idx = int(p[3])
    assert 0 <= idx <= 29, ("settings index out of range", idx)
    assert not re.search(r"\s\d+$", p[1])
    assert not re.search(r"\s\d+$", p[2])
    assert not any(re.fullmatch(r"\d+", x.strip()) for x in p[5].split("|") if x.strip())
    key = "\t".join(p[1:])
    assert key not in action_keys, ("duplicate semantic action", r)
    action_keys.add(key)
assert len(actions) >= 700, ("too few meaningful action templates", len(actions))

syn_keys = set()
for r in synonyms:
    p = r.split("\t")
    assert len(p) >= 4 and p[1].strip() and p[2].strip(), ("bad synonym row", r)
    assert not re.search(r"\s\d+$", p[1])
    assert not p[1].endswith(" עכשיו")
    key = "\t".join(p[1:4])
    assert key not in syn_keys, ("duplicate synonym", r)
    syn_keys.add(key)
assert len(synonyms) >= 500, ("too few meaningful synonyms", len(synonyms))

response_keys = set()
response_texts = set()
trigger_keys = set()
for r in responses:
    p = r.split("\t")
    assert len(p) >= 4 and p[1].strip() and p[3].strip(), ("bad response row", r)
    assert not re.search(r"\[\d+\]\s*$", p[1])
    assert not re.search(r"\b(Absolutely|Of course|Definitely|Certainly|Indeed)\.?\s*$", p[2], re.I)
    assert "?" not in p[1] and "؟" not in p[1], ("question in chat response", r)
    key = p[1] + "\t" + p[3]
    assert key not in response_keys, ("duplicate response row", r)
    response_keys.add(key)
    assert p[1] not in response_texts, ("duplicate Hebrew response text", r)
    response_texts.add(p[1])
    trigger_keys.add(p[3])
assert len(responses) == 9000, ("responses.tsv must contain exactly 9,000 rows", len(responses))
assert len(response_texts) == 9000, ("responses must contain 9,000 unique Hebrew responses", len(response_texts))
assert len(trigger_keys) == 200, ("responses must contain exactly 200 daily-chat trigger groups", len(trigger_keys))

required = {"מחשבון","שעון","דרייב","גוגל דרייב","גוגל פליי","כרום","יוטיוב","מצלמה","גלריה","סייר קבצים","Google Drive","Google Play","Chrome","YouTube","Calculator","Clock"}
app_aliases = set()
for r in apps:
    p = r.split("\t")
    app_aliases.add(p[1].strip())
    app_aliases.add(p[2].strip())
    app_aliases.update(x.strip() for x in p[3].split("|") if x.strip())
missing = sorted(required - app_aliases)
assert not missing, ("missing core app aliases", missing)

print("Catalog verification OK:", {
    "apps": len(apps),
    "actions": len(actions),
    "synonyms": len(synonyms),
    "responses": len(responses),
    "response_triggers": len(trigger_keys),
})
