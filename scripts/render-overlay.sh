#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$ROOT/kustomize/templates"
BASE_TEMPLATE="$TEMPLATE_DIR/overlay.yaml.tpl"

app_instance_suffix() {
  local namespace="$1"
  case "$namespace" in
    bym-oslo-kommune-no)
      printf '%s' "$namespace"
      ;;
    *)
      printf '%s' "${namespace//-/_}"
      ;;
  esac
}

# Beta ligger bak et eget path-prefiks i samme ingress-vert som produksjon.
env_prefix() {
  local environment="$1"
  case "$environment" in
    beta) printf '/beta' ;;
    *) printf '' ;;
  esac
}

# De tre tidligere Viken-fylkene slipper også inn brukere fra den nedlagte fylkeskommunen og
# frid-iks. Det følger ikke av namespacet, så det må stå her.
extra_user_org_ids() {
  local org_id="$1"
  case "$org_id" in
    afk.no | bfk.no | ofk.no) printf 'viken.no frid-iks.no' ;;
    *) printf '' ;;
  esac
}

role_pairs() {
  local org_id="$1"
  local indent='              '
  local user_org_id
  {
    for user_org_id in "$org_id" $(extra_user_org_ids "$org_id"); do
      printf '%s"%s":["USER"],\n' "$indent" "$user_org_id"
    done
    printf '%s"vigo.no":["DEVELOPER", "USER"],\n' "$indent"
    printf '%s"novari.no":["DEVELOPER", "USER"]' "$indent"
  }
}

while IFS= read -r file; do
  rel="${file#"$ROOT/kustomize/overlays/"}"
  dir="$(dirname "$rel")"

  namespace="${dir%%/*}"
  environment="${dir##*/}"

  export NAMESPACE="$namespace"
  export ORG_ID="${namespace//-/.}"
  export APP_INSTANCE="fint-flyt-intgr-conf-service_$(app_instance_suffix "$namespace")"
  export CONTEXT_PATH="$(env_prefix "$environment")/$namespace"
  export ROLE_PAIRS="$(role_pairs "$ORG_ID")"

  tmp="$(mktemp)"
  envsubst < "$BASE_TEMPLATE" > "$tmp"
  mv "$tmp" "$file"
done < <(find "$ROOT/kustomize/overlays" -name kustomization.yaml -print | sort)
