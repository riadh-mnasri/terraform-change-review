#!/usr/bin/env bash
set -euo pipefail

readonly REPOSITORY="riadh-mnasri/terraform-change-review"
readonly KEYSERVER="hkps://keyserver.ubuntu.com"
readonly KEY_DIRECTORY="${HOME}/.config/terraform-change-review"
readonly PRIVATE_KEY_FILE="${KEY_DIRECTORY}/maven-central-signing.asc"

fail() {
    printf 'Erreur : %s\n' "$1" >&2
    exit 1
}

for command_name in gpg gh awk grep; do
    command -v "$command_name" >/dev/null 2>&1 || fail "commande manquante : ${command_name}"
done

gh auth status --hostname github.com >/dev/null 2>&1 || \
    fail 'connectez gh au compte ayant accès au dépôt (gh auth login)'

secret_names="$(gh secret list --repo "$REPOSITORY" | awk '{ print $1 }')"
for required_secret in MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_PASSWORD; do
    if ! printf '%s\n' "$secret_names" | grep -qxF "$required_secret"; then
        fail "le secret GitHub ${required_secret} doit être configuré avant de lancer ce script"
    fi
done

if [[ -e "$PRIVATE_KEY_FILE" ]]; then
    fail "le fichier existe déjà : ${PRIVATE_KEY_FILE}; ne l'écrasez pas, utilisez la clé existante ou choisissez une sauvegarde sûre"
fi

printf '%s\n' 'Ce script va créer une clé GPG, publier sa clé publique sur Ubuntu Keyserver,' \
    'exporter la clé privée dans un fichier local protégé et envoyer la clé privée et sa phrase secrète' \
    'aux secrets GitHub Actions du dépôt.'
read -r -p 'Continuer ? [y/N] ' confirmation
[[ "$confirmation" == 'y' || "$confirmation" == 'Y' ]] || fail 'opération annulée'

printf '\n%s\n' 'Lancement de l’assistant GPG.' \
    'Choisissez RSA and RSA, une taille de 4096 bits, une expiration, puis définissez une phrase secrète.'
gpg --full-generate-key

read -r -p 'Saisissez l’empreinte ou l’identifiant de la clé que vous venez de créer : ' key_selector
[[ -n "$key_selector" ]] || fail 'identifiant de clé vide'

fingerprint="$(gpg --with-colons --list-secret-keys "$key_selector" 2>/dev/null | awk -F: '$1 == "fpr" { print $10; exit }')"
[[ -n "$fingerprint" ]] || fail 'aucune clé privée trouvée pour cet identifiant'
gpg --list-secret-keys "$fingerprint" >/dev/null 2>&1 || fail 'la clé sélectionnée est introuvable'

printf '\nEmpreinte sélectionnée : %s\n' "$fingerprint"
read -r -p 'Publier la clé publique sur keyserver.ubuntu.com ? [y/N] ' publish_confirmation
[[ "$publish_confirmation" == 'y' || "$publish_confirmation" == 'Y' ]] || fail 'opération annulée avant publication de la clé publique'

gpg --keyserver "$KEYSERVER" --send-keys "$fingerprint"

mkdir -p "$KEY_DIRECTORY"
chmod 700 "$KEY_DIRECTORY"
umask 077
gpg --armor --export-secret-keys "$fingerprint" > "$PRIVATE_KEY_FILE"
[[ -s "$PRIVATE_KEY_FILE" ]] || fail 'l’export de la clé privée est vide'
chmod 600 "$PRIVATE_KEY_FILE"

gh secret set MAVEN_CENTRAL_SIGNING_KEY --repo "$REPOSITORY" < "$PRIVATE_KEY_FILE"

read -r -s -p 'Saisissez la phrase secrète de la clé GPG (elle ne sera pas affichée) : ' signing_password
printf '\n'
[[ -n "$signing_password" ]] || fail 'phrase secrète vide'
printf '%s' "$signing_password" | gh secret set MAVEN_CENTRAL_SIGNING_PASSWORD --repo "$REPOSITORY"
unset signing_password

secret_names="$(gh secret list --repo "$REPOSITORY" | awk '{ print $1 }')"
for configured_secret in \
    MAVEN_CENTRAL_USERNAME \
    MAVEN_CENTRAL_PASSWORD \
    MAVEN_CENTRAL_SIGNING_KEY \
    MAVEN_CENTRAL_SIGNING_PASSWORD; do
    if ! printf '%s\n' "$secret_names" | grep -qxF "$configured_secret"; then
        fail "GitHub ne liste pas le secret attendu : ${configured_secret}"
    fi
done

printf '\nConfiguration de signature terminée.\n'
printf 'Clé privée conservée localement dans : %s\n' "$PRIVATE_KEY_FILE"
printf '%s\n' 'Conservez ce fichier et la phrase secrète dans un endroit sûr. Le script n’a pas publié le package.'
