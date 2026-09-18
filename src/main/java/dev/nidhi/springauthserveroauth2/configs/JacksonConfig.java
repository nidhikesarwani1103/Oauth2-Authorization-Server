package dev.nidhi.springauthserveroauth2.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {

        ClassLoader classLoader = getClass().getClassLoader();

        return JsonMapper.builder()
                .addModules(
                        SecurityJacksonModules.getModules(classLoader)
                )
                .build();
    }
}
