package nepaBackend;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;

@Service
public class DocService {
	@Autowired
	private DocRepository docRepository;
	
	public List<EISDoc> getAllDistinctBy(int id, List<Integer> idList1, List<Integer> idList2) {
		return docRepository.queryBy(id, idList1, idList2);
	}
	
	public EISDoc saveEISDoc(EISDoc doc) {
		return docRepository.save(doc);
	}
	
//	public List<String> getAllTitles() {
//		return docRepository.queryAllTitles();
//	}
	
	// TODO: Do a natural language mode search to get top X (5-10?) suggestions
	// and then plug them into the search box as selectable suggestions
	public List<String> getByTitle(String title) {
		return docRepository.queryByTitle(title);
	}

	public Optional<EISDoc> findById(Long id) {
		return docRepository.findById(id);
	}

	public List<EISDoc> getAllDistinctBy(Long _id, List<Integer> idList1, List<Integer> idList2) {
		return docRepository.queryBy(_id, idList1, idList2);
	}

	public List<EISDoc> findAllByAgency(String agency) {
		return docRepository.findAllByAgency(agency);
	}

	public List<EISDoc> findAll() {
		return docRepository.findAll();
	}
	
	public List<EISDoc> findMissingProcesses() {
		return docRepository.findMissingProcesses();
	}

	public List<EISDoc> findAllDuplicates() {
		return docRepository.findAllDuplicates();
	}

	public List<EISDoc> findAllByProcessId(Long processId) {
		return docRepository.findAllByProcessId(processId);
	}

	public List<EISDoc> findAllDuplicatesProcess() {
		return docRepository.findAllDuplicatesProcess();
	}

	public List<EISDoc> sizeUnder200() {
		return docRepository.sizeUnder200();
	}

	public List<EISDoc> findAllDuplicatesCloseDates() {
		return docRepository.findAllDuplicatesCloseDates();
	}
	
	public List<EISDoc> findAllSameTitleType() {
		return docRepository.findAllSameTitleType();
	}

	public boolean existsByTitleTypeDateOld(String title, String documentType, LocalDate registerDate) {
		return docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(title, documentType, registerDate).isPresent();
	}
	/** Title is compared alphanumerically (special characters/punctuation "ignored" on each side, 
	 * spaces still matter); type and date are compared exactly */
	public boolean existsByTitleTypeDate(String title, String documentType, LocalDate registerDate) {
		return docRepository.findByTitleTypeDateCompareAlphanumericOnly(title, documentType, registerDate).isPresent();
	}

	public List<EISDoc> findAllFinalsWithFirstRodDates() {
		return docRepository.findAllFinalsWithFirstRodDates();
	}

	public List<String> findMissingFilenames() {
		return docRepository.findMissingFilenames();
	}
	
	/** Returns records with attached files and listed size > 0 bytes, but nothing has been indexed. 
	 * This implies something went wrong with extraction, parsing or indexing - if it couldn't extract
	 * or parse, then the archive may be corrupted and may need to be fixed (replaced). */
	public List<EISDoc> findNotIndexed() {
		return docRepository.findNotIndexed();
	}
	
	/** List of records with size > 0 bytes, yet no files are extracted/recorded */
	public List<EISDoc> findNotExtracted() {
		return docRepository.findNotExtracted();
	}

}
