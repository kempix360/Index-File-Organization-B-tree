package memory;

public class Record {
    public static final int RECORD_SIZE = 12;
    private final int first;
    private final int second;
    private final int third;
    private final int key;

    public Record(int length, int width, int height) {
        this.first = length;
        this.second = width;
        this.third = height;
        this.key = length * width;
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
