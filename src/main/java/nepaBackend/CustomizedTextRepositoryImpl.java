package nepaBackend;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.query.dsl.QueryBuilder;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

import nepaBackend.model.DocumentText;

public class CustomizedTextRepositoryImpl implements CustomizedTextRepository {
	  @PersistenceContext
	  private EntityManager em;
	
	  @Override
	  public List<DocumentText> search(String terms, int limit, int offset) {
	    FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
	
	    QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
	        .buildQueryBuilder().forEntity(DocumentText.class).get();
	    Query luceneQuery = queryBuilder
	        .keyword()
	        .onFields("name", "description")
	        .matching(terms)
	        .createQuery();
	
	    // wrap Lucene query in a javax.persistence.Query
	    javax.persistence.Query jpaQuery =
	        fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
	
	    jpaQuery.setMaxResults(limit);
	    jpaQuery.setFirstResult(offset);
	
	    // execute search
	    return jpaQuery.getResultList();
	  }
}
