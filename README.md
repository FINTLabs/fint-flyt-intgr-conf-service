# FINT Flyt Integration Configuration Service

Spring Boot-tjeneste som samler katalogdomenet i FINT Flyt — integration, configuration,
value converting og discovery — i én utrullbar enhet.

Arkitekturen er beskrevet med C4-diagrammer i [docs/architecture](docs/architecture/README.md).

## Pakkestruktur

Ett Gradle-modul med én pakke per domene under `no.novari.flyt.catalog`:

- `no.novari.flyt.catalog.valueconverting`
- `no.novari.flyt.catalog.discovery`
- `no.novari.flyt.catalog.integration`
- `no.novari.flyt.catalog.configuration`

Pakkeskillet er et krav, ikke en preferanse: hvert domene har sitt eget databaseskjema, og
tjenesten trenger derfor én persistence unit per skjema. `@EnableJpaRepositories` velger
repositories per pakke, så hvert domene må ligge i sin egen pakke.

Underpakkene er ikke like på tvers av domenene:

- value-converting har `api`, `application`, `domain` og `infrastructure`.
- integration har `api`, `application`, `kafka`, `persistence`, `validation` og `web`.
- discovery og configuration har controller, tjeneste og repository i domenets toppakke, med
  blant annet `kafka`, `mapping`, `model` og `validation` under.

Ved siden av domenepakkene ligger tre tverrgående pakker: `database` med oppsettet de fem
persistence unitene deler, `openapi` med felles OpenAPI-oppsett, og `validation` med
hjelpeklassene for validering som alle domenene bruker.

### Oppslag mellom domenene

Integration, configuration og discovery slår opp i hverandre med vanlige kall mellom
tjenesteklassene:

- Når en integrasjon aktiveres, henter `IntegrationValidatorFactory` konfigurasjonen fra
  `ConfigurationService`.
- Når en konfigurasjon valideres, henter `ConfigurationValidatorFactory` integrasjonen fra
  `IntegrationService` og metadataen fra `IntegrationMetadataService`.

Domenene bruker hverandres DTO-er direkte.

### Validering

Configuration-domenet har sin egen `ValidatorFactory`, `configurationHibernateValidatorFactory`.
Den brukes bare av configuration, både for konfigurasjonsvalideringen og for Hibernates validering
ved lagring i configuration sin persistence unit. MVC-valideringen og de andre domenene bruker
Spring Boots `defaultValidator`.

Integration sin persistence unit har Hibernates bean-validering ved lagring slått av.

### Komponentskanning

`@SpringBootApplication` navngir pakkene den skanner. `flyt-web-resource-server` og
`no.novari:kafka` registrerer bare deler av bønnene sine gjennom auto-konfigurasjon — resten er
`@Service`-klasser som forutsetter at konsumenten skanner bibliotekets egen pakke. Pakkene står
oppført eksplisitt, framfor å skanne hele `no.novari`.

## Persistering

Hvert domene har sitt eget skjema i `fint-flyt`-databasen per tenant. Fire persistence units er
påkrevd fordi `configuration_aud` og `value_converting_aud` hver har en ekte fremmednøkkel til en
`revinfo`-tabell i sitt eget skjema, og Envers tillater kun én revisjonsentitet per persistence
unit.

Hvert domene har derfor sin egen `DataSource`, `EntityManagerFactory`, `PlatformTransactionManager`
og `Flyway`, satt opp i `<Domene>PersistenceConfig` i domenepakken. De fire er likestilte — ingen av
dem er primærkandidat. Kun configuration og valueconverting registrerer revisjonsentiteten fra
`flyt-audit-starter`, slik at hver av dem løser `revinfo` mot sitt eget skjema.

### Primærkandidaten er tjenestens eget skjema

Spring Boots autokonfigurasjon krever én `@Primary` `DataSource` og `EntityManagerFactory`.
Den rollen har `OwnSchemaPersistenceConfig`, som peker på tjenestens eget skjema — det pgerator
oppretter etter applikasjonsnavnet. Skjemaet er tomt og ubrukt, og persistence uniten har ingen
entiteter.

At det er tomt er poenget. En `@Transactional`, en `EntityManager` eller et repository som glemmer
sin qualifier faller tilbake på primærkandidaten. Peker den på et domeneskjema, leser eller skriver
feilen stille mot ekte data; peker den på det tomme skjemaet, feiler den på manglende tabell med én
gang. Valget gjør også de fire domenene symmetriske, framfor at ett av dem er privilegert av en
vilkårlig grunn.

Skulle katalogen senere få tabeller som deles av alle domenene — eller de fire skjemaene bli
konsolidert — er det dette skjemaet de hører hjemme i, og primærkandidaten peker allerede dit.

### Skjemanavn

Skjemanavnene hører hjemme i konfigurasjon, ikke i kode: hver persistence unit setter
`hibernate.default_schema`, og entitetene har ingen `@Table(schema = ...)`. Hibernate kvalifiserer
dermed alle tabellnavn med skjemaet, også Envers-tabellene og `revinfo`.

Det holder ikke å la `search_path` velge skjemaet. Databasen nås gjennom en tilkoblingspooler i
transaksjonsmodus (derav `prepareThreshold=0` i URL-en), der en serverforbindelse deles mellom
klienter per transaksjon. `search_path` er sesjonstilstand og følger derfor ikke med fra
`hikari.schema` til spørringen — den treffer i stedet det forrige klient etterlot, eller rollens
standard, som er tjenestens eget, tomme skjema. `hikari.schema` er beholdt for ren JDBC, men
ingenting i applikasjonen skal være avhengig av den. `ProductionLikeDatabaseTest` kjører
applikasjonen gjennom PgBouncer med databasebruker, skjemaprefikser og grants som i drift.

Alle fem skjemaene deler tenant-prefiks, så prefikset utledes fra databasebrukeren flaiserator
setter, ved å fjerne tjenestens eget skjemanavn fra den. Utrullingen trenger dermed ingen
per-tenant-konfigurasjon utover det overlayene allerede setter. `novari.flyt.catalog.database.schema-prefix`
overstyrer utledningen — tom streng gir uprefiksede skjemanavn, som er det lokal utvikling og
testene bruker.

### Flyway

Migreringene ligger under én katalog per domene i `src/main/resources/db/migration/`, med én
`Flyway`-bønne per skjema. Hvert skjema har sin egen `flyway_schema_history`, så versjonsnumrene
kolliderer ikke selv om alle fire har en `V1__init.sql`. Ved oppstart validerer Flyway mot
historikken, og applikasjonen verifiserer dermed at den snakker med det skjemaet den tror.

**De eksisterende `.sql`-filene må aldri endres.** Flyway sammenligner `script` og `checksum` mot
radene som allerede står i `flyway_schema_history`; avviker de, stopper oppstarten.

Checksummen beregnes linje for linje over filinnholdet. Linjeskift, final newline og blanke linjer
påvirker den derfor ikke, men **trailing whitespace gjør det** — og det er nettopp den slags endring
en editor gjør uoppfordret. `.editorconfig` slår av `trim_trailing_whitespace` for katalogen, og
`insert_final_newline` i tillegg, slik at filene forblir byte-identiske.

`src/test/resources/legacy-flyway-checksums.txt` holder checksummene som står i
`flyway_schema_history` i drift, og en test sammenligner filene mot dem. Endres en migrering,
feiler CI med filnavn og begge verdiene. Den filen skal ikke oppdateres for å få testen grønn — da
er invarianten brutt, og migreringen skal tilbakestilles.

## Kafka

Topic-konfigurasjon opprettes og endres når konsument-bønnene bygges, altså ved oppstart.

Consumer group er `${fint.application-id}`, med `auto.offset.reset=earliest` som er bibliotekets
default. Meldinger kan derfor bli behandlet på nytt. Request/reply-konsumentene er rene lesninger,
og et avspilt svar kan ikke matches mot en pågående forespørsel fordi correlation-ID-ene er UUID-er.

Topic-navnet bygges av org-id og domenekontekst, ikke av applikasjons-ID:

| Topic                                                                                | Klient                                                                  |
|--------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `request.value-converting.by.value-converting-id`                                    | mapping-service                                                         |
| `request.integration.by.source-application-id-and-source-application-integration-id` | gateways bygget på flyt-gateway-starter eller flyt-web-instance-gateway |
| `request.active-configuration-id.by.integration-id`                                  | mapping-service                                                         |
| `request.mapping.by.configuration-id`                                                | mapping-service                                                         |
| `event.integration-metadata-received`                                                | acos-gateway og gateways bygget på flyt-gateway-starter                 |

`event.integration-metadata-received` er den eneste hendelsen katalogen konsumerer, og den eneste
kontrakten der avspillingen har en skrivende effekt. Avspillingen er ufarlig fordi konsumet er
idempotent i to lag: `IntegrationMetadataEventHandler` sin `exists`-sjekk og den unike constrainten
på `(source_application_id, source_application_integration_id, version)` i `V1__init.sql`.

## OpenAPI-dokumentasjon

Swagger UI og OpenAPI-spesifikasjonen er bare tilgjengelig ved direkte tilgang til tjenesten. Stiene
ligger bevisst utenfor ingress-rutene for det interne API-et:

- `/swagger-ui.html`
- `/v3/api-docs`
- `/v3/api-docs.yaml`

Hvert domene har sin egen gruppe, som velges i Swagger UI. Felles informasjon og sikkerhetsoppsett
ligger i `no.novari.flyt.catalog.openapi`; gruppene ligger i domenepakkene.

I FINTLabs beta, port-forward Kubernetes-servicen:

```shell
kubectl -n fintlabs-no port-forward service/fint-flyt-intgr-conf-service 8080:8080
```

Dokumentasjonen er da tilgjengelig på:

- `http://localhost:8080/beta/fintlabs-no/swagger-ui.html`
- `http://localhost:8080/beta/fintlabs-no/v3/api-docs`

Dokumentasjonen krever ikke bearer-token over port-forward. Kall mot det dokumenterte interne
API-et krever fortsatt gyldig JWT.

## Kjøre lokalt

Forutsetninger:

- Java 25
- Docker (for Docker Compose og for Testcontainers i testene)

`docker-compose.yaml` starter PostgreSQL og Kafka. Postgres-containeren monterer
`scripts/local-postgres-init.sql`, som oppretter tjenestens eget skjema slik pgerator gjør i drift.
Skriptet kjører bare når datavolumet er tomt — har du et volum fra før, trengs
`docker compose down -v` én gang.

```shell
docker compose up -d                                    # PostgreSQL på 5441, Kafka på 9192
./gradlew check                                         # ktlint + tester
SPRING_PROFILES_ACTIVE=local-staging ./gradlew bootRun  # kjører på port 8095
```

## Utrulling

- `kustomize/base/` — `Application`-ressursen for fint-flyt-intgr-conf-service.
- `kustomize/overlays/<org>/<env>/` — genereres per tenant og miljø.
- `kustomize/templates/overlay.yaml.tpl` — envsubst-malen alle overlays rendres fra.

Regenerer overlays etter endringer i malen:

```shell
./scripts/render-overlay.sh
```

Applikasjonsnavnet gir databasebrukeren, og dermed skjemaet, navnet sitt: flaiserator oppretter
én `PGUser` per applikasjon, og skjemaet følger mønsteret `{tenant}_{applikasjonsnavn}_db` i databasen `fint-flyt`.

Overlayene setter ingress-rute, `server.servlet.context-path`, probe- og metrics-stier, Kafka-acl og
autoriserte org-id-er per tenant. Alt utledes av namespace og miljø, med to unntak som står i
`render-overlay.sh`: de tre tidligere Viken-fylkene slipper også inn brukere fra `viken.no` og
`frid-iks.no`, og oauth2-secreten i `ra-no` bruker andre nøkkelnavn enn de øvrige namespacene.

## Låsing av avhengigheter

Bygget bruker [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html), slik at hvert bygg løser opp nøyaktig de samme direkte og transitive versjonene. De låste versjonene ligger i de committede `*.lockfile`-filene: `gradle.lockfile` for prosjektets avhengigheter og de andre for Gradle-plugins. En PR viser derfor alle transitive versjonsendringer den fører med seg.

**Automatisk (Dependabot):** Dependabot oppdaterer lockfilene i sine egne PR-er, så det trengs ingen manuelle steg. Hvis to Dependabot-PR-er kommer i konflikt i en lockfile, rebaser Dependabot den gjenværende etter at den første er merget. Skjer ikke det, kommenter `@dependabot recreate` på PR-en.

**Manuelt:** Alle endringer du gjør for hånd som påvirker oppløsningen av avhengigheter, krever at lockfilene regenereres. Det gjelder versjoner, `extra[...]`-properties, constraints, nye eller fjernede avhengigheter og plugin-versjoner:

```bash
./gradlew dependencies buildEnvironment --write-locks
```

Commit de oppdaterte lockfilene sammen med endringen. Glemmer du det, feiler bygget både lokalt og i CI med en feilmelding om at den oppløste versjonen «is not part of the dependency lock state».
