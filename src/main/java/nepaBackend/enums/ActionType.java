package nepaBackend.enums;

public enum ActionType {
	DOWNLOAD_ARCHIVE(0), DOWNLOAD_ONE(1), DETAILS_CLICK(2);
	int val;
	ActionType(int val){
		this.val = val;
	}
}
