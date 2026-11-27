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
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantLifecycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	//이번 step에 번식하는 꽃, 수확된 과수의 종류 (없으면 null)
	// 계산에서 기록 -> collect -> 반영(지우기)
	// 번식한 꽃은 다음 상태(nextPlantData)가 null이 되므로 next가 아닌 별도 필드에 둠
	// 반영에서 지우므로 계산이 생략된 블록의 셀이 지난 step의 신호를 다시 알리지 않음
	private Plant seedingPlant   = null;
	private Plant harvestedPlant = null;

	//규칙 탄생에서 이웃 3개 중 하나를 고를 때 사용 (탄생 로직은 식물 담당이 구현)
	// 보드 전체가 같은 Random을 공유 -> 테스트에서 시드를 고정하면 결과도 고정
	private final Random random;

	//보드를 만들 때 프로토타입 하나에만 Random을 넣으면 create()가 모든 셀에 같은 Random을 전달
	public Resident(Random random)
	{
		this.random = random;
	}


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
		//외로움(이웃 0~1개): 씨앗·새싹·꽃은 이 이유로는 죽지 않음 (PlantLifecycle이 판정)
		boolean lonely = neighbors < 2;

		//식물이 없는 셀 -> 원래의 규칙
		if(currentPlantData == null){
			seedingPlant = harvestedPlant = null;

			//규칙 탄생: 탄생 조건을 만족하고 이웃에 식물이 있으면 이웃 식물 중 무작위 1개의 종류로 탄생
			// 이웃 칸 하나를 고르는 방식 -> 사과 2 + 오렌지 1이면 사과 2/3 (이웃 3개 중 무작위 1개)
			// 씨앗과의 충돌은 GenerationManager ②에서 bornPlant를 후보로 함께 골라 결정
			if(!amAlive && ruleSatisfied){
				List<Plant> parents = plantNeighbors(north, south, east, west,
													 northeast, northwest, southeast, southwest);
				if(!parents.isEmpty()){
					Plant parent = parents.get(random.nextInt(parents.size()));
					willBeAlive   = true;
					nextPlantData = new PlantData(parent);	//Plant는 공유 객체 -> 부모 종류 그대로, 시작 단계부터
					return true;
				}
			}

			//이웃에 식물이 없으면 원본 규칙 그대로 (일반 모드)
			willBeAlive = ruleSatisfied;
			nextPlantData = null;
			return !isStable();
		}

		//심어진 식물이 있는 셀

		//현재 상태를 바탕으로 모든 셀을 계산하기 위해 current를 변경하지 않고 복사본 생성
		PlantData next = currentPlantData.copy();

		//이웃 잡초 여부 계산
		boolean growthBlocked = isWeed(north) || isWeed(northeast) || isWeed(northwest) || isWeed(east) ||
								isWeed(south) || isWeed(southeast) || isWeed(southwest) || isWeed(west);

		//성장 가능 여부는 생존 판정 전 단계로 판단
		//  시든 나무 -> false -> 회복한 step에는 성장하지 않음
		boolean canGrow = lifecycle.canGrow(next, ruleSatisfied, lonely, growthBlocked);

		//다음 식물 상태 판정 (시듦·회복·사망, 회복은 이웃 잡초와 무관)
		boolean alive = lifecycle.applySurvival(next, ruleSatisfied, lonely);

		//살아있고 성장 가능한 상태에서 현재 state의 성장 동작 실행
		//  번식 차례의 꽃이 생존 조건을 못 채우면 onStep 없음 -> 번식 없이 사망
		if(alive && canGrow)
			next.getState().onStep(next);


		//State가 표시한 신호를 기록 (collect애서 전달)
		seedingPlant   = next.reproduces() ? next.getPlant() : null;
		harvestedPlant = next.harvested()  ? next.getPlant() : null;
		//번식한 꽃은 씨를 뿌리고 소멸 (seedingPlant는 위에서 기록했으므로 ②에서 씨를 뿌림)
		if(next.removed())
			alive = false;

		//다음 생존 여부와 plantData 업데이트
		willBeAlive = alive;
		//사망하면 PlantData를 null로
		nextPlantData = alive ? next : null;

		//plant -> 생존 여부가 그대로여도 내부 상태가 변함 -> 항상 변경 계산(최적화 필요 시 수정)
		return true;
	}

	//살아 있는 이웃 중 식물이 있는 칸의 종류 목록 (규칙 탄생 후보, 잡초 포함)
	private static List<Plant> plantNeighbors(Cell... neighbors){
		List<Plant> plants = new ArrayList<>();
		for(Cell c : neighbors)
			if(c instanceof Resident && c.isAlive() && ((Resident)c).currentPlantData != null)
				plants.add(((Resident)c).currentPlantData.getPlant());
		return plants;
	}

	//현재 세대의 식물 데이터 (없으면 null)
	// 같은 패키지 안에서만 읽기용: 테스트가 apply로 칸에 내려가 단계를 확인할 때 사용
	PlantData plantData()
	{
		return currentPlantData;
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

	//빈 셀에 식물 심기 (식물 모드 클릭, 잡초 생성)
	// 현재와 다음 상태를 함께 바꿈 -> 다음 계산에서 기존 식물로 취급
	//  현재 점유된 칸은 이번 step의 꽃 번식 대상에서 제외 (이번 계산에서 죽더라도 제외)
	public void plant(PlantData data){
		currentPlantData = data;
		nextPlantData = data;
		amAlive = willBeAlive = true; //심으면 살아있는 셀로 변경
	}

	//식물 제거 (식물 모드 클릭)
	// 현재와 다음 상태를 함께 지움 -> 데이터를 남기면 이웃의 isWeed()가 죽은 잡초를 봄
	public void remove()
	{
		currentPlantData = nextPlantData = null;
		amAlive = willBeAlive = false;
		seedingPlant = harvestedPlant = null;
	}

	//씨앗 번식
	// 다음 상태에 사용 (다음 상태 반영시 씨앗이 생성 현재 세대 계산 결과에는 영향 없음)
	public void sow(PlantData data)
	{
		nextPlantData = data;
		willBeAlive = true;
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
		seedingPlant = harvestedPlant = null;	//②에서 이미 알렸으므로 지움
		return changed;
	}

	public void redraw(Graphics g, Rectangle here, boolean drawAll)
    {   g = g.create();
		//식물이 있는 칸: 색 대신 종류·단계 그림 (Plant.getImage), 식물이 없으면 원래 색
		boolean showPlant = amAlive && currentPlantData != null;
		g.setColor(amAlive && !showPlant ? LIVE_COLOR : DEAD_COLOR );
		g.fillRect(here.x+1, here.y+1, here.width-1, here.height-1);
		if(showPlant)
			drawPlantImage(g, here, currentPlantData.getPlant().getImage(currentPlantData.getState()));

		// Doesn't draw a line on the far right and bottom of the
		// grid, but that's life, so to speak. It's not worth the
		// code for the special case.

		g.setColor( BORDER_COLOR );
		g.drawLine( here.x, here.y, here.x, here.y + here.height );
		g.drawLine( here.x, here.y, here.x + here.width, here.y  );
		g.dispose();
	}

	//식물 그림을 칸 크기에 맞춰 그림 (그림 파일 이름은 식물 종류가 정함: Plant.getImage)
	// 그림은 resources/plant/에서 처음 한 번만 읽고 이후에는 재사용
	private static final java.util.Map<String, Image> PLANT_IMAGES = new java.util.concurrent.ConcurrentHashMap<>();
	private static void drawPlantImage(Graphics g, Rectangle here, String fileName)
	{
		if(fileName == null)
			return;
		Image image = PLANT_IMAGES.computeIfAbsent(fileName, Resident::loadPlantImage);
		if(image == null)
			return;
		if(g instanceof Graphics2D)		//작게 줄일 때 깨지지 않도록 부드럽게
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(image, here.x+1, here.y+1, here.width-1, here.height-1, null);
	}

	private static Image loadPlantImage(String fileName)
	{
		java.net.URL url = Resident.class.getResource("/plant/" + fileName);
		if(url == null)
			return null;		//그림이 없으면 그리지 않음 (바탕색만 보임)
		try
		{	return javax.imageio.ImageIO.read(url);
		}
		catch(java.io.IOException e)
		{	return null;
		}
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

	//이번 step의 계산 결과를 자기 좌표와 함께 알림 (내부 필드는 밖에 내주지 않음)
	//번식·잡초 후보 수집: 다음 상태 계산 뒤, transition으로 실제 반영하기 전에 호출
	//현재 점유 여부와 계산 결과를 함께 보고해 같은 현재 빈 칸 기준으로 후보를 경쟁시킨다.
	public void collect(int row, int column, CellReport report)
	{
		report.currentCell(row, column, amAlive);

		//다음 세대에도 살아 있는 식물: 지금 식물이 있고 다음 상태에도 식물이 있음
		boolean hasSurvivingPlant = currentPlantData != null && nextPlantData != null;
		//이번에 태어나는 식물: 지금은 비어 있고 다음 상태에 식물이 있음 (규칙 탄생)
		Plant bornPlant = (currentPlantData == null && nextPlantData != null)
								? nextPlantData.getPlant() : null;
		report.cell(row, column, hasSurvivingPlant, bornPlant);

		if(seedingPlant != null)
			report.seeding(row, column, seedingPlant);

		if(harvestedPlant != null)
			report.harvested(harvestedPlant);
	}

	public void	   clear()			{
		amAlive = willBeAlive = false;
		currentPlantData = nextPlantData = null;
		seedingPlant = harvestedPlant = null;
	}
	public boolean isAlive()		{return amAlive;			    }
	//기존 코드
	//public Cell    create()			{return new Resident();			}
	public Cell    create()			{return new Resident(random);	}	//같은 Random을 새 셀에 전달 (Prototype)
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
		{	memento.markAsAlive( upperLeft );

			//식물이 있는 칸은 식물 데이터도 저장 (Undo용 Memento일 때만)
			if( currentPlantData != null && memento instanceof PlantMemento )
				((PlantMemento)memento).markPlant( upperLeft, currentPlantData );
		}

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
