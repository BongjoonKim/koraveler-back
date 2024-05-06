package server.koraveler.menus.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import server.koraveler.menus.model.Menus;

public interface MenusRepo extends MongoRepository<Menus, String> {
}
