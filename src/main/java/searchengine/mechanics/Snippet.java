package searchengine.mechanics;

import lombok.Getter;
import lombok.Setter;

//текст и кол. найденных слов
/**Сниппет- часть текста с искомыми словами.*/
@Getter
@Setter
public class Snippet {
    /**Текст сниппета.*/
    String text;
    /**кол. разных лемм в сниппете*/
    int wordCount;

    public Snippet(String text, int wordCount) {
        this.text = text;
        this.wordCount = wordCount;
    }
}
