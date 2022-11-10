package com.xinyi.geekbang.error100;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author xinyi
 * @Date 2022/11/7 上午11:31
 */
@RestController
@RequestMapping("/lock")
@Slf4j
public class LockErrorController {

    volatile int a = 1;
    volatile int b = 1;

    @RequestMapping("/error")
    public String error(){

        new Thread(()->{
            // add 加synchronized 关键字正确
            add();
        }).start();

        new Thread(()->{
            compare();
        }).start();
        return "ok";

    }
    @RequestMapping("wrong")
    public Integer wrong(@RequestParam(value = "count",defaultValue = "1000000") int count){
        Data.reset();
        IntStream.rangeClosed(1,count).parallel().forEach(i -> {
            new Data().wrong();
        });
        return Data.getCounter();
    }

    private synchronized void add(){
        log.info("start add ");
        IntStream.rangeClosed(1,10000).forEach(i->{
            a++;
            b++;
        });
        log.info("a sum :{},b sum {}",a,b);
        log.info("end add");
    }
    private synchronized void compare(){
        log.info("start compare");
        IntStream.rangeClosed(1,10000).forEach(i->{
            if (a < b){
                log.info("a:{},b:{},{}",a,b,a>b);
            }
        });
        log.info("end compare");
    }

    private ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<>();

    private List<Item> createCart(){
        return IntStream.rangeClosed(1,3)
                .mapToObj(i -> "item" + ThreadLocalRandom.current().nextInt(items.size())
                        + "threadName" + Thread.currentThread().getName())
                .map(name -> items.get(name)).collect(Collectors.toList());
    }
    private boolean createOrder(List<Item> items){
        // 存放所以的锁
        List<ReentrantLock> reentrantLocks = new ArrayList<>();
        items.forEach(item -> {
//            if (item.lock.tryLock(10, TimeUnit.SECONDS)){
//
//            }
        });
        return false;
    }

}
class Data{
    @Getter
    private static int counter = 0;

    private static Object object = new Object();

    public static int reset(){
        counter = 0;
        return counter;
    }

    public void wrong() {
        synchronized (object){
            counter++;
        }
    }

}
@lombok.Data
@RequiredArgsConstructor
class Item{
    final String name;
    int remaining = 1000;
    @ToString.Exclude
    ReentrantLock lock = new ReentrantLock();

}
