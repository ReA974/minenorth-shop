# Génère le pack de boutiques illégales « Surplus Noir » : tenues et équipement tactique du mod ZeroContact (zerocontact:*).
# Les ids sont vérifiés contre ZeroContact_MineNorthRP (objets enregistrés en Java + objets des packs de données) : le script s'arrête
# si un objet n'existe pas. Le matériel de la Police Nationale (uniforme, brassard, béret, JPC PN, masque tactique PN) n'est pas vendu.
# Usage : python -I tools/gen_surplus.py [fichier de sortie]
import glob
import json
import os
import re
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(ICI))
ZC = os.path.join(WS, "ZeroContact_MineNorthRP")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(WS, "..", "push serveur", "config", "minenorth_shops", "packs", "surplus_noir.json")

Z = "zerocontact:"

# (boutique, [(rubrique, [(id, prix en euros)])]) ; une boutique a au plus 54 articles, d'où deux comptoirs.
BOUTIQUES = [
    ("Surplus Noir - Tenues", [
        ("Uniformes", [("uniform_british23_top", 220), ("uniform_british23_bottom", 160), ("uniform_g99_top", 220),
                       ("uniform_g99_bottom", 160), ("uniform_spn_top", 220), ("uniform_spn_bottom", 160),
                       ("uniform_g3pro_top", 260), ("uniform_g3pro_pants", 190), ("uniform_g3pro_ctsfo_top", 260),
                       ("uniform_g3pro_ctsfo_pants", 190), ("uniform_hound_wolf_top", 200), ("uniform_hound_wolf_pants", 120),
                       ("uniform_wedge_top", 240), ("uniform_wedge_pants", 170)]),
        # prix selon la classe de protection du gilet
        ("Gilets pare-balles", [("armor_jpc_v1", 450), ("armor_fcpc_v5", 700), ("armor_hexgrid_black", 700), ("armor_jpc_v2", 700),
                                ("armor_jpc_v2_swimmer_cut", 700), ("armor_thor_black", 700), ("armor_avs", 900),
                                ("armor_c2r_lite", 900), ("armor_hound_wolf", 900), ("armor_defender_2", 1100),
                                ("armor_iotv_gen4", 1100), ("armor_lv119_wedge", 1100), ("armor_6b2", 1200),
                                ("armor_untar_blue", 1400), ("armor_iba_otv", 1400), ("armor_6b23_1", 1600), ("armor_6b23_2", 1600),
                                ("armor_6b43", 2800)]),
        ("Casques & cagoules", [("cap_cyan", 60), ("cap_boss", 60), ("helmet_6b47_ratnik_emr", 500), ("helmet_6b47_ratnik_arc", 500),
                                ("helmet_hound_wolf", 650), ("helmet_phonetalker_iiia", 700), ("helmet_untar_blue", 700),
                                ("helmet_c2r", 700), ("helmet_tbh_iiia", 800), ("helmet_airframe", 950),
                                ("helmet_bastion_black", 1100), ("helmet_bastion_green", 1100), ("helmet_bastion_multicam", 1100),
                                ("helmet_british23", 1100), ("helmet_altyn_visor", 1500), ("mask_tagilla_ybey", 1800),
                                ("mask_tagilla_manhunt", 1800)]),
    ]),
    ("Surplus Noir - Équipement", [
        ("Plaques balistiques", [("plate_slime", 450), ("plate_steel", 600), ("plate_cult_locust", 900),
                                 ("plate_ballistic_convoy", 1300), ("plate_colossus", 1800)]),
        ("Masques à gaz", [("mask_zk", 200), ("mask_pmk2", 220), ("mask_mp5", 280), ("mask_m50", 300)]),
        ("Porte-chargeurs", [("rigs_alice_olive", 250), ("rigs_sop_mr_desert", 280), ("rigs_cr498_desert", 300),
                             ("rigs_thunderbolt_gray", 320), ("rigs_fast_tac", 350)]),
        ("Sacs à dos", [("backpack_british23_red", 300), ("backpack_vkbo_olive", 320), ("backpack_t20_multicam", 400),
                        ("backpack_t20_umbra", 400), ("backpack_6sh118", 450)]),
        ("Casques radio", [("headset_comtac2", 350), ("headset_comtac2_brown", 350), ("headset_c2r", 380),
                           ("headset_amp", 450), ("headset_amp_black", 450), ("headset_hound_wolf", 450)]),
        ("Vision nocturne", [("nvg_pvs31", 1800), ("nvg_pvs31a", 1800), ("sb_pvs31a", 1800), ("nvg_gpnvg", 3500),
                             ("sb_gpnvg", 3500), ("nvg_t7", 4500)]),
        ("Brassards", [("armband_black", 20), ("armband_red", 20), ("armband_blue", 20), ("armband_white", 20),
                       ("armband_green", 20), ("armband_yellow", 20), ("armband_flora", 20)]),
        ("Accessoires", [("kit_armor", 150), ("battery", 40), ("beacon", 150), ("bastion_cover_black", 80),
                         ("tmc_admin_pouch", 120), ("ss_navboard", 200), ("prc_148", 400), ("prc_148_black", 400)]),
    ]),
]
INTERDITS = {"uniform_pn_haut", "uniform_pn_bas", "brassard_pn", "helmet_berret_pn", "armor_jpc_v2_pn_gs", "mask_cold_fear"}
INTERDITS |= {p + g for g in ("sous_officier", "officier", "commissaire")      # équipement PN par grade (03/11)
              for p in ("brassard_pn_", "armor_jpc_v2_pn_", "helmet_pn_")}
INTERDITS |= {"armor_jpc_v2_pn_raid", "helmet_pn_raid"}                       # RAID


def ids_connus():
    ids = set()
    for f in glob.glob(os.path.join(ZC, "forge", "src", "main", "java", "**", "*.java"), recursive=True):
        with open(f, encoding="utf-8") as h:
            ids.update(re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', h.read()))
    for f in glob.glob(os.path.join(ZC, "common", "src", "main", "resources", "data", "zerocontact", "default_pack", "*",
                                    "data", "zerocontact", "items", "*.json")):
        with open(f, encoding="utf-8") as h:
            ids.add(json.load(h)["id"])
    return ids


connus = ids_connus()
shops, erreurs = [], []
for nom, rubriques in BOUTIQUES:
    assert len(nom) <= 32, nom
    entries = []
    for rubrique, objets in rubriques:
        assert len(rubrique) <= 20, rubrique
        for oid, prix in objets:
            if oid not in connus or oid in INTERDITS:
                erreurs.append(oid)
            entries.append({"item": Z + oid, "quantity": 1, "price": int(round(prix * 100)), "category": rubrique})
    assert len(entries) <= 54, (nom, len(entries))
    assert len({e["item"] for e in entries}) == len(entries), nom
    shops.append({
        "name": nom, "mode": "SELL", "allowCash": True, "allowCard": False,       # marché noir : espèces seulement
        "licence": "", "policeGrade": -1, "pompier": False, "mairie": False,
        "companyId": 0, "companyDirection": False, "illegal": True,
        "illegalMinutes": 0, "illegalRadius": -1.0, "entries": entries,
    })
if erreurs:
    sys.exit("Objets inconnus ou interdits : " + ", ".join(erreurs))

pack = {
    "_info": "Vendeur illégal de tenues et d'équipement tactique ZeroContact (prix en centimes, espèces seulement). "
             "Import : /shops importer surplus_noir. Régénéré par minenorth_shops/tools/gen_surplus.py.",
    "shops": shops,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(pack, f, ensure_ascii=False, indent=2)
for s in shops:
    print("%-28s %2d articles" % (s["name"], len(s["entries"])))
non_vendus = sorted(connus - INTERDITS - {e["item"][len(Z):] for s in shops for e in s["entries"]})
print("Objets ZeroContact non vendus :", ", ".join(non_vendus))
print("->", os.path.normpath(OUT))
