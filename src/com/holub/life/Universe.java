package com.holub.life;

import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Random;

import com.holub.io.Files;
import com.holub.life.game.SimulationMode;
import com.holub.ui.MenuSite;

import com.holub.life.Cell;
import com.holub.life.Clock;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.game.GameSession;
import com.holub.life.plant.Plant;
import com.holub.life.plant.factory.SelectablePlant;
import com.holub.life.plant.factory.WeedFactory;

/**
 * The Universe is a mediator that sits between the Swing
 * event model and the Life classes. It is also a singleton,
 * accessed via Universe.instance(). It handles all
 * Swing events and translates them into requests to the
 * outermost Neighborhood. It also creates the Composite
 * Neighborhood.
 *
 * @include /etc/license.txt
 */

public class Universe extends JPanel
{	private 		final GameSession	session;	//GameSession 객체를 통해 게임 보드 관리

	//창 위쪽 버튼 줄 (Life가 붙임): Undo 버튼은 항상, 식물 선택·수확량은 식물 모드에서만 보임
	private final JToolBar plantBar = new JToolBar();

	//매 세대 잡초 후보를 요청할 Factory. 사용자 선택 식물과 분리해 한 번만 만든다.
	private final WeedFactory weedFactory = new WeedFactory();
	private final Map<SelectablePlant, JToggleButton> plantButtons = new EnumMap<>(SelectablePlant.class);

	//Undo 버튼 (되돌릴 동작이 없으면 비활성)
	private final JButton undoButton = new JButton("Undo");

	//식물 모드에서만 보이는 버튼 줄 항목 (식물 선택 버튼, 수확량 표시)
	private final java.util.List<JComponent> plantOnlyItems = new java.util.ArrayList<>();

	//수확량 표시 (버튼 줄 오른쪽): 과수 종류마다 [열매 그림] 개수
	private final Map<Class<? extends Plant>, JLabel> harvestLabels = new LinkedHashMap<>();
	private static	final Universe 	theInstance = new Universe();

	/** The default height and width of a Neighborhood in cells.
	 *  If it's too big, you'll run too slowly because
	 *  you have to update the entire block as a unit, so there's more
	 *  to do. If it's too small, you have too many blocks to check.
	 *  I've found that 8 is a good compromise.
	 */
	private static final int  DEFAULT_GRID_SIZE = 8;

	//보드 바깥쪽 블록 수: 5 x 5 블록 (블록 하나는 DEFAULT_GRID_SIZE 칸) -> 40 x 40 칸
	// 식물 그림이 보이도록 칸을 키우면서 창이 화면(약 800px)에 들어가게 칸 수를 줄임 (원래 8 -> 64 x 64 칸)
	private static final int  BOARD_BLOCKS = 5;

	/** The size of the smallest "atomic" cell---a Resident object.
	 *  This size is extrinsic to a Resident (It's passed into the
	 *  Resident's "draw yourself" method.
	 */
	private static final int  DEFAULT_CELL_SIZE = 18;

	// The constructor is private so that the universe can be created
	// only by an outer-class method [Neighborhood.createUniverse()].

	private Universe()
	{	// Create the nested Cells that comprise the "universe." A bug
		// in the current implementation causes the program to fail
		// miserably if the overall size of the grid is too big to fit
		// on the screen.

		//보드와 GameSession이 함께 쓸 Random
		// 한 Random을 공유해야 시드 하나로 탄생·번식·잡초 생성의 결과가 모두 정해짐
		Random random = new Random();

		//보드 생성
		// Resident 프로토타입에 Random을 넣으면 Neighborhood가 create()로 셀을 만들 때 같은 Random이 전달됨
		Cell board = new Neighborhood
						(	BOARD_BLOCKS,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident(random)	//기존 코드: new Resident()
							)
						);

		//생성한 보드를 GameSession에 전달
		// GameSession이 이후의 입력, 세대 진행 등을 계산
		session = new GameSession(board, random);	//기존 코드: new GameSession(board)

		//session에서 넓이를 조회해 화면 크기 계산
		final Dimension PREFERRED_SIZE =
						new Dimension
						(  session.widthInCells() * DEFAULT_CELL_SIZE,
						   session.widthInCells() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized(ComponentEvent e)
				{
					// Make sure that the cells fit evenly into the
					// total grid size so that each cell will be the
					// same size. For example, in a 64x64 grid, the
					// total size must be an even multiple of 63.

					Rectangle bounds = getBounds();
					bounds.height /= session.widthInCells();
					bounds.height *= session.widthInCells();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white	 );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true			 );

		addMouseListener					//{=Universe.mouse}
		(	new MouseAdapter()
			{	public void mousePressed(MouseEvent e)
				{	Rectangle bounds = getBounds();
					bounds.x = 0;
					bounds.y = 0;
					// 클릭 좌표를 Session에 전달하고, 처리 후 화면을 다시 그린다.
					session.userClicked(e.getPoint(), bounds);
					refreshControls();		//클릭도 Undo 대상 -> Undo 버튼 상태 갱신
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Clear",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	session.clear();
					refreshControls();		//Clear도 Undo 대상
					repaint();
				}
			}
		);

		MenuSite.addLine			// {=Universe.load.setup}
		(	this, "Grid", "Load",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doLoad();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Store",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doStore();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Exit",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
		        {	System.exit(0);
		        }
			}
		);


		MenuSite.addLine( this, "Mode", "Conway",
				new ActionListener()
				{	public void actionPerformed(ActionEvent e)
				{	changeMode( SimulationMode.CONWAY );
				}
				}
		);

		MenuSite.addLine( this, "Mode", "Plant",
				new ActionListener()
				{	public void actionPerformed(ActionEvent e)
				{	changeMode( SimulationMode.PLANT );
				}
				}
		);

		buildPlantBar();

		updateModeMenu();

		Clock.instance().addClockListener //{=Universe.clock.subscribe}
		(	new Clock.Listener()
			{	public void tick()
				{
					//자동 실행·수동 진행 모두 매 세대 잡초 후보 요청. 일반 모드는 GameSession이 무시한다.
					if(session.nextGeneration(weedFactory))
						refreshNow();
					//tick은 타이머 스레드에서 오므로 화면(Swing) 갱신은 EDT에서
					SwingUtilities.invokeLater( () -> refreshControls() );
				}
			}
		);
	}

	// 모드 전환: 정지 → 모델 변경 → 메뉴 상태 → 다시 그리기
	private void changeMode(SimulationMode mode)
	{	Clock.instance().stop();		//정지
		session.changeMode(mode);		//보드 비우기 -> 모드 변경
		updateModeMenu();				//갱신
		refreshControls();				//수확량 0, Undo 비활성 (모드 전환 시 GameSession이 초기화)
		repaint();						//다시 그리기
	}

	// Undo: 정지 → 직전 동작 되돌리기 → 모드(Load를 되돌리면 바뀔 수 있음)·버튼 상태 → 다시 그리기
	private void undo()
	{	Clock.instance().stop();
		if( session.undo() )
		{	updateModeMenu();
			refreshControls();
			repaint();
		}
	}

	// 버튼 줄 상태를 GameSession에 맞춤: 수확량 표시, Undo 버튼 활성화 (EDT에서 호출)
	private void refreshControls()
	{	showHarvest( session.harvestCounts() );
		undoButton.setEnabled( session.canUndo() );
	}

	// 현재 모드의 메뉴 항목 비활성화 (현재 모드 표시 및 현재 모드 재선택 불가)
	private void updateModeMenu()
	{
		SimulationMode mode = session.getMode();
		MenuSite.getMyMenuItem(this, "Mode", "Conway").setEnabled( mode != SimulationMode.CONWAY );
		MenuSite.getMyMenuItem(this, "Mode", "Plant" ).setEnabled( mode != SimulationMode.PLANT  );

		updatePlantBar(mode);
	}

	// 버튼 줄 만들기: Undo 버튼 + SelectablePlant 목록으로 식물 선택 버튼 생성 (잡초 제외)
	// 버튼을 누르면 그 식물의 팩토리를 GameSession에 넘김 -> 이후 빈 칸 클릭 시 그 식물이 심김
	private void buildPlantBar()
	{
		plantBar.setFloatable(false);

		//Undo: 두 모드 모두에서 사용 (세대 진행·클릭·Clear·Load를 하나씩 되돌림)
		undoButton.setEnabled(false);
		undoButton.setToolTipText("직전 동작 되돌리기 (세대 진행·클릭·Clear·Load)");
		undoButton.addActionListener( e -> undo() );
		plantBar.add(undoButton);

		//여기부터 식물 모드에서만 보이는 항목
		JToolBar.Separator undoSeparator = new JToolBar.Separator();
		plantBar.add(undoSeparator);
		plantOnlyItems.add(undoSeparator);

		JLabel plantLabel = new JLabel(" 심을 식물: ");
		plantBar.add(plantLabel);
		plantOnlyItems.add(plantLabel);

		ButtonGroup group = new ButtonGroup();		//한 번에 하나만 선택
		for(SelectablePlant plant : SelectablePlant.values())
		{
			JToggleButton button = new JToggleButton( plant.label() );
			button.setEnabled( plant.isEnabled() );
			if(!plant.isEnabled())
				button.setToolTipText("추후 추가 예정");
			button.addActionListener( e -> session.selectPlant( plant.factory() ) );

			group.add(button);
			plantButtons.put(plant, button);
			plantBar.add(button);
			plantOnlyItems.add(button);
		}

		//수확량: 과수 종류마다 [열매 그림] 0 (과수가 추가되면 자동으로 늘어남)
		JToolBar.Separator harvestSeparator = new JToolBar.Separator();
		plantBar.add(harvestSeparator);
		plantOnlyItems.add(harvestSeparator);

		JLabel harvestTitle = new JLabel(" 수확: ");
		plantBar.add(harvestTitle);
		plantOnlyItems.add(harvestTitle);
		for(SelectablePlant plant : SelectablePlant.values())
		{
			if(!plant.isHarvestable())
				continue;
			JLabel label = new JLabel(" 0  ", fruitIcon(plant.fruitImage()), SwingConstants.LEFT);
			harvestLabels.put(plant.plantType(), label);
			plantBar.add(label);
			plantOnlyItems.add(label);
		}
	}

	// 수확량 표시용 열매 아이콘 (resources/plant/의 그림을 16px로)
	private static Icon fruitIcon(String fileName)
	{
		java.net.URL url = (fileName == null) ? null : Universe.class.getResource("/plant/" + fileName);
		if(url == null)
			return null;
		return new ImageIcon( new ImageIcon(url).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH) );
	}

	// 과수 종류별 수확 개수를 화면에 표시 (없는 종류는 0)
	private void showHarvest(Map<Class<? extends Plant>, Integer> counts)
	{
		for(Map.Entry<Class<? extends Plant>, JLabel> entry : harvestLabels.entrySet())
			entry.getValue().setText(" " + counts.getOrDefault(entry.getKey(), 0) + "  ");
	}

	// 식물 선택·수확량은 식물 모드에서만 보여 주고(Undo 버튼은 항상), 아직 아무것도 안 골랐으면 첫 번째 식물을 자동 선택
	// -> 식물 모드로 바꾸자마자 바로 칸을 눌러 심을 수 있음
	private void updatePlantBar(SimulationMode mode)
	{
		boolean plantMode = (mode == SimulationMode.PLANT);
		for(JComponent item : plantOnlyItems)
			item.setVisible(plantMode);

		if(plantMode && plantButtons.values().stream().noneMatch(JToggleButton::isSelected))
		{
			for(SelectablePlant plant : SelectablePlant.values())
				if(plant.isEnabled())
				{	plantButtons.get(plant).doClick();
					break;
				}
		}

		//버튼 줄이 보이거나 사라지면 창 크기 다시 맞춤 (Life 창에 붙은 뒤에만)
		Window window = SwingUtilities.getWindowAncestor(this);
		if(window != null)
			window.pack();
	}

	// Life가 창 위쪽(BorderLayout.NORTH)에 붙이는 버튼 줄 (Undo, 식물 선택, 수확량)
	public JComponent plantToolbar()
	{	return plantBar;
	}


	/** Singleton Accessor. The Universe object itself is manufactured
	 *  in Neighborhood.createUniverse()
	 */

	public static Universe instance()
	{	return theInstance;
	}

	private void doLoad()
	{	try
		{
			FileInputStream in = new FileInputStream(
			   Files.userSelected(".",".life","Life File","Load"));

			Clock.instance().stop();		// stop the game and

			//게임 보드 관련 작업은 session으로 분리
			session.load(in);

			in.close();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Read Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}

		updateModeMenu();	//메뉴 상태를 모드에 맞추기
		refreshControls();	//Load도 Undo 대상
		repaint();
	}

	private void doStore()
	{	try
		{
			FileOutputStream out = new FileOutputStream(
				  Files.userSelected(".",".life","Life File","Write"));

			Clock.instance().stop();		// stop the game

			//보드 관련 작업 session으로 분리
			session.store(out);

			out.close();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Write Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Override paint to ask the outermost Neighborhood
	 *  (and any subcells) to draw themselves recursively.
	 *  All knowledge of screen size is also encapsulated.
	 *  (The size is passed into the outermost <code>Cell</code>.)
	 */

	public void paint(Graphics g)
	{
		Rectangle panelBounds = getBounds();
		Rectangle clipBounds  = g.getClipBounds();

		// The panel bounds is relative to the upper-left
		// corner of the screen. Pretend that it's at (0,0)
		panelBounds.x = 0;
		panelBounds.y = 0;
		session.redraw(g, panelBounds, true);		//{=Universe.redraw1}
	}

	/** Force a screen refresh by queing a request on
	 *  the Swing event queue. This is an example of the
	 *  Active Object pattern (not covered by the Gang of Four).
	 *  This method is called on every clock tick. Note that
	 *  the redraw() method on a given <code>Cell</code>
	 *  does nothing if the <code>Cell</code> doesn't
	 *  have to be refreshed.
	 */

	private void refreshNow()
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Graphics g = getGraphics();
					if( g == null )		// Universe not displayable
						return;
					try
					{
						Rectangle panelBounds = getBounds();
						panelBounds.x = 0;
						panelBounds.y = 0;
						session.redraw(g, panelBounds, false); //{=Universe.redraw2}
					}
					finally
					{	g.dispose();
					}
				}
			}
		);
	}
}
