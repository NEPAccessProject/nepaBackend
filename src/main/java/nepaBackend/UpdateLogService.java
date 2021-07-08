package nepaBackend;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;
import nepaBackend.model.UpdateLog;

@Service
public class UpdateLogService {
	@Autowired
	private UpdateLogRepository updateLogRepository;
	@Autowired
	private DocRepository docRepository;


	/** Restore given EISDoc from given update log, save eisdoc, save pre-restore update log; 
	 * return restored EISDoc */
	public EISDoc restore(EISDoc docToRestore, UpdateLog logToRestoreFrom, String userID) {
		UpdateLog newLog = newUpdateLogFromEIS(docToRestore, userID);
		EISDoc restored = fillEISFromUpdateLog(docToRestore, logToRestoreFrom);
		
		EISDoc saved = docRepository.save(restored);
		
		updateLogRepository.save(newLog);
		
		return saved;
	}
	
	/** Creates update log using fields from a given EISDoc, and user ID.
	 *  Does not actually save it to database. */
	public UpdateLog newUpdateLogFromEIS(EISDoc recordToUpdate, String id) {
		UpdateLog updateLog = new UpdateLog();
		
		updateLog.setDocumentId(recordToUpdate.getId());
		updateLog.setAgency(recordToUpdate.getAgency());
		updateLog.setDepartment(recordToUpdate.getDepartment());
		updateLog.setCooperatingAgency(recordToUpdate.getCooperatingAgency());
		updateLog.setTitle(recordToUpdate.getTitle());
		updateLog.setDocument(recordToUpdate.getDocumentType());
		updateLog.setFilename(recordToUpdate.getFilename());
		updateLog.setState(recordToUpdate.getState());
		updateLog.setFolder(recordToUpdate.getFolder());
		updateLog.setLink(recordToUpdate.getLink());
		updateLog.setNotes(recordToUpdate.getNotes());
		updateLog.setSummary(recordToUpdate.getSummaryText());
		updateLog.setDate(recordToUpdate.getRegisterDate());
		updateLog.setProcessId(recordToUpdate.getProcessId());
		
		updateLog.setUserId(Long.parseLong(id));
		
		return updateLog;
	}
	
	public EISDoc fillEISFromUpdateLog(EISDoc recordToRestore, UpdateLog logToRestoreFrom) {
		
		recordToRestore.setAgency(logToRestoreFrom.getAgency());
		recordToRestore.setDepartment(logToRestoreFrom.getDepartment());
		recordToRestore.setCooperatingAgency(logToRestoreFrom.getCooperatingAgency());
		recordToRestore.setTitle(logToRestoreFrom.getTitle());
		recordToRestore.setDocumentType(logToRestoreFrom.getDocument());
		recordToRestore.setFilename(logToRestoreFrom.getFilename());
		recordToRestore.setState(logToRestoreFrom.getState());
		recordToRestore.setFolder(logToRestoreFrom.getFolder());
		recordToRestore.setLink(logToRestoreFrom.getLink());
		recordToRestore.setNotes(logToRestoreFrom.getNotes());
		recordToRestore.setSummaryText(logToRestoreFrom.getSummary());
		recordToRestore.setRegisterDate(logToRestoreFrom.getDate());
		recordToRestore.setProcessId(logToRestoreFrom.getProcessId());
		
		return recordToRestore;
	}

	public List<BigInteger> getDistinctDocumentsFromDateRange(String start, String end, String userid) {
		if(userid.isBlank()) {
			return updateLogRepository.getDistinctDocumentsFromDateRange(start,end);
		} else {
			return updateLogRepository.getDistinctDocumentsFromDateRangeAndUser(start,end,Long.parseLong(userid));
		}
	}

	public Optional<UpdateLog> getEarliestByDocumentIdAfterDateAndUser(Long id, String dateStart, String user) {
		if(user.isBlank()) {
			return updateLogRepository.getByDocumentIdAfterDateTime(id, dateStart);
		} else {
			return updateLogRepository.getByDocumentIdAfterDateTimeByUser(id, dateStart, Long.parseLong(user));
		}
	}
	
}