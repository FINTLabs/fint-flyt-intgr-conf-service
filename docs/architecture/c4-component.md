# C4 nivå 3: Komponenter

Komponentene i `fint-flyt-intgr-conf-service`. Hvert domene er en egen pakke under
`no.novari.flyt.catalog` med egen persistence unit, egen feilhåndterer og egen OpenAPI-gruppe.

## Diagram

```mermaid
C4Component
    title Komponenter i fint-flyt-intgr-conf-service

    Component(valueconverting, "Value-converting", "catalog.valueconverting", "Verdikonverteringer, historikk")
    ContainerQueue_Ext(kafka, "Kafka", "Delt klynge", "tenant.flyt.*")
    System_Ext(clients, "Klienter", "Frontend, gateways, mapping-service")
    Component(discovery, "Discovery", "catalog.discovery", "Integrasjons- og instansmetadata")
    Component(integration, "Integration", "catalog.integration", "Integrasjoner, aktivering")
    Component(configuration, "Configuration", "catalog.configuration", "Konfigurasjoner, mapping, historikk")
    Component(security, "Sikkerhet", "flyt-security-starter", "JWT, tilgang per kildeapplikasjon")
    Component(persistence, "Persistens", "catalog.database", "Én persistence unit per skjema")
    System_Ext(authorization, "authorization-service", "Tilgang og brukernavn")
    ContainerDb(db, "fint-flyt", "PostgreSQL", "Ett skjema per domene")

    Rel(clients, valueconverting, "HTTPS")
    Rel(clients, kafka, "Metadata")
    Rel(clients, discovery, "HTTPS")
    Rel(clients, integration, "HTTPS")
    Rel(clients, configuration, "HTTPS")
    Rel(kafka, discovery, "Metadata")
    BiRel(integration, configuration, "Oppslag")
    Rel(configuration, discovery, "Metadata")
    Rel(security, authorization, "HTTPS")
    Rel(persistence, db, "JDBC")

    UpdateRelStyle(clients, valueconverting, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(clients, kafka, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(clients, discovery, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(clients, integration, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(clients, configuration, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(kafka, discovery, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(integration, configuration, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(configuration, discovery, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(security, authorization, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(persistence, db, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateElementStyle(valueconverting, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(kafka, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(clients, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(discovery, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(integration, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(configuration, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(security, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(persistence, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(authorization, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(db, $bgColor="#C4B5FD", $fontColor="#1F2937", $borderColor="#8B5CF6")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

Blå er komponenter i applikasjonen, turkis er eksternt og lilla er databasen. Diagrammet viser komponentene med
kontakt utenfor applikasjonen. Alle fire domenene går gjennom Persistens og Sikkerhet, og Validering
og Audit brukes av flere domener. De er beskrevet under [Tverrgående](#tverrgående).

## Oppslag mellom domenene

Domenene kaller hverandres tjenesteklasser direkte og bruker hverandres DTO-er. Ingen av
oppslagene går over nettverket.

```mermaid
sequenceDiagram
    autonumber
    actor Bruker as Flyt-frontend
    participant CC as ConfigurationController
    participant CVF as ConfigurationValidatorFactory
    participant IS as IntegrationService
    participant IMS as IntegrationMetadataService
    participant IC as IntegrationController
    participant IUVS as IntegrationUpdateValidationService
    participant IVF as IntegrationValidatorFactory
    participant CS as ConfigurationService

    Bruker->>CC: POST /konfigurasjoner
    CC->>CVF: getValidator(integrationId, metadataId)
    CVF->>IS: findById(integrationId)
    CVF->>IMS: getById(metadataId)
    CVF-->>CC: Validator med integrasjon, metadata og verditype per nøkkel
    CC->>CS: save(...)

    Bruker->>IC: PATCH /integrasjoner/{id} med activeConfigurationId
    IC->>IUVS: validate(integrationId, oppdatert integrasjon)
    IUVS->>IVF: getPatchValidator(integrationId, configurationId)
    IVF->>CS: findById(configurationId, excludeMapping = true)
    IVF-->>IUVS: Validator med konfigurasjonen
    IC->>IS: updateById(...)
```

Mangler integrasjonen, metadataen eller instansmetadataen, avvises konfigurasjonen med en egen
feil for hver av dem. Aktiveringen avvises hvis konfigurasjonen ikke finnes, ikke er fullført eller
hører til en annen integrasjon.

## Integration

Pakke: [`no.novari.flyt.catalog.integration`](../../src/main/kotlin/no/novari/flyt/catalog/integration)

Integrasjonene: hvilken kildeapplikasjon og integrasjons-ID de gjelder, destinasjonen, om de er
aktive og hvilken konfigurasjon som er aktiv.

| Klasse                                           | Ansvar                                                              |
| ------------------------------------------------ | ------------------------------------------------------------------- |
| `api.IntegrationController`                      | `/api/intern/integrasjoner`, med tilgangssjekk per kildeapplikasjon |
| `application.IntegrationService`                 | Oppslag, opprettelse og oppdatering                                 |
| `application.IntegrationUpdateValidationService` | Validerer en endring før den lagres                                 |
| `validation.IntegrationValidatorFactory`         | Bygger validatoren med konfigurasjonen fra Configuration            |
| `persistence.IntegrationRepository`              | Spring Data-repository mot integration-skjemaet                     |
| `web.GlobalExceptionHandler`                     | Problem Details for `IntegrationController`                         |

Persistence uniten har Hibernates bean-validering ved lagring slått av. Valideringen skjer i
API-laget: med `@Valid` ved opprettelse og i `IntegrationUpdateValidationService` ved endring.

## Configuration

Pakke: [`no.novari.flyt.catalog.configuration`](../../src/main/kotlin/no/novari/flyt/catalog/configuration)

Konfigurasjonene: hvilken integrasjon og metadataversjon de gjelder, om de er fullført, og
mappingtreet fra instansfelter til destinasjonens felter.

| Klasse                                     | Ansvar                                                                      |
| ------------------------------------------ | --------------------------------------------------------------------------- |
| `ConfigurationController`                  | `/api/intern/konfigurasjoner`                                               |
| `ConfigurationHistoryController`           | `/history` og `/{id}/history`                                               |
| `ConfigurationService`                     | Oppslag, lagring med versjonering, oppdatering og sletting                  |
| `ConfigurationHistoryService`              | Envers-historikk                                                            |
| `mapping.*`                                | Mapping mellom entiteter og DTO-er for hele mappingtreet                    |
| `validation.ConfigurationValidatorFactory` | Bygger validatoren med integrasjon og metadata fra Integration og Discovery |
| `validation.ValidatorConfiguration`        | Domenets egen `ValidatorFactory`, `configurationHibernateValidatorFactory`  |
| `validation.constraints.*`                 | Regler for mappingtreet, nøkler og verdityper                               |
| `ApiExceptionHandler`                      | Feilsvar for de to kontrollerne                                             |

Domenets `ValidatorFactory` brukes både av `ConfigurationValidatorFactory` og av Hibernate ved
lagring i configuration sin persistence unit. MVC-valideringen og de andre domenene bruker Spring
Boots `defaultValidator`.

Endepunktene sjekker roller på det interne API-et, men ikke tilgang per kildeapplikasjon.

## Value-converting

Pakke: [`no.novari.flyt.catalog.valueconverting`](../../src/main/kotlin/no/novari/flyt/catalog/valueconverting)

Verdikonverteringene: tabeller som oversetter verdier fra kildeapplikasjonen til verdier
destinasjonen forstår.

| Klasse                                                 | Ansvar                                                        |
| ------------------------------------------------------ | ------------------------------------------------------------- |
| `api.ValueConversionController`                        | `/api/intern/value-convertings`, med filtrering og paginering |
| `api.ValueConversionHistoryController`                 | `/history` og `/{id}/history`                                 |
| `application.ValueConversionService`                   | Oppslag, opprettelse, erstatning og sletting                  |
| `domain.ValueConversionHistoryService`                 | Envers-historikk                                              |
| `infrastructure.persistence.ValueConversionRepository` | Spring Data-repository med spesifikasjoner for filtrering     |
| `api.ValueConversionExceptionHandler`                  | Problem Details for de to kontrollerne                        |

## Discovery

Pakke: [`no.novari.flyt.catalog.discovery`](../../src/main/kotlin/no/novari/flyt/catalog/discovery)

Metadataen kildeapplikasjonene publiserer: én versjon per endring av en integrasjon, med
instansmetadata som beskriver feltene, kategoriene og objektsamlingene i en instans.

| Klasse                                                | Ansvar                                            |
| ----------------------------------------------------- | ------------------------------------------------- |
| `IntegrationMetadataController`                       | `/api/intern/metadata`                            |
| `IntegrationMetadataService`                          | Oppslag og lagring av metadataversjoner           |
| `kafka.IntegrationMetadataEventConsumerConfiguration` | Topic og feilhåndtering for metadatahendelsene    |
| `kafka.IntegrationMetadataEventHandler`               | `@KafkaListener`, lagrer en ny versjon én gang    |
| `mapping.*`                                           | Mapping mellom entiteter og DTO-er                |
| `IntegrationMetadataRequestExceptionHandler`          | Feilsvar for `IntegrationMetadataController`      |

Lagringen fra hendelsene er idempotent: `IntegrationMetadataEventHandler` sjekker om versjonen
finnes, og databasen har en unik constraint på kildeapplikasjon, integrasjons-ID og versjon.

## Persistens

Pakke: [`no.novari.flyt.catalog.database`](../../src/main/kotlin/no/novari/flyt/catalog/database),
pluss `<Domene>PersistenceConfig` i hver domenepakke.

```mermaid
flowchart LR
    subgraph app["fint-flyt-intgr-conf-service"]
        own["OwnSchemaPersistenceConfig<br/>@Primary, ingen entiteter"]
        int["IntegrationPersistenceConfig"]
        conf["ConfigurationPersistenceConfig<br/>Envers"]
        vc["ValueConvertingPersistenceConfig<br/>Envers"]
        disc["DiscoveryPersistenceConfig"]
    end
    subgraph db["fint-flyt"]
        s0[("fint_flyt_intgr_conf_service_db")]
        s1[("fint_flyt_integration_service_db")]
        s2[("fint_flyt_configuration_service_db")]
        s3[("fint_flyt_value_converting_service_db")]
        s4[("fint_flyt_discovery_service_db")]
    end
    own --> s0
    int --> s1
    conf --> s2
    vc --> s3
    disc --> s4
```

Hvert domene har sin egen `DataSource`, `EntityManagerFactory`, `PlatformTransactionManager` og
`Flyway`. Fem persistence units er nødvendig fordi `configuration_aud` og `value_converting_aud`
har fremmednøkkel til hver sin `revinfo`, og Envers tillater én revisjonsentitet per persistence
unit.

Primærkandidaten peker på tjenestens eget, tomme skjema. En `@Transactional`, `EntityManager` eller
et repository uten qualifier feiler dermed på manglende tabell i stedet for å treffe et
domeneskjema. `TransactionManagerBindingTest` sjekker at hver `@Transactional` i et domene navngir
domenets transaksjonsmanager.

## Tverrgående

| Komponent  | Hvor                                                                            | Innhold                                                                        |
| ---------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Validering | [`catalog.validation`](../../src/main/kotlin/no/novari/flyt/catalog/validation) | `ValidationErrorsFormattingService` og `HibernateConstraintValidator`          |
| OpenAPI    | [`catalog.openapi`](../../src/main/kotlin/no/novari/flyt/catalog/openapi)       | Felles OpenAPI-oppsett. Hvert domene har sin egen gruppe i `<domene>.openapi`. |
| Sikkerhet  | `flyt-security-starter`, `catalog.security`                                     | Filterkjeder, roller per organisasjon og `AuthorizationService` (`FlytAccess`) |
| Audit      | `flyt-audit-starter`                                                            | Auditkolonner, Envers-historikk og visningsnavn for aktører                    |
| Kafka      | `fint-kafka-starter`, `flyt-kafka`                                              | Topic-navn, `NewTopic` og feilhåndtering for metadatakonsumenten               |

Bibliotekene registrerer bønnene sine med auto-konfigurasjon, så `@SpringBootApplication` skanner bare
`no.novari.flyt.catalog`.

## Videre lesning

- [Systemkontekst](c4-context.md)
- [Containere](c4-container.md)
