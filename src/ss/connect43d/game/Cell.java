package ss.connect43d.game;

public class Cell {
	private Mark mark;
	
	public Cell() {
		this.mark = Mark.EMPTY;
	}
	
	public void setMark(Mark mark) {
		this.mark = mark;
	}
	
	public Mark getMark() {
		return this.mark;
	}
}
