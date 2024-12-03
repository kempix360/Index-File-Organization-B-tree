package memory;
import java.util.ArrayList;
import java.util.List;

public class PageRecordFile {
    private final int pageID;
    private List<Record> records = new ArrayList<>();

    public PageRecordFile(int pageID, List<Record> records) {
        this.pageID = pageID;
        this.records = records;
    }
}
