package no.novari.flyt.catalog.discovery.model.dtos

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntegrationMetadataDtoJsonDeserializationTest {
    @Test
    fun `deserializes arkivering metadata request`() {
        val dto =
            jacksonObjectMapper().readValue<IntegrationMetadataDto>(
                """
                {
                  "sourceApplicationId": "6",
                  "sourceApplicationIntegrationId": "sak",
                  "integrationDisplayName": "Arkivering",
                  "version": 4,
                  "instanceMetadata": {
                    "instanceValueMetadata": [
                      {
                        "key": "organisasjonsNavn",
                        "type": "STRING",
                        "displayName": "Organisasjonsnavn"
                      },
                      {
                        "key": "instansId",
                        "type": "STRING",
                        "displayName": "Instans ID"
                      },
                      {
                        "key": "organisasjonsNummer",
                        "type": "STRING",
                        "displayName": "Organisasjonsnummer"
                      },
                      {
                        "key": "prosjektNavn",
                        "type": "STRING",
                        "displayName": "Prosjektnavn"
                      },
                      {
                        "key": "hovedLeverandor",
                        "type": "STRING",
                        "displayName": "Hovedleverandør"
                      },
                      {
                        "key": "behandlet",
                        "type": "STRING",
                        "displayName": "Behandlet"
                      },
                      {
                        "key": "status",
                        "type": "STRING",
                        "displayName": "Status"
                      },
                      {
                        "key": "behandletEpost",
                        "type": "STRING",
                        "displayName": "Behandlet e-post"
                      },
                      {
                        "key": "type",
                        "type": "STRING",
                        "displayName": "Type"
                      },
                      {
                        "key": "template",
                        "type": "STRING",
                        "displayName": "Template"
                      },
                      {
                        "key": "deviationCode",
                        "type": "STRING",
                        "displayName": "DeviationCode"
                      },
                      {
                        "key": "deviationCodeFU",
                        "type": "STRING",
                        "displayName": "DeviationCodeFU"
                      },
                      {
                        "key": "prosjektId",
                        "type": "STRING",
                        "displayName": "ProsjektId"
                      },
                      {
                        "key": "avdeling",
                        "type": "STRING",
                        "displayName": "Avdeling"
                      }
                    ],
                    "instanceObjectCollectionMetadata": [
                      {
                        "displayName": "Vedlegg",
                        "key": "vedlegg",
                        "objectMetadata": {
                          "instanceValueMetadata": [
                            {
                              "key": "tittel",
                              "displayName": "Tittel",
                              "type": "STRING"
                            },
                            {
                              "key": "filnavn",
                              "displayName": "Filnavn",
                              "type": "STRING"
                            },
                            {
                              "key": "fildato",
                              "displayName": "Fildato",
                              "type": "STRING"
                            },
                            {
                              "key": "fil",
                              "displayName": "Fil",
                              "type": "FILE"
                            },
                            {
                              "key": "mediatype",
                              "displayName": "Mediatype",
                              "type": "STRING"
                            }
                          ]
                        }
                      }
                    ],
                    "categories": [
                      {
                        "displayName": "Hoveddokument",
                        "content": {
                          "instanceValueMetadata": [
                            {
                              "key": "hovedDokumentTittel",
                              "displayName": "Tittel",
                              "type": "STRING"
                            },
                            {
                              "key": "hovedDokumentFilnavn",
                              "displayName": "Filnavn",
                              "type": "STRING"
                            },
                            {
                              "key": "hovedDokumentdato",
                              "displayName": "Fildato",
                              "type": "STRING"
                            },
                            {
                              "key": "hovedDokumentFil",
                              "displayName": "Fil",
                              "type": "FILE"
                            },
                            {
                              "key": "hovedDokumentMediatype",
                              "displayName": "Mediatype",
                              "type": "STRING"
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        val instanceMetadata = requireNotNull(dto.instanceMetadata)
        val hoveddokumentMetadata = requireNotNull(instanceMetadata.categories.single().content)
        val hoveddokumentFil =
            hoveddokumentMetadata.instanceValueMetadata.single { instanceValueMetadata ->
                instanceValueMetadata.key == "hovedDokumentFil"
            }

        assertEquals(6L, dto.sourceApplicationId)
        assertEquals("sak", dto.sourceApplicationIntegrationId)
        assertEquals("Arkivering", dto.integrationDisplayName)
        assertEquals(4L, dto.version)
        assertEquals(14, instanceMetadata.instanceValueMetadata.size)
        assertEquals("vedlegg", instanceMetadata.instanceObjectCollectionMetadata.single().key)
        assertEquals(InstanceValueMetadata.Type.FILE, hoveddokumentFil.type)
    }
}
