package searchengine.mechanics;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import searchengine.config.Site;

@Component
@Aspect
@Slf4j
public class LoggingAspect {
//    private static final Logger parserLogger = LoggerFactory.getLogger("pageParserLogger");
    private static final Logger indexingLogger = LoggerFactory.getLogger("indexingLogger");
//    //прерванный во сне поток PageParser'а
//    @AfterThrowing(pointcut="execution(* PageParser.compute())", throwing = "ex")
//    public void onInterruptedPageParser(InterruptedException ex, JoinPoint joinPoint){
//        PageParser parser = (PageParser) joinPoint.getTarget();
//        parserLogger.error("InterruptedException при старте парсинга " + parser.getPageUrl());
//    }
//    //ответ от сайта не получен
//    @AfterThrowing(pointcut = "exception(* PageParser.getResponse())", throwing = "ex")
//    public void onNoResponse(IOException ex, JoinPoint joinPoint){
//        PageParser parser = (PageParser) joinPoint.getTarget();
//        parserLogger.error("Response не получен: " + ex.getCause().toString() + " url:" + parser.getPageUrl());
//    }
//    //ошибка парсинга html в Document
//    @AfterThrowing(pointcut = "exception(* PageParser.compute())", throwing = "ex")
//    public void onNoParse(IOException ex, JoinPoint joinPoint){
//        PageParser parser = (PageParser) joinPoint.getTarget();
//        parserLogger.error("ошибка при парсинге: (document = siteResponse.parse()) " + ex.getCause().toString() + " url:" + parser);
//    }
//    //ошибка совместного доступа к карте запущенных задач (в парсере)
//    @AfterThrowing(pointcut = "execution(* Don't Show Toolbar)", throwing = "ex")
//    public void onConcModExcParser(ConcurrentModificationException ex){
//        indexingLogger.error("ConcurrentModificationException");
//    }
//    //прерванный во сне поток ожидания завершения работ
//    @AfterThrowing(pointcut = "execution(* Indexing.waitOfIndexingEnd())", throwing = "ex")
//    public void onInterruptedIndexing(InterruptedException ex){
//        indexingLogger.error("InterruptedException");
//    }
//    //ошибка совместного доступа к карте запущенных задач (в Indexing)
//    @AfterThrowing(pointcut = "execution(* Indexing.waitOfIndexingEnd())", throwing = "ex")
//    public void onConcModExcIndexing(ConcurrentModificationException ex){
//        indexingLogger.error("ConcurrentModificationException");
//    }
//
//    @AfterThrowing(pointcut = "execution(* PageParser.compute())", throwing = "ex")
//    public void onAnyErrorOnPageParser(JoinPoint joinPoint, Exception ex){
//        PageParser pp = (PageParser) joinPoint.getTarget();
//        String url = pp.getPageUrl();
//        String cause = ex.getCause().toString();
//        String exMessage = ex.getMessage();
//        String msg = "< " + url + " : " + cause + " : " + exMessage + " >";
//        Logger allLogPars = LoggerFactory.getLogger("allErrorPageParser");
//        allLogPars.error(msg);
//        System.out.println("<<<<<<<<<" + msg);
//    }

//    @Before("execution(* PageParser.luceneGo(..))")
//    public void beforeLuceneGo(JoinPoint jp){
//        PageParser pp = (PageParser) jp.getTarget();
//        System.out.println("@Before .. " + pp.getPageUrl());
//    }
//
//    @AfterThrowing(pointcut="execution(* PageParser.luceneGo(..))", throwing = "ex")
//    public void onLuceneGo(JoinPoint jp, Exception ex){
//        System.out.println("перехвачено");
//        System.out.println(ex.getMessage());
//        System.out.println(ex.getCause());
//        ex.printStackTrace();
//    }

    @Before("execution(* Indexing.stop(..))")
    public void beforeStop(){
        indexingLogger.info(" Stop");
    }

    @AfterReturning(value = "execution(* Indexing.stop(..))", returning = "result")
    public void afterStop(Object result){
        String msg = " /stopIndexing with result = " + ((Boolean) result).toString();
        indexingLogger.info(msg);
    }

    @Before("execution(* Indexing.clearDB(..))")
    public void beforeClearDB(){
        indexingLogger.info(" Clear DB");
    }

    @Before("execution(* Indexing.clearSiteData(..))")
    public void beforeClearSiteData(JoinPoint joinPoint){
        String msg = " Clear site " + ((String) joinPoint.getTarget() );
        indexingLogger.info(msg);
    }

    @Before(value = "execution(* Indexing.startFromList(..))")
    public void beforeSFL(){
        indexingLogger.info("##### Start indexing.");
    }

//    @AfterReturning(pointcut = "execution(* Indexing.startFromList(..))", returning = "result")
//    public void afterSFL(Object result){
//        String msg = "End indexing with result = " + ( (Boolean) result ) .toString();
//        indexingLogger.info(msg);
//    }

    @Before("execution(* Indexing.goIndex(..))")
    public void beforeGoIndex(JoinPoint joinPoint){
        Site site = (Site) joinPoint.getTarget();
        String msg = "index " + site.getUrl();
        indexingLogger.info(msg);
    }

}
