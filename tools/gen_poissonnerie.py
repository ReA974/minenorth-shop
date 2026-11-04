# Génère le pack de boutiques « poissonnerie » (rachat des poissons du mod Starcatcher).
# Lit les fiches de poissons directement dans le jar de Starcatcher (data/starcatcher/starcatcher/fish/*.json).
# Usage : python -I tools/gen_poissonnerie.py [chemin/vers/starcatcher-x.jar] [fichier de sortie]
import json
import sys
import zipfile

JAR = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\diabl\curseforge\minecraft\Instances\dev\mods\starcatcher-3.1.4.5-FORGE-1.20.1.jar"
OUT = sys.argv[2] if len(sys.argv) > 2 else r"..\..\push serveur\config\minenorth_shops\packs\poissonnerie.json"

# Prix de rachat d'un poisson, en centimes, selon sa rareté Starcatcher. Rubrique affichée dans la boutique.
PRIX = {
    "common": (1500, "Commun"),
    "uncommon": (2500, "Peu commun"),
    "rare": (4500, "Rare"),
    "epic": (8000, "Épique"),
    "legendary": (18000, "Légendaire"),
}

# Une boutique a au plus 54 articles : trois comptoirs.
BOUTIQUES = [
    ("Poissonnerie - Courants", ["common", "uncommon"]),
    ("Poissonnerie - Rares", ["rare", "epic"]),
    ("Poissonnerie - Légendaires", ["legendary"]),
]

ORDRE = list(PRIX)
poissons = {r: [] for r in PRIX}
with zipfile.ZipFile(JAR) as z:
    for n in sorted(z.namelist()):
        if not (n.startswith("data/starcatcher/starcatcher/fish/") and n.endswith(".json")):
            continue
        d = json.loads(z.read(n).decode("utf-8"))
        ci = d["catch_info"]
        if ci.get("type", "fish") != "fish":   # modèles de forge (extra), trophées
            continue
        if d["rarity"] in poissons:            # les déchets (trash : bottes, algues...) ne sont pas rachetés
            poissons[d["rarity"]].append(ci["item"]["identifier"])

shops = []
for nom, raretes in BOUTIQUES:
    entries = []
    for r in raretes:
        prix, rubrique = PRIX[r]
        for item in poissons[r]:
            entries.append({"item": item, "quantity": 1, "price": prix, "category": rubrique})
    assert len(entries) <= 54, nom
    shops.append({
        "name": nom, "mode": "BUY", "allowCash": False, "allowCard": True,
        "licence": "", "policeGrade": -1, "pompier": False, "mairie": False,
        "companyId": 0, "companyDirection": False, "illegal": False,
        "illegalMinutes": 0, "illegalRadius": -1.0, "entries": entries,
    })

# Comptoir de vente du matériel : filet de pêche des bateaux (mod garage, minenorth_rp_vehicles).
shops.append({
    "name": "Poissonnerie - Matériel de pêche", "mode": "SELL", "allowCash": True, "allowCard": True,
    "licence": "", "policeGrade": -1, "pompier": False, "mairie": False,
    "companyId": 0, "companyDirection": False, "illegal": False,
    "illegalMinutes": 0, "illegalRadius": -1.0,
    "entries": [{"item": "minenorth_rp_vehicles:filet_peche", "quantity": 1, "price": 35000, "category": "Filets"},
                {"item": "minenorth_rp_vehicles:bobine_fil_peche", "quantity": 1, "price": 4000, "category": "Réparation"}],
})

pack = {
    "_info": "Rachat des poissons Starcatcher (prix en centimes, par poisson, selon la rareté), paiement sur le compte bancaire ; "
             "vente du filet de pêche des bateaux. "
             "Import : /shops importer poissonnerie. Régénéré par minenorth_shops/tools/gen_poissonnerie.py.",
    "shops": shops,
}
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(pack, f, ensure_ascii=False, indent=2)
for s in shops:
    print(s["name"], len(s["entries"]), "articles")
