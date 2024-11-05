import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteCrudServiceImpl;

import java.util.Optional;


@RequiredArgsConstructor
public class SiteCrudServiceImplTest {
//    @Autowired
    private final SiteCrudServiceImpl service;
//    @Autowired
    private final JdbcTemplate jdbcTemplate;


    @Test
    @DisplayName("создать запись в таблице site")
    public void createSiteTest(){
        //todo
        String name = "site-name";
        String url = "site-url";
        Optional<Integer> id = service.addSite(url, name);
        Site  site = jdbcTemplate.queryForObject("SELECT * FROM site WHERE id=" + id , Site.class);

        Assertions.assertTrue(site.getName().equals(name));
        Assertions.assertTrue(site.getUrl().equals(url));
    }


//    @AfterEach


}
