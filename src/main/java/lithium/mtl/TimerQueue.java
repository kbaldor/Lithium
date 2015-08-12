package lithium.mtl;

import lithium.Handler;
import lithium.Transaction;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Created by kbaldor on 8/1/15.
 */
public class TimerQueue {
    public static void cancel(TimerEntry handler) {
        if(timerQ.contains(handler)) timerQ.remove(handler);
    }

    public static class TimerEntry implements Comparable<TimerEntry> {
        private final long expirationTime_ms;
        private final Handler<Transaction> action;
        private static long nextSeq;
        //TODO: I don't think that this should matter. Determine whether this is the case.
        private long seq;

        public TimerEntry(long expirationTime_ms, Handler<Transaction> action) {
            this.expirationTime_ms = expirationTime_ms;
            this.action = action;
            this.seq = nextSeq++;
        }

        @Override
        public int compareTo(TimerEntry o) {
            int answer = (int)(expirationTime_ms-o.expirationTime_ms);
            if (answer == 0) {  // Same rank: preserve chronological sequence.
                if (seq < o.seq) answer = -1; else
                if (seq > o.seq) answer = 1;
            }
            return answer;
        }

    }


    private static final PriorityQueue<TimerEntry> timerQ = new PriorityQueue<TimerEntry>();
    private static void flushTimerQ(){
        synchronized (timerQ) {
            long transactionStartTime = System.currentTimeMillis();
            if (!timerQ.isEmpty() && (timerQ.peek().expirationTime_ms < transactionStartTime)) {
                long timerExpirationTime = timerQ.peek().expirationTime_ms;
                ArrayList<TimerEntry> timerEntries = new ArrayList<TimerEntry>();
                while (!timerQ.isEmpty() && (timerQ.peek().expirationTime_ms == timerExpirationTime)) {
                    timerEntries.add(timerQ.poll());
                }
                Transaction.run(new Handler<Transaction>() {
                    @Override
                    public void run(Transaction transaction) {
                        transaction.transactionTime = timerExpirationTime;
//                        System.out.println("Running timer entry.");
                        for (TimerEntry entry : timerEntries) {
                            entry.action.run(transaction);
                        }
                    }
                });
            }
        }
    }

    static private Object myTimeoutThreadLock = new Object();
    static private Thread myTimeoutThread;

    static private void signalTimeoutThread() {

        synchronized (myTimeoutThreadLock) {
            if(myTimeoutThread==null){
                myTimeoutThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(true) { // run forever TODO: create shutdown mechanism
                            long currentTime_ms = System.currentTimeMillis();
                            Transaction.run((t) -> flushTimerQ());
                            long sleepTime_ms = timerQ.isEmpty()?1000:(timerQ.peek().expirationTime_ms - currentTime_ms);
                            try {
                                Thread.sleep(sleepTime_ms);
                            } catch (InterruptedException e) {
//                              This likely means that a new event has been added. Now we'll just look
//                              for an event and sleep
                            }
                        }
                    }
                });
                myTimeoutThread.start();
            }
            myTimeoutThread.interrupt();
        }
    }



    public static TimerEntry addFutureEvent(long delay_ms, Handler<Transaction> event){
        long transactionTime = delay_ms;
        if(Transaction.getCurrentTransaction()!=null){
            delay_ms+=Transaction.getCurrentTransaction().getTransactionTime();
        } else {
            delay_ms+=System.currentTimeMillis();
        }
        TimerEntry entry = new TimerEntry(delay_ms, event);
        synchronized (timerQ) {
            timerQ.add(entry);
            signalTimeoutThread();
        }
        return entry;
    }

}
