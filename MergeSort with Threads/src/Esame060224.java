
import java.util.ArrayList;
import java.util.concurrent.Semaphore;


public class Esame060224 {

    public static void main(String[] args) throws InterruptedException{
        int N = 20;
        MergeQueue mergeQueue = new MergeQueue(N);

        MergeThreads[] mergeThreads = new MergeThreads[N];
        for(int i = 0; i < N; i++){
            int v = (int)(Math.random() * 101);
            System.out.println("value: " + v);
            mergeQueue.add(new int[] {v});

        }

        for(int i = 0; i < N; i++){
            mergeThreads[i] = new MergeThreads(mergeQueue);
            mergeThreads[i].setName("MT" + i);
            mergeThreads[i].start();
        }

        int[] r = mergeQueue.getResult();
        System.out.println("result = "+MergeThreads.ArrayToString(r));
        for(MergeThreads mergeThread : mergeThreads){
            mergeThread.interrupt();
        }
    }
}


class MergeQueue {
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);
    private Semaphore last = new Semaphore(0);
    private ArrayList data = new ArrayList();
    private int N;

    public MergeQueue(int N){
        this.N = N;
    }

    public void add(Object o) throws InterruptedException {
        mutex.acquire();
        data.add(o);
        int[] a = (int[]) o;
        if(a.length == N)
            last.release();
        mutex.release();
        piene.release();
    }

    public Object[] get() throws InterruptedException {
        piene.acquire(2);
        mutex.acquire();
        Object[] r = new Object[2];
        r[0] = data.remove(0);
        r[1] = data.remove(0);
        mutex.release();
        return r;
    }

    public int[] getResult() throws InterruptedException {
        last.acquire();
        int[] r = (int[]) data.remove(0);
        return r;
    }
}

class MergeThreads extends Thread {
    MergeQueue mq;

    public MergeThreads(MergeQueue mq){
        this.mq = mq;
    }

    public void run() {
        try{
            while(true){
                Object[] v = mq.get();
                int[] r = merge((int[]) v[0], (int[]) v[1]);
                System.out.println(getName() +
                        " array1 =" + ArrayToString((int[]) v[0])+
                        " array2 =" + ArrayToString((int[]) v[1])+
                        " result =" + ArrayToString(r));
                mq.add(r);
            }
        } catch(InterruptedException ie){

        }
    }

    public static String ArrayToString(int[] v) {
        String s = "[";
        for(int i=0;i<v.length; i++) {
            s += v[i]+ " ";
        }
        s += "]";
        return s;
    }

    private int[] merge(int[] a1, int[] a2) {
        int pos1 = 0, pos2 = 0, pos = 0;
        int[] r = new int[a1.length + a2.length];

        while( pos1 < a1.length && pos2 < a2.length) {
            if(a1[pos1] <= a2[pos2]) {
                r[pos] = a1[pos1];
                pos++;
                pos1++;
            } else {
                r[pos] = a2[pos2];
                pos++;
                pos2++;
            }
        }
        while(pos1 < a1.length) //cycles necessary if there's still some values inside one array or the other
            r[pos++] = a1[pos1++];
        while(pos2 < a2.length)
            r[pos++] = a2[pos2++];
        return r;
    }
}