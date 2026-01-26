package ledprobabilitymodel.math;

public class Combinatorics {
    public static double nCk(int n, int k){ // builds Combination for (n k)
        if (k < 0 || k > n)
            return 0;
        if ( k > n - k) k = n - k;

        double result = 1.0;
        for (int i = 1; i <= k; i++) {
            result *= (n - (k - i));
            result /= i;
        }
        return result;
    }
}
