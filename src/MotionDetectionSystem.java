import org.omg.CORBA.FREE_MEM;
//import sun.jvm.hotspot.jdi.BooleanTypeImpl;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by constie on 01.12.2017.
 */
public class MotionDetectionSystem implements MotionDetectionSystemInterface {
    private ConcurrentSkipListMap<Integer, Boolean> frameStatuses = new ConcurrentSkipListMap<>();
    private ConcurrentSkipListMap<Integer, int[][]> frameImages = new ConcurrentSkipListMap<>();
//    private BlockingMap<Integer, int[][]>
    private BlockingQueue taskQueue = null;

//    private ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Boolean, int[][]>> frameMap = new ConcurrentSkipListMap<>();
//    private Map<Integer, Point2D.Double> results = Collections.synchronizedMap(new TreeMap<>());
    private ImageConverterInterface imgConvrt;
    private ResultConsumerInterface rcinter;
    private Point2D.Double imgResults;
    private int threadsNo = 0;
    private AtomicInteger iter;
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    //    private AtomicInteger frameNo =  new AtomicInteger(0);
//    private AtomicInteger iter = new AtomicInteger(0);
    private LinkedBlockingQueue<Integer> pairsList = new LinkedBlockingQueue<>();
    private ConcurrentSkipListMap<Integer, Point2D.Double> results = new ConcurrentSkipListMap<>();
    private Map<Integer, Thread> threadsArray = Collections.synchronizedMap(new TreeMap<>());

    @Override
    public void setThreads(int threads) {
        threadsNo = threads;
        if (threads <= executor.getMaximumPoolSize()) {
            executor.setCorePoolSize(threads);
            executor.setMaximumPoolSize(threads);
        } else {
            executor.setMaximumPoolSize(threads);
            executor.setCorePoolSize(threads);
        }
    }

    @Override
    public void setImageConverter(ImageConverterInterface ici) {
        imgConvrt = ici;

        iter = new AtomicInteger(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (results) {
                    while (true) {
                        PMO_SystemOutRedirect.println("iter.get(): " + iter.get() + " results.get(iter): " + results.get(iter.get()));
                        if (results.containsKey(iter.get())) {
                            int dummyatom = iter.get();
                            rcinter.accept(dummyatom, results.get(dummyatom));
                            iter.incrementAndGet();
                        }
                        if (!results.containsKey(iter.get())) {
                            try {
                                results.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }
//    public Runnable worker(){
//                    return new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (pairsList) {
//                                try {
//                                    pairsList.wait();
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                            while (!pairsList.isEmpty()) {
//                                PMO_SystemOutRedirect.println("now running: " + Thread.currentThread().getName());
//                                int frameNo = 0;
//                        synchronized (pairsList) {
//                                try {
//                                    frameNo = pairsList.take();
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                        }
//                                if((frameNo != (frameImages.size()-1)) && frameImages.get(frameNo) != null && frameImages.get(frameNo + 1) != null) {
//                                    Point2D.Double result = imgConvrt.convert(frameNo, frameImages.get(frameNo), frameImages.get(frameNo + 1));
//                                    synchronized (results) {
//                                        results.put(frameNo, result);
//                                    }
//                                    sendResults();
//
//                                }
//                                sendResults();
//
//                            }
//
//
//                        }
//                    });
//    }

    public Runnable worker(int frame, int[][] frameImage, int[][] pairedFrameImage){
        return () -> {
            Point2D.Double result = imgConvrt.convert(frame, frameImage, pairedFrameImage);
            synchronized (results) {
                results.put(frame, result);
                results.notify();
            }
        };
    }

    @Override
    public void setResultListener(ResultConsumerInterface rci) {
        rcinter = rci;

    }

    @Override
    public void addImage(int frameNumber, int[][] image) {


        synchronized (frameImages) {
            for (Integer frame : frameImages.keySet()){
                if (frame == frameNumber - 1) executor.execute(worker(frame, frameImages.get(frame), image));
                if (frame == frameNumber + 1) executor.execute(worker(frameNumber, image, frameImages.get(frame)));
            }
            frameImages.put(frameNumber, image);
        }

//        synchronized (pairsList) {
//            synchronized (frameStatuses){
//                int lowestFrame = frameStatuses.firstKey();
//                PMO_SystemOutRedirect.println("lowestFrame: " + lowestFrame);
//                int secondLowestFrame = frameStatuses.higherKey(lowestFrame);
//                PMO_SystemOutRedirect.println("secondLowestFrame: " + secondLowestFrame);
//                while (secondLowestFrame - lowestFrame != 1 || frameStatuses.get(lowestFrame)) {
//
//                    if (frameStatuses.lastKey() == secondLowestFrame) {
//                        frameStatuses.put(secondLowestFrame, Boolean.TRUE);
//                        pairsList.add(secondLowestFrame);
//                        executor.execute(worker());
//                    }
//                    lowestFrame = secondLowestFrame;
//                    secondLowestFrame = frameStatuses.higherKey(lowestFrame);
//                }
//                if(!frameStatuses.get(lowestFrame)) {
//                    frameStatuses.put(lowestFrame, Boolean.TRUE);
//                    pairsList.add(lowestFrame);
//                    executor.execute(worker());
//                }
//            }
//
//        }

    }


//    private int checkFirstPair(ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Boolean, int[][]>> framesArray){
//
//        int lowestFrame = framesArray.firstKey();
//        int secondLowestFrame = framesArray.higherKey(lowestFrame);
//        while (secondLowestFrame - lowestFrame != 1 || framesArray.get(lowestFrame).firstKey()) {
//            lowestFrame = secondLowestFrame;
//            if (framesArray.lastKey() == lowestFrame) {
//                framesArray.put(lowestFrame, Boolean.TRUE);
//                PMO_SystemOutRedirect.println("low: " + lowestFrame);
//                return lowestFrame;
//            }
//            secondLowestFrame = framesArray.higherKey(lowestFrame);
//        }
//        framesArray.put(lowestFrame, Boolean.TRUE);
//        PMO_SystemOutRedirect.println("lo: " + lowestFrame);
//        return lowestFrame;
//
//
//    }



//    for(int i=0; i<threadsNo; i++) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (pairsList) {
//                    try {
//                        pairsList.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                while (!pairsList.isEmpty()) {
//
//                    int frameNo;
//                    synchronized (pairsList){
//                        frameNo = pairsList.get(0);
//                        if(frameImages.get(frameNo + 1) == null){
//                            break;
//                        }
//                        pairsList.remove(0);
//                    }
//                    Point2D.Double result = imgConvrt.convert(frameNo, frameImages.get(frameNo), frameImages.get(frameNo + 1));
//
//                    results.put(frameNo, result);
//                    sendResults();
//                }
//
//            }
//        }).start();
//
//
//    }
}

