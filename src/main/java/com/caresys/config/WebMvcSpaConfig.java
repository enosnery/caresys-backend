package com.caresys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.Map;

@Configuration
public class WebMvcSpaConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Adiciona automaticamente o prefixo /api a todos os controllers REST do pacote com.caresys.controller
        configurer.addPathPrefix("/api", HandlerTypePredicate.forBasePackage("com.caresys.controller"));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve os arquivos estáticos do build do Vue (classpath:/static/) e do Swagger UI (classpath:/META-INF/resources/)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/META-INF/resources/", "classpath:/META-INF/resources/webjars/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // Não redireciona chamadas de API ou Swagger para o index.html do Vue
                        if (resourcePath.startsWith("api/") ||
                            resourcePath.equals("api") ||
                            resourcePath.startsWith("v3/") ||
                            resourcePath.startsWith("swagger-ui") ||
                            resourcePath.startsWith("webjars/")) {
                            return null;
                        }
                        Resource indexHtml = new ClassPathResource("/static/index.html");
                        return indexHtml.exists() ? indexHtml : null;
                    }
                });
    }

    /**
     * Endpoint leve de Health Check para o Koyeb (não aciona o banco de dados PostgreSQL,
     * permitindo que o banco entre em modo sleep/scale-to-zero quando ocioso).
     */
    @RestController
    public static class HealthCheckController {
        @GetMapping("/api/health")
        public ResponseEntity<Map<String, String>> health() {
            return ResponseEntity.ok(Map.of("status", "UP", "timezone", "America/Sao_Paulo"));
        }
    }
}
