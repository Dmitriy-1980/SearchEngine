package searchengine.mechanics;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.RecursiveAction;
//класс-задача которая отслеживает завершение индексации

//@Component
//@RequiredArgsConstructor
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
        indexing.waitOfIndexingEnd();
        System.out.println("Индексация закончена ");
        indexing.setIsRunning(false);
        String msg = "Indexing completed. Duration(ms)=" + String.valueOf(System.currentTimeMillis() - start);
        log.indLog(msg, "info");
        log.indLog("--------","info");
        log.parsLog("--------", "info");
    }
}
