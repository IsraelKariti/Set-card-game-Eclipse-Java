package bguspl.set.ex;


import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

import bguspl.set.Env;


public class BlockingQu{
	// a vector, used to implement the queue
    private Vector<Integer> vec;
    private final int MAX;

    public BlockingQu(int max) {
        MAX = max;
        vec = new Vector<>();
     }

    public synchronized int size(){
        return vec.size();
    }

    public synchronized void put(Integer e){
        while(size()>=MAX){
            try{
                this.wait();
            } catch (InterruptedException ignored){}
        }

        vec.add(e);
        // wakeup everybody. If someone is waiting in the take()
        // method, it can now perform the take.
        this.notifyAll();
    }

    public synchronized Integer take(){
        while(size()==0){
            try{
                this.wait();
            } catch (InterruptedException ignored){}
        }

        Integer e = vec.get(0);
        vec.remove(0);
        // wakeup everybody. If someone is waiting in the put()
        // method, it can now perform the put.
        this.notifyAll();
        return e;
    }
    
    public synchronized void remove(Integer slot) {
    	for(Integer slotInteger : vec) {
    		if(slotInteger.intValue() == slot) {
    			vec.remove(slotInteger);
    			break;
    		}
    	}
    }
    
    public synchronized boolean checkIfTokenExist(Integer slot) {
    	
    	for(Integer currSlot : vec) {
			if(currSlot.intValue() == slot) {
				return true;
			}
		}
    	
    	return false;
    }
    
    public synchronized void clear() {
    	vec.clear();
    }
    
    public synchronized int get(int index) {
    	return vec.get(index);
    }
}