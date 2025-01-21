package searchengine.mechanics;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.RecursiveAction;
//класс-задача которая отслеживает завершение индексации

//@Component
//@RequiredArgsConstructor
public class WaitOfIndexEnd extends RecursiveAction {
    private final Indexing indexing;

    public WaitOfIndexEnd(Indexing indexing) {
        this.indexing = indexing;
    }

    @Override
    protected void compute() {
        indexing.waitOfIndexingEnd();
        System.out.println("Индексация закончена ");
        indexing.setIsRunning(false);
    }
}
