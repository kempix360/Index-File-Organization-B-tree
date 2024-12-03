package memory;

public class Record {
    public static final int RECORD_SIZE = 16;
    private final int first;
    private final int second;
    private final int third;
    private final int key;

    public Record(int first, int second, int third, int key) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.key = key;
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    public int getThird() {
        return third;
    }

    public int getKey() {
        return key;
    }

    @Override
    public String toString() {
        return first + " " + second + " " + third + " Key: " + key;
    }
}
