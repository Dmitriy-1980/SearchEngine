package searchengine.explore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repositories.SiteRepository;

import java.util.concurrent.ExecutorService;

@Component
public class Explore implements Runnable{
    @Autowired//нужна тут эта аннотация?? Или внедрение автоматически пройдет?
    SitesList sitesList;


    @Override
     public void run(){
         for (Site item : sitesList.getSites()){
             //*todo работа с каждым сайтом
     }
    }

    private void removeOldData(String siteName){
        //* todo удаление из БД (site, page) предыдущих записей по этому сайту
    }

    private void exploreSite(String siteName){
        //todo "чтение" сайта на заданную глубину
        //сразу глубокое сканирование (1-2-3 уровень), не горизонтальное.
    }

}
