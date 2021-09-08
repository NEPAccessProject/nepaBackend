package nepaBackend.enums;

/** DOWNLOAD_ARCHIVE(0), DOWNLOAD_ONE(1), DETAILS_CLICK(2); */
public enum ActionType {
	DOWNLOAD_ARCHIVE(0), DOWNLOAD_ONE(1), DETAILS_CLICK(2), UNKNOWN(3), PROCESS_CLICK(4);
	int val;
	ActionType(int val){
		this.val = val;
	}
}
