package server.nadeliv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {

    @Value("${cloud.aws.ses.region:ap-northeast-2}")
    private String sesRegion;

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(sesRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
