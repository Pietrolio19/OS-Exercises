import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        LuggageQueue lugQueue = new LuggageQueue();
        Progressive_id pid = new Progressive_id();
        int N = 4;
        int M = 6;
        int P = 100;
        int T = 2000;
        int X = 100;

        CheckInThread[] ct = new CheckInThread[N];
        for(int i = 0; i < N; i++){
            ct[i] = new CheckInThread(lugQueue, M, pid, X);
            ct[i].setName("CT"+i);
            ct[i].start();
        }

        LuggageStationThread[] lst = new LuggageStationThread[2*M];
        for(int i = 0; i < M; i++){
            lst[i*2] = new LuggageStationThread(lugQueue, i, T, P);
            lst[i*2].setName("LST1/"+i);
            lst[i*2].start();

            lst[i*2+1] = new LuggageStationThread(lugQueue, i, T, P);
            lst[i*2+1].setName("LST2/"+i);
            lst[i*2+1].start();
        }

        for(int i = 0; i < 10; i++){
            Thread.sleep(1000);
            System.out.println("NumLuggage: " + lugQueue.nLuggage + " " + "NumWaiting: " + LuggageStationThread.nWait + " " + "NumSent: " + LuggageStationThread.nSent);

        }

        System.out.println("CheckIn Finished!");
        for(int i = 0; i < N; i++){
            ct[i].interrupt();
        }
        while(lugQueue.luggages.size() > 0){
            Thread.sleep(1000);
            System.out.println("NumLuggage: " + lugQueue.nLuggage + " " + "NumWaiting: " + LuggageStationThread.nWait + " " + "NumSent: " + LuggageStationThread.nSent);
        }

        lugQueue.setDone();
        System.out.println("Last Call!!!");
        for(LuggageStationThread t : lst){
            t.join();
        }
        System.out.println("NumLuggage: " + lugQueue.nLuggage + " " + "NumWaiting: " + LuggageStationThread.nWait + " " + "NumSent: " + LuggageStationThread.nSent);
    }
}

class Luggage{
    int weight;
    int flight_id;
    int service_id;

    public Luggage(int weight, int flight_id, int service_id){
        this.weight = weight;
        this.flight_id = flight_id;
        this.service_id = service_id;
    }
}

class Progressive_id{
    int id = 1;

    public int getId(){
        return id++;
    }
}

class LuggageQueue{
    ArrayList<Luggage> luggages;
    int nLuggage = 0;
    boolean done = false;

    public LuggageQueue(){
        luggages = new ArrayList<Luggage>();
    }

    public synchronized void putLuggage(Luggage lug){
        luggages.add(lug);
        nLuggage++;
        notifyAll();
    }

    private int checkFlight(int flight){
        int pos = -1;
        for(int i = 0; i < luggages.size(); i++){
            if(luggages.get(i).flight_id == flight){
                pos = i;
                break;
            }
        }
        return pos;
    }

    public synchronized Luggage getLuggage(int id) throws InterruptedException {
        int pos = -1;
        while(!done && (pos = checkFlight(id)) == -1){
            wait();
        }
        if(done)
            return null;

        Luggage v = luggages.remove(pos);
        return v;
    }

    public void setDone(){
        done = true;
        notifyAll();
    }

}

class CheckInThread extends Thread{
    LuggageQueue queue;
    int nFlight;
    int X;
    Progressive_id pid;

    public CheckInThread(LuggageQueue queue, int f, Progressive_id pid, int x){
        this.queue = queue;
        this.nFlight = f;
        this.pid = pid;
        this.X = x;
    }

    public void run(){
        try{
            while(true){
                Luggage luggage = new Luggage(5 + (int)(Math.random()*(20-5)), (int)(Math.random()*nFlight), pid.getId());
                queue.putLuggage(luggage);
                sleep(X);
            }
        }
        catch(InterruptedException e){
            System.out.println("CheckInThread interrupted");
        }
    }
}

class LuggageStationThread extends Thread{
    LuggageQueue queue;
    static int nWait = 0;
    static int nSent = 0;
    int id_flight;
    int T;
    int P;

    public LuggageStationThread(LuggageQueue queue, int f, int t, int p){
        this.queue = queue;
        this.id_flight = f;
        this.T = t;
        this.P = p;
    }

    public void run(){
        try {
            int waitingWeight = 0;
            int inWait = 0;
            boolean go = true;
            while (go) {
                while (waitingWeight < P) {
                    Luggage luggage = queue.getLuggage(id_flight);
                    if (luggage == null) {
                        go = false;
                        break;
                    }
                    waitingWeight += luggage.weight;
                    inWait++;
                }
                synchronized (LuggageStationThread.class) {
                    nWait++;
                }
            }
            if (inWait > 0) {
                System.out.println(getName() + " consegna " + inWait + " bag " + waitingWeight + "kg");
                sleep(T);
                synchronized (LuggageStationThread.class) {
                    nWait -= inWait;
                    nSent += inWait;
                }
                waitingWeight = 0;
                inWait = 0;
            }
        }
        catch(InterruptedException e){
            System.out.println("LuggageStationThread interrupted");
        }
    }
}
