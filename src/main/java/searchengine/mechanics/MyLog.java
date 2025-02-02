package searchengine.mechanics;


import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@NoArgsConstructor
public class MyLog {
    private final Logger parserLogger = LoggerFactory.getLogger("pageParserLogger");
    private final Logger indexingLogger = LoggerFactory.getLogger("indexingLogger");
    private final Logger searchLogger = LoggerFactory.getLogger("searchLogger");
    private final Logger traceLogger = LoggerFactory.getLogger("traceLogger");

    private long indexingLogTime = System.currentTimeMillis();//время последнего сообщения
    private long parserLogTime = System.currentTimeMillis();
    private long traceLogTime = System.currentTimeMillis();
    private long searchLogTime = System.currentTimeMillis();

    private String getMessage(long lastTime, String message){
       return
        "(pass_time(ms):"
          + (System.currentTimeMillis() - lastTime) + ") | "
                + message;
    }

    public void indLog(String msg, String lvl){
        msg = getMessage(indexingLogTime, msg);
        indexingLogTime = System.currentTimeMillis();
        switch (lvl){
            case "trace" -> {indexingLogger.trace(msg); break;}
            case "debug" -> {indexingLogger.debug(msg); break;}
            case "info" -> {indexingLogger.info(msg); break;}
            case "warn" -> {indexingLogger.warn(msg); break;}
            case "error" -> {indexingLogger.error(msg); break;}
        }
    }

    public void parsLog(String msg, String lvl){
        msg = getMessage(parserLogTime, msg);
        parserLogTime = System.currentTimeMillis();
        switch (lvl){
            case "trace" -> {parserLogger.trace(msg); break;}
            case "debug" -> {parserLogger.debug(msg); break;}
            case "info" -> {parserLogger.info(msg); break;}
            case "warn" -> {parserLogger.warn(msg); break;}
            case "error" -> {parserLogger.error(msg); break;}
        }
    }

    public void traceLog(String msg, String lvl){
        msg = getMessage(traceLogTime, msg);
        traceLogTime = System.currentTimeMillis();
        switch (lvl){
            case "trace" -> {traceLogger.trace(msg); break;}
            case "debug" -> {traceLogger.debug(msg); break;}
            case "info" -> {traceLogger.info(msg); break;}
            case "warn" -> {traceLogger.warn(msg); break;}
            case "error" -> {traceLogger.error(msg); break;}
        }
    }

    public void searchLog(String msg, String lvl){
        msg = getMessage(searchLogTime, msg);
        searchLogTime = System.currentTimeMillis();
        switch (lvl){
            case "trace" -> {searchLogger.trace(msg); break;}
            case "debug" -> {searchLogger.debug(msg); break;}
            case "info" -> {searchLogger.info(msg); break;}
            case "warn" -> {searchLogger.warn(msg); break;}
            case "error" -> {searchLogger.error(msg); break;}
        }
    }

}
