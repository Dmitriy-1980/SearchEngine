package searchengine.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommandResult  {
    private boolean result;
    private String error;
}
