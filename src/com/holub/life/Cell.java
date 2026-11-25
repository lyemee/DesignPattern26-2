package com.holub.life;
import java.awt.*;

import com.holub.life.Storable;
import com.holub.life.plant.PlantData;

/***
 * This interface is the basic unit that comprises a life board.
 * It's implemented both by {@link Resident} (which represents
 * an individual cell on the board) and {@link Neighborhood},
 * which represents a group of cells.
 *
 * @include /etc/license.txt
 */

public interface Cell
{
	/** Figure out the next state of the cell, given the specified
	 *  neighbors.
	 *  @return true if the cell is unstable (changed state).
	 */
	boolean figureNextState(	Cell north, 	Cell south,
								Cell east,		Cell west,
								Cell northeast, Cell northwest,
								Cell southeast, Cell southwest );

	/** Access a specific contained cell located at the edge of the
	 *  composite cell.
	 *  @param row 		The requested row. Must be on the edge of
	 *  				the block.
	 *  @param column	The requested column. Must be on the edge
	 *  				of the block.
	 *  @return	true	if the the state changed.
	 */
	Cell edge( int row, int column );

	/** Transition to the state computed by the most recent call to
	 *  {@link #figureNextState}
	 *  @return true if a changed of state happened during the transition.
	 */
	boolean transition();

	/** Redraw yourself in the indicated
	 *  rectangle on the indicated Graphics object if ncessary. This
	 *  method is meant for a conditional redraw, where some of the
	 *  cells might not be refreshed (if they haven't changed state,
	 *  for example).
	 *  @param g redraw using this graphics,
	 *  @param here a rectangle that describes the bounds of the
	 *  current cell.
	 *  @param drawAll if true, draw an entire compound cell;
	 *  otherwise, draw only the subcells that need to be redrawn.
	 */

	void redraw(Graphics g, Rectangle here, boolean drawAll);

	//기존 코드 (apply로 대체, 확인용으로 남겨 둠)
	// 픽셀 좌표로 셀을 찾아가는 경로와 셀에서 하는 일(생존 여부 뒤집기)이 한 메서드에 묶여 있었음
	///** A user has clicked somewhere within you.
	// *  @param here The position of the click relative to the bounding
	// *  			rectangle of the current Cell.
	// */
	//void userClicked(Point here, Rectangle surface);

	//(row, column) 셀까지 재귀로 내려가 action 실행 (원본 userClicked를 대체)
	// row, column: 이 셀의 왼쪽 위 칸을 (0, 0)으로 하는 셀 좌표
	// Neighborhood: 몇 번째 하위 셀인지 계산 -> 하위 셀 안의 좌표로 재귀 -> 블록 활성화, 경계 셀이면 경계 표시
	// Resident: action.execute(this)
	// DUMMY: 보드 밖이라 아무것도 하지 않음
	void apply(int row, int column, CellAction action);

	//모든 Resident가 자기 좌표와 함께 이번 step의 계산 결과를 report에 알림
	// row, column: 이 셀의 왼쪽 위 칸이 보드에서 갖는 좌표
	// Neighborhood: 하위 셀마다 좌표를 계산해 재귀 (읽기만 하고 상태는 바꾸지 않음)
	// Resident: report.cell(), seeding(), harvested() 호출
	// DUMMY: 아무것도 하지 않음
	void collect(int row, int column, CellReport report);

	/** Return true if this cell or any subcells are alive.
	 */
	boolean isAlive();

	/** Return the specified width plus the current cell's width
	 */
	int widthInCells();

	/** Return a fresh (newly created) object identical to yourself
	 *  in content.
	 */
	Cell create();

	/** Returns a Direction indicated the directions of the cells
	 *  that have changed state.
	 *  @return A Direction object that indicates the edge or edges
	 *  		on which a change has occured.
	 */

	Direction isDisruptiveTo();

	/** Set the cell and all subcells into a "dead" state.
	 */

	void clear();

	/**
	 *	The Cell.Memento interface stores the state
	 *	of a Cell and all its subcells for future restoration.
	 *
	 *	@see Cell
	 */

	interface Memento extends Storable
	{	/** On creation of the memento, indicate that a cell is
		 *  alive.
		 */
		void markAsAlive	(Point location);

		/** On restoration of a cell from a memento, indicate that
		 *  a cell is alive.
		 */
		boolean isAlive	(Point location);
	}

	//살아 있는 칸에 더해 식물 데이터(종류·단계·진행도)까지 저장하는 Memento (Undo용)
	// 기존 transfer() 순회 경로를 그대로 쓰고, Resident가 이 타입일 때만 식물 데이터를 함께 주고받음
	// -> 기존 .life 저장(NeighborhoodState)은 영향 없음
	interface PlantMemento extends Memento
	{	//저장: 이 칸의 식물 데이터 기록
		void markPlant	(Point location, PlantData data);

		//복원: 이 칸의 식물 데이터, 식물이 없던 칸이면 null
		PlantData plantAt(Point location);
	}

	/**  This method is used internally to save or restore the state
	 *   of a cell from a memento.
	 *   @return true if this cell was modified by the transfer.
	 */
	boolean transfer( Storable memento, Point upperLeftCorner,
													boolean doLoad );

	/** Possible value for the "load" argument to transfer() */
	public static boolean STORE = false;

	/** Possible value for the "load" argument to transfer() */
	public static boolean LOAD = true;

	/** This method is used by container of the outermost cell.
	 *  It is not used internally. It need be implemented only by
	 *  whatever class defines the outermost cell in the universe.
	 *  Other cell implementions should throw an
	 *  UnsupportedOperationException when this method is called.
	 */
	Storable createMemento();

	/** The DUMMY Singleton represents a permanently dead (thus stable)
	 * 	cell. It's used for the edges of the grid. It's a singleton.
	 * 	The Dummy class is private, but it is accessed through
	 * 	the public DUMMY field, declared below. I'd like this
	 * 	class to be private, but the JLS doesn't allow private
	 * 	members in an interface.
	 */

	public static final Cell DUMMY = new Cell()
	{
		public boolean figureNextState(Cell n,  Cell s,  Cell e,  Cell w,
									   Cell ne, Cell nw, Cell se, Cell sw)
											{return true;  			  }

		public Cell		 edge(int r, int c)	{return this;  			  }
		public boolean	 isAlive()		   	{return false; 			  }
		public Cell 	 create()	   	   	{return this;			  }
		public Direction isDisruptiveTo()	{return Direction.NONE;	  }
		public void 	 clear()		   	{						  }
		public int 		 widthInCells()  	{return 0;				  }
		public boolean	 transition()		{return false;			  }

		//public void	userClicked(Point h, Rectangle s				){}	//기존 코드 (apply로 대체)
		public void apply(int r, int c, CellAction action			){}
		public void collect(int r, int c, CellReport report			){}
		public void redraw 		(Graphics g, Rectangle here,
												 boolean drawAll	){}

		public boolean transfer( Storable m, Point ul, boolean load )
		{	return false;
		}

		public Storable createMemento()
		{	throw new UnsupportedOperationException(
						"Cannot create memento of dummy block");
		}
	};
}
