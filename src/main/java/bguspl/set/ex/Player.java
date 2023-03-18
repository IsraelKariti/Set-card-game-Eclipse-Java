package bguspl.set.ex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

import bguspl.set.Env;
import java.util.logging.Level;
/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {
	
	// create blocking q for 3 slots
	private BlockingQu keyQueue= new BlockingQu(3);
	
	
    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    
    private Dealer dealer;
    
    boolean penalty = false;
    boolean reward = false;
    public boolean play = false;
    private boolean shouldRemoveToken = false;
    private int tokenToBeRemoved = -1;
    private boolean shouldHandleKeyPress = false;
    private int key = -1;
    private long supposeToFinishPenaltyTime = -1;
    private boolean waitingForResponseFromDealer = false;
    private Vector<Integer> futureDemolition = new Vector<>();
    private boolean clearEraseCardsTokensFromQueueFlag = false;
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer=dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        //System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        if (!human) 
        	createArtificialIntelligence();

        while (!terminate) {
	        synchronized (this) {
				try {
					
					System.out.println("player "+ id + " going to sleep now");
					wait();
					System.out.println("player "+ id + " wake up now");
					System.out.println("player "+id+" penalty is "+ penalty);
					System.out.println("player "+id+" reward is "+ reward);
					System.out.println("player "+id+" shouldRemoveToken is "+shouldRemoveToken);
					System.out.println("player "+id+" shouldHandleKeyPress is "+shouldHandleKeyPress);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
				// TODO implement main player loop
	        	//System.out.println("runnnnnnnnnnnn");
	        	//
	        	if(penalty) {// if the dealer punish the player for 3 tokens not a set
	        		System.out.println("player "+id+" penalty!");
	            	waitingForResponseFromDealer = false;
	            	
	            	// create a thread that changed the time of the player in the ui 
	            	new Thread(() -> {
	            		long count = env.config.penaltyFreezeMillis;
	            		while(count>=0) {
		            		try {
		        				env.ui.setFreeze(id, count);
		        				Thread.sleep(1000);
		        				count = count - 1000;
		        			} catch (InterruptedException e) {
		        				// TODO Auto-generated catch block
		        				e.printStackTrace();
		        			}
	            		}
	            		
	            	}).start();
	            	
	    			try {
	            		//System.out.println("penalty go to sleep for 1 sec!");
	            		
	            		// when do i need to wake up?
	            		supposeToFinishPenaltyTime = System.currentTimeMillis() + env.config.penaltyFreezeMillis;
						
	            		// check if wake up time has passed
	            		while(System.currentTimeMillis() < supposeToFinishPenaltyTime)
						{
	            			// if the wake up time is still in the future go to sleep again
	            			System.out.println("player "+id+" PENALTY go to sleep now  "+System.currentTimeMillis() );
	            			shouldHandleKeyPress = false;
	            			wait(supposeToFinishPenaltyTime - System.currentTimeMillis());
	            			
							System.out.println("player "+id+" wake up from PENALTY! "+ System.currentTimeMillis());
						}
	            		System.out.println("player "+id+" wake up penalty is OVER!!!!");
	
	    				//Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
	        		// clean the key queue 
	        		
	        		
	        		if(!human) {
	        			
	        			clearQueue();
	        			
	        			for (int i=0; i<12; i++) {
	        				table.removeToken(id, i);
	        			}
	        		}
	        		
	        		System.out.println("player "+id+" clear q after penalty!");
	        		penalty = false;
	        	}
	        	if(reward) {
	            	waitingForResponseFromDealer = false;

	            	// thread that create the counter decrease
	            	new Thread(() -> {
	            		long count = env.config.pointFreezeMillis;
	            		while(count>=0) {
		            		try {
		        				env.ui.setFreeze(id, count);
		        				Thread.sleep(1000);
		        				count = count - 1000;
		        			} catch (InterruptedException e) {
		        				// TODO Auto-generated catch block
		        				e.printStackTrace();
		        			}
	            		}
	            	}).start();
	            	
	        		try {
	        			///////////////////////////////
	        			long supposeToFinishPointTime = System.currentTimeMillis() + env.config.pointFreezeMillis;
						
	            		// check if wake up time has passed
	            		while(System.currentTimeMillis() < supposeToFinishPointTime)
						{
	            			// if the wake up time is still in the future go to sleep again
	            			System.out.println("player "+id+" POINT go to sleep now  "+System.currentTimeMillis() );
	            			shouldHandleKeyPress = false;
	            			wait(supposeToFinishPointTime - System.currentTimeMillis());
	            			
							System.out.println("player "+id+" wake up from POINT TIME! "+ System.currentTimeMillis());
						}
	            		System.out.println("player "+id+" wake up point is OVER!!!!");
	        			//////////////////
	        			//wait(env.config.pointFreezeMillis);
	        		} catch (InterruptedException e) {
	        			// TODO Auto-generated catch block
	        			e.printStackTrace();
	        		}
	        		
	        		clearQueue();
	        		System.out.println("player "+id+" clear q after reward!");
	
	        		reward = false;
	        	}
	        	if(shouldRemoveToken) {
	        		System.out.println("player "+id+" removing token "+tokenToBeRemoved);
	        		keyQueue.remove(tokenToBeRemoved);
	        		int keyQLen = keyQueue.size();
	        		String str = "player "+ id + " after removal keyQ size is now "+ keyQLen + " : " ;
	        		System.out.println();
	        		for(int i = 0 ;i < keyQLen ;i++)
	        			str+=(","+ keyQueue.get(i));
	        		System.out.println(str);
	        		shouldRemoveToken = false;
	        	}
	        	if(shouldHandleKeyPress) {
	        		System.out.println("player "+id+" handling key press: "+ key);

	        		keyPressHandleByPlayer(key);
	        		shouldHandleKeyPress = false;
	        	}
	        	if(clearEraseCardsTokensFromQueueFlag) {
	        		clearEraseCardsTokensFromQueueFlag = false;
	        		clearEraseCardsTokensFromQueue();
	        	}
	        }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        //System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    
     }
    
    // this method will be called if some of the cards that hols my tokens were erased
    private void clearEraseCardsTokensFromQueue() {
    	for(int i = 0 ;i < keyQueue.size() ;i++) {
    		table.removeToken(id, keyQueue.get(i));
    	}
    	keyQueue.clear();
//    	int i = 0;
//    	while( i < keyQueue.size()) {
//    		
//    		// get the slot
//    		int slot = keyQueue.get(i);
//    		// check a token is in the table
//    		boolean b = table.checkIfTokenExist(slot, id);
//    		if(!b) {
//    			// if the token is not in the table remove from the keyQ
//    			keyQueue.remove(slot);
//    		}
//    		else {
//				i++;
//			}
//    	}
	}

	/**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement

    	System.out.println("player "+ id+" slot "+slot +" pressed by keyboard");
    	
    	// if the player has 3 cards that are waiting for evaluation from the dealer dont do anything
    	if(waitingForResponseFromDealer ) {
        	System.out.println("player "+id+" slot "+slot +" discarded, reason: waitingForResponseFromDealer");

    		return;
    	}
    	
    	shouldHandleKeyPress = true;
    	key = slot;
    	synchronized (this) {
    		System.out.println("player " + id+" keyPressed "+ slot);
    		notify();
		}
    }
    
    private void keyPressHandleByPlayer(int slot) {
    	
    	System.out.println("player "+id +" is now handling key press "+slot);
    	
    	// check if the token already exist on the table in this slot
    	if(keyQueue.checkIfTokenExist(slot)) {
    		System.out.println("player" + id+"token already exist on slot: "+ slot);
    		// remove the slot from the local blocked queue
    		keyQueue.remove(slot);
    		
    		// remove the slot from the table
    		table.removeToken(id, slot);
    		
    		return;
    	}
    	
    	// check if the slot is in the future demolition
    	synchronized (this) {
    		for(Integer integer : futureDemolition)
        		if(integer.intValue() == slot) {
        			System.out.println("player "+ id+ " slot "+ slot +" dismissed, reason: demolition");
        			
        			return;
        		}
		}
    	
    	
    	// the new slot is checked and wan not found in the blocked queue
    	
    	int size = keyQueue.size();
    	if (size==3) {
    		System.out.println("player " + id + " slot: "+ slot + "  DUMPED!");
    		return;
    	}
    	
    	    	// now we know that the blocked queue is availabe to

    	
    	// check with the table if the slot is available
    	if(table.getCardBySlot(slot)==-1) {
    		System.out.println("player "+ id+" couldn't find card by this slot.........");
    		return;
    	}
    	System.out.println("player " + id + " place token on slot: "+ slot);

    	//placeToken
    	table.placeToken(id, slot);
    	//System.out.println("place token on slot: "+ slot);

    	//adding to the queue of tokens
    	keyQueue.put(slot);
    	
    	//System.out.println("add slot to keyqueue: "+ slot);

    	if (keyQueue.size()==3) {
        	System.out.println("player "+id+" now there are 3 tokens "+keyQueue.get(0)+", "+ keyQueue.get(1)+", "+ keyQueue.get(2));
        	waitingForResponseFromDealer = true;
    		dealer.notifyEvalutationFromPlayer(id);
    	}
    }
    /**
     * Penalize a player and perform other related actions.
     */
    public synchronized void penalty() {
    	System.out.println("player "+ id + " got penalty from dealer!");
        // TODO implement
    	penalty = true;
    	notifyAll();
    }
    
    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public synchronized void point() {
        // TODO implement
    	reward = true;
    	System.out.println("dealer awards point to player "+id+" and than notify!");
    	notify();
    	

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
    	
    	
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
        	
        	env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        	boolean flag = true;
            //System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
//        	int[] slotArr = {-1, -1, -1};
//        	int slotArrIndex = 0;
        	boolean shouldClearQueue = true;
            while (!terminate) {
            	//boolean shouldClearQueue =true;
            	if(play) {
	                // TODO implement player key press simulator
            		
            		try {
            			// time that takes to ai to generate new key press
	                	long sleepTime = ThreadLocalRandom.current().nextLong(20, 40);
	                    synchronized (this) { 
	                    	wait(sleepTime); 
	                    }
	                } catch (InterruptedException ignored) {}
            		
	            	// generate a key
            		
            		int randSlot = ThreadLocalRandom.current().nextInt(12);
            		
            		// generate a key that is not the same as the the keys in the keyQueue
	            	while( keyQueue.checkIfTokenExist(randSlot) ) {
	            		randSlot = ThreadLocalRandom.current().nextInt(12);
	            	}
	            	
	                // tell the player about the new slot 
	                keyPressed(randSlot);
	                
            	}
            	else if(shouldClearQueue)
            	{
            		shouldClearQueue = false;
            		System.out.println("player "+id+" sould clear que");
            		clearQueue();
            		
            	}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
            //System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    	terminate = true;
    }


    
    
    
    public void clearQueue() {
    	//System.out.println( "stack trace:\n"+ Arrays.toString( Thread.currentThread().getStackTrace() ));
    	System.out.println("player "+id+" clear QUEUE");
		keyQueue.clear();
		
    }

    public int getScore() {
        return score;
    }
    
    public synchronized void removeSlotFromKeyQueue(int slot) {
    	System.out.println("dealer told player "+ id+" to remove slot token in "+ slot);
    	shouldRemoveToken = true;
        tokenToBeRemoved = slot;
        futureDemolition.add(slot);
        notify();
    	
    }
    
    // this is called if the player asked the dealer to check 3 cards, 
    //but until the cards were checked by the dealer they already been replace 
    //because another player previously asked to check all or some of them first
    public synchronized void setWaitingForResponseFromDealer() {
    	waitingForResponseFromDealer = false;
    	
    	clearEraseCardsTokensFromQueueFlag = true;
    	
    	
    	notify();
    }
    
    public synchronized void clearFutureDemolition() {
    	futureDemolition.clear();
    }
}
