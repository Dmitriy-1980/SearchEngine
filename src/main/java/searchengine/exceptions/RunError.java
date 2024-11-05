package searchengine.exceptions;
/** исключение вызванное неожиданным результатом запроса
 * @param String msg - сообщение*/
public class RunError extends RuntimeException{
    private String msg;

    public RunError(String msg){
        this.msg = msg;
    }
}
