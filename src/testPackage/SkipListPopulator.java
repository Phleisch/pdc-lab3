package testPackage;

import java.util.Random;

import skiplistPackage.LockFreeSkipList;

public class SkipListPopulator {
	public static final int INT_MIN = 0;
	public static final int INT_MAX = (int) 1e7;
	public static final int INT_MEAN = (int) 5e6;
	public static final int INT_STD = (int) 5e7 / 3;
	private static Random r =	new Random();
	
	public static LockFreeSkipList<Integer> populate(LockFreeSkipList<Integer> skipList, int n, String mode) {
		if(mode.equals("uniform")) {
			for(int i = 0; i < n; i++) {
				skipList.add(r.nextInt(INT_MAX));
			}
		}else if(mode.equals("normal")) {
			for(int i = 0; i < n; ) {  // Intentionally don't increment the for loop.
				int next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				if(INT_MIN <= next && next <= INT_MAX) {
					skipList.add(next);
					i++;
				}
			}
		}else {
			System.out.println("INVALID MODE SELECTED IN SkipListPopulator.populate!");
		}
		return skipList;
	}

}
