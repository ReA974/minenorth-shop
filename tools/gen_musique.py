# Génère le pack de boutique « musique » (instruments et matériel du mod minenorth-musique).
# Les ids sont vérifiés contre les objets enregistrés dans MusiqueItems.java : le script s'arrête si un objet n'existe pas.
# Usage : python -I tools/gen_musique.py [fichier de sortie]
import json
import os
import re
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(ICI))
SRC = os.path.join(WS, "minenorth-musique", "src", "main", "java", "fr", "minenorth", "musique", "MusiqueItems.java")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(WS, "..", "push serveur", "config", "minenorth_shops", "packs", "musique.json")

M = "minenorth_musique:"
NOM = "La Note Bleue"

# (rubrique, [(id, prix en euros)]) ; la guitare Or est réservée aux VIP (retirée aux autres joueurs) : jamais en boutique.
RAYONS = [
    ("Guitares", [("guitare_electrique", 450), ("guitare_electrique_noire", 480), ("guitare_electrique_rouge", 480),
                  ("guitare_electrique_bleue", 480), ("guitare_electrique_blanche", 480), ("guitare_metal", 650),
                  ("guitare_acoustique", 280), ("guitare_classique", 220)]),
    ("Basses & claviers", [("basse", 420), ("piano_numerique", 900), ("synthe", 750)]),
    ("Cordes & vents", [("violon", 380), ("saxophone", 600)]),
    ("Batterie", [("batterie", 1200), ("baguettes", 25)]),
    ("Scène & son", [("micro", 90), ("enceinte_portable", 180), ("jukebox", 1500)]),
    ("Vinyles", [("disque_vinyle", 15)]),
]

with open(SRC, encoding="utf-8") as f:
    connus = set(re.findall(r'reg\("([a-z0-9_]+)"', f.read()))

entries, inconnus = [], []
for rubrique, objets in RAYONS:
    assert len(rubrique) <= 20, rubrique
    for oid, prix in objets:
        if oid not in connus:
            inconnus.append(oid)
        entries.append({"item": M + oid, "quantity": 1, "price": int(round(prix * 100)), "category": rubrique})
if inconnus:
    sys.exit("Objets inconnus : " + ", ".join(inconnus))
assert len(entries) <= 54

pack = {
    "_info": "Magasin d'instruments de musique (prix en centimes). Import : /shops importer musique. "
             "Régénéré par minenorth_shops/tools/gen_musique.py.",
    "shops": [{
        "name": NOM, "mode": "SELL", "allowCash": True, "allowCard": True,
        "licence": "", "policeGrade": -1, "pompier": False, "mairie": False,
        "companyId": 0, "companyDirection": False, "illegal": False,
        "illegalMinutes": 0, "illegalRadius": -1.0, "entries": entries,
    }],
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(pack, f, ensure_ascii=False, indent=2)
print("%s : %d articles -> %s" % (NOM, len(entries), os.path.normpath(OUT)))
