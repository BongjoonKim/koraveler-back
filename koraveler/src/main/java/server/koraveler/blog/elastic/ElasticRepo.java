package server.koraveler.blog.elastic;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.blog.model.Documents;

@Repository
@Qualifier("ElasticRepo")
public interface ElasticRepo extends ElasticsearchRepository<Documents, String> {

}
