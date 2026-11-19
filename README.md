# Pocket Black Screen 1.0

**Écoutez un podcast, une vidéo ou de la musique sans déclencher de commandes par accident.**

Activez Pocket Black Screen depuis les réglages rapides avant de glisser votre téléphone dans votre poche. Un écran noir opaque intercepte les appuis et les glissements dans la zone couverte. Deux appuis rapides au même endroit vous rendent la main.

- **Activation en un geste** depuis la tuile au cadenas, même quand aucun média ne joue.
- **Arrêt au choix** par double-appui, depuis la tuile ou via l’action **Désactiver** de la notification.
- **Durée réglable** de 30, 60, 120 ou 240 minutes.
- **Fonctionnement local**, sans publicité ni autorisation d’accès à Internet.

Le lecteur garde la main sur le son. La lecture peut dépendre de ses règles, de l’abonnement et du comportement d’Android lorsque l’écran s’éteint physiquement.

## Versions Android

| Version | État de la compatibilité |
| --- | --- |
| Android 11 et antérieurs (API 30 ou moins) | L’APK exige au minimum l’API 31. Android refuse l’installation sur ces versions. |
| Android 12 (API 31) | Installation vérifiée sur un émulateur Android. Le fonctionnement sur téléphone physique reste à valider. |
| Android 12L à Android 16 (API 32 à 36) | Versions admises par l’APK. Le code prévoit les adaptations d’Android 13 pour les notifications et d’Android 14 pour la tuile et le service. Les parcours complets restent à tester sur ces versions. |

`minSdk = 31` fixe la version minimale d’installation. `targetSdk = 36` indique que l’application cible les règles d’Android 16. Sur Xiaomi et les autres appareils à interface constructeur, les réglages de superposition et le maintien de la protection peuvent varier. L’état des essais figure dans la [matrice de recette](RECETTE.md).

## Installer et configurer

Depuis le dépôt, générez l’APK debug avec `./gradlew :app:assembleDebug` à la racine du projet. Gradle crée `app/build/outputs/apk/debug/app-debug.apk`. Les prérequis sont détaillés dans [Projet Android Studio](#projet-android-studio).

1. Copiez l’APK sur le téléphone et ouvrez-le depuis le gestionnaire de fichiers. Si Android le demande, autorisez l’installation depuis cette source. Vous pouvez retirer cette autorisation après l’installation.
2. Ouvrez **Pocket Black Screen** et touchez **Configurer la superposition**. Si Android affiche une liste d’applications, choisissez **Pocket Black Screen**, puis activez **Autoriser l’affichage par-dessus d’autres applications**. De retour dans l’application, la ligne **Superposition** doit indiquer **Autorisé**.
3. Sur Xiaomi, si nécessaire, vérifiez les réglages propres à **Pocket Black Screen** dans **Paramètres → Applications → Gérer les applications → Pocket Black Screen → Autres autorisations**. Selon la version de MIUI, la rubrique peut mentionner les fenêtres flottantes ou les fenêtres contextuelles en arrière-plan.
4. Autorisez **État du téléphone** si l’appareil dispose de la téléphonie. Vérifiez que les notifications de l’application et la catégorie **Protection tactile** sont actives. À partir d’Android 13, accordez l’autorisation de notification si l’application la demande.
5. Déployez les réglages rapides ou le Centre de contrôle, ouvrez **Modifier** (crayon), puis ajoutez la tuile **Pocket Black Screen**. Sur les versions de MIUI avec un Centre de contrôle séparé, glissez depuis le coin supérieur droit. Si la tuile ouvre les réglages de l’application, vérifiez les lignes qui affichent **À configurer**.

Avec ADB déjà configuré pour une session de développement, on peut aussi installer l’APK par USB avec `adb install -r PocketBlackScreen-v1.0-debug.apk`. L’application n’a besoin ni d’ADB ni du débogage USB pour fonctionner.

## Utiliser et récupérer la main

- Démarrer la lecture dans son lecteur, si souhaité, puis toucher la tuile. Le petit cadenas gris disparaît après trois secondes. Un nouvel appui le fait revenir temporairement. Deux appuis brefs et proches, espacés d’au plus 300 ms, désactivent la protection au relâchement du deuxième appui. Les appuis longs, les glissements et les séquences à plusieurs doigts sont absorbés.
- Si le panneau système est accessible, toucher la tuile active ou l’action **Désactiver** de la notification. La touche Marche/Arrêt reste utilisable. Elle permet de suspendre la couche lors de l’extinction, mais la session peut reprendre après le déverrouillage.
- Si la couche gêne encore l’utilisation, ouvrir les réglages Android par les commandes système et choisir **Forcer l’arrêt** dans la fiche de l’application quand cette fiche est accessible. Si le panneau et les réglages restent inaccessibles, utiliser le menu système de Marche/Arrêt pour redémarrer. Si le menu est inaccessible ou si le téléphone ne répond pas, utiliser le redémarrage forcé du Redmi, en maintenant la touche Marche/Arrêt jusqu’au redémarrage. La protection ne se réactive jamais au boot. Vérifier les sorties de secours sur le Redmi avec la matrice avant un usage quotidien.

## Compatibilité et limites

| Comportement | Mise en œuvre et conditions |
| --- | --- |
| Zone couverte | `TYPE_APPLICATION_OVERLAY`, fond `#000000` opaque, saisie tactile absorbée. Android contrôle la zone accessible et l’empilement des fenêtres. Barres, panneau de réglages rapides, écran de verrouillage, interfaces sensibles et gestes système restent soumis au système. Une application peut demander à Android de masquer les fenêtres de superposition. Android peut déplacer, redimensionner ou masquer la fenêtre. |
| Activation | La tuile est la seule entrée qui active une session. Une activité transparente, exclue des applications récentes, devient visible brièvement pour replier le panneau et lancer le service au premier plan. Selon le lecteur et MIUI, le passage par cette activité peut affecter la reprise de la vidéo. L’application ne prend pas le focus audio, ne lance aucun média et ne modifie pas la luminosité. |
| Extinction et réveil | `SCREEN_OFF` retire la fenêtre. Sur `SCREEN_ON` et `USER_PRESENT`, le service vérifie que l’écran est interactif et que le verrouillage a pris fin avant toute reprise. L’application ne maintient pas l’écran allumé et ne demande aucun réveil. Si MIUI retarde ou omet le signal, la session peut rester suspendue. Dans ce cas, arrêter la session depuis la tuile ou la notification, puis retoucher la tuile après déverrouillage. Si MIUI tue le processus, la session se termine. |
| Appels | Sur API 31+, `TelephonyCallback.CallStateListener` surveille la sonnerie mobile, y compris sur les abonnements SIM actifs connus du système. La couche se retire à l’état `RINGING`. La détection dépend des événements de la pile téléphonique et du maintien du service par MIUI. Les appels d’applications VoIP et certains modes opérateur peuvent suivre une autre voie. Vérifier chaque SIM et les modes opérateur utilisés sur le Redmi. Les interfaces téléphoniques et d’urgence protégées restent contrôlées par Android. |
| Délai | Durée de 30, 60, 120 ou 240 minutes. La valeur par défaut est 120 minutes et un changement s’applique à la session suivante. L’échéance utilise `elapsedRealtime()` et inclut les périodes de veille. Vérification sur le thread principal et lors des transitions de l’écran, avec une alarme inexacte de secours compatible Doze. Android peut différer l’alarme ou la livraison des événements. Le délai reste vérifié avant toute reprise et au premier appui après l’échéance. |
| Notification | La notification est requise au démarrage comme deuxième moyen d’arrêt. Une notification ordinaire, une batterie faible ou l’absence de lecture audio ne ferment pas la protection. Le système décide de l’accès au panneau et peut suspendre ou arrêter le service selon sa politique. |
| MIUI | Si MIUI coupe le service pendant une session, vérifier, dans la fiche de l’application, le réglage de batterie de Pocket Black Screen. Tester **Aucune restriction** pour cette application uniquement si la configuration par défaut coupe une session volontaire. L’application ne demande pas d’exemption de batterie et n’a besoin d’aucun démarrage automatique. |
| Audio | Pocket Black Screen ne demande pas d’accès aux médias. La lecture après extinction physique de l’écran ou lors d’un changement de fenêtre dépend du lecteur, de l’abonnement éventuel et d’Android. |

Références Android pour les fenêtres, les appels, la tuile, le démarrage des services et les alarmes. [WindowManager](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY) · [TelephonyCallback](https://developer.android.com/reference/android/telephony/TelephonyCallback.CallStateListener) · [TileService](https://developer.android.com/reference/android/service/quicksettings/TileService) · [Services au premier plan](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) · [Alarmes](https://developer.android.com/develop/background-work/services/alarms/schedule)

## Autorisations

| Autorisation | Usage |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Autorisation spéciale pour poser la fenêtre noire. |
| `READ_PHONE_STATE` | Demande uniquement sur les appareils déclarant la téléphonie. Sert à détecter la sonnerie et à suivre les identifiants d’abonnements actifs. Le code ne lit aucun numéro, contact ou contenu d’appel. |
| `POST_NOTIFICATIONS` | Demande à partir d’Android 13 pour rendre visible la commande de secours. |
| `FOREGROUND_SERVICE` | Maintient le service pendant la session et affiche sa notification. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | À partir d’Android 14, décrit le service de protection tactile, temporaire et déclenché par l’utilisateur. Le sous-type est déclaré dans le manifeste. Sur Android 12 et 13, la promotion du service utilise le type historique `NONE`. |

L’application fonctionne localement. Elle ne demande ni accès Internet, ni service d’accessibilité, ni accès aux notifications d’autres applications. Les réglages de durée restent sur l’appareil. L’état de session, son jeton et son échéance vivent en mémoire. Les journaux consignent l’opération technique et le type d’erreur.

## Projet Android Studio

Ouvrir ce dossier dans Android Studio avec le SDK Android 36, les Build Tools 36.0.0 et un JDK 17 ou compatible (par exemple JDK 21). Le wrapper Gradle 8.13 utilise AGP 8.13.2 et Kotlin 2.2.21. `minSdk` vaut 31 et `compileSdk` et `targetSdk` valent 36. Pour une compilation en ligne de commande, régler `ANDROID_HOME` ou laisser Android Studio créer `local.properties`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # avec un appareil ou émulateur Android 12+
```

Sous Windows, utiliser `gradlew.bat`. Le build debug produit `app/build/outputs/apk/debug/app-debug.apk`.

### Organisation

- `MainActivity` et `SettingsRepository` affichent les autorisations et conservent la durée. `PocketTileService` et l’activité transparente gèrent la demande d’activation.
- `PocketService` possède l’état, les ressources et les échéances. Les événements sont sérialisés sur le thread principal. Les séquences admises passent par `INACTIVE → STARTING → ACTIVE` ou `SUSPENDED_FOR_LOCK`, avec des allers-retours possibles entre les deux derniers états. L’arrêt passe par `STOPPING` puis `INACTIVE`. Les échecs d’initialisation suivent aussi ce chemin de nettoyage.
- `OverlayController` gère l’attachement, le fond noir et le cadenas. `DoubleTapDetector` valide les deux gestes et efface l’historique si la taille ou la configuration change. La fenêtre reste la même pendant les rotations hors suspension.
- `DeviceStateMonitor` et `CallStateMonitor` écoutent uniquement pendant une session. La notification et l’alarme pointent vers un récepteur privé avec des `PendingIntent` immuables associés à un jeton différent à chaque session. Le service annule les deux à l’arrêt et ne reprend pas après la mort du processus.

Les cas T01 à T18 et les observations de vérification figurent dans la [matrice de recette](RECETTE.md).
