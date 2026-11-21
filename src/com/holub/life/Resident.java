package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.Colors;	// Contains constants specifying various
							// colors not defined in java.awt.Color.
import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Direction;
import com.holub.life.Neighborhood;
import com.holub.life.Universe;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantLifecycle;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

public final class Resident implements Cell
{
	private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
	private static final Color LIVE_COLOR 	= Color.RED;
	private static final Color DEAD_COLOR   = Colors.LIGHT_YELLOW;

	private boolean amAlive 	= false;
	private boolean willBeAlive	= false;
	//식물 모드에서 B/S 규칙을 지키지 못해도 살아있으면 willBeAlive = true

	//식물 데이터: 식물이 없는 셀은 null
	//current -> 현재 세대의 식물 데이터
	private PlantData currentPlantData = null;
	//next -> 다음 세대의 식물 데이터
	private PlantData nextPlantData = null;

	//식물의 생존/쇠약 판정 위임 (필드 데이터가 없는 객체이므로 모든 resident가 하나를 공유)
	private static final PlantLifecycle lifecycle = new PlantLifecycle();


	private boolean isStable(){return amAlive == willBeAlive;}

	/** figure the next state.
	 *  @return true if the cell is not stable (will change state on the
	 *  next transition().
	 */
	public boolean figureNextState(
							Cell north, 	Cell south,
							Cell east, 		Cell west,
							Cell northeast, Cell northwest,
							Cell southeast, Cell southwest )
	{
		verify( north, 		"north"		);
		verify( south, 		"south"		);
		verify( east, 		"east"		);
		verify( west, 		"west"		);
		verify( northeast,	"northeast"	);
		verify( northwest,	"northwest" );
		verify( southeast,	"southeast" );
		verify( southwest,	"southwest" );

		int neighbors = 0;

		if( north.	  isAlive()) ++neighbors;
		if( south.	  isAlive()) ++neighbors;
		if( east. 	  isAlive()) ++neighbors;
		if( west. 	  isAlive()) ++neighbors;
		if( northeast.isAlive()) ++neighbors;
		if( northwest.isAlive()) ++neighbors;
		if( southeast.isAlive()) ++neighbors;
		if( southwest.isAlive()) ++neighbors;

		boolean ruleSatisfied = (neighbors==3 || (amAlive && neighbors==2));

		//식물이 없는 셀 -> 원래의 규칙
		if(currentPlantData == null){
			willBeAlive = ruleSatisfied;
			nextPlantData = null;
			return !isStable();
		}

		//심어진 식물이 있는 셀

		//현재 상태를 바탕으로 모든 셀을 계산하기 위해 current를 변경하지 않고 복사본 생성
		PlantData next = currentPlantData.copy();

		//다음 식물 상태 판정
		boolean alive = lifecycle.applySurvival(next, ruleSatisfied);

		//이웃 잡초 여부 계산
		boolean growthBlocked = isWeed(north) || isWeed(northeast) || isWeed(northwest) || isWeed(east) ||
								isWeed(south) || isWeed(southeast) || isWeed(southwest) || isWeed(west);


		//살아있고 성장 가능한 상태에서 현재 state의 성장 동작 실행
		if(alive && lifecycle.canGrow(next, ruleSatisfied, growthBlocked))
			next.getState().onStep(next);


		//다음 생존 여부와 plantData 업데이트
		willBeAlive = alive;
		//사망하면 PlantData를 null로
		nextPlantData = alive ? next : null;

		//plant -> 생존 여부가 그대로여도 내부 상태가 변함 -> 항상 변경 계산(최적화 필요 시 수정)
		return true;
	}

	//이웃이 잡초인지 확인
	private static boolean isWeed(Cell neighbor){
		if(!(neighbor instanceof Resident)) //보드 가장자리 -> resident가 아님(Cell.Dummy)
			return false;

		PlantData data = ((Resident)neighbor).currentPlantData;

		//Plantdata가 null이 아니고 blocksGrowth가 true라면 weed로 판정
		return data != null && data.getPlant().blocksGrowth();
	}

	//일반 모드 클릭: 생존 여부를 반대로 바꿈 (원본 userClicked) -> 클릭 외에도 사용되어 메서드 시그니쳐 변경
	// willBeAlive는 그대로 -> 다음 계산에서 다시 결정
	public void flipAlive()
	{
		amAlive = !amAlive;
	}

	//빈 셀에 식물 심기
	public void plant(PlantData data){
		currentPlantData = data;
		nextPlantData = data;
		amAlive = willBeAlive = true; //심으면 살아있는 셀로 변경
	}

	private void verify( Cell c, String direction )
	{	assert (c instanceof Resident) || (c == Cell.DUMMY)
				: "incorrect type for " + direction +  ": " +
				   c.getClass().getName();
	}

	/** This cell is monetary, so it's at every edge of itself. It's
	 *  an internal error for any position except for (0,0) to be
	 *  requsted since the width is 1.
	 */
	public Cell	edge(int row, int column)
	{	assert row==0 && column==0;
		return this;
	}

	public boolean transition()
	{	boolean changed = isStable();
		amAlive = willBeAlive;
		currentPlantData = nextPlantData; //계산해 둔 다음 세대 식물 데이터 반영(식물모드가 아닐 땐 null)
		return changed;
	}

	public void redraw(Graphics g, Rectangle here, boolean drawAll)
    {   g = g.create();
		g.setColor(amAlive ? LIVE_COLOR : DEAD_COLOR );
		g.fillRect(here.x+1, here.y+1, here.width-1, here.height-1);

		// Doesn't draw a line on the far right and bottom of the
		// grid, but that's life, so to speak. It's not worth the
		// code for the special case.

		g.setColor( BORDER_COLOR );
		g.drawLine( here.x, here.y, here.x, here.y + here.height );
		g.drawLine( here.x, here.y, here.x + here.width, here.y  );
		g.dispose();
	}

	//기존 코드
	/*
	public void userClicked(Point here, Rectangle surface)
	{
		amAlive = !amAlive;
	}
	 */

	// 받은 CellAction 동작을 수행
	public void apply(int row, int column, CellAction action)
	{
		assert row == 0 && column == 0;	//Resident는 한 칸이라 (0, 0)만 가능
		action.execute(this);
	}

	public void	   clear()			{
		amAlive = willBeAlive = false;
		currentPlantData = nextPlantData = null;
	}
	public boolean isAlive()		{return amAlive;			    }
	public Cell    create()			{return new Resident();			}
	public int 	   widthInCells()	{return 1;}

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL ;
	}

	public boolean transfer(Storable blob,Point upperLeft,boolean doLoad)
	{
		Memento memento = (Memento)blob;
		if( doLoad )
		{	if( amAlive = willBeAlive = memento.isAlive(upperLeft) )
				return true;
		}
		else if( amAlive )  					// store only live cells
			memento.markAsAlive( upperLeft );

		return false;
	}

	/** Mementos must be created by Neighborhood objects. Throw an
	 *  exception if anybody tries to do it here.
	 */
	public Storable createMemento()
	{	throw new UnsupportedOperationException(
					"May not create memento of a unitary cell");
	}
}
