
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 10;
        int M = 8;
        int K = 5;
        int X = 100;
        int T = 100;
        int TT = 10;
        UnlimitedQueue queue = new UnlimitedQueue();
        SensorQueue[] sensorQueues = new SensorQueue[N];
        for(int i = 0; i < N; i++){
            sensorQueues[i] = new SensorQueue(K);
        }

        AcquireThread[] at = new AcquireThread[N];
        for (int i = 0; i < N; i++) {
            at[i] = new AcquireThread(X, i, queue);
            at[i].setName("AT" + i);
            at[i].start();
        }

        WorkerThread[] wt = new WorkerThread[M];
        for (int i = 0; i < M; i++) {
            wt[i] = new WorkerThread(T, TT, queue, sensorQueues);
            wt[i].setName("WT" + i);
            wt[i].start();
        }

        ProcessorThread pt = new ProcessorThread(sensorQueues);
        pt.setName("PT");
        pt.start();

        Thread.sleep(10000);
        for(int i = 0; i < N; i++){
            at[i].interrupt();
        }

        for(WorkerThread workerThread : wt){
            workerThread.interrupt();
        }

        pt.interrupt();
    }
}


class Message{
    long acq_time;
    int acq_value;
    int sensor_id;

    public Message(long acq_time, int acq_value, int sensor_id) {
        this.acq_time = acq_time;
        this.acq_value = acq_value;
        this.sensor_id = sensor_id;
    }
}

class UnlimitedQueue{
    ArrayList<Message> queue;
    Semaphore mutex;
    Semaphore full;

    public UnlimitedQueue(){
        this.queue = new ArrayList<>();
        this.mutex = new Semaphore(1);
        this.full = new Semaphore(0);
    }

    public void putMessage(Message msg) throws InterruptedException {
        mutex.acquire();
        queue.add(msg);
        mutex.release();
        full.release();
    }

    public Message getMessage() throws InterruptedException {
        full.acquire();
        mutex.acquire();
        Message r = queue.remove(0);
        mutex.release();
        return r;
    }
}

class SensorQueue{
    ArrayList<Message> sums;
    int K;
    Semaphore empty;
    Semaphore full;
    Semaphore mutex;


    public SensorQueue(int k){
        this.K = k;
        this.sums = new ArrayList<>(K);
        this.empty = new Semaphore(K);
        this.full = new Semaphore(0);
        this.mutex = new Semaphore(1);
    }

    public void putMsg(Message msg) throws InterruptedException {
        empty.acquire();
        mutex.acquire();
        sums.add(msg);
        mutex.release();
        full.release();
    }

    public int getSum() throws InterruptedException {
        full.acquire(K);
        mutex.acquire();
        int sum = 0;
        for(Message message : sums){
            sum += message.acq_value;
        }
        Message r = sums.remove(0);
        full.release(K-1);
        empty.release();
        mutex.release();
        return sum;
    }
}

class AcquireThread extends Thread{
    int X;
    int sensor;
    UnlimitedQueue queue;

    public AcquireThread(int X, int sensor, UnlimitedQueue queue){
        this.X = X;
        this.sensor = sensor;
        this.queue = queue;
    }

    public void run(){
        int i = sensor;
        try{
            while(true){;
                Message msg = new Message(System.currentTimeMillis(), i++, sensor);
                queue.putMessage(msg);
                sleep(X);
            }

        }
        catch(InterruptedException e){
            System.out.println("AcquireThread Interrupted!");
        }
    }
}

class WorkerThread extends Thread{
    int T, TT;
    UnlimitedQueue queue;
    SensorQueue[] sensor;

    public WorkerThread(int T, int TT, UnlimitedQueue queue, SensorQueue[] sensor){
        this.T = T;
        this.TT = TT;
        this.queue = queue;
        this.sensor = sensor;
    }

    public void run(){
        try{
            while(true){
                Message msg = queue.getMessage();
                sleep(T + (int)(Math.random()*(TT-T)));
                sensor[msg.sensor_id].putMsg(msg);
            }
        }
        catch(InterruptedException e){
            System.out.println("WorkerThread Interrupted!");
        }
    }
}

class ProcessorThread extends Thread{
    SensorQueue[] sensor;

    public ProcessorThread(SensorQueue[] sensor){
        this.sensor = sensor;
    }
    public void run(){
        try{
            while(true){
                for(SensorQueue s : sensor){
                    int sum = s.getSum();
                    System.out.print(sum + " ");
                }
                System.out.println();
            }
        }
        catch(InterruptedException e){
            System.out.println("ProcessorThread Interrupted!");
        }
    }
}