# C4 nivå 2: Containere

Katalogtjenesten består av én applikasjon og én database. Kafka er en delt plattformtjeneste.

## Diagram

```mermaid
C4Container
    title Containere i katalogtjenesten

    System_Ext(services, "FLYT-tjenester", "Gateways og mapping-service")
    System_Ext(frontend, "Flyt-frontend", "Brukergrensesnitt")
    ContainerQueue_Ext(kafka, "Kafka", "Delt klynge", "tenant.flyt.*")
    Container(app, "fint-flyt-intgr-conf-service", "Kotlin, Spring Boot 4.2", "Internt API for fire domener")
    System_Ext(authorization, "authorization-service", "Tilgang og brukernavn")
    ContainerDb(db, "fint-flyt", "PostgreSQL bak PgBouncer", "Fem skjemaer per tenant")

    Rel(frontend, app, "/api/intern", "HTTPS")
    Rel(services, app, "/api/intern-klient", "HTTPS")
    Rel(services, kafka, "Metadata")
    Rel(kafka, app, "Metadata")
    Rel(app, db, "JPA, Flyway", "JDBC")
    Rel(app, authorization, "Tilgang, navn", "HTTPS")

    UpdateRelStyle(frontend, app, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(services, app, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-60", $offsetY="-10")
    UpdateRelStyle(services, kafka, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(kafka, app, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(app, db, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(app, authorization, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateElementStyle(frontend, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(services, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(kafka, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(app, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(authorization, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(db, $bgColor="#C4B5FD", $fontColor="#1F2937", $borderColor="#8B5CF6")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

Blå er katalogtjenesten, lilla er databasen og turkis er eksternt. Tjenesten henter også JWKS og klienttoken fra FINT IdP.

## fint-flyt-intgr-conf-service

Spring Boot-applikasjon som samler de fire katalogdomenene. Den bygges til et distroless
Java 25-image og rulles ut som en Flaiserator-`Application` i namespacet til hver tenant.

| Egenskap  | Verdi                                                                                           |
| --------- | ----------------------------------------------------------------------------------------------- |
| Teknologi | Kotlin, Spring Boot 4.2, Spring Data JPA, Hibernate Envers, Flyway, springdoc                   |
| Utrulling | Kubernetes via Flaiserator, én instans per tenant i beta og i produksjon                        |
| Ressurser | 2048Mi minnetak, `-Xmx1536M`, 500m CPU-tak, 150m CPU-forespørsel                                |
| Kontekst  | `server.servlet.context-path` er `/{tenant}` i produksjon og `/beta/{tenant}` i beta            |
| Helse     | `/actuator/health`, `/actuator/health/readiness`, `/actuator/health/liveness`                   |
| Metrikker | `/actuator/prometheus`                                                                          |
| Konfig    | [kustomize/base/flais.yaml](../../kustomize/base/flais.yaml) og ett overlay per tenant og miljø |

### HTTP-API

Alt ligger under `/api/intern` og krever et brukertoken fra FINT IdP, utstedt til FLYT-frontend. Brukeren må
høre til tenanten eller en operatør (`vigo.no`, `novari.no`), se `flyt-security-starter`. Swagger UI og OpenAPI-spesifikasjonen ligger utenfor
ingress-rutene og nås bare med port-forward, se [README](../../README.md#openapi-dokumentasjon).

| Ressurs                         | Operasjoner                                                                                          |
| ------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `/api/intern/integrasjoner`     | `GET` liste, også paginert, `GET /{id}`, `POST`, `PATCH /{id}`                                       |
| `/api/intern/konfigurasjoner`   | `GET` liste, `GET /{id}`, `POST`, `PATCH /{id}`, `DELETE /{id}`, `GET /history`, `GET /{id}/history` |
| `/api/intern/value-convertings` | `GET` liste, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`, `GET /history`, `GET /{id}/history`   |
| `/api/intern/metadata`          | Fire `GET`-varianter på kildeapplikasjon, `GET /{id}/instans-metadata`, `POST`                       |

Hver ressurs har sin egen ingress-rute.

### Kafka

Topic-navnene bygges som `{tenant}.flyt.{type}.{ressurs}`.

| Topic                                 | Rolle      | Motpart          |
| ------------------------------------- | ---------- | ---------------- |
| `event.integration-metadata-received` | Konsumerer | Instans-gateways |

Consumer group er applikasjonsnavnet, med `auto.offset.reset=earliest`. Metadatahendelsen er
idempotent i to lag: en `exists`-sjekk i applikasjonen og en unik constraint på kildeapplikasjon,
integrasjons-ID og versjon.

## fint-flyt-database

PostgreSQL-databasen `fint-flyt`, felles for Flyt-tjenestene i en tenant. Katalogtjenesten bruker
fem skjemaer med felles tenant-prefiks:

| Skjema                                  | Innhold                                                      |
| --------------------------------------- | ------------------------------------------------------------ |
| `fint_flyt_integration_service_db`      | Integrasjoner                                                |
| `fint_flyt_configuration_service_db`    | Konfigurasjoner, mappingtrær, Envers-revisjoner og `revinfo` |
| `fint_flyt_value_converting_service_db` | Verdikonverteringer, Envers-revisjoner og `revinfo`          |
| `fint_flyt_discovery_service_db`        | Integrasjonsmetadata og instansmetadata                      |
| `fint_flyt_intgr_conf_service_db`       | Tjenestens eget skjema, tomt                                 |

Databasebrukeren opprettes av Flaiserator etter applikasjonsnavnet, og prefikset utledes fra den.
Databasen nås gjennom PgBouncer i transaksjonsmodus, så Hibernate kvalifiserer alle tabellnavn
med skjemaet i stedet for å stole på `search_path`.

Hvert domeneskjema har sin egen Flyway-historikk. Flyway validerer ved oppstart, og
migreringsfilene må ikke endres. Se [README](../../README.md#flyway).

## Bygg og utrulling

| Workflow                                      | Når                                       | Hva                                                         |
| --------------------------------------------- | ----------------------------------------- | ----------------------------------------------------------- |
| [CI](../../.github/workflows/CI.yaml)         | Pull request og push til `main`           | Bygger imaget, og publiserer det ved push til `main`        |
| [CD](../../.github/workflows/CD.yaml)         | Når CI-bygget er ferdig                   | Utrulling til alle tenants i begge klyngene                 |
| [MD](../../.github/workflows/MD.yaml)         | Manuelt                                   | Bygg og utrulling av valgt branch til én tenant i én klynge |
| [CodeQL](../../.github/workflows/codeql.yaml) | Pull request, push til `main` og ukentlig | Statisk sikkerhetsanalyse                                   |

Beta kjører i `aks-beta-fint-2021-11-23` og produksjon i `aks-api-fint-2022-02-08`.

## Videre lesning

- [Systemkontekst](c4-context.md)
- [Komponenter](c4-component.md)
