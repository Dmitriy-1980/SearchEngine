package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    //удаление индексов по списку id страниц
    void deleteAllByPageIdIn(List<Integer> listPageId);

    //удаление индексов по page_id
    void deleteAllByPageId(int pageId);
}
