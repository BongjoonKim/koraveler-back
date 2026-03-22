package server.nadeliv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableElasticsearchRepositories("server.nadeliv.blog.elastic")
//@EnableMongoRepositories(basePackages = {
//        "server.nadeliv.blog.repo",
//        "server.nadeliv.users.repo",
//        "server.nadeliv.menus.repo",
//        "server.nadeliv.connections.bookmarks.repo",
//        "server.nadeliv.folders.repo"
//})
@SpringBootApplication
@EnableScheduling
public class NadelivApplication {

    public static void main(String[] args) {
        SpringApplication.run(NadelivApplication.class, args);
    }

}
