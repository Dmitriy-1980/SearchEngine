package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

//@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
}
