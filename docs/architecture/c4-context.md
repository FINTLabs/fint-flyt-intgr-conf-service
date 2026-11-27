# C4 nivå 1: Systemkontekst

Katalogtjenesten eier katalogen over hva FINT Flyt skal ta imot og hvordan det skal leveres:
integrasjonene, konfigurasjonene som beskriver mappingen, verdikonverteringene og metadataen
kildeapplikasjonene publiserer om sine integrasjoner.

Tjenesten kjører én instans per tenant og miljø. Diagrammet viser én slik instans.

## Diagram

```mermaid
C4Context
    title Systemkontekst for katalogtjenesten

    Person_Ext(users, "Flyt-brukere", "Fylkene, og utviklere i Novari og Vigo")
    System_Ext(frontend, "Flyt-frontend", "Setter opp integrasjoner")
    System_Ext(gateways, "Instans-gateways", "flyt-gateway-starter, acos-gateway")
    System(katalog, "Katalogtjenesten", "Integrasjoner, konfigurasjoner, verdikonverteringer, metadata")
    System_Ext(mapping, "mapping-service", "Mapper instanser")
    System_Ext(authorization, "authorization-service", "Tilgang og brukernavn")

    Rel(users, frontend, "Bruker")
    Rel(frontend, katalog, "Leser og endrer", "HTTPS")
    Rel(gateways, katalog, "Metadata, oppslag", "Kafka, HTTPS")
    Rel(mapping, katalog, "Henter konfigurasjon", "HTTPS")
    Rel(katalog, authorization, "Tilgang, navn", "HTTPS")

    UpdateRelStyle(users, frontend, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(frontend, katalog, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(gateways, katalog, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-55", $offsetY="-45")
    UpdateRelStyle(mapping, katalog, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(katalog, authorization, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateElementStyle(users, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(frontend, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(gateways, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(katalog, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(mapping, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(authorization, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

Blå er katalogtjenesten, turkis er eksterne systemer. FINT IdP utsteder tokenene og er ikke tegnet med.

## Brukere

| Bruker      | Type   | Behov                                                                                          |
| ----------- | ------ | ---------------------------------------------------------------------------------------------- |
| Flyt-bruker | Person | Opprette og aktivere integrasjoner, bygge konfigurasjoner og vedlikeholde verdikonverteringer. |
| Utvikler    | Person | Sette opp og feilsøke integrasjoner på vegne av fylkene.                                       |

Begge ser bare kildeapplikasjonene authorization-service gir dem tilgang til. Brukeren må høre til
tenanten eller en operatør. afk, bfk og ofk slipper også inn `viken.no` og `frid-iks.no`, satt i overlayet.

## Eksterne systemer

| System                | Retning | Integrasjon  | Hva det brukes til                                                                                    |
| --------------------- | ------- | ------------ | ----------------------------------------------------------------------------------------------------- |
| Flyt-frontend         | Inn     | HTTPS/JSON   | Det interne API-et under `/api/intern`.                                                               |
| Instans-gateways      | Inn     | Kafka, HTTPS | Publiserer integrasjonsmetadata og slår opp integrasjonen for en innkommende instans.                 |
| mapping-service       | Inn     | HTTPS        | Henter aktiv konfigurasjon for en integrasjon, mappingen for en konfigurasjon og verdikonverteringer. |
| authorization-service | Ut      | HTTPS        | Hvilke kildeapplikasjoner en bruker har tilgang til og visningsnavn for aktører.                      |
| FINT IdP              | Ut      | OIDC/OAuth2  | Validering av innkomne JWT og client credentials mot authorization-service.                           |

Kafka og PostgreSQL er plattformtjenester og vises på containernivå.

## Sentrale flyter

### Sette opp en integrasjon

1. En kildeapplikasjon publiserer metadata om integrasjonen sin, enten som hendelse på Kafka eller
   med `POST /api/intern/metadata`.
2. Brukeren oppretter integrasjonen i Flyt-frontend (`POST /api/intern/integrasjoner`).
3. Brukeren bygger en konfigurasjon mot en metadataversjon (`POST /api/intern/konfigurasjoner`).
   Katalogen validerer at integrasjonen og metadataen hører sammen, og at mappingen bare refererer
   til felter metadataen har.
4. Brukeren aktiverer integrasjonen med konfigurasjonen (`PATCH /api/intern/integrasjoner/{id}`).
   Katalogen avviser aktiveringen hvis konfigurasjonen ikke er fullført eller hører til en annen
   integrasjon.

### Ta imot en instans

1. En gateway mottar en instans og slår opp integrasjonen på kildeapplikasjon og
   kildeapplikasjonens integrasjons-ID over Kafka.
2. mapping-service henter aktiv konfigurasjon, mappingen og eventuelle verdikonverteringer over
   Kafka, og mapper instansen.

## Videre lesning

- [Containere](c4-container.md)
- [Komponenter](c4-component.md)
