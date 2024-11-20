package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.LemmaEntity;

public interface IndexRepository extends JpaRepository<LemmaEntity, Integer> {
}
