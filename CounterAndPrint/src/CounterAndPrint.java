
import java.lang.management.ManagementFactory;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;


public class CounterAndPrint {
    public static void main(String[] args) throws InterruptedException{
        int TG = 100;
        int TC = 500;
        Counter counter = new Counter(0);
        MsgQueue msgQueue = new MsgQueue();

        Generator[] gt = new Generator[3];
        for(int i=0; i < gt.length; i++){
            gt[i] = new Generator(counter, msgQueue, TG);
            gt[i].setName("gt" + i);
            gt[i].start();
        }

        Consumer[] ct = new Consumer[3];
        for(int i=0; i < ct.length; i++){
            ct[i] = new Consumer(msgQueue, TC);
            ct[i].setName("ct" + i);
            ct[i].start();
        }
        Thread.sleep(10000);

        for(int i = 0; i < gt.length; i++){
            gt[i].interrupt();
        }

        msgQueue.closing = true;
        if(msgQueue.getSize()>0)
            msgQueue.vuote.acquire();

        System.out.println("counter:" + counter.currentValue);
        for(int i = 0; i < ct.length; i++)
            ct[i].interrupt();
    }
}

class Counter{
    int currentValue;
    private Semaphore mutex = new Semaphore(1);

    Counter(int value){
        this.currentValue = value;
    }

    public int getValue() throws InterruptedException{
        mutex.acquire();
        ++currentValue;
        int r = currentValue;
        mutex.release();
        return r;
    }
}

class Message{
    int t, v;

    public Message(int t, int v){
        this.t = t;
        this.v = v;
    }

    @Override
    public String toString() {
        return "(t: "+t+", v:"+v+")";
    }
}

class MsgQueue {
    ArrayList<Message> msgQ = new ArrayList<>();
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote = new Semaphore(0);
    boolean closing = false;

    public void putMessage(Message msg) throws InterruptedException {
        mutex.acquire();
        msgQ.add(msg);
        piene.release();
        mutex.release();
    }

    public Message getMessage() throws InterruptedException {
        piene.acquire();
        mutex.acquire();

        int minIndex = 0;
        for (int i = 1; i < msgQ.size(); i++) {
            if (msgQ.get(i).t < msgQ.get(minIndex).t) {
                minIndex = i;
            }
        }
        Message msg = msgQ.remove(minIndex);
    if(closing && msgQ.size()==0)
            vuote.release();
        mutex.release();
        return msg;
    }

    public int getSize() throws InterruptedException{
        mutex.acquire();
        int size = msgQ.size();
        mutex.release();
        return size;
    }
}

class Generator extends Thread{
    public Counter counter;
    public MsgQueue msgQueue;
    int TG;

    public Generator(Counter c, MsgQueue q, int tg){
        this.counter = c;
        this.msgQueue = q;
        this.TG = tg;
    }

    public void run(){
        try{
            while(true){
                int value = counter.getValue();
                Message msg = new Message(value, (int)(Math.random()*100));
                msgQueue.putMessage(msg);
                sleep(TG);
            }
        } catch(InterruptedException e){}
    }
}

class Consumer extends Thread{
    public MsgQueue msgQueue;
    int TC;

    public Consumer(MsgQueue q, int tc){
        this.msgQueue = q;
        this.TC = tc;
    }

    public void run(){
        try{
            while(true){
                Message msg = msgQueue.getMessage();
                System.out.println(getName()+" msg: "+msg);
                sleep(TC);
            }
        } catch(InterruptedException e){}
    }
}

