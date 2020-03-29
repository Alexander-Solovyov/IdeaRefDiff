package refdiffIdea.core.cst;

import com.intellij.openapi.util.TextRange;

public class TokenPosition {
	private final int start;
	private final int end;

	public TokenPosition(TextRange range) {
		this.start = range.getStartOffset();
		this.end =  range.getEndOffset();
	}

	public TokenPosition(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}
	
}
