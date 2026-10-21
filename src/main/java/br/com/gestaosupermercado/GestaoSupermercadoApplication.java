package br.com.gestaosupermercado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestaoSupermercadoApplication {

    public static void main(final String[] args) {
        SpringApplication.run(GestaoSupermercadoApplication.class, args);
    }
}
