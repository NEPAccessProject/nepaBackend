package nepaBackend;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISMatch;

@Service
public class EISMatchService {
	@Autowired
	private EISMatchRepository matchRepository;
	
	public List<EISMatch> getAllBy(int id, BigDecimal match_percent) {
		return matchRepository.queryBy(id, match_percent);
	}
	
	public void saveMatch(EISMatch match) {
		matchRepository.save(match);
	}

	public List<EISMatch> getAllBy(Long _id, BigDecimal match_percent) {
		return matchRepository.queryBy(_id, match_percent);
	}

	public List<Object> getAllPairs() {
		return matchRepository.getMetaPairs();
	}

	public List<Object> getAllPairsAtLeastOneFile() {
		return matchRepository.getMetaPairsAtLeastOneFile();
	}

	public List<Object> getAllPairsTwoFiles() {
		return matchRepository.getMetaPairsTwoFiles();
	}

	public List<EISMatch> findAllByDocument1(int id) {
		return matchRepository.findAllByDocument1(id);
	}
	public List<EISMatch> findAllByDocument2(int id) {
		return matchRepository.findAllByDocument2(id);
	}

	// this ends up being too big if the caller doesn't split up the lists, so let's split it up
	// into batches in order to utilize deleteInBatch
	public void deleteInBatch(List<EISMatch> matches) {
		List<List<EISMatch>> batches = getBatches(matches,1000);
		for(List<EISMatch> batch : batches) {
			matchRepository.deleteInBatch(batch);
		}
	}
	
	// so we don't overflow with deleteInBatch()
	private static <T> List<List<T>> getBatches(List<T> collection, int batchSize) {
	    return IntStream.iterate(0, i -> i < collection.size(), i -> i + batchSize)
	            .mapToObj(i -> collection.subList(i, Math.min(i + batchSize, collection.size())))
	            .collect(Collectors.toList());
	}
	
	
}
