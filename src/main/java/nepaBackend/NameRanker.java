package nepaBackend;

import java.util.Collections;
import java.util.List;


public class NameRanker {

    private NameComparator nameComparator;


    public NameRanker() {
    	nameComparator = new NameComparator();
    }

    /** Modifies order of elements in filenames in-place: Collections.sort(filenames, NameComparator) */
    public void rank(List<String> filenames) {
        Collections.sort(filenames, this.nameComparator);
    }
}