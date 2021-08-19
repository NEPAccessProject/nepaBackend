package nepaBackend.enums;

/** RESULTS(0), DETAILS(1); */
public enum ActionSource {
	RESULTS(0), DETAILS(1), UNKNOWN(2);
	int val;
	ActionSource(int val){
		this.val = val;
	}
}
