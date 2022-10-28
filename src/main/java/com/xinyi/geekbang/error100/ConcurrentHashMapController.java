package com.xinyi.geekbang.error100;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @Author xinyi
 * @Date 2022/10/20 下午1:58
 */
@RestController
@RequestMapping("/hashMap")
@Slf4j
public class ConcurrentHashMapController {

    public static final int THREAD_COUNT = 10;
    public static final int COUNT = 1000;

    /**
     * ConcurrentHashMap 错误使用问题 1
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/wrong")
    public String wrong() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(COUNT - 100);
        log.info("concurrentHashMap is size: {}",concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1,10).parallel().forEach(i->{
            int gap = COUNT - concurrentHashMap.size();
            log.info("gap size :{}",gap);
            concurrentHashMap.putAll(getData(gap));
        }));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        log.info(" end concurrentHashMap size :{}",concurrentHashMap.size());
        return "ok";
    }

    @GetMapping("/right")
    public String right() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(COUNT - 100);
        log.info("concurrentHashMap is size: {}",concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1,10).parallel().forEach(i->{
            synchronized (concurrentHashMap) {
                int gap = COUNT - concurrentHashMap.size();
                log.info("gap size :{}",gap);
                concurrentHashMap.putAll(getData(gap));
            }
        }));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        log.info(" end concurrentHashMap size :{}",concurrentHashMap.size());
        return "ok";
    }

    private ConcurrentHashMap<String, Long> getData(int count){
        return LongStream.rangeClosed(1,count)
                .boxed()
                .collect(Collectors.toConcurrentMap(i-> UUID.randomUUID().toString(), Function.identity(),
                        (o1, o2)-> o1,ConcurrentHashMap::new));
    }

    public static final int LOOP_COUNT = 10000000;
    public static final int NOW_THREAD_COUNT = 10;
    public static final int ITEM_COUNT = 10;

    private ConcurrentHashMap<String, Long> normalUse() throws InterruptedException {
        ConcurrentHashMap<String,Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(NOW_THREAD_COUNT);
        forkJoinPool.execute(()->{
            IntStream.rangeClosed(1,LOOP_COUNT).parallel().forEach(i->{
                String key = "item " + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                synchronized (freqs) {
                    if (freqs.containsKey(key)){
                        freqs.put(key,freqs.get(key) + 1);
                    }else {
                        freqs.put(key,1L);
                    }
                }
            });
        });
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1,TimeUnit.HOURS);
        return freqs;
    }
    private Map<String, Long> goodUse() throws InterruptedException {
        ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(NOW_THREAD_COUNT);
        forkJoinPool.execute(()->{
            IntStream.rangeClosed(1,LOOP_COUNT).parallel().forEach(i->{
                String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                freqs.computeIfAbsent(key, k -> new LongAdder()).increment();
            });
        });
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1,TimeUnit.HOURS);
        return freqs.entrySet().stream().collect(Collectors.toConcurrentMap(e->e.getKey(),e->e.getValue().longValue()));

    }

    /**
     * computeIfAbsent 使用性能提升
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/good")
    public String good() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("normalUse");
        ConcurrentHashMap<String, Long> normalHashMap = normalUse();
        stopWatch.stop();
        // 校验元素数量
        Assert.isTrue(normalHashMap.size() == ITEM_COUNT,"normalHashMaps size error");
        Assert.isTrue(normalHashMap.entrySet()
                        .stream()
                        .mapToLong(item -> item.getValue())
                        .reduce(0,Long::sum) == LOOP_COUNT,"LOOP_COUNT is error ");

        stopWatch.start("goodUse");
        Map<String, Long> goodUses = goodUse();
        stopWatch.stop();
        Assert.isTrue(goodUses.size() == ITEM_COUNT,"error");
        Assert.isTrue(goodUses.entrySet()
                .stream()
                .mapToLong(item -> item.getValue())
                .reduce(0,Long::sum) == LOOP_COUNT,"error");

        log.info(stopWatch.prettyPrint());
        return "ok";

    }
    @GetMapping("/write")
    public Map write(){
        List<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList();
        List<Integer> arrayList = Collections.synchronizedList(new ArrayList<>());
        StopWatch stopWatch = new StopWatch();
        int loopCount = 100000;
        stopWatch.start("copyOnWriteArrayList");
        IntStream.rangeClosed(1,loopCount).parallel().forEach(x -> {
            copyOnWriteArrayList.add(ThreadLocalRandom.current().nextInt(loopCount));
        });
        stopWatch.stop();
        stopWatch.start("arrayList");
        IntStream.rangeClosed(1,loopCount).parallel().forEach(x->{
            arrayList.add(ThreadLocalRandom.current().nextInt(loopCount));
        });
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
        Map map = new HashMap();
        map.put("copyOnWriteArrayList",copyOnWriteArrayList.size());
        map.put("arrayList",arrayList.size());
        return map;
    }
}
