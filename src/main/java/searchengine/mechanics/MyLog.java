package searchengine.mechanics;


import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@NoArgsConstructor
public class MyLog {
    private final Logger indexingLogger = LoggerFactory.getLogger("indexingLogger");
    private final Logger parserLogger = LoggerFactory.getLogger("pageParserLogger");
    private final Logger traceLogger = LoggerFactory.getLogger("traceLog");

    private long indexingLogTime = System.currentTimeMillis();//время последнего сообщения
    private long parserLogTime = System.currentTimeMillis();
    private long traceLogTime = System.currentTimeMillis();

    private String timeAfterLastEvent(long lastTime, String message){
       return
        "(pass_time(ms):"
          + String.valueOf(System.currentTimeMillis()-lastTime)
          + ") | " + message;
    }

    public void indLog(String msg, String lvl){
        msg = timeAfterLastEvent(indexingLogTime, msg);
        switch (lvl){
            case "trace" -> {indexingLogger.trace(msg); break;}
            case "debug" -> {indexingLogger.debug(msg); break;}
            case "info" -> {indexingLogger.info(msg); break;}
            case "warn" -> {indexingLogger.warn(msg); break;}
            case "error" -> {indexingLogger.error(msg); break;}
        }
    }

    public void parsLog(String msg, String lvl){
        msg = timeAfterLastEvent(parserLogTime, msg);
        switch (lvl){
            case "trace" -> {parserLogger.trace(msg); break;}
            case "debug" -> {parserLogger.debug(msg); break;}
            case "info" -> {parserLogger.info(msg); break;}
            case "warn" -> {parserLogger.warn(msg); break;}
            case "error" -> {parserLogger.error(msg); break;}
        }
    }

    public void traceLog(String msg, String lvl){
        msg = timeAfterLastEvent(traceLogTime, msg);
        switch (lvl){
            case "trace" -> {traceLogger.trace(msg); break;}
            case "debug" -> {traceLogger.debug(msg); break;}
            case "info" -> {traceLogger.info(msg); break;}
            case "warn" -> {traceLogger.warn(msg); break;}
            case "error" -> {traceLogger.error(msg); break;}
        }
    }

}
