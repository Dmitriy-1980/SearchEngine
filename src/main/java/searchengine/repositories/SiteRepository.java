package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Site;

public interface SiteRepository extends CrudRepository<Site, Integer> {
}
