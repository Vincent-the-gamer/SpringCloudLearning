package application.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class PaymentService {
    /* 正常访问的方法 */
    public String paymentInfo_OK(Integer id){
        return "线程池： " + Thread.currentThread().getName() + "   paymentInfo_OK,id:  " + id + "\t" + "O(∩_∩)O哈哈~";
    }
    public String paymentInfo_TimeOut(Integer id){
        int timeNumber = 3;
        try{
            TimeUnit.SECONDS.sleep(timeNumber);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return "线程池： " + Thread.currentThread().getName() + "   paymentInfo_Timeout,id:  " + id + "\t" + "寄了， 耗时(秒)：" + timeNumber;
    }
}
