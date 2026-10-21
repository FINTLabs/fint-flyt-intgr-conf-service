package no.novari.flyt.catalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * `flyt-web-resource-server` og `no.novari:kafka` registrerer bare en del av bønnene sine gjennom
 * auto-konfigurasjon; resten er `@Service`-klasser som forutsetter at konsumenten skanner pakkene
 * deres. De fire gamle tjenestene fikk det gratis fordi applikasjonsklassen deres lå i `no.novari`.
 * Her er pakkene navngitt framfor å skanne hele `no.novari` — da er det synlig hvilke bibliotek som
 * har forventningen, og hva som må legges til når et domene trenger flere.
 */
@SpringBootApplication(
    scanBasePackages = [
        "no.novari.flyt.catalog",
        "no.novari.flyt.webresourceserver",
        "no.novari.kafka",
    ],
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
