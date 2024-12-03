package data;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class UniqueKeyGenerator {
    private static final Set<Integer> usedKeys = new HashSet<>();
    private static final Random random = new Random();

    public int generateUniqueKey() {
        int key;
        do {
            key = random.nextInt(Integer.MAX_VALUE); // Generate a positive random integer
        } while (usedKeys.contains(key));
        usedKeys.add(key);
        return key;
    }
}
