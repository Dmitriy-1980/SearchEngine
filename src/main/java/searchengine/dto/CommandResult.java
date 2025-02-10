package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**Простой объект- отклик на выданную команду.
 * Два поля- boolean и String - результат и сообщение.*/
@Data
@AllArgsConstructor
public class CommandResult  {
    private boolean result;
    private String error;



}
