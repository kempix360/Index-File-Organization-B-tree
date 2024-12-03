package memory;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class PageBtreeFile {
    private int pageID;
    private int parentPageID;
    private List<Integer> keys;
    private List<Integer> addresses;
    private List<Integer> children;

    public PageBtreeFile(int pageID, int parentPageID) {
        this.pageID = pageID;
        this.parentPageID = parentPageID;
        this.keys = new ArrayList<>();
        this.addresses = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public PageBtreeFile(int pageID, int parentPageID, List<Integer> keys, List<Integer> addresses, List<Integer> children) {
        this.pageID = pageID;
        this.parentPageID = parentPageID;
        this.keys = keys;
        this.addresses = addresses;
        this.children = children;
    }

    public int getPageID() {
        return pageID;
    }

    public int getParentPageID() {
        return parentPageID;
    }

    private List<Integer> getKeys() {
        return keys;
    }

    private List<Integer> getAddresses() {
        return addresses;
    }

    private List<Integer> getChildren() {
        return children;
    }

    public String serialize() {
        StringBuilder serialized = new StringBuilder();

        serialized.append("pageID=").append(pageID).append(" | ");
        serialized.append("parentPageID=").append(parentPageID).append(" | ");
        serialized.append("numKeys=").append(keys.size()).append(" | ");
        serialized.append("numChildren=").append(children.size()).append(" | ");
        serialized.append("keys=").append(keys).append(" | ");
        serialized.append("addresses=").append(addresses).append(" | ");
        serialized.append("children=").append(children);

        return serialized.toString();
    }

}
