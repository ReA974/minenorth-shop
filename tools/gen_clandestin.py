# Génère le pack de boutiques illégales « clandestin » :
#  - « Vendeur outils drogue » : tout ce qu'il faut pour produire (graines et ingrédients introuvables autrement, culture, labo, coupe).
#    Même nom que la boutique clandestine du serveur (#21) : l'import lui ajoute seulement les articles qui lui manquent.
#  - « Le Fourgue » : outils de vol (voitures, maisons, radars, coffres de banque) et de quoi rester discret.
# Les ids sont vérifiés dans les sources des mods (objets enregistrés) : le script s'arrête si un objet n'existe pas.
# Usage : python -I tools/gen_clandestin.py [fichier de sortie]
import glob
import json
import os
import re
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(ICI))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(WS, "..", "push serveur", "config", "minenorth_shops", "packs", "clandestin.json")

# espace de noms -> dossier du mod
MODS = {
    "minenorthdrogue": "minenorth-drogue", "minenorthvol": "minenorth-vol", "minenorthportes": "minenorth-portes",
    "minenorthpolice": "minenorth-police", "minenorth_eurobank": "minenorth-eurobank", "minenorth_vetements": "minenorth-vetements",
}
D, V, P = "minenorthdrogue:", "minenorthvol:", "minenorthportes:"
POL, BQ, VET = "minenorthpolice:", "minenorth_eurobank:", "minenorth_vetements:"

BOUTIQUES = [
    ("Vendeur outils drogue", True, [
        ("Graines", [(D + "cannabis_seeds", 10), (D + "coca_seeds", 10), (D + "ephedra_seeds", 10), (D + "poppy_seeds", 15),
                     (D + "mushroom_spores", 20)]),
        ("Ingrédients", [(D + "chemical", 10), (D + "sassafras_oil", 40), (D + "ergot", 30), (D + "empty_bag", 10, 64)]),
        ("Culture", [(D + "grow_tent_small", 500), (D + "grow_tent_long", 900), (D + "potting_soil", 10), (D + "fertilizer", 15),
                     (D + "uv_lamp", 100), (D + "led_lamp", 250), (D + "watering_can", 100), (D + "drip_kit", 150)]),
        ("Labo", [(D + "lab", 100), (D + "cooking_oven", 100), (D + "drying_rack", 200), (D + "roll_table", 100),
                  (D + "pack_table", 100), (D + "pill_press", 400), (D + "air_extractor", 300), (D + "carbon_filter", 250)]),
        ("Coupe", [(D + "cut_table", 150), (D + "baking_soda", 5), (D + "paracetamol", 5), (D + "caffeine", 8)]),
        ("Santé", [(D + "naloxone", 60)]),
    ]),
    ("Le Fourgue", False, [
        ("Voitures", [(V + "screwdriver", 80), (V + "lockpicks", 150), (V + "lockpick_kit", 300), (V + "hacker_tool", 1200),
                      (V + "fake_plates", 250)]),
        ("Maisons", [(P + "lockpick", 120)]),
        ("Radars", [(POL + "pirate_radar", 400)]),
        ("Banque", [(BQ + "drill", 1500), (BQ + "drill_mk2", 3500), (BQ + "drill_mk3", 7000), (BQ + "hacker", 2500)]),
        ("Discrétion", [(VET + "cagoule_noire_helmet", 30), (VET + "cagoule_kaki_helmet", 30), (VET + "bandana_noir_helmet", 15),
                        (VET + "gants_cuir_noir", 75)]),
    ]),
]


def ids_connus():
    """Objets enregistrés de chaque mod : register("id" / reg("id" dans les sources Java."""
    ids = set()
    for ns, dossier in MODS.items():
        trouves = set()
        for f in glob.glob(os.path.join(WS, dossier, "src", "main", "java", "**", "*.java"), recursive=True):
            with open(f, encoding="utf-8", errors="replace") as h:
                trouves.update(re.findall(r'\b(?:register|reg)\(\s*"([a-z0-9_]+)"', h.read()))
        # les vêtements sont générés par tenue : on se fie aussi aux clés de langue
        lang = glob.glob(os.path.join(WS, dossier, "src", "main", "resources", "assets", ns, "lang", "fr_fr.json"))
        if lang:
            with open(lang[0], encoding="utf-8") as h:
                trouves.update(m.group(1) for k in json.load(h) for m in [re.match(r"item\.%s\.([a-z0-9_]+)$" % ns, k)] if m)
        ids.update(ns + ":" + i for i in trouves)
    return ids


connus = ids_connus()
shops, erreurs = [], []
for nom, cartes, rubriques in BOUTIQUES:
    assert len(nom) <= 32, nom
    entries = []
    for rubrique, objets in rubriques:
        assert len(rubrique) <= 20, rubrique
        for it in objets:
            item, prix, qte = it[0], it[1], (it[2] if len(it) > 2 else 1)
            if item not in connus:
                erreurs.append(item)
            entries.append({"item": item, "quantity": qte, "price": int(round(prix * 100)), "category": rubrique})
    assert len(entries) <= 54, (nom, len(entries))
    shops.append({
        "name": nom, "mode": "SELL", "allowCash": True, "allowCard": cartes,
        "licence": "", "policeGrade": -1, "pompier": False, "mairie": False,
        "companyId": 0, "companyDirection": False, "illegal": True,
        "illegalMinutes": 0, "illegalRadius": -1.0, "entries": entries,
    })
if erreurs:
    sys.exit("Objets inconnus : " + ", ".join(erreurs))

pack = {
    "_info": "Boutiques clandestines : production de drogue et outils de vol (prix en centimes). Import : /shops importer clandestin. "
             "« Vendeur outils drogue » existant : seuls les articles manquants sont ajoutés. Régénéré par minenorth_shops/tools/gen_clandestin.py.",
    "shops": shops,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(pack, f, ensure_ascii=False, indent=2)
for s in shops:
    print("%-24s %2d articles" % (s["name"], len(s["entries"])))
vendus = {e["item"] for s in shops for e in s["entries"]}
print("Objets de la drogue non vendus :", ", ".join(sorted(i for i in connus if i.startswith(D) and i not in vendus)))
print("->", os.path.normpath(OUT))
