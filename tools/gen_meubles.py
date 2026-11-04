# Génère le pack des magasins de meubles « meubles » et le catalogue de l'application Déco du téléphone :
#  - « Maison Nordik » : meubles à petit prix, façon grande enseigne suédoise (bois brut, literie, tapis, rideaux).
#  - « Atelier Prestige » : design haut de gamme (salon, multimédia, cuisine équipée, billard, pianos) + l'établi d'Immersive Furniture.
#  - « Brocante du Port » : vintage et objets chinés (tourne-disque, tableaux anciens, plantes, petits objets).
#  - Collections VIP / VIP+ : seulement dans l'application Déco (jamais en magasin), réservées par l'avantage « decoVip » du mod VIP.
# Les objets Yuushya n'ont pas de traduction française : chaque article reçoit son nom français en NBT (display.Name).
# Immersive Furniture : les meubles sont des créations (NBT) faites à l'établi ; on vend l'établi et le matériau, pas les créations.
# Les ids sont vérifiés : Yuushya et Immersive Furniture dans leurs jars (dossier mods du serveur ou du client), nos mods dans leurs sources.
# Le script s'arrête si un objet n'existe pas ou si un jar est introuvable.
# Usage : python -I tools/gen_meubles.py [dossier des mods]
#   sorties : ../push serveur/config/minenorth_shops/packs/meubles.json  et  ../push serveur/config/Minenorth-portes/deco-catalogue.json
import glob
import json
import os
import re
import sys
import zipfile

ICI = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(ICI))
PUSH = os.path.join(WS, "..", "push serveur")
OUT_PACK = os.path.join(PUSH, "config", "minenorth_shops", "packs", "meubles.json")
OUT_DECO = os.path.join(PUSH, "config", "Minenorth-portes", "deco-catalogue.json")

# dossiers où chercher les jars des mods tiers (le premier qui contient le jar gagne)
DOSSIERS_MODS = ([sys.argv[1]] if len(sys.argv) > 1 else []) + [
    os.path.join(PUSH, "mods"),
    os.path.join(os.path.expanduser("~"), "curseforge", "minecraft", "Instances", "*", "mods"),
    os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "mods"),
]
JARS = {"yuushya": "yuushya-*forge*.jar", "immersive_furniture": "immersive_furniture-*.jar"}

# nos mods : espace de noms -> dossier
NOS_MODS = {"minenorthbar": "minenorth-bar", "minenorth_musique": "minenorth-musique"}

Y, IF, BAR, MUS = "yuushya:", "immersive_furniture:", "minenorthbar:", "minenorth_musique:"

# (enseigne, [(rubrique, [(id, prix en euros, nom français ou None = nom du jeu)])])
BOUTIQUES = [
    ("Maison Nordik", [
        ("Tables", [(Y + "raw_oak_table", 45, "Table en chêne brut"), (Y + "raw_birch_table", 45, "Table en bouleau brut"),
                    (Y + "raw_spruce_table", 45, "Table en sapin brut"), (Y + "raw_acacia_table", 50, "Table en acacia brut"),
                    (Y + "raw_dark_oak_table", 55, "Table en chêne foncé brut"), (Y + "raw_bamboo_table", 40, "Table en bambou"),
                    (Y + "oak_foldable_table", 25, "Table pliante en chêne"), (Y + "acacia_foldable_table", 25, "Table pliante en acacia")]),
        ("Rangements", [(Y + "raw_oak_drawer", 60, "Commode en chêne brut"), (Y + "raw_birch_drawer", 60, "Commode en bouleau brut"),
                        (Y + "raw_spruce_drawer", 60, "Commode en sapin brut"), (Y + "raw_bamboo_drawer", 55, "Commode en bambou"),
                        (Y + "raw_oak_vertical_drawer", 70, "Chiffonnier en chêne brut"),
                        (Y + "raw_birch_vertical_drawer", 70, "Chiffonnier en bouleau"),
                        (Y + "plate_shelf", 35, "Étagère à assiettes"), (Y + "magazine_shelf", 30, "Porte-revues")]),
        ("Lits", [(Y + "bed_frame_1", 120, "Cadre de lit en chêne"), (Y + "bed_frame_0", 120, "Cadre de lit en acacia"),
                  (Y + "bed_frame_2", 120, "Cadre de lit gris"), (Y + "bed_frame_3", 120, "Cadre de lit blanc")]
                 + [(Y + "inner_bed_%d" % i, 60, "Literie motif %d" % (i + 1)) for i in range(8)]),
        ("Assises", [(Y + "classroom_chair", 20, "Chaise d'écolier"), (Y + "office_chair", 65, "Chaise de bureau")]),
        ("Tapis", [(Y + "carpet_%d" % i, 15, "Tapis motif %d" % (i + 1)) for i in range(8)]),
        ("Luminaires", [(Y + "desk_lamp_0", 15, "Lampe de chevet 1"), (Y + "desk_lamp_2", 18, "Lampe de bureau"),
                        (Y + "desk_lamp_1", 15, "Lampe de chevet 2"), (Y + "fluorescent_lamp", 12, "Néon"),
                        (Y + "incandescent_lamp", 8, "Ampoule"), (Y + "strip_light", 14, "Réglette LED"),
                        (Y + "round_ceiling_lamp", 22, "Plafonnier rond")]),
        ("Rideaux", [(Y + "white_curtain", 18, "Rideau blanc"), (Y + "blue_curtain", 18, "Rideau bleu"), (Y + "red_curtain", 18, "Rideau rouge"),
                     (Y + "yellow_curtain", 18, "Rideau jaune"), (Y + "cyan_curtain", 18, "Rideau cyan")]),
        ("Salle de bain", [(Y + "toilet", 90, "Toilettes"), (Y + "sink", 70, "Lavabo"), (Y + "mirror", 25, "Miroir sur pied"),
                           (Y + "mounted_mirror", 30, "Miroir mural")]),
    ]),
    ("Atelier Prestige", [
        ("Salon", [(Y + "couch", 1400, "Canapé design"), (Y + "cushion_armchair", 800, "Fauteuil capitonné"),
                   (Y + "marble_table", 950, "Table en marbre"), (Y + "stone_table", 600, "Table en pierre"),
                   (Y + "big_table", 700, "Grande table design"), (Y + "medium_table", 450, "Table basse design"),
                   (Y + "display_cabinet", 1200, "Vitrine d'exposition"), (Y + "decoration_bookshelf", 650, "Bibliothèque déco")]),
        ("Multimédia", [(Y + "tv", 1100, "Téléviseur"), (Y + "mounted_tv", 1300, "Téléviseur mural"), (Y + "audio", 500, "Chaîne hi-fi"),
                        (Y + "audio_large", 900, "Enceinte colonne"), (Y + "centre_speaker", 400, "Enceinte centrale"),
                        (Y + "monitor_with_keyboard_and_mouse", 1500, "Poste informatique"), (Y + "gaming_chair", 750, "Fauteuil gamer")]),
        ("Cuisine", [(Y + "refrigerator", 1300, "Réfrigérateur"), (Y + "gas_stove", 900, "Gazinière"), (Y + "microwaver", 250, "Micro-ondes"),
                     (Y + "kettle", 60, "Bouilloire"), (Y + "sink_", 400, "Évier"), (Y + "washing_machine", 1000, "Lave-linge"),
                     (Y + "seasoning_box", 40, "Boîte à épices")]),
        ("Luminaires", [(Y + "modern_lamp_0", 350, "Lampadaire design"), (Y + "modern_lamp_1", 220, "Applique design"),
                        (Y + "modern_lamp_2", 300, "Suspension design"), (Y + "modern_lamp_ceiling", 280, "Plafonnier moderne"),
                        (Y + "ring_light", 260, "Anneau lumineux"), (Y + "hanging_light", 240, "Suspension sur mesure")]),
        ("Confort", [(Y + "ceiling_fan", 320, "Ventilateur de plafond"), (Y + "floor_fan_", 140, "Ventilateur sur pied"),
                     (Y + "air_conditioner", 900, "Climatiseur mural"), (Y + "air_conditioner_floor", 1200, "Climatiseur colonne"),
                     (Y + "venetian_blinds", 120, "Store vénitien")]),
        ("Déco", [(Y + "porcelain_vase", 300, "Vase en porcelaine"), (Y + "flower_painting", 450, "Tableau floral"),
                  (Y + "big_poster", 350, "Grand tableau"), (Y + "potted_tall_oak", 250, "Chêne en pot"), (Y + "potted_bamboo", 120, "Bambou en pot"),
                  (Y + "potted_blue_orchid", 90, "Orchidée bleue en pot"), (Y + "round_clock", 180, "Horloge ronde")]),
        ("Musique & jeux", [(Y + "electric_piano", 2500, "Piano électrique"), (Y + "piano", 3500, "Piano droit"),
                            (MUS + "piano_numerique", 900, None), (MUS + "jukebox", 1500, None),
                            (BAR + "table_billard", 4500, None), (BAR + "cible_flechettes", 250, None)]),
        ("Coffres", [(Y + "safe", 1500, "Coffre-fort"), (Y + "tall_safe", 2800, "Grand coffre-fort")]),
        ("Sur mesure", [(IF + "artisans_workstation", 2000, "Établi de l'artisan"), (IF + "crafting_material", 5, "Matériau d'artisanat")]),
    ]),
    ("Brocante du Port", [
        ("Vintage", [(Y + "old_tv", 120, "Vieille télé"), (Y + "vinyl_record_player", 220, "Tourne-disque"),
                     (Y + "vinyl_record_red", 15, "Vinyle rouge"), (Y + "vinyl_record_white", 15, "Vinyle blanc"),
                     (Y + "vinyl_record_yellow", 15, "Vinyle jaune"), (Y + "vinyl_record_cyan", 15, "Vinyle cyan"),
                     (Y + "boom_box", 80, "Radio-cassette"), (Y + "guitar", 90, "Guitare murale"), (Y + "binoculars", 35, "Jumelles")]),
        ("Tableaux", [(Y + "painting_%d" % i, 70, "Tableau ancien n°%d" % (i + 1)) for i in range(10)]
                     + [(Y + "small_poster_0", 20, "Petite affiche 1"), (Y + "small_poster_1", 20, "Petite affiche 2"),
                        (Y + "long_poster", 35, "Affiche en longueur"), (Y + "high_poster", 35, "Affiche en hauteur")]),
        ("Objets", [(Y + "books", 15, "Pile de livres"), (Y + "sorted_mug", 15, "Tasses alignées"), (Y + "bottles", 10, "Bouteilles vides"),
                    (Y + "mug", 5, "Tasse"), (Y + "plate", 5, "Assiette"), (Y + "piled_plate", 15, "Pile d'assiettes"),
                    (Y + "ragdoll", 30, "Poupée de chiffon"), (Y + "sleeping_orange_cat", 45, "Chat roux endormi"),
                    (Y + "black_cat", 40, "Chat noir en céramique"), (Y + "white_cat", 40, "Chat blanc en céramique"),
                    (Y + "patterned_slippers", 12, "Chaussons"), (Y + "wastepaper_basket", 10, "Corbeille à papier")]),
        ("Meubles anciens", [(Y + "raw_dark_oak_drawer", 45, "Commode en chêne foncé"), (Y + "raw_jungle_drawer", 45, "Commode exotique"),
                             (Y + "raw_mangrove_table", 35, "Table en palétuvier"), (Y + "raw_cherry_table", 40, "Table en merisier"),
                             (Y + "raw_cherry_drawer", 45, "Commode en merisier"), (Y + "tri_legged_chair", 8, "Chaise bancale"),
                             (Y + "crate", 12, "Caisse en bois"), (Y + "packed_crate", 10, "Cagette"),
                             (Y + "medieval_lamp", 40, "Lampe à l'ancienne"), (Y + "hanged_clothes", 20, "Vêtements suspendus"),
                             (Y + "hanger_rod", 15, "Tringle à vêtements")]),
        ("Plantes", [(Y + "potted_fern", 20, "Fougère en pot"), (Y + "potted_cactus", 15, "Cactus en pot"),
                     (Y + "potted_sunflower", 20, "Tournesol en pot"), (Y + "potted_red_tulip", 15, "Tulipe rouge en pot"),
                     (Y + "potted_white_tulip", 15, "Tulipe blanche en pot"), (Y + "potted_bush", 18, "Buisson en pot"),
                     (Y + "potted_sakura_sprout", 25, "Jeune cerisier en pot"), (Y + "potted_flower_basket", 30, "Panier fleuri")]),
    ]),
]

# Collections exclusives : application Déco seulement. vip = niveau de l'avantage « decoVip » requis (1 = VIP, 2 = VIP+).
COLLECTIONS_VIP = [
    ("Collection VIP", 1, [(Y + "gold_vase", 600, "Vase doré"), (Y + "potted_tall_sakura", 450, "Grand cerisier en pot"),
                           (Y + "potted_tall_maple", 450, "Grand érable en pot"), (Y + "decorated_refrigerator", 1500, "Frigo décoré"),
                           (Y + "mounted_tv_online", 1800, "Écran mural connecté"), (Y + "tv_online", 1500, "Télé connectée"),
                           (Y + "beverage_drawer", 900, "Cave à boissons"), (Y + "gothic_ceiling_lamp", 800, "Lustre gothique")]),
    ("Collection VIP+", 2, [(Y + "vending_machine_red_v", 2500, "Distributeur rouge"), (Y + "vending_machine_blue_v", 2500, "Distributeur bleu"),
                            (Y + "vending_machine_gray_v", 2500, "Distributeur gris"), (Y + "potted_yuuca", 300, "Yucca en pot"),
                            (Y + "sleeping_ragdoll", 250, "Poupée endormie")]),
]


def trouver_jar(motif):
    for d in DOSSIERS_MODS:
        for f in sorted(glob.glob(os.path.join(d, motif))):
            return f
    return None


def ids_jar(ns, jar):
    """Objets d'un mod tiers : modèles d'objet du jar (assets/<ns>/models/item/<id>.json)."""
    with zipfile.ZipFile(jar) as z:
        return {ns + ":" + m.group(1) for n in z.namelist() for m in [re.match(r"assets/%s/models/item/([a-z0-9_]+)\.json$" % ns, n)] if m}


def ids_sources():
    ids = set()
    for ns, dossier in NOS_MODS.items():
        for f in glob.glob(os.path.join(WS, dossier, "src", "main", "java", "**", "*.java"), recursive=True):
            with open(f, encoding="utf-8", errors="replace") as h:
                ids.update(ns + ":" + i for i in re.findall(r'\b(?:register|reg)\(\s*"([a-z0-9_]+)"', h.read()))
    return ids


def nbt_nom(nom):
    # display.Name : composant JSON entre apostrophes (SNBT), sans italique
    texte = json.dumps({"text": nom, "italic": False}, ensure_ascii=False)
    return "{display:{Name:'%s'}}" % texte.replace("\\", "\\\\").replace("'", "\\'")


connus = ids_sources()
for ns, motif in JARS.items():
    jar = trouver_jar(motif)
    if jar is None:
        sys.exit("Jar introuvable pour %s (%s) : passez le dossier des mods en argument." % (ns, motif))
    print("%-20s %s" % (ns, jar))
    connus |= ids_jar(ns, jar)

erreurs, vus = [], set()


def article(item, prix, nom, rubrique):
    assert len(rubrique) <= 20, rubrique
    assert nom is None or len(nom) <= 32, nom
    if item not in connus:
        erreurs.append(item)
    if item in vus:
        erreurs.append("doublon " + item)
    vus.add(item)
    e = {"item": item, "quantity": 1, "price": int(round(prix * 100)), "category": rubrique}
    if nom:
        e["nbt"] = nbt_nom(nom)
    return e


shops, deco = [], []
for nom, rubriques in BOUTIQUES:
    assert len(nom) <= 32, nom
    entries = [article(i, p, n, r) for r, objets in rubriques for i, p, n in objets]
    assert len(entries) <= 54, (nom, len(entries))
    shops.append({
        "name": nom, "mode": "SELL", "allowCash": True, "allowCard": True,
        "licence": "", "policeGrade": -1, "pompier": False, "mairie": False,
        "companyId": 0, "companyDirection": False, "illegal": False,
        "illegalMinutes": 0, "illegalRadius": -1.0, "entries": entries,
    })
    deco.append({"name": nom, "vip": 0, "entries": entries})
for nom, niveau, objets in COLLECTIONS_VIP:
    entries = [article(i, p, n, nom[:20]) for i, p, n in objets]
    assert len(entries) <= 54, (nom, len(entries))
    deco.append({"name": nom, "vip": niveau, "entries": entries})
if erreurs:
    sys.exit("Objets inconnus : " + ", ".join(erreurs))

pack = {
    "_info": "Magasins de meubles (prix en centimes) : Maison Nordik (petits prix), Atelier Prestige (haut de gamme), Brocante du Port. "
             "Import : /shops importer meubles. Mêmes articles dans l'application Déco du téléphone (deco-catalogue.json). "
             "Régénéré par minenorth_shops/tools/gen_meubles.py.",
    "shops": shops,
}
catalogue = {
    "_info": "Catalogue de l'application Déco du téléphone (mod Portes) : les trois magasins de meubles + collections VIP (vip = niveau de "
             "l'avantage decoVip requis). Prix en centimes. Régénéré par minenorth_shops/tools/gen_meubles.py ; relu par /deco recharger.",
    "enseignes": deco,
}
for chemin, contenu in ((OUT_PACK, pack), (OUT_DECO, catalogue)):
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    with open(chemin, "w", encoding="utf-8") as f:
        json.dump(contenu, f, ensure_ascii=False, indent=2)
    print("->", os.path.normpath(chemin))
for s in deco:
    print("%-20s %2d articles%s" % (s["name"], len(s["entries"]), " (VIP %d)" % s["vip"] if s["vip"] else ""))
