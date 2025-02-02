package searchengine.mechanics;

import java.util.concurrent.RecursiveAction;
//класс-задача которая отслеживает завершение индексации

/**Простая задача. Нужна для "долгой жизни в параллеле".
 * Ожиданеи связано с методом Indexing.waitOfIndexingEnd
 * который постоянно прощупывает список запущенныхзадач.*/
public class WaitOfIndexEnd extends RecursiveAction {
    private final Indexing indexing;
    private final long start;
    private MyLog log = new MyLog();

    public WaitOfIndexEnd(Indexing indexing, long start) {
        this.indexing = indexing;
        this.start = start;
    }

    @Override
    protected void compute() {
        System.out.println("Индексация запускается. ");
        try{ indexing.waitOfIndexingEnd();}
            catch (Exception ex)
            { log.indLog("WaitOfIndexEnd.compute(): Ошибка ожидателя окончания. : " + ex.getCause(), "error");
              indexing.setIsRunning(false);
            }
        System.out.println("Индексация закончена. ");
        log.indLog("WaitOfIndexEnd.compute(): индексация закончена.","info");

        indexing.setIsRunning(false);
        String msg = "Indexing completed. Duration(ms)=" + String.valueOf(System.currentTimeMillis() - start);
        log.indLog(msg, "info");
        log.indLog("--------","info");
        log.parsLog("--------", "info");
    }
}
