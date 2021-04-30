package nepaBackend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneConfig {
    /**
     * Lucene index, storage location
     */
//    private static final String LUCENEINDEXPATHDOCUMENTTEXT="/DocumentText";
//    private static final String LUCENEINDEXPATHEISDOC="/EISDoc";
    /**
     * Create an Analyzer instance
     * 
     * @return
     */
    @Bean
    public StandardAnalyzer analyzer() {
        return new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    }

    /**
     * Lucene EISDoc table index location
     * 
     * @return Directory or null if IOException
     * @throws 
     */
//    @Bean
//    public static Directory directoryEISDoc() {
//        try {
//            Path path = Paths.get(Globals.getMetaIndexString());
//            File file = path.toFile();
//            if(!file.exists()) {
//                // If the folder does not exist, create
//                file.mkdirs();
//            }
//            return FSDirectory.open(path);
//        } catch(IOException e) {
//        	return null;
//        }
//    }
    

    /**
     * Lucene DocumentText table index location
     * 
     * @return Directory or null if IOException
     * @throws 
     */
//    @Bean
//    public static Directory directoryDocumentText() {
//
//        try {
//	        Path path = Paths.get(Globals.getIndexString());
//	        File file = path.toFile();
//	        if(!file.exists()) {
//	            // If the folder does not exist, create
//	            file.mkdirs();
//	        }
//	        return FSDirectory.open(path);
//	    } catch(IOException e) {
//	    	return null;
//	    }
//    }
    
    /**
     * indexWriter init
     * 
     * @param directory
     * @param analyzer
     * @return
     * @throws IOException
     */
//    @Bean
//    public IndexWriter indexWriter(Directory directory, Analyzer analyzer) throws IOException {
//        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
//        MultiIndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
//        // Clear index
//        indexWriter.deleteAll();
//        indexWriter.commit();
//        return indexWriter;
//    }

    /**
     * Searcher Manager init
     * 
     * @param directory
     * @return
     * @throws IOException
     */
//    @Bean
//    public SearcherManager searcherManager(Directory directory, IndexWriter indexWriter) throws IOException {
//        SearcherManager searcherManager = new SearcherManager(indexWriter, false, false, new SearcherFactory());
//        ControlledRealTimeReopenThread cRTReopenThead = new ControlledRealTimeReopenThread(indexWriter, searcherManager,
//                5.0, 0.025);
//        cRTReopenThead.setDaemon(true);
//        // Thread name
//        CRTReopenThead. setName ("Update IndexReader Thread");
//        // Open threads
//        cRTReopenThead.start();
//        return searcherManager;
//    }


    /**
     * IndexReader initialized in config as bean with the goal that we only have to do this 
     * expensive operation once
     * 
     * @return IndexReader if successful, null if it runs into IOException
     * @throws 
     */
    @Bean
    public static IndexReader textReader() {
    	try {
    		File indexFile = new File(Globals.getIndexString());
    		Directory directory = FSDirectory.open(indexFile.toPath());
    		IndexReader textReader = DirectoryReader.open(directory);
    		
    		return textReader;
    	}
    	catch(IOException e) {
    		return null;
    	}
    }
    

//    @Bean
//    public static IndexReader metaReader() {
//    	try {
////    		File indexFile2 = new File(Globals.getMetaIndexString());
////    		Directory directory2 = FSDirectory.open(indexFile2.toPath());
//    		IndexReader metaReader = DirectoryReader.open(directoryEISDoc());
//    		
//    		return metaReader;
//    	}
//    	catch(IOException e) {
//    		return null;
//    	}
//    }
    
    /**
     * MultiReader initialized in config as bean with the goal that we only have to do this 
     * expensive operation once
     * 
     * @return MultiReader if successful, null if it runs into IOException
     * @throws 
     */
//    @Bean
//    public static MultiReader multiReader() {
//    	try {
//    		MultiReader multiIndexReader = new MultiReader(textReader(), metaReader());
//    		
//    		return multiIndexReader;
//    	}
//    	catch(IOException e) {
//    		return null;
//    	}
//    }
    
    /**
     * Searcher initialized in config as bean with the goal that we only have to do this 
     * expensive operation once
     * 
     * @return IndexSearcher if successful, null if reader ran into IOException
     * @throws 
     */
    @Bean
    public IndexSearcher indexSearcher() {
    	try {
    		File indexFile2 = new File(Globals.getMetaIndexString());
    		Directory directory2 = FSDirectory.open(indexFile2.toPath());
    		IndexReader metaReader = DirectoryReader.open(directory2);
    		
			MultiReader multiIndexReader = new MultiReader(textReader(), metaReader);

    		IndexSearcher indexSearcher = new IndexSearcher(multiIndexReader);
    		
    		return indexSearcher;
	    	
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
    	catch(Exception e) {
			System.out.println("Non-IO Problem creating indexSearcher");
    		e.printStackTrace();
			return null;
    	}
    	
	}
//    	MultiReader indexReader = multiReader();
//		
//    	if(indexReader != null) {
//    		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
//    		
//    		return indexSearcher;
//    	} else {
//    		return null;
//    	}
//    }
}