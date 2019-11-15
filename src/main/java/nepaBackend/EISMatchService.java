package nepaBackend;

import java.math.BigDecimal;
import java.util.List;

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
}
