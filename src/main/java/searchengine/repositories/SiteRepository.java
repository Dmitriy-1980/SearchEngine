package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

//@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query(nativeQuery = true, value = "Select id FROM site WHERE url=:paramUrl;")
    List<Site> findByUrl(@Param("paramUrl") String url);

    @Query(nativeQuery = true, value = "SELECT * FROM site WHERE site.id=:paramId")
    List<Site> findById(@Param("paramId") int id);

}
