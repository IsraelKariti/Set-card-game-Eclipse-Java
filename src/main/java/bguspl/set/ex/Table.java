package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

	volatile boolean[][][] playerTokens= new boolean[6][3][4];
	
	
    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }
    
    //return the index of the first EMPTY slot, return -1 if none
    public int getEmptySlot() {
    	for (int i=0; i<slotToCard.length; i++) {
    		if (slotToCard[i]==null)
    			return i;
    	}
    	return -1;
    }
    
    //return the first EXISTING card on the table, -1 if none
    public int getCardSlot() {
    	for (int i=0; i<slotToCard.length; i++) {
    		if (slotToCard[i]!=null)
    			return i;
    	}
    	return -1;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        
        //place the card visualy on the table
        env.ui.placeCard(card, slot);
    }
    
    public int getCardBySlot (int slot) {
    	if(slotToCard[slot] == null)
    		return -1;
    	else
    		return slotToCard[slot];
    }
    
    public void removeCardByCardNumber(int card) {
    	removeCard(cardToSlot[card]);
    }
    
    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        
        
        int row=slot/4;
    	int col=slot%4;
    	//iterate all players and changing the token matrix value to false
    	for (boolean [][] player: playerTokens)
    		player[row][col]=false;
    	
    	
        env.ui.removeTokens(slot);
        env.ui.removeCard(slot);
        
        cardToSlot[slotToCard[slot]] = null;
        slotToCard[slot] = null;
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public synchronized void placeToken(int player, int slot) {
        // TODO implement
    	
    	env.ui.placeToken(player, slot);
    	int row=slot/4;
    	int col=slot%4;
    	playerTokens [player][row][col]=true;
    	
    }
    
    public boolean checkIfTokenExist(int player, int slot) {
    	int row=slot/4;
    	int col=slot%4;
    	return playerTokens [player][row][col];
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public synchronized boolean removeToken(int player, int slot) {
        // TODO implement
    	System.out.println("table remove token: "+ slot);
    	int row=slot/4;
    	int col=slot%4;
    	//checks if there is a token on the card
    	   	if (playerTokens [player][row][col]==true) {
    		playerTokens [player][row][col]=false;
    		
    		env.ui.removeToken(player, slot);
    		return true;
    	}
    	
        return false;
    }
    
    public int [] convertSlotArrToCard (int [] slotArray) {
    	
    	int [] cardArray= new int [slotArray.length];
    	for (int i=0; i<cardArray.length; i++) {
    		cardArray[i]=slotToCard[slotArray[i]];
    	}
    	return cardArray;
    }
}
