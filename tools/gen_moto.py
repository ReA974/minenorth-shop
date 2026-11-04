# Génère le pack de boutique « moto » : magasin d'équipement de motard « Moto Riders » (mod minenorth-vetements).
# Les ids sont vérifiés dans le fichier de langue du mod vêtements (objets enregistrés) : le script s'arrête si un objet n'existe pas.
# Usage : python -I tools/gen_moto.py [fichier de sortie]
import json
import os
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(ICI))
LANG = os.path.join(WS, "minenorth-vetements", "src", "main", "resources", "assets", "minenorth_vetements", "lang", "fr_fr.json")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(WS, "..", "push serveur", "config", "minenorth_shops", "packs", "moto.json")

V = "minenorth_vetements:"
NOM = "Moto Riders"

# (rubrique, [(id d'objet, prix en euros)]) ; le casque blanc « motard police » n'est pas vendu (tenue de service)
RAYONS = [
    ("Casques intégraux", [("casque_integral_shoey_noir_helmet", 380), ("casque_integral_arai_helmet", 620),
                           ("casque_integral_agve_helmet", 690), ("casque_integral_chrome_helmet", 750)]),
    ("Casques jet", [("casque_jet_belle_creme_helmet", 240), ("casque_jet_noir_helmet", 190), ("casque_jet_rouge_helmet", 240)]),
    ("Blousons", [("blouson_moto_dayneze_chestplate", 590), ("blouson_moto_alpinestarz_chestplate", 560),
                  ("blouson_moto_cafe_chestplate", 420), ("blouson_moto_revite_chestplate", 290),
                  ("blouson_moto_furygane_chestplate", 270), ("blouson_moto_police_chestplate", 220)]),
    ("Pantalons", [("pantalon_moto_cuir_leggings", 390), ("pantalon_moto_textile_leggings", 210), ("jean_moto_leggings", 160)]),
    ("Gants", [("gants_moto_racing", 120), ("gants_moto_cuir", 80), ("gants_moto_hiver", 95)]),
    ("Bottes", [("bottes_moto_racing_boots", 380), ("bottes_moto_touring_boots", 240), ("bottes_moto_vintage_boots", 260)]),
]

with open(LANG, encoding="utf-8") as f:
    connus = {k.split(".", 2)[2] for k in json.load(f) if k.startswith("item.minenorth_vetements.")}

entries, inconnus = [], []
for rubrique, objets in RAYONS:
    assert len(rubrique) <= 20, rubrique
    for oid, prix in objets:
        if oid not in connus:
            inconnus.append(oid)
        entries.append({"item": V + oid, "quantity": 1, "price": int(round(prix * 100)), "category": rubrique})
if inconnus:
    sys.exit("Objets inconnus : " + ", ".join(inconnus))
assert len(entries) <= 54 and len(NOM) <= 32
pack = {
    "_info": "Magasin d'équipement de motard (prix en centimes) : casques intégraux et jets, blousons cuir et textile, pantalons, gants, bottes. "
             "Import : /shops importer moto. Régénéré par minenorth_shops/tools/gen_moto.py.",
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
