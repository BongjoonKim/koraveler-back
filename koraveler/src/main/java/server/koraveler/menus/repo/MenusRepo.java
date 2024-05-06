package server.koraveler.menus.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.menus.model.Menus;

@Repository
public interface MenusRepo extends MongoRepository<Menus, String> {
    public Menus findByLabel(String label);
}
