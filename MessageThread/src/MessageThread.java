
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class MessageThread{
    public static void main(String[] args) throws InterruptedException {
        int K1 = 5;
        int K2 = 8;
        int M = 6;

        int NC = (M*K1+K2-1)/K2;
        MsgQueue msgQ = new MsgQueue(K2);
        MsgCollector msgC = new MsgCollector(NC);
        ProcessorThread[] threadList = new ProcessorThread[10];
        for(int i=0; i<threadList.length; i++){
            threadList[i] = new ProcessorThread(msgQ, msgC, K2);
            threadList[i].setName("Pr"+i);
            threadList[i].start();
        }
        PrinterThread printer = new PrinterThread(msgC, NC);
        printer.setName("PRT");
        printer.start();

        for(int i=0; i<M; i++){
            ArrayList<Integer> x = new ArrayList<>();
            for(int j=0; j<x.size(); j++){
                x.add(j);
            }
            msgQ.put(x);
        }
        msgQ.put(null);
        printer.join();
        for(int i=0; i<threadList.length; i++){
            threadList[i].interrupt();
        }
    }

}

class MsgQueue{
    Semaphore piene = new Semaphore(0);
    Semaphore mutex = new Semaphore(1);
    ArrayList<Integer> data = new ArrayList<>();
    int id = 0;
    int K2;

    public MsgQueue(int K2){
        this.K2 = K2;
    }

    void put(ArrayList<Integer> v) throws InterruptedException{
        if (v == null) {
            piene.release(K2);
            return;
        }
        mutex.acquire();
        data.addAll(v);
        mutex.release();
        piene.release(v.size());
    }

    ArrayList<Integer> get(int n) throws InterruptedException{
        piene.acquire(n);
        mutex.acquire();
        ArrayList<Integer> r = new ArrayList<>();
        r.add(id++);
        for(int i=0; data.size() > 0 && i < n; i++){
            r.add(data.removeFirst());
        }
        mutex.release();
        if(r.size() == 1)
            return null;
        return r;
    }
}

class MsgCollector{
    Semaphore[] s;
    ArrayList<Integer>[] data;

    public MsgCollector(int NC){
        s = new Semaphore[NC];
        for(int i=0; i < s.length; i++){
            s[i] = new Semaphore(0);
        }
        data = new ArrayList[NC];
    }

    void put(ArrayList<Integer> v) throws InterruptedException{
        data[v.get(0)] = v;
        s[v.get(0)].release();
    }

    ArrayList<Integer> get(int m) throws InterruptedException{
        s[m].acquire();
        return data[m];
    }


}

class ProcessorThread extends Thread{

    MsgQueue msgQ;
    MsgCollector msgC;
    int K2;

    ProcessorThread(MsgQueue queue, MsgCollector collector, int K2){
        this.msgQ = queue;
        this.msgC = collector;
        this.K2 = K2;
    }
    public void run(){
        try{
            while(true) {
                ArrayList<Integer> x = msgQ.get(K2);
                if(x == null)
                    break;

                sleep(1000+ (int) (Math.random()*2000));
                for(int i=0; i<x.size();i++){
                    x.set(i, x.get(i)*2);
                }
                msgC.put(x);
            }
        } catch(InterruptedException e){}
    }
}

class PrinterThread extends Thread{

    MsgCollector msgC;
    private int NC;

    PrinterThread(MsgCollector collector, int NC){
        this.msgC = collector;
        this.NC = NC;
    }

    public void run(){
        try {
            while(true){
                ArrayList<Integer> xx;
                for(int i=0; i<NC; i++){
                    xx = msgC.get(i);
                    System.out.println(xx);
                }
            }
        }catch(InterruptedException e){}
    }

}