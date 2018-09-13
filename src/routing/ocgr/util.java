package routing.ocgr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

public class util {
	public util() { }
	
	public double list_mean(List l, int size) {
	    List<Double> mylist = l.subList(0, size);
		return mylist.stream().mapToDouble(val -> val).average().orElse(0.0);
	}
	
    // TODO: AUXILIARY METHODS --> COULD BE MOVED TO UTILS
    
	/*
	 * from: https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
	 */
	public static double round (double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places,  RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
}
