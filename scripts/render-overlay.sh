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
additional_org_ids_patch() {
  local org_id="$1"
  case "$org_id" in
    afk.no | bfk.no | ofk.no)
      cat <<'EOF_PATCH'

      - op: add
        path: "/spec/env/-"
        value:
          name: "novari.flyt.security.tenant.additional-org-ids"
          value: "viken.no,frid-iks.no"
EOF_PATCH
      ;;
    *) printf '' ;;
  esac
}

# Secreten til oauth2-klienten i ra-no bruker andre nøkkelnavn enn i de øvrige namespacene.
authorization_sso_patches() {
  local namespace="$1"
  case "$namespace" in
    ra-no)
      cat <<'EOF_PATCH'

      - op: replace
        path: "/spec/env/1/valueFrom/secretKeyRef/key"
        value: "fint.flyt.authorization.sso.client-id"
      - op: replace
        path: "/spec/env/2/valueFrom/secretKeyRef/key"
        value: "fint.flyt.authorization.sso.client-secret"
EOF_PATCH
      ;;
    *) printf '' ;;
  esac
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
  export ADDITIONAL_ORG_IDS_PATCH="$(additional_org_ids_patch "$ORG_ID")"
  export AUTHORIZATION_SSO_PATCHES="$(authorization_sso_patches "$namespace")"

  tmp="$(mktemp)"
  envsubst < "$BASE_TEMPLATE" > "$tmp"
  mv "$tmp" "$file"
done < <(find "$ROOT/kustomize/overlays" -name kustomization.yaml -print | sort)
