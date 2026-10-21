# Arkitektur

Arkitekturen til fint-flyt-intgr-conf-service er beskrevet med [C4-modellen](https://c4model.com),
med diagrammer i Mermaid. Blå bokser er en del av katalogtjenesten, grå er eksterne.

1. [Systemkontekst](c4-context.md): hvem som bruker katalogtjenesten, og hvilke systemer den
   samarbeider med.
2. [Containere](c4-container.md): applikasjonen og databasen, API-et, Kafka-kontraktene og
   utrullingen.
3. [Komponenter](c4-component.md): de fire domenene, oppslagene mellom dem, persistensoppsettet
   og de tverrgående komponentene.

Kodenivået er ikke beskrevet her. Koden og OpenAPI-spesifikasjonen springdoc genererer, er
kilden for det.
