package nepaBackend;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;

import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.NEPAFile;

@Service
public class NEPAFileService {
	@Autowired
	private NEPAFileRepository nepaFileRepo;
	@Autowired
	private DocRepository docRepository;
	@Autowired
	private TextRepository textRepository;

	/** 
	 * Given folder name and document id, purge all nepafiles for those values. 
	 * First, get the filenames from those nepafiles and purge all documenttexts for those 
	 * filenames and document IDs. 
	 * 
	 * If there are duplicate filenames, we have to pick one to delete arbitrarily. Caller should account
	 * for the very unlikely issue where the filenames are the same yet contents differ.
	 * 
	 * Should also return paths needed to delete the relevant files from disk.
	 **/
	public List<String> deleteAllForFolderAndId(String folderName, String eisdoc_id, String newFolder) {
		List<String> results = new ArrayList<String>();
		
		EISDoc eisdoc = docRepository.findById( Long.parseLong(eisdoc_id) ).get();

		// If we're deleting the newest "set" we can expect to update the folder name to a previous one.
		if(Globals.saneInput(newFolder)) { 
			// Update folder
			eisdoc.setFolder(newFolder);
			eisdoc = docRepository.save(eisdoc);
		}
		
		
		List<NEPAFile> nepafiles = nepaFileRepo.findAllByFolderAndEisdocIn( folderName, eisdoc );
		for(NEPAFile nepafile : nepafiles) {
			try {
				DocumentText docText = textRepository
							.findByEisdocAndFilenameIn( eisdoc, nepafile.getFilename() )
							.get();
				// Path to file on disk that must be deleted separately
				results.add(nepafile.getRelativePath() + nepafile.getFilename());
				// Delete document text
				textRepository.delete(docText);
			} catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
				// Duplicate file, and therefore more than one result: Delete one arbitrarily
				// Note: Theoretically possible to have duplicate filename but not duplicate contents
				List<DocumentText> texts = textRepository.findAllByEisdocAndFilenameIn(eisdoc, nepafile.getFilename());
				results.add(nepafile.getRelativePath() + nepafile.getFilename());
				textRepository.delete(texts.get(0));
			}
		}
		for(NEPAFile nepafile: nepafiles) {
			// Delete nepafile
			nepaFileRepo.delete(nepafile);
		}
		
		
		// Return list of relative file paths to be deleted from disk
		return results;
	}
	
}