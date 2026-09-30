package no.novari.flyt.catalog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
import org.springframework.transaction.annotation.Transactional

/**
 * `@Transactional` uten qualifier løses mot primærkandidaten, som her er tjenestens eget tomme
 * skjema — ikke domenets. Repositories er allerede bundet riktig gjennom
 * `@EnableJpaRepositories(transactionManagerRef = …)`, så det er kun tjenestelaget som må navngi
 * manageren selv, og feilen ville vært stille.
 *
 * Testen dekker hele domenepakken framfor de to metodene som finnes i dag, slik at en ny
 * `@Transactional` uten qualifier også fanges. Hvert domene som flyttes inn legger til sin rad.
 */
class TransactionManagerBindingTest {
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "no.novari.flyt.catalog.valueconverting, valueConvertingTransactionManager, 2",
        "no.novari.flyt.catalog.discovery,       discoveryTransactionManager,       0",
    )
    fun `every transactional method in a domain names that domain's transaction manager`(
        domainPackage: String,
        expectedTransactionManager: String,
        expectedTransactionalMethods: Int,
    ) {
        val transactionAttributeSource = AnnotationTransactionAttributeSource()

        val qualifiersByMethod =
            componentsIn(domainPackage)
                .flatMap { type -> type.methods.map { type to it } }
                .mapNotNull { (type, method) ->
                    transactionAttributeSource
                        .getTransactionAttribute(method, type)
                        ?.let { "${type.simpleName}.${method.name}" to it.qualifier }
                }.toMap()

        assertThat(qualifiersByMethod).hasSize(expectedTransactionalMethods)
        assertThat(qualifiersByMethod).allSatisfy { method, qualifier ->
            assertThat(qualifier).describedAs(method).isEqualTo(expectedTransactionManager)
        }
    }

    private fun componentsIn(domainPackage: String): List<Class<*>> =
        ClassPathScanningCandidateComponentProvider(false)
            .apply {
                addIncludeFilter(AnnotationTypeFilter(Component::class.java))
                addIncludeFilter(AnnotationTypeFilter(Transactional::class.java))
            }.findCandidateComponents(domainPackage)
            .map { Class.forName(checkNotNull(it.beanClassName)) }
}
