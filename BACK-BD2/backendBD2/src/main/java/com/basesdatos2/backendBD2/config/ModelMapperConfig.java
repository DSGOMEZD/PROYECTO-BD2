package com.basesdatos2.backendBD2.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.record.RecordModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ModelMapper compartido por los servicios para pasar de entidad a DTO y viceversa.
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        // RecordModule: permite leer los DTO de entrada, que son records.
        ModelMapper mm = new ModelMapper().registerModule(new RecordModule());
        // STRICT: solo copia campos con el mismo nombre exacto; el resto se llena a mano.
        mm.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mm;
    }
}
