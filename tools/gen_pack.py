# -*- coding: utf-8 -*-
"""Génère src/main/resources/shop_packs/boutiques-minenorth.json (boutiques prêtes à l'emploi, importées par /shops importer).

Prix en euros ici, écrits en centimes dans le pack. Les ids sont vérifiés contre les fichiers de langue des mods sources
(minenorth-vetements, minenorth-pets, minenorth-drogue) : le script s'arrête si un objet n'existe pas.
Lancer depuis la racine du mod : python tools/gen_pack.py
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WS = os.path.dirname(ROOT)
OUT = os.path.join(ROOT, "src", "main", "resources", "shop_packs", "boutiques-minenorth.json")

V = "minenorth_vetements:"
P = "minenorthpets:"
D = "minenorthdrogue:"


def known_ids():
    ids = set()
    for mod, ns in (("minenorth-vetements", "minenorth_vetements"), ("minenorth-pets", "minenorthpets"), ("minenorth-drogue", "minenorthdrogue")):
        lang = os.path.join(WS, mod, "src", "main", "resources", "assets", ns, "lang", "fr_fr.json")
        with open(lang, encoding="utf-8") as f:
            for k in json.load(f):
                m = re.match(r"item\.%s\.([a-z0-9_]+)$" % ns, k)
                if m:
                    ids.add(ns + ":" + m.group(1))
    return ids


def outfit(oid, haut=None, bas=None, pieds=None, tete=None):
    """Pièces d'une tenue du mod Vêtements (suffixes _helmet / _chestplate / _leggings / _boots)."""
    out = []
    if tete is not None: out.append((V + oid + "_helmet", tete))
    if haut is not None: out.append((V + oid + "_chestplate", haut))
    if bas is not None: out.append((V + oid + "_leggings", bas))
    if pieds is not None: out.append((V + oid + "_boots", pieds))
    return out


def shop(name, cats, **settings):
    entries = []
    for cat, items in cats:
        for it in items:
            item, price = it[0], it[1]
            qty = it[2] if len(it) > 2 else 1
            e = {"item": item, "quantity": qty, "price": int(round(price * 100))}
            if cat: e["category"] = cat
            entries.append(e)
    s = {"name": name, "mode": "SELL", "allowCash": True, "allowCard": True}
    s.update(settings)
    s["entries"] = entries
    return s


SHOPS = [
    # --- streetwear : les tenues de marque du mod (sport / rue)
    shop("Northside Streetwear", [
        ("Survêtements", outfit("nayk_noir", 65, 45, 85, 30) + outfit("nayk_gris", 60, 45, 85) + outfit("nayk_rouge", 65, 45, 85, 30)
         + outfit("abidas_bleu", 60, 45, 80) + outfit("abidas_noir", 60, 45, 80)
         + outfit("pumba_rouge", 55, 40, 70) + outfit("pumba_blanc", 55, 40, 70)),
        ("Hype", outfit("supreem_rouge", 140, tete=45) + outfit("supreem_noir", 160, tete=45)
         + outfit("jordane_rouge", 90, 60, 180) + outfit("champyon_gris", 75, 50)),
        ("Denim", outfit("levys_jean", 70, 80, 90)),
        ("Bombers", outfit("bomber_kaki", 130) + outfit("bomber_noir", 130) + outfit("bomber_marine", 130)),
    ]),
    # --- basiques du quotidien
    shop("Urban Basics", [
        ("Sweats", outfit("sweat_gris", 55) + outfit("sweat_noir", 55) + outfit("sweat_beige", 55)
         + outfit("sweat_bordeaux", 55) + outfit("sweat_vert", 55)),
        ("Pantalons", outfit("jogging_gris", bas=40) + outfit("jogging_noir", bas=40) + outfit("cargo_kaki", bas=55)
         + outfit("cargo_noir", bas=55) + outfit("cargo_beige", bas=55) + outfit("jean_brut", bas=65) + outfit("jean_noir", bas=65)),
    ]),
    # --- luxe : pièces griffées, doudounes, manteaux, cuir
    shop("Maison Aurèle", [
        ("Griffé", outfit("guchi_vert", 450, pieds=550, tete=320) + outfit("louis_vuiten", 900, 650, 750)
         + outfit("lacroste_vert", 110, 85) + outfit("lacroste_blanc", 110, 85)),
        ("Doudounes", outfit("nord_fasse", 280, 120, tete=60) + outfit("doudoune_noire", 950) + outfit("doudoune_rouge", 950)
         + outfit("doudoune_marine", 950) + outfit("doudoune_creme", 950) + outfit("doudoune_kaki", 950)
         + outfit("gilet_noir", 380) + outfit("gilet_bordeaux", 380)),
        ("Manteaux", outfit("perfecto_noir", 320) + outfit("perfecto_marron", 320) + outfit("manteau_camel", 420)
         + outfit("manteau_noir", 420) + outfit("manteau_gris", 420) + outfit("trench_beige", 390)),
        ("Cagoules griffées", [(V + "cagoule_guchi_helmet", 380), (V + "cagoule_guchi_noire_helmet", 420),
                               (V + "cagoule_vuiten_helmet", 450), (V + "cagoule_supreem_helmet", 180), (V + "cagoule_nayk_helmet", 90)]),
    ]),
    # --- costumes et soirée
    shop("Atelier Valmont", [
        ("Costumes", outfit("costard_noir", 380, 180, 220) + outfit("costard_marine", 380, 180, 220)
         + outfit("costard_gris", 380, 180, 220) + outfit("costard_blanc", 420, 200, 220)
         + outfit("costard_noir_pin", 450) + outfit("costard_marine_pin", 450) + outfit("costard_gris_pin", 450)
         + outfit("smoking", 520, 240, 260)),
        ("Soirée", outfit("robe_soiree_rouge", 450, 150, 240) + outfit("robe_soiree_noire", 450, 150, 240)
         + outfit("robe_cocktail", 320, 120, 200)),
    ]),
    # --- plage
    shop("Riviera Plage", [
        ("Été", outfit("robe_ete", 85, 35)),
        ("Bain", outfit("bikini_rouge", 45, 35) + outfit("bikini_turquoise", 45, 35) + outfit("bikini_noir", 60, 45)
         + outfit("maillot_bleu", bas=30) + outfit("maillot_tropical", bas=30)),
    ]),
    # --- couvre-chefs (emplacement casque)
    shop("Chapellerie du Nord", [
        ("Chapeaux", outfit("haut_de_forme_noir", tete=120) + outfit("haut_de_forme_blanc", tete=160) + outfit("chapeau_melon", tete=90)
         + outfit("fedora_gris", tete=75) + outfit("fedora_marron", tete=75) + outfit("panama", tete=70) + outfit("chapeau_cowboy", tete=65)),
        ("Casquettes", outfit("casquette_noire", tete=25) + outfit("casquette_blanche", tete=25) + outfit("casquette_rouge", tete=25)
         + outfit("casquette_marine", tete=25) + outfit("casquette_beige", tete=25)
         + outfit("casquette_inverse_noire", tete=28) + outfit("casquette_inverse_rouge", tete=28) + outfit("casquette_inverse_marine", tete=28)
         + outfit("casquette_inverse_blanche", tete=28) + outfit("casquette_inverse_kaki", tete=28)),
        ("Bonnets & bobs", outfit("bonnet_noir", tete=20) + outfit("bonnet_gris", tete=20) + outfit("bonnet_rouge", tete=20)
         + outfit("bonnet_moutarde", tete=20) + outfit("bonnet_marine", tete=20)
         + outfit("bob_noir", tete=22) + outfit("bob_beige", tete=22) + outfit("bob_blanc", tete=22) + outfit("bob_kaki", tete=22)),
    ]),
    # --- accessoires portés en plus des vêtements (emplacements visage, cou, poignet, mains, dos)
    shop("Comptoir Accessoires", [
        ("Lunettes", [(V + "lunettes_aviator_helmet", 140), (V + "lunettes_wayfarer_helmet", 120), (V + "lunettes_sport_helmet", 85),
                      (V + "lunettes_rose_helmet", 60), (V + "lunettes_rondes", 95), (V + "lunettes_masque", 80)]),
        ("Bijoux", [(V + "chaine_or", 1500), (V + "chaine_argent", 450), (V + "chaine_croix", 650), (V + "collier_perles", 800)]),
        ("Montres", [(V + "montre_or", 4500), (V + "montre_argent", 1800), (V + "montre_sport", 220), (V + "gourmette_or", 900)]),
        ("Sacs", [(V + "sac_dos_noir", 60), (V + "sac_dos_rouge", 60), (V + "sac_dos_kaki", 60), (V + "banane_noire", 40),
                  (V + "banane_guchi", 480), (V + "sacoche_noire", 120)]),
        ("Gants & écharpes", [(V + "gants_cuir_noir", 75), (V + "gants_cuir_marron", 75), (V + "gants_laine", 25),
                              (V + "echarpe_grise_helmet", 35), (V + "echarpe_rouge_helmet", 55), (V + "echarpe_noire", 35)]),
        ("Visage", [(V + "bandana_rouge_helmet", 15), (V + "bandana_noir_helmet", 15),
                    (V + "cagoule_noire_helmet", 30), (V + "cagoule_kaki_helmet", 30)]),
    ]),
    # --- animalerie : nourriture et accessoires (minenorth-pets, sans recette)
    shop("Animalerie Patte d'Or", [
        ("Croquettes", [(P + "croquettes_chien", 12), (P + "croquettes_chat", 12), (P + "granules_rongeur", 8),
                        (P + "nourriture_reptile", 10), (P + "graines_perroquet", 8)]),
        ("Accessoires", [(P + "bandana_rouge", 15), (P + "bandana_bleu", 15), (P + "noeud_papillon", 20), (P + "collier_strass", 45),
                         (P + "manteau_animal", 40), (P + "gilet_neon", 30), (P + "chapeau_animal", 25), (P + "casquette_animal", 20)]),
        ("Identification", [(P + "collier_identification", 50)]),
    ]),
    # --- complète la boutique clandestine existante (#21 « Vendeur outils drogue ») : seulement les objets qui lui manquent.
    #     Créée telle quelle (illégale) si un serveur ne l'a pas.
    shop("Vendeur outils drogue", [
        ("Graines", [(D + "cannabis_seeds", 10), (D + "coca_seeds", 10), (D + "ephedra_seeds", 10), (D + "poppy_seeds", 15)]),
        ("Culture", [(D + "grow_tent_small", 500), (D + "grow_tent_long", 900), (D + "uv_lamp", 100), (D + "watering_can", 100),
                     (D + "potting_soil", 10)]),
        ("Labo", [(D + "chemical", 10), (D + "lab", 100), (D + "cooking_oven", 100), (D + "pack_table", 100), (D + "roll_table", 100),
                  (D + "empty_bag", 10, 64)]),
    ], illegal=True),
]


def main():
    ids = known_ids()
    bad, seen = [], set()
    for s in SHOPS:
        assert len(s["name"]) <= 32, s["name"]
        assert len(s["entries"]) <= 54, (s["name"], len(s["entries"]))
        for e in s["entries"]:
            assert len(e.get("category", "")) <= 20, e["category"]
            if e["item"] not in ids: bad.append(e["item"])
            if e["item"].startswith(V): seen.add(e["item"])
    if bad:
        sys.exit("Objets inconnus : " + ", ".join(bad))
    vet = sorted(i for i in ids if i.startswith(V) and not i.split(":")[1].startswith(("itemGroup",)))
    missing = [i for i in vet if i not in seen]
    print("Vêtements non vendus :", missing or "aucun")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    pack = {"_info": "Pack de boutiques MineNorth. Import : /shops importer boutiques-minenorth. Crée les boutiques absentes (par nom) "
                     "et ajoute seulement les articles manquants aux boutiques existantes du même nom. Prix en centimes.",
            "shops": SHOPS}
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=2)
    for s in SHOPS:
        print("%-24s %2d articles" % (s["name"], len(s["entries"])))
    write_catalog()


def write_catalog():
    """BOUTIQUES.md : catalogue complet (boutique, rubrique, id, prix), pour les admins."""
    lines = ["# Catalogue du pack `boutiques-minenorth`", "",
             "Généré par `tools/gen_pack.py` (ne pas éditer à la main). Import et pose des PNJ : voir le README, section « Boutiques prêtes à l'emploi ».", ""]
    for s in SHOPS:
        flags = " (illégale)" if s.get("illegal") else ""
        lines += ["## " + s["name"] + flags, "", "| Rubrique | Objet | Lot | Prix |", "|---|---|---|---|"]
        for e in s["entries"]:
            euros = ("%.2f" % (e["price"] / 100)).replace(".", ",") + " €"
            lines.append("| %s | `%s` | %d | %s |" % (e.get("category", ""), e["item"], e["quantity"], euros))
        lines.append("")
    with open(os.path.join(ROOT, "BOUTIQUES.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


if __name__ == "__main__":
    main()
