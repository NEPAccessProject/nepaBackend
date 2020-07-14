package nepaBackend.enums;

public enum SearchType {
	ALL(0), EXACT(1);
	int val;
	SearchType(int val){
		this.val = val;
	}
}
