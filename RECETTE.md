# Recette de Pocket Black Screen 1.0

## Redmi Note 10 Pro · Android 12

Exécuter sur le téléphone cible et renseigner pour chaque ligne le comportement constaté et l’un des statuts **Non exécuté**, **Réussi** ou **Échoué**. Les résultats d’émulateur figurent dans une section distincte. Les cases physiques sont initialisées à **Non exécuté**.

Relever la version Android et son correctif, la version et la région MIUI, les SIM et modes opérateur sans numéro, ainsi que le lecteur et sa version.

| ID | Préconditions | Étapes | Résultat attendu | Critère de réussite | Résultat observé | Statut |
| --- | --- | --- | --- | --- | --- | --- |
| T01 | APK installé, tuile ajoutée, Android déverrouillé. | Refuser une permission, toucher la tuile, puis l’accorder. Retoucher la tuile depuis une autre application. | La configuration s’ouvre si une permission manque. Une fois configurée, la tuile affiche une fenêtre noire et indique l’état actif. | Aucune protection avec un prérequis manquant. Apparition de la couche et état actif après activation. | — | Non exécuté |
| T02 | Session inactive. | Ouvrir l’icône, sélectionner 60 min, fermer et relancer. Rétablir 120 min. | Les paramètres français s’ouvrent et la durée choisie est conservée. L’écran n’est pas recouvert. | Bon réglage après relance, aucun bouton d’activation et aucune session ouverte. | — | Non exécuté |
| T03 | Toutes les lectures à l’arrêt. | Activer depuis la tuile. | La couche apparaît sans lecture audio. | Le fond couvre la zone applicative puis le double-appui retire la couche. | — | Non exécuté |
| T04 | Appareil déverrouillé, écran allumé. | Activer, chronométrer le cadenas, attendre, toucher une fois, puis attendre. | Le cadenas s’affiche seul sur le noir, disparaît après environ 3 s et revient temporairement au premier appui. | Les deux disparitions sont observées et la protection reste active. | — | Non exécuté |
| T05 | Page derrière la couche contenant un bouton et une liste défilable. | Toucher le bouton une fois, glisser, maintenir le doigt et poser deux doigts. Désactiver par double-appui. | Les événements dans la zone couverte ne déclenchent rien derrière et les gestes invalides ne désactivent pas. | La liste et l’état du bouton derrière sont identiques après l’arrêt. | — | Non exécuté |
| T06 | Couche active, pointeurs à l’écart des bords système. | Faire deux appuis brefs au même endroit en 300 ms maximum. Toucher ensuite un contrôle du lecteur. | Le fond est retiré au deuxième relâchement. La tuile redevient inactive. | Le contrôle reçoit dès le geste suivant un appui normal. | — | Non exécuté |
| T07 | Couche active. | Effectuer deux appuis brefs au même endroit à plus de 300 ms d’intervalle, puis un glissement et un appui long. | La protection reste active dans les trois cas. | Aucun déverrouillage fortuit. | — | Non exécuté |
| T08 | Rotation automatique autorisée, application supportant portrait et paysage, lecture vidéo en cours. | Activer en portrait. Tourner en paysage puis dans les deux sens. | La zone applicative est recouverte aux nouvelles dimensions. | Pas de fuite visuelle perceptible, de scintillement perceptible ou de crash. L’orientation de l’application derrière reste libre. | — | Non exécuté |
| T09 | Audio en cours, protection active. | Appuyer sur Volume + et Volume −. | Android modifie le volume et la session reste active. | Les deux modifications sont audibles ou visibles dans l’interface système. | — | Non exécuté |
| T10 | Protection active, extinction par bouton disponible. | Appuyer sur Marche/Arrêt pour éteindre puis réveiller l’écran. | Extinction et réveil normaux. Pas de réveil forcé ni d’interface système masquée. | Le bouton et l’interface de verrouillage restent utilisables. | — | Non exécuté |
| T11 | État du téléphone autorisé, session active, autre téléphone disponible. | Faire sonner chaque SIM, puis refaire avec le téléphone cible verrouillé. Essayer les modes opérateur utilisés. | La protection se termine dès que l’état de sonnerie est livré. Après l’appel, elle reste arrêtée. | Les commandes d’appel sont utilisables et la tuile reste inactive à la fin. | — | Non exécuté |
| T12 | Code PIN ou biométrie configurés, échéance non atteinte. | Activer. Éteindre puis réveiller, vérifier l’écran verrouillé, déverrouiller. Refaire en PIN et en biométrie. | La couche est absente sur les écrans d’authentification. Elle revient si Android publie le déverrouillage et confirme un écran interactif déverrouillé. | PIN et biométrie utilisables. Reprise constatée pour chaque parcours ou anomalie MIUI documentée. | — | Non exécuté |
| T13 | Canal Protection tactile actif, couche active. | Ouvrir le panneau système. Vérifier qu’une notification ordinaire ne change pas l’état. Appuyer sur **Désactiver**. Réactiver et essayer l’arrêt via la tuile. Sur une autre session, essayer le redémarrage de secours. | Notification et tuile arrêtent la fenêtre si le panneau est accessible. Le redémarrage coupe la session. | Chaque chemin accessible rend le tactile normal et la tuile inactive. Noter explicitement l’accessibilité du panneau. | — | Non exécuté |
| T14 | Durée de 120 min. | Activer et relever l’heure monotone ou chronométrer. Vérifier avant et après 120 min, puis refaire avec une veille couvrant l’échéance. | La protection se termine autour de l’échéance selon l’ordonnancement Android. Elle ne reprend pas après expiration. | Pas d’arrêt prématuré observé. Couche absente après traitement de l’échéance ou à la première transition de réveil. | — | Non exécuté |
| T15 | Réglage modifié, protection active. | Redémarrer puis déverrouiller et ouvrir la tuile et les paramètres. | Aucune reprise. Réglage conservé. | Zone tactile normale, état inactif et même durée qu’avant le redémarrage. | — | Non exécuté |
| T16 | Batterie près du seuil bas et téléphone débranché. | Activer avant l’avertissement système de batterie basse et attendre son affichage. | Aucune fermeture propre à Pocket Black Screen. Les politiques de batterie Android et MIUI peuvent s’appliquer. | La notification de batterie n’arrête pas la couche à elle seule. Documenter séparément une mise en veille ou un arrêt système. | — | Non exécuté |
| T17 | Lecture audio en cours et lecteur configuré normalement. | Activer, écouter avec l’écran allumé et la couche noire pendant au moins une minute, puis désactiver. Refaire avec les lecteurs utilisés. | Pas de pause ni de prise du focus audio due à la protection. | L’écoute se poursuit et les contrôles du lecteur sont utilisables après la désactivation. | — | Non exécuté |
| T18 | Rotation automatique et lecteur compatible. | Faire un premier appui, pivoter pendant un geste, toucher une fois dans la nouvelle orientation, attendre, puis faire un double-appui valide. | La rotation annule le geste en cours. Le nouvel appui seul ne ferme pas la couche. | Protection inchangée après la rotation et le geste isolé, puis désactivation normale après le double-appui. | — | Non exécuté |

## Vérifications de compilation

- Build debug et APK de tests instrumentés compilés avec Gradle 8.13 et JDK 17.
- 15 tests unitaires réussis. Ils couvrent le geste, les limites temporelles et spatiales, l’annulation sur glissement, appui long ou multitouch, l’échéance monotone et les transitions concurrentes.
- Android Lint a signalé zéro erreur et trois avis de mise à jour des outils de build ou de test.
- Signature APK debug v2 vérifiée avec `apksigner` pour API 31+. Métadonnées vérifiées avec `aapt2` (`fr.pocketblackscreen`, version 1.0, API minimale 31 et cible 36).
- L’archive du projet a été extraite dans un dossier distinct. Le build debug et les 15 tests unitaires ont réussi depuis cette copie.
- L’alignement de l’APK distribué a été vérifié avec `zipalign`.

## Essai sur émulateur Android 12

Installation de l’APK debug réussie sur l’image AOSP x86-64 sous Android 12 (API 31). Le gestionnaire de paquets est devenu utilisable après le redémarrage du processus système de l’émulateur en mode logiciel.

Le processus système de l’émulateur a ensuite redémarré de nouveau pendant l’installation du paquet de tests. Les quatre tests instrumentés sont compilés et restent à exécuter. Ils vérifient l’ouverture des réglages sans activation, le rendu noir opaque, la consommation des gestes et l’annulation du double-appui après un changement de pointeurs. Les essais d’interface et la recette Redmi restent à effectuer.
