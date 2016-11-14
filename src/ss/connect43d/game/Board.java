package ss.connect43d.game;

import java.util.ArrayList;
import java.util.List;

public class Board {
	private int sizeX;
	private int sizeY;
	private int sizeZ;
	private int winLength;

	private Cell[] cells;
	private List<List<Cell>> winningLines;

	public Board(int sizeX, int sizeY, int sizeZ, int winLength) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.winLength = winLength;

		this.cells = new Cell[this.sizeX * this.sizeY * this.sizeZ];

		for (int i = 0; i < this.cells.length; i++) {
			this.cells[i] = new Cell();
		}

		this.winningLines = this.getWinningLines();
	}

	public boolean isCell(int index) {
		return index >= 0 && index < this.cells.length;
	}

	public boolean isCell(int x, int y, int z) {
		return this.isCell(this.getIndexFromXYZ(x, y, z));
	}

	public Cell getCell(int index) {
		return this.isCell(index) ? this.cells[index] : null;
	}

	public Cell getCell(int x, int y, int z) {
		return this.getCell(this.getIndexFromXYZ(x, y, z));
	}

	public Mark getWinningMark() {
		for (List<Cell> line : winningLines) {
			Mark last = null;
			boolean foundWinner = true;

			for (Cell cell : line) {
				if (last == null) {
					last = cell.getMark();
				}

				if (last.getOwner() == null || last.getOwner() != cell.getMark().getOwner()) {
					foundWinner = false;
					// TODO: Performance increase: break out of top loop early.
				}
			}

			if (foundWinner) {
				return last;
			}
		}

		return null;
	}

	public boolean isValidMove(int x, int y) {
		Cell cell = this.getCell(x, y, sizeZ - 1);
		return cell != null && cell.getMark() == Mark.EMPTY;
	}

	public void doMove(Mark mark, int x, int y) {
		// DoMove assumes you have checked whether the move is valid. If you
		// don't, bad things will happen.
		for (int z = sizeZ - 1; z >= 0; z--) {
			// Get cell below this cell
			Cell below = this.getCell(x, y, z - 1);

			if (below == null || below.getMark() != Mark.EMPTY) {
				// Drop mark on top of empty cell
				this.getCell(x, y, z).setMark(mark);
				break;
			}
		}
	}

	private int getIndexFromXYZ(int x, int y, int z) {
		if (x >= 0 && y >= 0 && z >= 0 && x < this.sizeX && y < this.sizeY && z < this.sizeZ) {
			return x + y * this.sizeX + z * this.sizeX * this.sizeY;
		} else {
			return -1;
		}
	}

	private int getXFromIndex(int index) {
		return index % this.sizeX;
	}

	private int getYFromIndex(int index) {
		return (index - this.getZFromIndex(index) * this.sizeX * this.sizeY) / this.sizeX;
	}

	private int getZFromIndex(int index) {
		return index / (this.sizeX * this.sizeY);
	}

	private List<List<Cell>> getWinningLines() {
		List<List<Cell>> winningLines = new ArrayList<List<Cell>>();

		// Find all lines of length this.winLength for faster future reference
		for (int i = 0; i < this.cells.length; i++) {
			List<List<Cell>> lines = getWinningLinesForCell(i);
			for (List<Cell> line : lines) {
				if (line.size() == this.winLength) {
					winningLines.add(line);
				}
			}
		}

		return winningLines;
	}

	private List<List<Cell>> getWinningLinesForCell(int index) {
		List<List<Cell>> winningLines = new ArrayList<List<Cell>>();

		int x = this.getXFromIndex(index);
		int y = this.getYFromIndex(index);
		int z = this.getZFromIndex(index);

		// Find 4-long lines in all 26 possible directions (filter (0,0,0))
		for (int dX = -1; dX <= 1; dX++) {
			for (int dY = -1; dY <= 1; dY++) {
				for (int dZ = -1; dZ <= 1; dZ++) {
					if (dX == 0 && dY == 0 && dZ == 0) {
						continue;
					}

					winningLines.add(getCellsInWinningLine(x, y, z, dX, dY, dZ));
				}
			}
		}

		return winningLines;
	}

	private List<Cell> getCellsInWinningLine(int startX, int startY, int startZ, int dX, int dY, int dZ) {
		List<Cell> cellsInLine = new ArrayList<Cell>(this.winLength);

		int x = startX;
		int y = startY;
		int z = startZ;

		// Return the line (max length this.winLength) starting at (startX,
		// startY, startZ) going in direction (dX, dY, dZ)
		do {
			cellsInLine.add(this.getCell(x, y, z));

			x += dX;
			y += dY;
			z += dZ;
		} while (cellsInLine.size() < this.winLength && this.getCell(x, y, z) != null);

		return cellsInLine;
	}
}
