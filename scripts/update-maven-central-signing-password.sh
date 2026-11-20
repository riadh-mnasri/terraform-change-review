#!/usr/bin/env bash
set -euo pipefail

readonly REPOSITORY="riadh-mnasri/terraform-change-review"
readonly KEY_FILE="${HOME}/.config/terraform-change-review/maven-central-signing.asc"

fail() {
    printf 'Erreur : %s\n' "$1" >&2
    exit 1
}

for command_name in gpg gh awk; do
    command -v "$command_name" >/dev/null 2>&1 || fail "commande manquante : ${command_name}"
done

[[ -s "$KEY_FILE" ]] || fail "clé privée exportée introuvable : ${KEY_FILE}"
gh auth status --hostname github.com >/dev/null 2>&1 || \
    fail 'connectez gh au compte ayant accès au dépôt (gh auth login)'

fingerprint="$(gpg --with-colons --import-options show-only --import "$KEY_FILE" 2>/dev/null | \
    awk -F: '$1 == "fpr" { print $10; exit }')"
[[ -n "$fingerprint" ]] || fail 'impossible de lire l’empreinte de la clé exportée'

read -r -s -p 'Saisissez la phrase secrète de cette clé GPG : ' signing_password
printf '\n'
[[ -n "$signing_password" ]] || fail 'phrase secrète vide'

if ! printf '%s' "$signing_password" | gpg --batch --yes --pinentry-mode loopback \
    --passphrase-fd 0 --local-user "$fingerprint" --armor --detach-sign \
    --output /dev/null /dev/null; then
    unset signing_password
    fail 'cette phrase ne déverrouille pas la clé locale; le secret GitHub n’a pas été modifié'
fi

printf '%s' "$signing_password" | \
    gh secret set MAVEN_CENTRAL_SIGNING_PASSWORD --repo "$REPOSITORY"
unset signing_password

if ! gh secret list --repo "$REPOSITORY" | awk '{ print $1 }' | \
    grep -qxF MAVEN_CENTRAL_SIGNING_PASSWORD; then
    fail 'GitHub ne liste pas le secret attendu'
fi

printf '%s\n' 'Phrase vérifiée localement et secret GitHub mis à jour.'
