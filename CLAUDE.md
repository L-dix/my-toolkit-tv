# Toolkit TV — notes pour Claude

Fork de teja2495/my-toolkit-tv, personnalisé pour une seule TV. Lire ce fichier en début de session.

## L'utilisateur
- TV **Philips sous Android TV 14** (Google TV), télécommande. Pas de PC, un **iPhone** : tout passe par la TV et GitHub.
- Répondre **en français**, court. Les tokens comptent : pas de diagnostics à répétition ni de photos d'écran à demander sans raison.
- Il teste sur la TV et renvoie une photo si ça ne va pas. Proposer avant de coder quand le choix lui revient.

## Compiler et livrer
- Pas de SDK Android dans le conteneur cloud : **c'est la CI qui compile**. Après chaque push, vérifier le résultat du workflow `Release APK` (une erreur Kotlin = pas de mise à jour pour la TV).
- Pousser sur le remote **`fork`** (`L-dix/my-toolkit-tv`, branche `main`), pas sur `origin` (le dépôt d'origine, refusé).
- Chaque push sur `main` publie la release `v1.0.<n°_de_run>` avec `toolkit-tv.apk` et `toolkit-installer.apk`. Lien fixe : https://github.com/L-dix/my-toolkit-tv/releases/latest/download/toolkit-tv.apk
- Signature : secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` (sert aussi de mot de passe de la clé), `ANDROID_KEY_ALIAS`.
- **Mise à jour sur la TV** : ouvrir l'appli *Toolkit Installer* › « Mettre à jour Toolkit ». Elle installe par session PackageInstaller : sans ça, Android 13+ bloque l'accessibilité d'une appli installée depuis un fichier APK (« paramètres restreints »).

## Fonctionnement
- Le service d'accessibilité (`core/ToolkitAccessibilityService.kt`) fait de Toolkit l'accueil : interception de Home, appui long = réglages rapides, très long = accueil Google TV. Il gère aussi « Forcer l'arrêt » automatique et la minuterie de veille.
- Réglages dans `LauncherPrefs` (SharedPreferences « launcher »), menu Options (`ui/OptionsDialog.kt`), sauvegarde des réglages en code texte / QR (`TK1:`).
- Raccourcis (maintien sur l'accueil) :
  - **1** = ouvre Kick là où on l'a laissé ;
  - **2** = bordure arc-en-ciel ;
  - **3** = playlist YouTube.
  - Valeurs par défaut dans `data/Shortcuts.kt`.

## Déjà essayé, ne pas refaire
- **Kick** (`com.kick.mobile`) :
  - aucun lien ne l'ouvre sur une chaîne (`kick.com/<nom>`, `/videos`, `kick://…`) ;
  - ses éléments ne sont pas cliquables par l'accessibilité, et les clics simulés (gestes) étaient trop aléatoires ;
  - la lecture du live dans VLC (API non documentée `kick.com/api/v2/channels/<nom>`) n'a pas marché sur la TV.
  - Choix de l'utilisateur : la touche 1 ouvre simplement Kick là où on l'a laissé (sans `FLAG_ACTIVITY_CLEAR_TASK`). Ne plus y toucher.
- **Économiseur d'écran / anti-burn / Ambilight** :
  - la TV ne laisse pas une appli lancer son économiseur ;
  - le jumelage avec l'API JointSpace de Philips (Ambilight) échoue par délai dépassé.
  - Tout a été retiré à la demande de l'utilisateur, qui garde l'économiseur de la TV.
- **TalkBack** s'active si on se trompe de service dans Accessibilité : le désactiver là-même, la synthèse vocale se coupe dans les réglages de la TV.
