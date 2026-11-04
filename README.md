# Boutiques MineNorthRP (`minenorth_shops`)

Mod Forge 1.20.1 : boutiques créées et configurées **en jeu via une interface** (même style que MineNorth Bank),
en **vente** ou en **rachat**, assignables à **n'importe quelle entité**. Paiement en espèces ou par carte via
`minenorth_eurobank` (dépendance obligatoire).

## Compilation

1. Compiler EuroBank (`./gradlew build` dans `systemebanqueminenorth`).
2. Copier `build/libs/minenorth_eurobank-1.0.0.jar` dans `libs/` de ce projet.
   (Autre version : changer `eurobank_version` dans `gradle.properties`.)
3. `./gradlew build` (JDK 17 : `JAVA_HOME=~/.jdks/ms-17.0.20.1`, Gradle échoue sous Java 27) → `build/libs/minenorth-shops-1.1.0.jar`
   (nom en tiret : supprimer un ancien `minenorth_shops-…jar`).
4. Sur le serveur : mettre les **deux** jars dans `mods/` (côté client aussi, il y a des écrans).

## Écran de boutique (joueurs)

Charte MineNorth (fond bleu nuit, blocs et boutons arrondis), taille adaptée à la fenêtre (utilisable en échelle d'interface 3 et 4).
- Les noms longs passent sur deux ou trois lignes (réduits si besoin), sinon coupés avec « .. » et le nom complet en infobulle.
- **Vitrine 3D** de l'article choisi, qui tourne (glisser à la souris pour la faire tourner) : un vêtement (pièce d'armure, chapeau,
  ou accessoire du mod `minenorth-vetements`) est **porté par votre propre personnage**, rien n'est équipé pour de vrai ;
  les autres objets (armes...) sont affichés en grand.

## Utilisation (admins / op niveau 2)

| Action | Comment |
|---|---|
| Ouvrir le panneau | `/shops` |
| Créer une boutique | Taper un nom → **Créer** (on arrive directement dans l'édition) |
| Vente / Rachat | Bouton de mode : *Vend aux joueurs* ↔ *Achète aux joueurs* |
| Moyens de paiement | **Espèces : oui/non**, **Carte : oui/non** (en rachat : *Compte* = argent versé sur le compte) |
| Ajouter un article | **Choisir dans l'inventaire** : votre inventaire s'affiche, cliquez l'objet (par défaut : l'objet en main). NBT conservé (enchantements, nom, données de mods). La quantité est préremplie avec la taille du tas. Mettre le prix du lot → **Ajouter** |
| Modifier un article | Cliquer la ligne → changer prix/quantité → **Appliquer**. **Monter** = réordonner, **Retirer** = supprimer |
| Conditions de la boutique | Trois **listes déroulantes** (un clic ouvre la liste, un clic choisit) : **Licence** (liste lue dans la config de `minenorth_permis`, *Aucune* = retirer), **Accès** (*Tous*, *Police*, *Police off.+*, *Police comm.*, *Pompiers*) et **Statut** (*Légale* / *Illégale*) |
| Rubrique d'un article | Champ **Rubrique** à côté de l'objet, lors de **Ajouter** ou **Appliquer** (vide = sans rubrique). Dans `shops.json` : `"category": "Armes"` sur l'article. Les joueurs voient des onglets (Tous, une par rubrique, Autres) |
| Tester | **Aperçu** ouvre la boutique comme un joueur |

### Assigner une boutique à une entité

N'importe quelle entité : villageois, armor stand, PNJ d'un autre mod, cadre, animal…

- **Outil de liaison** (bouton dans le panneau, ou `/shops linker <id>`) : clic droit sur l'entité = assigner,
  accroupi + clic droit = retirer.
- **Commande** : `/shops assign @e[type=villager,limit=1,sort=nearest] <id>` — `/shops unassign <entités>`.
- `/shops info <entité>` : quelle boutique est liée.
- Admin accroupi + clic droit (main vide) sur un vendeur : ouvre directement l'édition de sa boutique.

Par défaut, l'entité devient invulnérable, immobile (IA coupée) et ne despawn plus. Retirer la boutique restaure ces
réglages. Le tout est configurable.

### Licence requise (mod MineNorth Permis)

Une boutique peut exiger une licence de `minenorth_permis` (ex. port d'armes pour l'armurerie). Sans licence valide
(possédée et non expirée), le joueur peut **consulter** la boutique mais ne peut ni acheter ni vendre : les boutons sont
grisés, la licence s'affiche en rouge dans l'en-tête, et le serveur refuse toute transaction.

- Dans le panneau : bouton **Licence requise** de la boutique.
- En commande : `/shops licence <id> <licence>` (autocomplétion des licences), `/shops licence <id> aucune` pour retirer.
- La licence s'applique à toute la boutique, en vente comme en rachat.

`minenorth_permis` est une dépendance **optionnelle** (version 1.3.0 ou plus) : sans lui, le mod boutiques fonctionne
normalement. Par sécurité, une boutique qui exige une licence est **bloquée** si le mod permis est absent (on ne vend
pas d'armes à tout le monde à cause d'un jar manquant).

### Boutiques réservées aux pompiers

Liste **Accès → Pompiers**, ou `/shops pompier <id> <on|off>`. Seuls les membres des pompiers / SAMU (mod Secours, via MineNorth API)
peuvent ouvrir la boutique et y acheter ; les ops peuvent la consulter. Réservée **police** ou **pompiers**, pas les deux.
Sans le mod Secours, la boutique est **bloquée** (même principe que la police). `restrictions.pompierServiceRequis = true` exige en plus
d'être en service (tablette Secours).

### Boutiques réservées à la mairie ou à une entreprise

Dans le panneau d'édition, la liste **Accès** propose maintenant **Mairie**, **Entreprise** (tous les membres) et **Direction entr.** (patron et grades avec droit de gestion).
Pour une entreprise, choisissez-la dans la liste déroulante **Entreprise** (toutes les entreprises actives, défilement à la molette). Les **OP** passent toujours.

- **Mairie** : le maire élu et les agents qu'il a nommés dans la tablette de la mairie (mod État, via MineNorth API).
- **Entreprise** : les membres de l'entreprise choisie (mod Entreprises). Si l'entreprise est dissoute, la boutique est refusée à tous sauf aux OP.
- Commandes : `/shops mairie <id> <on|off>` ; `/shops entreprise <id> <nom|aucune> [direction]` (complétion des noms d'entreprise).
- Même principe de sécurité que police / pompiers : sans le mod État (ou Entreprises), la boutique est **bloquée**.
- Une boutique n'a qu'un seul accès réservé à la fois (police, pompiers, mairie ou entreprise).

### Boutiques illégales (deux entités, une seule présente à la fois)

Liste **Statut → Illégale**, ou `/shops illegal <id> on [minutes] [distance]`. Assignez ensuite **2 entités** à la boutique avec l'outil de
liaison (une à chaque emplacement). Toutes les `intervalleMinutes` (15 par défaut), celle qui est visible **disparaît** et l'autre **apparaît** ;
si un joueur est à moins de `distanceJoueurs` (10 blocs par défaut) de l'entité visible ou de l'emplacement suivant, le changement est
repoussé jusqu'à ce qu'il parte.

- L'entité cachée est retirée du monde (ses données complètes sont gardées dans `world/data/minenorth_shops_illegal.dat`) et recréée à son
  emplacement quand son tour revient. Une entité dont le chunk n'est pas chargé est traitée dès que le chunk se charge ; l'échéance (horloge
  réelle) survit aux redémarrages.
- Redevenue légale (ou rotation coupée), la boutique retrouve toutes ses entités.
- Avec une seule entité assignée, rien ne change (pas d'alternance).
- `/shops illegal <id> statut` : emplacements, actif, minutes avant le prochain changement. `/shops illegal <id> basculer` : changement tout de suite.
- Réglages par boutique : `minutes` (0 = config) et `distance` (-1 = config, 0 = jamais repoussé), aussi dans `shops.json`
  (`illegalMinutes`, `illegalRadius`).

### Boutiques prêtes à l'emploi (packs)

Le mod fournit le pack **`boutiques-minenorth`** : 14 boutiques remplies (magasins de vêtements par enseigne fictive, chaussures, sport,
tenues de travail, accessoires, animalerie, et compléments pour la boutique clandestine de drogue). Catalogue complet (ids et prix) : [BOUTIQUES.md](BOUTIQUES.md).

| # | Boutique | Contenu | Prix |
|---|---|---|---|
| 1 | Northside Streetwear | survêtements de marque, hype (box logo, rétro), denim, bombers | 30 – 180 € |
| 2 | Urban Basics | t-shirts, marinière, débardeur, chemises, sweats à capuche, shorts, joggings, cargos, jeans | 12 – 65 € |
| 3 | Maison Aurèle | luxe : pièces griffées, doudounes, manteaux, perfectos, cagoules griffées | 60 – 950 € |
| 4 | Atelier Valmont | costumes, smoking, costumes Miami, robes de soirée, cocktail, paillettes, moulante, souliers, talons | 90 – 520 € |
| 5 | Riviera Plage | robe d'été, bikinis, maillot une pièce, shorts de bain, chemises hawaïennes, tongs, claquettes | 15 – 390 € |
| 6 | Sneakers & Co | baskets (running, montantes, toile, chunky, luxe), bottines Chelsea, santiags, boots, rangers | 65 – 690 € |
| 7 | Sport Arena | maillots de foot et de basket, shorts, brassière, legging | 25 – 70 € |
| 8 | Boutique Lola | crop tops, top à bretelles, jupe plissée, mini-jupe en cuir, jean mom | 25 – 95 € |
| 9 | Grand Nord Outdoor | parkas fourrées, chapka, après-ski, bottes fourrées | 60 – 320 € |
| 10 | Pro Équipement | tenues de travail : BTP, garage, cuisine, salle (serveur), livraison | 15 – 75 € |
| 11 | Chapellerie du Nord | chapeaux, casquettes, bonnets, bobs | 20 – 160 € |
| 12 | Comptoir Accessoires | lunettes, bijoux, montres, sacs (dont sacs de rangement), gants, écharpes, bandanas, cagoules | 15 – 4 500 € |
| 13 | Animalerie Patte d'Or | croquettes (5 sortes), 8 accessoires d'animaux, collier d'identification | 8 – 50 € |
| 14 | Vendeur outils drogue | graines (cannabis, coca, éphédra, **pavot**), tentes, lampe UV, labo, tables, sachets | **illégale** |

Tous les objets du mod `minenorth-vetements` sont vendus quelque part (vérifié par le script), **sauf** la liste `RESERVES` de
`tools/gen_pack.py`, que le script interdit dans toutes les boutiques : tenues de service pompiers, SAMU, Police Nationale et RAID
(données par les mods métiers ou les OP) et objets réservés aux grades VIP. Le n° réel de chaque boutique est donné
par l'import (ils suivent les boutiques déjà présentes sur le serveur).

**Importer** (op niveau 2) : `/shops importer boutiques-minenorth` (`/shops importer` seul liste les packs).
- Une boutique **absente** (comparée par nom) est créée avec ses réglages et ses articles.
- Une boutique **du même nom déjà présente** reçoit seulement les articles qui lui manquent : ses prix, réglages et articles ne changent pas.
  Ex. sur le serveur, la boutique clandestine « Vendeur outils drogue » (#21) reçoit les graines de pavot, la grande tente et la table à rouler.
- Relancer l'import ne crée pas de doublon. Un objet d'un mod absent est ignoré (compté dans le message, détaillé dans les logs).
- Les packs sont dans `config/minenorth_shops/packs/` : le pack fourni y est copié s'il n'y est pas (jamais écrasé). Un pack se modifie
  à la main (même format que `shops.json`, prix en centimes, `id` inutile) ou se régénère : `python tools/gen_pack.py` (vérifie les ids
  dans les fichiers de langue des mods, réécrit le pack et `BOUTIQUES.md`).

**Mise à jour du 03/11/2026 (garde-robe et chaussures)** : le pack passe de 9 à 14 boutiques.
1. Copier le nouveau `config/minenorth_shops/packs/boutiques-minenorth.json` du paquet serveur (le jar ne remplace jamais un pack déjà présent
   dans `config/`), avec le nouveau `mods/minenorth-vetements-1.0.0.jar` (sinon les nouveaux objets sont ignorés à l'import).
2. `/shops importer boutiques-minenorth` : crée **Sneakers & Co**, **Sport Arena**, **Boutique Lola**, **Grand Nord Outdoor** et
   **Pro Équipement**, et ajoute leurs nouveaux articles à Urban Basics, Atelier Valmont, Riviera Plage et Comptoir Accessoires (prix et
   réglages existants inchangés). Le message indique combien de boutiques sont créées et d'articles ajoutés.
3. Poser un vendeur pour chacune des 5 nouvelles boutiques (ci-dessous). Pro Équipement peut être réservée à une entreprise (`company`).

**Poser un vendeur** (pour chaque boutique, au bon endroit) :
1. Faire apparaître un PNJ, par ex. `/summon villager ~ ~ ~ {VillagerData:{profession:"minecraft:nitwit"},CustomName:'"Maison Aurèle"'}`
   (ou n'importe quelle entité : armor stand, PNJ d'un autre mod…).
2. `/shops linker <n°>` puis clic droit sur le PNJ (ou `/shops assign @e[type=villager,limit=1,sort=nearest] <n°>`).
   Il devient invulnérable et immobile ; accroupi + clic droit avec l'outil le libère.
3. Boutique illégale : lier **deux** entités à deux endroits (une seule est présente à la fois, voir plus haut).
4. Vérifier : `/shops info @e[type=villager,limit=1,sort=nearest]`, puis clic droit en joueur.

### Pack `poissonnerie` : rachat des poissons Starcatcher

Pack **séparé** (fichier de config seulement, pas dans le jar) : `config/minenorth_shops/packs/poissonnerie.json`. Il rachète les
**114 poissons** du mod de pêche **Starcatcher** (`starcatcher:*`), un prix par poisson selon sa rareté, versé **sur le compte bancaire**
(pas d'espèces). Une boutique a au plus 54 articles, d'où **trois comptoirs** à poser côte à côte :

| Boutique | Rubriques | Prix par poisson |
|---|---|---|
| Poissonnerie - Courants | Commun (27) · Peu commun (26) | 15 € · 25 € |
| Poissonnerie - Rares | Rare (23) · Épique (23) | 45 € · 80 € |
| Poissonnerie - Légendaires | Légendaire (15) | 180 € |

- Non rachetés : les déchets de pêche (bottes, algues séchées, coquillages, pince de crabe), les trophées, les modèles de forge.
- Taille et poisson **doré** : Starcatcher les note sur l'objet, mais la boutique ne compare pas les données d'un article sans NBT,
  donc **tout poisson d'une espèce est payé au même prix** (pas de bonus). Pas de limite par jour non plus (`limites.lotsMax` seulement).
- Changer les prix : modifier `PRIX` dans `tools/gen_poissonnerie.py` puis `python -I tools/gen_poissonnerie.py` (lit les fiches de poissons
  dans le jar Starcatcher), **avant** l'import. Après l'import, les prix se changent dans le panneau `/shops` (un nouvel import n'écrase pas).
- Installer : `/shops importer poissonnerie`, puis lier un PNJ à chacune des trois boutiques (voir « Poser un vendeur »).
- Icône sur la carte (`minenorth_map`) : créer le point de la carte avec un nom contenant « Rachat de poisson » ou « Acheteur de poisson »
  → pastille dorée d'acheteur avec le poisson (`rachat_poisson`).

### Pack `musique` : magasin d'instruments « La Note Bleue »

Pack **séparé** (fichier de config seulement, pas dans le jar) : `config/minenorth_shops/packs/musique.json`. Une boutique qui **vend**
les objets du mod `minenorth-musique` (espèces ou carte), 19 articles :

| Rubrique | Articles | Prix |
|---|---|---|
| Guitares | électrique sunburst, noire, rouge, bleue, blanche · Flying V métal · acoustique · classique | 220 – 650 € |
| Basses & claviers | basse électrique · piano numérique · synthétiseur | 420 – 900 € |
| Cordes & vents | violon · saxophone | 380 – 600 € |
| Batterie | batterie (bloc) · baguettes | 25 – 1 200 € |
| Scène & son | micro · enceinte portable · jukebox | 90 – 1 500 € |
| Vinyles | disque vinyle | 15 € |

- La **guitare Or (VIP)** n'est pas vendue : elle est réservée aux grades VIP et retirée aux autres joueurs.
- Changer les prix ou les articles : modifier `RAYONS` dans `tools/gen_musique.py` puis `python -I tools/gen_musique.py` (vérifie les ids
  dans `MusiqueItems.java`), **avant** l'import.
- Installer : `/shops importer musique`, puis lier un PNJ à la boutique (voir « Poser un vendeur »).

### Pack `surplus_noir` : vendeur illégal de tenues tactiques (ZeroContact)

Pack **séparé** (fichier de config seulement, pas dans le jar) : `config/minenorth_shops/packs/surplus_noir.json`. Deux boutiques
**illégales**, **espèces seulement**, qui vendent les tenues et l'équipement du mod `zerocontact` (une boutique a au plus 54 articles) :

| Boutique | Rubriques | Prix |
|---|---|---|
| Surplus Noir - Tenues (49) | Uniformes (14) · Gilets pare-balles (18, prix selon la classe de protection) · Casques & cagoules (17) | 60 – 2 800 € |
| Surplus Noir - Équipement (46) | Plaques balistiques · Masques à gaz · Porte-chargeurs · Sacs à dos · Casques radio · Vision nocturne · Brassards · Accessoires (kit de réparation, radios, piles…) | 20 – 4 500 € |

- **Pas de matériel de la Police Nationale** (uniforme, brassard, béret, JPC PN, masque tactique PN) : le script refuse ces ids.
  Ni munitions, ni plaque d'identité, ni œuf de raider.
- Changer les prix ou les articles : modifier `BOUTIQUES` dans `tools/gen_surplus.py` puis `python -I tools/gen_surplus.py` (vérifie les ids
  dans les sources de `ZeroContact_MineNorthRP`), **avant** l'import.
- Installer : `/shops importer surplus_noir`, puis lier **deux** entités à chaque boutique (deux planques, une seule présente à la fois,
  voir « Boutiques illégales »). Les deux boutiques peuvent partager les mêmes planques avec deux PNJ côte à côte.

### Pack `clandestin` : production de drogue et outils de vol

Pack **séparé** (fichier de config seulement) : `config/minenorth_shops/packs/clandestin.json`. Deux boutiques **illégales** :

| Boutique | Rubriques | Paiement |
|---|---|---|
| Vendeur outils drogue (30) | Graines (cannabis, coca, éphédra, pavot, spores) · Ingrédients (produit chimique, huile de sassafras, ergot, sachets) · Culture (tentes, terreau, engrais, lampes UV/LED, arrosoir, goutte-à-goutte) · Labo (labo, four, séchoir, tables, presse à pilules, extracteur, filtre à charbon) · Coupe (table de coupe, bicarbonate, paracétamol, caféine) · Santé (naloxone) | espèces ou carte |
| Le Fourgue (15) | Voitures (tournevis, crochets, kit, outil électronique, fausses plaques) · Maisons (crochet de porte) · Radars (pirate de radar) · Banque (perceuses niv. 1-3, outil hacker) · Discrétion (cagoules, bandana, gants) | espèces seulement |

- « Vendeur outils drogue » porte le **même nom** que la boutique clandestine du serveur (#21) : l'import lui ajoute seulement les articles
  qui lui manquent (ses prix et réglages ne changent pas). Les drogues finies ne sont pas vendues : il faut les produire.
- Changer les prix ou les articles : modifier `BOUTIQUES` dans `tools/gen_clandestin.py` puis `python -I tools/gen_clandestin.py` (vérifie les
  ids dans les sources des mods drogue, vol, portes, police, eurobank et vêtements), **avant** l'import.
- Installer : `/shops importer clandestin`, puis lier **deux** entités au Fourgue (deux planques, voir « Boutiques illégales »).

### Pack `meubles` : magasins de meubles (Yuushya, Immersive Furniture, nos blocs)
Généré par `tools/gen_meubles.py` (`python -I tools/gen_meubles.py [dossier des mods]`), qui **vérifie chaque id** dans les jars de Yuushya et d'Immersive Furniture (dossier `mods` du serveur, puis instances CurseForge, puis `.minecraft`) et dans les sources de nos mods, et s'arrête s'il en manque un. Trois enseignes (54 articles au plus, rubriques de 20 caractères, prix en centimes) :
- **Maison Nordik** (54) : petits prix façon grande enseigne suédoise : tables et commodes en bois brut, cadres de lit et literies, tapis, luminaires, rideaux, salle de bain.
- **Atelier Prestige** (50) : haut de gamme : canapé, fauteuil, table en marbre, vitrine, multimédia, cuisine équipée, luminaires design, climatisation, pianos, jukebox, billard, coffres-forts, et l'établi d'Immersive Furniture (« Sur mesure » : les meubles d'Immersive Furniture sont des créations faites à l'établi, on vend l'établi et le matériau).
- **Brocante du Port** (54) : vintage : tourne-disque et vinyles, tableaux anciens, petits objets, meubles anciens, plantes.
Yuushya n'a pas de traduction française : chaque article porte son **nom français en NBT** (`display.Name`). Le même script écrit `config/Minenorth-portes/deco-catalogue.json` (application Déco du téléphone, mod Portes) avec en plus les **collections VIP / VIP+**, jamais vendues en magasin. Import : `/shops importer meubles`, puis poser un vendeur par magasin.

### Autres commandes

- `/shops list` — liste des boutiques.
- `/shops importer [pack]` — importe un pack de boutiques prêtes à l'emploi (voir ci-dessus).
- `/shops edit <id>` — ouvre l'édition.
- `/shops licence <id> <licence|aucune>` — licence exigée pour commercer.
- `/shops police <id> <tous|policier|officier|commissaire>` — boutique réservée à la police.
- `/shops pompier <id> <on|off>` — boutique réservée aux pompiers / secours.
- `/shops illegal <id> <on|off> [minutes] [distance]` | `statut` | `basculer` — boutique illégale (voir ci-dessus).
- `/shops open <id> [joueurs]` — ouvre une boutique sans entité (blocs de commande, PNJ d'autres mods qui exécutent
  des commandes…).

## Côté joueur

Clic droit sur le vendeur → écran de boutique : espèces et solde affichés, liste des articles (survol de l'icône =
infobulle), choix du nombre de lots (− / + / Max), puis **Payer en espèces** / **Payer par carte**
(ou **Vendre (espèces)** / **Vendre (compte)** en rachat). La monnaie est rendue automatiquement.

Règles de paiement (celles de l'API EuroBank) : par carte, il faut un compte, **sa propre carte** dans l'inventaire et un
solde suffisant. En rachat, les outils abîmés ne sont pas repris ; si l'article a du NBT, l'objet doit être identique.

## Application « Boutiques » du téléphone MRPhone (02/11)
Icône framboise **Boutiques** du téléphone patché (`minenorth-phone`, `JobsHook`, constante `BOUTIQUES`), visible pour tous : annuaire des boutiques **dans l'écran du téléphone** (`client/phone/PhoneShopsScreen`, compilé contre `libs/mrphone-0.9.4.jar` en `compileOnly`).
- Onglets **Magasins** (vente) / **Rachat** ; recherche par nom de boutique ou d'article ; catalogue (icône, nom, lot, prix, rubriques), moyens de paiement, licence / accès réservé, distance du vendeur et bouton **Itinéraire sur la carte** (repère + guidage du mod `minenorth_map`, par réflexion).
- **Consultation seulement** : on achète / vend toujours sur place, auprès du vendeur. Jamais les boutiques **illégales**, ni celles réservées à un métier que le joueur n'a pas (mêmes règles `PompierCompat` / `PoliceCompat` / `AccessCompat`).
- Serveur : `PhoneDirectory` (anti-martelage 0,5 s ; emplacement = premier vendeur lié parmi les entités chargées, sinon « emplacement inconnu »). Paquets `PhoneShopsPacket.Request` / `Listing` ; **protocole réseau 5 → 6** (jar à mettre à jour serveur ET clients).
- **Non testé en jeu.**

## Configuration

`world/serverconfig/minenorth_shops-server.toml`

```toml
[entites]
    protegerEntites = true   # invulnérable à l'assignation
    figerEntites = true      # IA coupée à l'assignation
    distanceMax = 8.0        # distance max joueur ↔ vendeur
[achats]
    lotsMax = 64             # lots max par transaction
[restrictions]
    pompierServiceRequis = false   # boutiques pompiers : exiger d'être en service
[illegal]
    actif = true             # false = plus aucune rotation
    intervalleMinutes = 15   # minutes entre deux changements d'emplacement
    distanceJoueurs = 10.0   # un joueur plus près repousse le changement (0 = jamais)
    verificationSecondes = 1 # fréquence des contrôles
```

Toutes les boutiques sont dans **`config/minenorth_shops/shops.json`** (un seul fichier, modifiable à la main).
Il est créé au premier lancement à partir des boutiques existantes (`world/data/minenorth_shops.dat`), réécrit à chaque
modification en jeu et **rechargé automatiquement** quand tu le modifies (pas de redémarrage). Si le JSON est invalide,
les boutiques en mémoire sont conservées et une copie `shops.json.broken-…` est faite.

```json
{ "nextId": 2, "shops": [ {
  "id": 1, "name": "Armurerie", "mode": "SELL", "allowCash": true, "allowCard": true,
  "licence": "port_armes", "policeGrade": -1,
  "entries": [ { "id": 1, "item": "minecraft:iron_sword", "nbt": "{Damage:0}", "quantity": 1, "price": 1500 } ]
} ] }
```
Prix en centimes (1500 = 15,00 €). `nbt` est optionnel (SNBT). Un article dont l'objet est inconnu est ignoré (log).

## Notes techniques

- Tout est validé côté serveur : session liée à l'entité (présente, toujours liée, à portée), prix attendu renvoyé
  par le client (refus si un admin a changé le prix entre-temps), permission op revérifiée à chaque action admin.
- L'id de la boutique est stocké dans les données persistantes de l'entité (`minenorth_shops:shop`).
- En rachat, le versement sur compte utilise `BankApi.refund`. Un alias `BankApi.deposit(p, cents)` dans EuroBank
  serait plus lisible.

### Boutique réservée à la police (mod MineNorth Police)

Une boutique peut être réservée aux policiers enregistrés (`/police grade`), avec un grade minimum :

| Réglage | Qui peut l'ouvrir et acheter |
|---|---|
| Police : non | tout le monde (défaut) |
| Police : oui | tout policier (Sous-officier et +) |
| Police : off.+ | Officier et Commissaire |
| Police : comm. | Commissaire uniquement |

- Dans le panneau : bouton **Police : …** de la boutique (chaque clic passe au réglage suivant).
- En commande : `/shops police <id> <tous|policier|officier|commissaire>`.
- Un non-policier qui clique sur le PNJ reçoit « Cette boutique est réservée à la police. » et rien ne s'ouvre.
  Les ops peuvent l'ouvrir pour la consulter, mais pas y acheter sans grade.
- Cumulable avec la licence et le choix espèces / carte.
- Par sécurité, une boutique police est **bloquée** si le mod Police est absent.

## Licence

**Tous droits réservés - MineNorthRP.** Réutilisation, copie, modification, décompilation / ingénierie
inverse (y compris par outils d'intelligence artificielle) et utilisation pour entraîner une IA sont
**interdites** sans autorisation écrite. Voir [LICENSE](LICENSE).

Annuaire du téléphone (02/11, suite) : les **boutiques d'entreprise** (`companyId` ≠ 0 ou entreprise nommée) n'y sont plus, même pour leurs membres : gérées par les joueurs, elles restent hors de l'annuaire public (comme le marché noir).

- 04/11 : pack `clandestin`, « Le Fourgue » : rubrique **Pistage** avec la balise espion (`minenorthvol:balise_espion`, 600 €).
