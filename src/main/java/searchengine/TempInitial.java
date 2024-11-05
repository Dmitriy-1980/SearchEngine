package searchengine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.model.*;
import searchengine.services.SiteCrudServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

//temporal initiation
@Component
@RequiredArgsConstructor
public class TempInitial {
    private final SiteCrudServiceImpl siteCrudService;
//    private final

    private Index index = new Index();
    private Lemma lemma = new Lemma();
    private Page page = new Page();
    private Site site = new Site();

    public void go(){
        site.setId(1);
        site.setName("mySite");
        site.setUrl("https://mops.ru");
        site.setStatus(IndexingStatus.INDEXED);
        site.setLastError("ahtung");
        site.setStatusTime(LocalDateTime.now());

        page.setId(1);
        page.setCode(405);
        page.setPath("/qqq/www/eee");
        page.setContent("eufeuifuehpjhgpwpgwjhpjw");
        page.setSiteId(site);

        lemma.setFrequency(0.45F);
        lemma.setId(1);
        lemma.setLemma("хвост");
        lemma.setId(1);

        index.setId(1);
        index.setRank(0.45F);
        index.setPageId(1);
        index.setLemmaId(1);

        Optional<Integer> answerAdd = siteCrudService.addSite("site-url", "site-name" );
        int id = answerAdd.get().intValue();
        System.out.println("добавлена запись site с id= " +id);

        site.setName("name-updated");
        siteCrudService.updateById(id, site);
        System.out.println("Изменена запись с id= " + id);

        Optional<Site> answerGet = siteCrudService.getById(id);
        System.out.println("Получить по id= " + id + " " + answerGet.get());

        siteCrudService.delById(id);

        System.out.println("stop 2");


    }

}
