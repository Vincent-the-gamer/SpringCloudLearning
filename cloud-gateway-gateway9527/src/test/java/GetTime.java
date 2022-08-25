import java.time.ZonedDateTime;

public class GetTime {
    //获得本地时区的时间
    public static void main(String[] args) {
        ZonedDateTime zbj = ZonedDateTime.now(); //默认时区
        System.out.println(zbj);
    }
}
