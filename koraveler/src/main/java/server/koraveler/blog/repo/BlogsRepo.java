package server.koraveler.blog.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.blog.model.Documents;

import java.util.List;

@Repository
public interface BlogsRepo extends MongoRepository<Documents, String> {
    @Override
    long count();

    Page<Documents> findAllByDraftIsFalseOrDraftIsNull(Pageable pageable);
    Page<Documents> findAllByTitleContainingIgnoreCaseOrContentsContainingIgnoreCase(String titleValue, String contentsValue, Pageable pageable);
}