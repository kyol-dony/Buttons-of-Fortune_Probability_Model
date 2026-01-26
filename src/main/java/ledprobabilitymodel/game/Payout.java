package ledprobabilitymodel.game;

// Provides payout and net calculation based on number of good LEDs claimed
public class Payout {
    public static int payout(int good){ // corresponding payout for # good LEDs claimed
        switch (good){
            case 1: return 5;
            case 2: return 10;
            case 3: return 30;
            default: return 0;
        }
    }

    public static int net(int good, int k){
        return payout(good) - k;
    }
}
