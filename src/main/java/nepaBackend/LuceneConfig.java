package nepaBackend;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneConfig {
    /**
     * Lucene index, storage location
     */
//    private static final String LUCENEINDEXPATH="lucene/indexDir/";
    /**
     * Create an Analyr instance
     * 
     * @return
     */
    @Bean
    public Analyzer analyzer() {
        return new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    }
//
//    /**
//     * Index location
//     * 
//     * @return
//     * @throws IOException
//     */
//    @Bean
//    public Directory directory() throws IOException {
//        
//        Path path = Paths.get(LUCENEINDEXPATH);
//        File file = path.toFile();
//        if(!file.exists()) {
//            // If the folder does not exist, create
//            file.mkdirs();
//        }
//        return FSDirectory.open(path);
//    }
//    
//    /**
//     * Create indexWriter
//     * 
//     * @param directory
//     * @param analyzer
//     * @return
//     * @throws IOException
//     */
//    @Bean
//    public IndexWriter indexWriter(Directory directory, Analyzer analyzer) throws IOException {
//        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
//        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
//        // Clear index
//        indexWriter.deleteAll();
//        indexWriter.commit();
//        return indexWriter;
//    }
//
//    /**
//     * Searcher Manager Management
//     * 
//     * @param directory
//     * @return
//     * @throws IOException
//     */
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
}