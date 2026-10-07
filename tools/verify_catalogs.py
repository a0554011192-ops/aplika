#!/usr/bin/env python3
from pathlib import Path
def read(name):
    return [x for x in Path("app/src/main/assets",name).read_text(encoding="utf-8").splitlines() if x.strip()]
checks={"apps.tsv":3000,"actions.tsv":4000,"responses.tsv":5000,"synonyms.tsv":6000}
for name,n in checks.items():
    rows=read(name)
    assert len(rows)==n,(name,len(rows),n)
    assert len(set(rows))==n,(name,"duplicate rows")
print("Catalog verification OK:",checks)
