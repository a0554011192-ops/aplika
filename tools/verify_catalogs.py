#!/usr/bin/env python3
from pathlib import Path
def read(name):
    return [x for x in Path("app/src/main/assets",name).read_text(encoding="utf-8").splitlines() if x.strip()]
checks={"apps.tsv":4000,"actions.tsv":4000,"responses.tsv":5000,"synonyms.tsv":6000}
for name,n in checks.items():
    rows=read(name)
    assert len(rows)==n,(name,len(rows),n)
    assert len(set(rows))==n,(name,"duplicate rows")

actions=read("actions.tsv")
allowed_codes={"SETTINGS","VOL_UP","VOL_DOWN","MUTE","UNMUTE","MEDIA_NEXT","MEDIA_PREV","MEDIA_PLAY","MEDIA_PAUSE","MEDIA_STOP"}
for r in actions:
    p=r.split("\t")
    assert len(p)>=6 and p[1].strip() and p[2].strip() and p[4].strip(),("bad action row",r)
    assert p[4] in allowed_codes,("unknown action code",p[4])
    idx=int(p[3])
    assert 0 <= idx <= 29,("settings index out of range",idx,r)

responses=read("responses.tsv")
response_keys=set()
for r in responses:
    p=r.split("\t")
    assert len(p)>=4 and p[1].strip() and p[3].strip(),("bad response row",r)
    k="\t".join(p[1:4])
    response_keys.add(k)
assert len(response_keys) <= len(responses)

synonyms=read("synonyms.tsv")
for r in synonyms:
    p=r.split("\t")
    assert len(p)>=3 and p[1].strip() and p[2].strip(),("bad synonym row",r)
apps=set()
for r in read("apps.tsv"):
    p=r.split("\t")
    if len(p)>=4:
        apps.add(p[1].strip())
        apps.add(p[2].strip())
        apps.update(x.strip() for x in p[3].split("|") if x.strip())
required={"מחשבון","שעון","דרייב","גוגל דרייב","גוגל פליי","כרום","יוטיוב","מצלמה","גלריה","סייר קבצים","Google Drive","Google Play","Chrome","YouTube","Calculator","Clock"}
missing=[x for x in required if x not in apps]
assert not missing,("missing core app aliases",missing)
print("Catalog verification OK:",checks,"action_codes",sorted(allowed_codes),"action_settings",30)
