package nepaBackend;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Globals {
    public static final boolean TESTING = false;
    // Database/file server URL to base folder containing all files exposed to DAL for download
    public static final String DOWNLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
    // Database/file server URL for Express service which handles new file uploads (and potentially updating or deleting files)
    public static final String UPLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:5309/";
    
    public static final List<String> EIS_TYPES = Arrays.asList("Draft Supplement",
			"Final Supplement",
			"Second Draft Supplemental",
			"Second Draft",
			"Adoption",
			"LF",
			"Revised Final",
			"LD",
			"Third Draft Supplemental",
			"Second Final",
			"Second Final Supplemental",
			"DC",
			"FC",
			"RF",
			"RD",
			"Third Final Supplemental",
			"DD",
			"Revised Draft",
			"NF",
			"F2",
			"D2",
			"F3",
			"DE",
			"FD",
			"DF",
			"FE",
			"A3",
			"A1");
    
	// TODO: Smarter sanity check
	public static final boolean saneInput(String sInput) {
		if(sInput == null) {
			return false;
		}
		return (sInput.trim().length() > 0);
	}

	public static final boolean saneInput(String[] sInput) {
		if(sInput == null || sInput.length == 0) {
			return false;
		}
		return true;
	}

	public static final boolean saneInput(boolean bInput) {
		return bInput;
	}

	// TODO: Validation for everything, like Dates
	
	public static final boolean saneInput(int iInput) {
		if(iInput > 0 && iInput <= Integer.MAX_VALUE) {
			return true;
		}
		return false;
	}

//	private static final Map<String,String> agencies = new HashMap<String, String>() {/**
//		 * 
//		 */
//		private static final long serialVersionUID = 4836502066730819061L;
//
//	{
//		agencies.put("ACHP","Advisory Council on Historic Preservation");
//		agencies.put("USAID","Agency for International Development");
//		agencies.put("ARS","Agriculture Research Service");
//		agencies.put("APHIS","Animal and Plant Health Inspection Service");
//		agencies.put("AFRH","Armed Forces Retirement Home");
//		agencies.put("BPA","Bonneville Power Administration");
//		agencies.put("BIA","Bureau of Indian Affairs");
//		agencies.put("BLM","Bureau of Land Management");
//		agencies.put("USBM","Bureau of Mines");
//		agencies.put("BOEM","Bureau of Ocean Energy Management");
//		agencies.put("BOP","Bureau of Prisons");
//		agencies.put("BR","Bureau of Reclamation");
//		agencies.put("Caltrans","California Department of Transportation");
//		agencies.put("CHSRA","California High-Speed Rail Authority");
//		agencies.put("CIA","Central Intelligence Agency");
//		agencies.put("NYCOMB","City of New York, Office of Management and Budget");
//		agencies.put("CDBG","Community Development Block Grant");
//		agencies.put("CTDOH","Connecticut Department of Housing");
//		agencies.put("BRAC","Defense Base Closure and Realignment Commission");
//		agencies.put("DLA","Defense Logistics Agency");
//		agencies.put("DNA","Defense Nuclear Agency");
//		agencies.put("DNFSB","Defense Nuclear Fac. Safety Board");
//		agencies.put("DSA","Defense Supply Agency");
//		agencies.put("DRB","Delaware River Basin Commission");
//		agencies.put("DC","Denali Commission");
//		agencies.put("USDA","Department of Agriculture");
//		agencies.put("DOC","Department of Commerce");
//		agencies.put("DOD","Department of Defense");
//		agencies.put("DOE","Department of Energy");
//		agencies.put("HHS","Department of Health and Human Services");
//		agencies.put("DHS","Department of Homeland Security");
//		agencies.put("HUD","Department of Housing and Urban Development");
//		agencies.put("DOJ","Department of Justice");
//		agencies.put("DOL","Department of Labor");
//		agencies.put("DOS","Department of State");
//		agencies.put("DOT","Department of Transportation");
//		agencies.put("TREAS","Department of Treasury");
//		agencies.put("VA","Department of Veteran Affairs");
//		agencies.put("DOI","Department of the Interior");
//		agencies.put("DEA","Drug Enforcement Administration");
//		agencies.put("EDA","Economic Development Administration");
//		agencies.put("ERA","Energy Regulatory Administration");
//		agencies.put("ERDA","Energy Research and Development Administration");
//		agencies.put("EPA","Environmental Protection Agency");
//		agencies.put("FSA","Farm Service Agency");
//		agencies.put("FHA","Farmers Home Administration");
//		agencies.put("FAA","Federal Aviation Administration");
//		agencies.put("FCC","Federal Communications Commission");
//		agencies.put("FEMA","Federal Emergency Management Agency");
//		agencies.put("FEA","Federal Energy Administration");
//		agencies.put("FERC","Federal Energy Regulatory Commission");
//		agencies.put("FHWA","Federal Highway Administration");
//		agencies.put("FMC","Federal Maritime Commission");
//		agencies.put("FMSHRC","Federal Mine Safety and Health Review Commission");
//		agencies.put("FMCSA","Federal Motor Carrier Safety Administration");
//		agencies.put("FPC","Federal Power Commission");
//		agencies.put("FRA","Federal Railroad Administration");
//		agencies.put("FRBSF","Federal Reserve Bank of San Francisco");
//		agencies.put("FTA","Federal Transit Administration");
//		agencies.put("USFWS","Fish and Wildlife Service");
//		agencies.put("FDOT","Florida Department of Transportation");
//		agencies.put("FDA","Food and Drug Administration");
//		agencies.put("USFS","Forest Service");
//		agencies.put("GSA","General Services Administration");
//		agencies.put("USGS","Geological Survey");
//		agencies.put("GLB","Great Lakes Basin Commission");
//		agencies.put("IHS","Indian Health Service");
//		agencies.put("IRS","Internal Revenue Service");
//		agencies.put("IBWC","International Boundary and Water Commission");
//		agencies.put("ICC","Interstate Commerce Commission");
//		agencies.put("JCS","Joint Chiefs of Staff");
//		agencies.put("MARAD","Maritime Administration");
//		agencies.put("MTB","Materials Transportation Bureau");
//		agencies.put("MSHA","Mine Safety and Health Administration");
//		agencies.put("MMS","Minerals Management Service");
//		agencies.put("MESA","Mining Enforcement and Safety");
//		agencies.put("MRB","Missouri River Basin Commission");
//		agencies.put("NASA","National Aeronautics and Space Administration");
//		agencies.put("NCPC","National Capital Planning Commission");
//		agencies.put("NGA","National Geospatial-Intelligence Agency");
//		agencies.put("NHTSA","National Highway Traffic Safety Administration");
//		agencies.put("NIGC","National Indian Gaming Commission");
//		agencies.put("NIH","National Institute of Health");
//		agencies.put("NMFS","National Marine Fisheries Service");
//		agencies.put("NNSA","National Nuclear Security Administration");
//		agencies.put("NOAA","National Oceanic and Atmospheric Administration");
//		agencies.put("NPS","National Park Service");
//		agencies.put("NSF","National Science Foundation");
//		agencies.put("NSA","National Security Agency");
//		agencies.put("NTSB","National Transportation Safety Board");
//		agencies.put("NRCS","Natural Resource Conservation Service");
//		agencies.put("NER","New England River Basin Commission");
//		agencies.put("NJDEP","New Jersey Department of Environmental Protection");
//		agencies.put("NRC","Nuclear Regulatory Commission");
//		agencies.put("OCR","Office of Coal Research");
//		agencies.put("OSM","Office of Surface Mining");
//		agencies.put("OBR","Ohio River Basin Commission");
//		agencies.put("RSPA","Research and Special Programs");
//		agencies.put("REA","Rural Electrification Administration");
//		agencies.put("RUS","Rural Utilities Service");
//		agencies.put("SEC","Security and Exchange Commission");
//		agencies.put("SBA","Small Business Administration");
//		agencies.put("SCS","Soil Conservation Service");
//		agencies.put("SRB","Souris-Red-Rainy River Basin Commission");
//		agencies.put("STB","Surface Transportation Board");
//		agencies.put("SRC","Susquehanna River Basin Commission");
//		agencies.put("TVA","Tennessee Valley Authority");
//		agencies.put("TxDOT","Texas Department of Transportation");
//		agencies.put("TPT","The Presidio Trust");
//		agencies.put("TDA","Trade and Development Agency");
//		agencies.put("USACE","U.S. Army Corps of Engineers");
//		agencies.put("USCG","U.S. Coast Guard");
//		agencies.put("CBP","U.S. Customs and Border Protection");
//		agencies.put("RRB","U.S. Railroad Retirement Board");
//		agencies.put("USAF","United States Air Force");
//		agencies.put("USA","United States Army");
//		agencies.put("USMC","United States Marine Corps");
//		agencies.put("USN","United States Navy");
//		agencies.put("USPS","United States Postal Service");
//		agencies.put("USTR","United States Trade Representative");
//		agencies.put("UMR","Upper Mississippi Basin Commission");
//		agencies.put("UMTA","Urban Mass Transportation Administration");
//		agencies.put("UDOT","Utah Department of Transportation");
//		agencies.put("WAPA","Western Area Power Administration");
//	}};
//
//	/** Return full name for given agency abbreviation if found, else return string unchanged */
//	public static String agencyAbbreviationToFull(String abbr) {
//		String fullName = agencies.get(abbr);
//		if(fullName == null) {
//			return abbr;
//		} else {
//			return fullName;
//		}
//	}
	
	public static String normalizeSpace(String str) {
		return org.apache.commons.lang3.StringUtils.normalizeSpace(str);
	}
	

	// Note: Can also have a backup URL set up for use if primary fails
}
