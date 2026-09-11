apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: $NAMESPACE

resources:
  - ../../../base

labels:
  - pairs:
      app.kubernetes.io/instance: $APP_INSTANCE
      fintlabs.no/org-id: $ORG_ID

patches:
  - patch: |-
      - op: replace
        path: "/spec/kafka/acls/0/topic"
        value: "$NAMESPACE.flyt.*"
      - op: replace
        path: "/spec/orgId"
        value: "$ORG_ID"
      - op: replace
        path: "/spec/ingress/routes/0/path"
        value: "$CONTEXT_PATH/api/intern/value-convertings"
      - op: replace
        path: "/spec/ingress/routes/1/path"
        value: "$CONTEXT_PATH/api/intern/metadata"
      - op: add
        path: "/spec/env/-"
        value:
          name: "novari.flyt.web-resource-server.security.api.internal.authorized-org-id-role-pairs-json"
          value: |
            {
$ROLE_PAIRS
            }
      - op: add
        path: "/spec/env/-"
        value:
         name: "novari.kafka.topic.orgId"
         value: "$NAMESPACE"
      - op: add
        path: "/spec/env/-"
        value:
         name: "server.servlet.context-path"
         value: "$CONTEXT_PATH"
      - op: replace
        path: "/spec/probes/startup/path"
        value: "$CONTEXT_PATH/actuator/health"
      - op: replace
        path: "/spec/probes/readiness/path"
        value: "$CONTEXT_PATH/actuator/health/readiness"
      - op: replace
        path: "/spec/probes/liveness/path"
        value: "$CONTEXT_PATH/actuator/health/liveness"
      - op: replace
        path: "/spec/observability/metrics/path"
        value: "$CONTEXT_PATH/actuator/prometheus"

    target:
      kind: Application
      name: fint-flyt-intgr-conf-service
