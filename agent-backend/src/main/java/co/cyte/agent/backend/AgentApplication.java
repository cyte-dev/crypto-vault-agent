package co.cyte.agent.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Punto de entrada principal de la aplicación Spring Boot (backend).
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "co.cyte.agent.backend", // Tu backend actual: controllers, filesystem, etc.
        "co.cyte.agent.core"     // Para detectar EncryptionService y otros servicios/dominio
})
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
