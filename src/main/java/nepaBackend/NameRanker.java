package nepaBackend;

import java.util.Collections;
import java.util.List;


public class NameRanker {

    private NameComparator nameComparator;


    public NameRanker() {
    	nameComparator = new NameComparator();
    }

    public void rank(List<String> filenames) {
        Collections.sort(filenames, this.nameComparator);
    }
}