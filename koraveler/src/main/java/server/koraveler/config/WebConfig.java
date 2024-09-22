package server.koraveler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${front.url.local}")
    private String frontLocalUrl;

    @Value("${front.url.prod}")
    private String frontProdUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontLocalUrl)
                .allowedOrigins(frontProdUrl)
                .allowedOrigins("https://www.koraveler.com")
                .allowedOrigins("www.koraveler.com")
        .allowedOrigins("https://www.koraveler.com/")
        .allowedOrigins("www.koraveler.com/")
        .allowedOrigins("https://www.koraveler.com:3002")
        .allowedOrigins("www.koraveler.com:3002")
        .allowedOrigins("https://www.koraveler.com:3002/")
        .allowedOrigins("www.koraveler.com:3002/")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

    }
}
