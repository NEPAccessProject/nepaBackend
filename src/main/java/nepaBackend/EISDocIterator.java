package nepaBackend;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.util.BytesRef;

import nepaBackend.model.EISDoc;


class EISDocIterator implements InputIterator
{
    private Iterator<EISDoc> docIterator;
    private EISDoc currentDoc;

    EISDocIterator(Iterator<EISDoc> docIterator) {
        this.docIterator = docIterator;
    }

    public boolean hasContexts() {
        return true;
    }

    public boolean hasPayloads() {
        return true;
    }

    public Comparator<BytesRef> getComparator() {
        return null;
    }

    // This method needs to return the key for the record; this is the
    // text we'll be autocompleting against.
    public BytesRef next() {
        if (docIterator.hasNext()) {
            currentDoc = docIterator.next();
            try {
                return new BytesRef(currentDoc.getTitle().getBytes("UTF8"));
            } catch (UnsupportedEncodingException e) {
                throw new Error("Couldn't convert to UTF-8");
            }
        } else {
            return null;
        }
    }

    // This method returns the payload for the record, which is
    // additional data that can be associated with a record and
    // returned when we do suggestion lookups.  In this example the
    // payload is a serialized Java object representing our product.
    public BytesRef payload() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(currentDoc);
            out.close();
            return new BytesRef(bos.toByteArray());
        } catch (IOException e) {
        	e.printStackTrace();
            throw new Error("Well that's unfortunate.");
        }
    }

    // This method returns the contexts for the record, which we can
    // use to restrict suggestions.  In this example we use the
    // regions in which a product is sold.
    public Set<BytesRef> contexts() {
//        try {
//            Set<BytesRef> regions = new HashSet();
//            for (String region : currentProduct.regions) {
//                regions.add(new BytesRef(region.getBytes("UTF8")));
//            }
//            return regions;
//        } catch (UnsupportedEncodingException e) {
//            throw new Error("Couldn't convert to UTF-8");
//        }
    	return null;
    }

    // This method helps us order our suggestions
    public long weight() {
        return currentDoc.getId();
    }
}