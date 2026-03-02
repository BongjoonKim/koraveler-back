package server.koraveler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableElasticsearchRepositories("server.koraveler.blog.elastic")
//@EnableMongoRepositories(basePackages = {
//        "server.koraveler.blog.repo",
//        "server.koraveler.users.repo",
//        "server.koraveler.menus.repo",
//        "server.koraveler.connections.bookmarks.repo",
//        "server.koraveler.folders.repo"
//})
@SpringBootApplication
@EnableScheduling
public class KoravelerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoravelerApplication.class, args);
    }

}
