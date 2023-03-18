package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
//we added
import java.util.Collections;


import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import java.util.logging.Level;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    private long nextTimeToDecreaseTimer = Long.MAX_VALUE;
    private int displayCounterTime;
    long[] decreaseTimerArr = new long[60];
    int decreaseTimerArrIndex = 0;
    public Thread dealerThread = Thread.currentThread();
    //private boolean shouldCheckPlayer = false;
	private boolean givePoint = false;
	private boolean givePenalty = false;
    private Player chosenPlayer= null;
    private Vector<Integer> requestsFromPlayresToCheck3Cards = new Vector<>();
    
    
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
    	
    	//reshuffleTime = System.currentTimeMillis()+60000;
    	// run all the players' threads
    	for(Player p : players)
    		new Thread(p).start();
    	
    	env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        //System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        while (!shouldFinish()) {
        	
        	displayCounterTime = (int)(env.config.turnTimeoutMillis/1000);
            placeAllCardsOnTable();
            
            // notify all robot player they can start generate
            for(Player player : players)
            	player.play = true;
            
            timerLoop();
            
            // tell all players the the round is over so they dont keep generating slots
            for(Player player : players)
            	player.play = false;
            
            //System.out.println("start new minute!!!");
            
            //updateTimerDisplay(false);
            removeAllCardsFromTable();
            
            
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        //System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
    	resetTimer();
    	
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            // now the dealer is awake 
            updateTimerDisplay(false);
            
            // remove cards (chekc for 3 tokens by any user)
            int requestsSize = requestsFromPlayresToCheck3Cards.size();
            System.out.println("dealer requestSize "+ requestsSize);
            while(requestsSize>0) {
            	//get the id of the player to check
            	int idToCheck = requestsFromPlayresToCheck3Cards.elementAt(0);
            	requestsFromPlayresToCheck3Cards.remove(0);
            	
            	System.out.println("dealer should check 3 cards is true for player "+ idToCheck);
	            removeCardsFromTable(idToCheck);
	            placeCardsOnTable();
	            
	            requestsSize = requestsFromPlayresToCheck3Cards.size();
	            //shouldCheckPlayer = false;
            }
            //System.out.println("===================="+System.currentTimeMillis());
            
            long remainingMilli = reshuffleTime - System.currentTimeMillis() ;
            if(remainingMilli < 0) {
            	//System.out.println("remainingMilli on break: " + remainingMilli);
            	break;
            	
            }
        }
        
        //System.out.println("round end");
    }
    
    private void resetTimer() {
    	displayCounterTime = (int)(env.config.turnTimeoutMillis/1000);
    	long currTime = System.currentTimeMillis();
    	//nextTimeToDecreaseTimer= currTime+1000;
    	reshuffleTime = currTime+ env.config.turnTimeoutMillis;
    	// array of times to dcrease clock during the minutes
    	decreaseTimerArr = new long[displayCounterTime+1];
    	for(int i = 0 ;i < displayCounterTime+1 ;i++)
    		decreaseTimerArr[i] = currTime + (i+1)*1000;
    	decreaseTimerArrIndex = 0;
    	
    	env.ui.setCountdown(env.config.turnTimeoutMillis, false);

    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
    	// calculate how long the dealer need to sleep until clock should be decreased
    	//long currTime = System.currentTimeMillis();
    	long sleepDuration = 10;// decreaseTimerArr[decreaseTimerArrIndex] - currTime;
    
    	// go to sleep
    	try {
    		//System.out.println("dealer is almost asleep");
			synchronized (this) {
				if(requestsFromPlayresToCheck3Cards.size()>0) {
					System.out.println("dealer TRY to go to sleep but can't because player waiting...");
					//shouldCheckPlayer = false;
				}
				else {
					// dealer wakes up (maybe by the player, maybe because sleep time is over
					//System.out.println("dealer go to sleep");
					wait(sleepDuration);
					System.out.println("dealer wake up");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    }
    
    // this is called by the player after generating the 3rd card
    public synchronized void notifyEvalutationFromPlayer(int id) {
    	System.out.println("player woke dealer");
    	
    	// wake up the dealer
    	requestsFromPlayresToCheck3Cards.add(id);
    	//shouldCheckPlayer = true;
    	notifyAll();
    }
    
    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
    	System.out.println("dealer updateTimerDisplay");
    	
    	long currTime = System.currentTimeMillis();

    	// check if the time to decrease the clock has passed
    	long remainingMilliSeconds = reshuffleTime - currTime ;
    	
    	// warning 
    	if(remainingMilliSeconds<= env.config.turnTimeoutWarningMillis ) {
    		if(remainingMilliSeconds<0) {
    			System.out.println("less than zero remainingMilliSeconds " + remainingMilliSeconds);
    			env.ui.setCountdown(0, true);
    		}
    		else {
    			System.out.println("remainingMilliSeconds " + remainingMilliSeconds);
    			env.ui.setCountdown(remainingMilliSeconds, true);
    		}

    	}
    	// noraml state - this shoots 100 times in a second
    	else if( currTime >= decreaseTimerArr[decreaseTimerArrIndex]	) {
    		System.out.println("noraml decrease of 0.01 + now is: "+ currTime);
//    		///
//    		// decrease the clock
//    		System.out.println("counter: "+ counterTime);
//    		System.out.println("\n\n");
    		displayCounterTime--;
    		
    		if(displayCounterTime == -1)
    			displayCounterTime = (int)(env.config.turnTimeoutMillis/1000);
    		
    		// increase the next time that the clock needs to be decreased
    		decreaseTimerArrIndex++;
    		
    		// update the ui
    		
    		env.ui.setCountdown(displayCounterTime*1000, false);
    	}
    }
    
    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    	//System.out.println("teminateeeeee");

    	for(Player player : players)
    		player.terminate();
    	
    	terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
    	boolean areThereNoSetsInDeck = env.util.findSets(deck, 1).size() == 0;
    	
    	System.out.println("areThereNoSetsInDeck: "+ areThereNoSetsInDeck);
    	
        return terminate || areThereNoSetsInDeck;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable(int idToCheck) {
        // TODO implement
    	System.out.println("dealer removeCardsFromTable");
    	boolean [][][] playerTokens=table.playerTokens;
    	int numPlayers= playerTokens.length;
    	
    	//for (int i=0; i<numPlayers; i++) {
    		//System.out.println("clear player : "+ i);
    		//board of single player
    		boolean[][] playerBoard = playerTokens[idToCheck];
    		int[] slotSet = new int[3];
    		int setIndex = 0;
    		int rows = playerBoard.length;
    		for (int j=0; j<rows; j++) {
    			int cols = playerBoard[j].length;
    			for (int k=0; k< cols; k++) {
    				if (playerBoard[j][k]) {
    					slotSet[setIndex] = j*4 + k;
    					setIndex++;
    				}
    			}
    		}
    		// check if there are 3 tokens by this player
    		if (setIndex==3) 
    			handle3Cards(slotSet, players[idToCheck]);
    		else {
				System.out.println("dealer no 3 cards were found for player "+ idToCheck);
				// clean the 
				players[idToCheck].setWaitingForResponseFromDealer();
			}
    	//}
    	
    	//System.out.println("remove cards finished");
    }
    
   
    
    private void placeAllCardsOnTable() {
        // TODO implement
    	System.out.println("dealer now placeAllCardsOnTable ");
    	//check if there is a legal set in the deck
    	 List<int[]> exsistSet= env.util.findSets(deck,1);
    	 
    	 //if there is no sets don't put any cards on the table
    	 if (exsistSet.size()==0) {
    		 System.out.println("dealer there are no more sets in the deck");
    		 
    	 }
    	 
    	 shuffleCards();
    	 
    	 int firstEmptySlot;
    	 //get all the empty slots on the table
    	 while ((firstEmptySlot=table.getEmptySlot()) != -1) {
	    	 //get the first card from the deck
	    	 int card= deck.remove(0);
	    	 //put the card on the table
	    	 table.placeCard(card,firstEmptySlot);
    	 }
    }
    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
    	System.out.println("dealer now placeCardsOnTable ");

    	 
    	 int firstEmptySlot;
    	 //get all the empty slots on the table
    	 while ((firstEmptySlot=table.getEmptySlot()) != -1) {
	    	 //get the first card from the deck
	    	 int card= deck.remove(0);
	    	 //put the card on the table
	    	 table.placeCard(card,firstEmptySlot);
    	 }

 		if(givePoint) {
 			System.out.println("give point");
 			givePoint = false;
 			chosenPlayer.point();
		
 		}
 		
		if(givePenalty) {
 			System.out.println("give penalty");

			givePenalty = false;
			chosenPlayer.penalty();
			
		}
		
		// now all the cards are back on the table so tell all the players that they dont need to be afraid to put cards wherever they want
		// in other words clear the futureDemolition
		for(Player player : players) {
			player.clearFutureDemolition();
		}
		
    }
    	
	//shuffle the cards 
    private void shuffleCards() {
		Collections.shuffle(deck);
    }
    
    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
    	System.out.println("remove all cards from table");
    	
    	int cardSlot;
    	while ((cardSlot=table.getCardSlot())!=-1) {
    		
    		//get the card 
    		int card= table.getCardBySlot(cardSlot);
    		
    		// remove the card from the table and it's tokens
    		table.removeCard(cardSlot);
    		
    		
    		// add the card back into the deck
    		deck.add(card);
    	}
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    	 removeAllCardsFromTable();
    	  
    	int maxScore = -1;
    	
    	int[] winners = new int[6];
    	int winnersIndex = 0;
    	// check who have the most points
    	for(Player player : players) {
    		
    		// get the curr player score
    		int currScore = player.getScore();
    		
    		// check if the curr player score is the highest so far
    		if(currScore > maxScore) {
    			
    			// reset the 
    			for( int i = 0 ;i < winners.length ;i++)
    				winners[i] = -1;
    			
    			// reset index
    			winnersIndex = 0;
    			
    			winners[winnersIndex] = player.id;
    			
    			// increase the index of the array of winners
    			winnersIndex++;
    			
    			maxScore = currScore;
    			
    		}
    		else if(currScore == maxScore) {
    			// if the score of the curr player is equal to the max score so far
    			winners[winnersIndex] = player.id;
    			
    			//  increase the index of array
    			winnersIndex++;
    			
    		}
    	}
    	
    	// make the winners array shorter
    	int[] finalWinnersArr = new int[winnersIndex];
    	for(int i = 0 ;i < winnersIndex ;i++)
    		finalWinnersArr[i]= winners[i]; 
    	
    	// now we have array of all winners ids
    	env.ui.announceWinner(finalWinnersArr);
    }
    
    //cheking if it is a legal set and act accorrdingly
    public void handle3Cards(int [] slotArray, Player player) {
        System.out.println("dealer handle3Cards");

    	//convert slot array to a cards array
    	int [] cardsToCheck= table.convertSlotArrToCard(slotArray);
    	//check if the set is legal
    	boolean isSet= env.util.testSet(cardsToCheck);
    	// 
    	//award and freeze
    	if (isSet) {
    		System.out.println("handleSet: is set");

    		// remove all 3 cards
    		for(int slot : slotArray) {
    			
    			// before removing card : remove the tokens from the players queues
    			for(Player p: players)
    				//if(p.id != player.id)/////////////////////////////////////////////////////
    					p.removeSlotFromKeyQueue(slot);
    			
    			// remove single card
    			table.removeCard(slot);
    		}
    		chosenPlayer = player;
    		givePoint = true;
    		
    		
    		// reset the time to 60
    		resetTimer();
    		
    		//System.out.println("handle set: counteTime "+ displayCounterTime);
    	}
    	else {
    		System.out.println("handleSet: is NOT set");

    		//penalty
    		//clean all player's tokens
//    		for (int i=0; i<12; i++) {
//    			table.removeToken(player.id, i);
//    		}

    		
    		// make the player freeze for 3 seconds
    		chosenPlayer = player;
    		givePenalty = true;


    	}
    	

    }
}
