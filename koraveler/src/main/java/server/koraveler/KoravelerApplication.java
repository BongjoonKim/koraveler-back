package server.koraveler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableElasticsearchRepositories("server.koraveler.blog.elastic")
@EnableMongoRepositories
@SpringBootApplication
public class KoravelerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoravelerApplication.class, args);
    }

}
