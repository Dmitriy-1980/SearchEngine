package searchengine.mechanics;

import lombok.Getter;
import lombok.Setter;

//текст и кол. найденных слов
@Getter
@Setter
public class Snippet {
    String text;
    int wordCount;

    public Snippet(String text, int wordCount) {
        this.text = text;
        this.wordCount = wordCount;
    }
}
