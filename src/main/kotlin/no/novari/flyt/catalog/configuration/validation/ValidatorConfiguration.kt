package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.ConstraintValidatorFactory
import jakarta.validation.Validation
import jakarta.validation.ValidatorFactory
import org.hibernate.validator.HibernateValidator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory

/**
 * I configuration-service var disse bønnene applikasjonens eneste validator, og de erstattet Spring Boots
 * `defaultValidator`. Her er de navngitt og bare brukt av configuration-domenet, slik at MVC-valideringen
 * og entitetsvalideringen i de andre domenene fortsatt går gjennom Spring Boots validator.
 */
@Configuration
class ValidatorConfiguration {
    @Bean
    fun configurationHibernateValidatorFactory(
        @Qualifier("configurationConstraintValidatorFactory") constraintValidatorFactory: ConstraintValidatorFactory,
    ): ValidatorFactory =
        Validation
            .byProvider(HibernateValidator::class.java)
            .configure()
            .constraintValidatorFactory(constraintValidatorFactory)
            .buildValidatorFactory()

    @Bean
    fun configurationConstraintValidatorFactory(
        autowireCapableBeanFactory: AutowireCapableBeanFactory,
    ): ConstraintValidatorFactory = SpringConstraintValidatorFactory(autowireCapableBeanFactory)
}
