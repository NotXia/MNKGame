/*
 *  Copyright (C) 2021 Pietro Di Lena
 *  
 *  This file is part of the MNKGame v1.0 software developed for the
 *  students of the course "Algoritmi e Strutture di Dati" first 
 *  cycle degree/bachelor in Computer Science, University of Bologna
 *  A.Y. 2020-2021.
 *
 *  MNKGame is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this file.  If not, see <https://www.gnu.org/licenses/>.
 */

package mnkgame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.Random;

import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Callable;

/**
 * Initializes, updates and starts the (M,N,K)-game.
 * <p>Usage: MNKGame &lt;M&gt; &lt;N&gt; &lt;K&gt; [MNKPlayer class name]</p>
 */
@SuppressWarnings("serial")
public class MNKGame extends JFrame {
	/** Game Board */
	private final MNKBoard B;

	// Final constants for graphics drawing
	private final int CELL_SIZE           = 100;             // cell width and height (square)
	private final int GRID_WIDTH          = 8;               // Grid-line's width
	private final int GRID_WIDHT_HALF     = GRID_WIDTH / 2;  // Grid-line's half-width
	private final int CELL_PADDING        = CELL_SIZE  / 6;  // Padding for CROSS/NOUGHTS
	private final int SYMBOL_SIZE         = CELL_SIZE - CELL_PADDING * 2; // width/height
	private final int SYMBOL_STROKE_WIDTH = 8; // pen's stroke width
	
	private final int BOARD_WIDTH;             // the drawing canvas
	private final int BOARD_HEIGHT;

	private DrawBoard board;  // Drawing canvas (JPanel) for the game board
	private JLabel statusBar;  // Status Bar

	private enum MNKGameType {
		HUMANvsHUMAN, HUMANvsCOMPUTER
	}

	private enum MNKPlayerType {
		HUMAN, COMPUTER
	}

	private boolean         runningPlayer = false;
	private MNKGameType     gameType;       // game type
	private MNKPlayerType[] Player = new MNKPlayerType[2]; 

	private static MNKPlayer ComPlayer = null;
	private final int TIMEOUT = 10; // 10 seconds timeout

	// Random number generator
	private Random Rand = new Random(System.currentTimeMillis());

	/** Private constructor to setup the game and the GUI components */
	private MNKGame(int M, int N, int K, MNKGameType type) {
		gameType = type;
    B        = new MNKBoard(M,N,K);

		BOARD_WIDTH  = CELL_SIZE * N;
		BOARD_HEIGHT = CELL_SIZE * M;

		board = new DrawBoard();  // Construct a drawing class (a JPanel)
		board.setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));

		// Add MouseEvent upon mouse-click
		board.addMouseListener(new MNKMouseAdapter());
 
		// Setup the status bar (JLabel) to display status message
		statusBar = new JLabel("  ");
		statusBar.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 15));
 		statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 4, 5));
 
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(board, BorderLayout.CENTER);
		cp.add(statusBar, BorderLayout.PAGE_END); 
 
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();  // pack all the components in this JFrame

		initGame(); // initialize board and variables	
	}

	private class MNKMouseAdapter extends MouseAdapter {

		private class StoppablePlayer implements Callable<MNKCell> {
			private final MNKPlayer P;
			private final MNKBoard  B;

			public StoppablePlayer(MNKPlayer P, MNKBoard B) {
				this.P = P;
				this.B = B;
			}
	
			public MNKCell call()  throws InterruptedException {
				return P.selectCell(B.getFreeCells(),B.getMarkedCells());
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {  // mouse-clicked handler
			int X = e.getX();
			int Y = e.getY();
			int i = Y / CELL_SIZE;
			int j = X / CELL_SIZE;

			if(B.gameState() == MNKGameState.OPEN) {	// Keep playing
				if(Player[B.currentPlayer()] == MNKPlayerType.HUMAN) { // Human player
					if (B.cellState(i,j) == MNKCellState.FREE)	// if position is already marked do nothing, wait for next click
						B.markCell(i,j);
				} else { // Software player
					final ExecutorService executor = Executors.newSingleThreadExecutor();
					final Future<MNKCell> task     = executor.submit(new StoppablePlayer(ComPlayer,B));
					executor.shutdown(); // Makes the  ExecutorService stop accepting new tasks

					MNKCell c = null;

					try { 
  					c = task.get(TIMEOUT, TimeUnit.SECONDS); 
					}
					catch(TimeoutException ex) {
						executor.shutdownNow();	
						System.err.println(ComPlayer.playerName() + " interrupted due to timeout");
						System.exit(1);
					}
					catch (Exception ex) { 
						System.err.println("Error: " + ComPlayer.playerName() + " interrupted due to exception");
						System.err.println(" " + ex);
						System.exit(1);
					}
					if (!executor.isTerminated())
    				executor.shutdownNow(); 

					if(B.cellState(c.i,c.j) == MNKCellState.FREE) {
						B.markCell(c.i,c.j);
					} else {
						System.err.println(ComPlayer.playerName() + "  selected an illegal move!");
						System.exit(1);
       		}
				}
			} else { // Restart game
				initGame();
			}
			repaint();
		}
	} 

	// If first game, random selection, otherwise switch
	private void selectPlayerTurn() {
		if(Player[0] == null) { // first game, random selection	
			Player[0] = MNKPlayerType.HUMAN; 
			Player[1] = MNKPlayerType.HUMAN; 
			if(gameType == MNKGameType.HUMANvsCOMPUTER)
				Player[Rand.nextInt(2)] = MNKPlayerType.COMPUTER;
		} else {                // from second game, switch
			MNKPlayerType tmp = Player[0];
			Player[0]         = Player[1];
			Player[1]         = tmp;
		}
	}

	private void initGame() {
		selectPlayerTurn();
		// Timed-out initializaton of the MNKPlayer
		if(gameType == MNKGameType.HUMANvsCOMPUTER) {
			final Runnable initPlayer = new Thread() {
 				@Override 
				public void run() { 
					ComPlayer.initPlayer(B.M,B.N,B.K,Player[0] == MNKPlayerType.COMPUTER);
				}
			};

			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future future = executor.submit(initPlayer);
			executor.shutdown();
			try { 
  			future.get(TIMEOUT, TimeUnit.SECONDS); 
			} 
			catch (TimeoutException e) {
				System.err.println("Error: " + ComPlayer.playerName() + " interrupted: initialization takes too much time");
				System.exit(1);
			}
			catch (Exception e) { 
				System.err.println(e);
				System.exit(1);		
			}
			if (!executor.isTerminated())
    		executor.shutdownNow();
		}

		B.reset();

		String P1 = Player[0] == MNKPlayerType.HUMAN ? "Human" : ComPlayer.playerName();
		String P2 = Player[1] == MNKPlayerType.HUMAN ? "Human" : ComPlayer.playerName();
		setTitle("(" + B.M + "," + B.N + "," + B.K + ")-Game   " + P1 + " vs " + P2);
    	setVisible(true);  // show this JFrame
			
		repaint();
	}
 
 
	/**
	 *  Inner class for custom graphics drawing.
	 */
	private class DrawBoard extends JPanel {
		@Override
		public void paintComponent(Graphics g) {  // invoke via repaint()
			super.paintComponent(g);    // fill background
			setBackground(Color.WHITE); // set its background color
 
			// Draw the grid-lines
			g.setColor(Color.LIGHT_GRAY);
			for (int row = 1; row < B.M; ++row) {
				g.fillRoundRect(0, CELL_SIZE * row - GRID_WIDHT_HALF,
					BOARD_WIDTH-1, GRID_WIDTH, GRID_WIDTH, GRID_WIDTH);
			}
			for (int col = 1; col < B.N; ++col) {
				g.fillRoundRect(CELL_SIZE * col - GRID_WIDHT_HALF, 0,
					GRID_WIDTH, BOARD_HEIGHT-1, GRID_WIDTH, GRID_WIDTH);
			}
 
			// Draw the Seeds of all the cells if they are not empty
			Graphics2D g2d = (Graphics2D)g;
			g2d.setStroke(new BasicStroke(SYMBOL_STROKE_WIDTH, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));  

			MNKCell[] list = B.getMarkedCells();
 
			for(MNKCell c : list) {
				int x1 = c.j * CELL_SIZE + CELL_PADDING;
				int y1 = c.i * CELL_SIZE + CELL_PADDING;
				MNKCellState s = c.state;
				if (s == MNKCellState.P1) {
					g2d.setColor(Color.RED);
					int x2 = (c.j + 1) * CELL_SIZE - CELL_PADDING;
					int y2 = (c.i + 1) * CELL_SIZE - CELL_PADDING;
					g2d.drawLine(x1, y1, x2, y2);
					g2d.drawLine(x2, y1, x1, y2);
				} else if(s == MNKCellState.P2) {
					g2d.setColor(Color.BLUE);
					g2d.drawOval(x1, y1, SYMBOL_SIZE, SYMBOL_SIZE);
				}
			} 

			// Print status-bar message
			switch(B.gameState()) {
				case OPEN:
					statusBar.setForeground(Color.BLACK);
					String symbol = B.currentPlayer() == 0 ? "X" : "O";
					String msg = Player[B.currentPlayer()] == MNKPlayerType.COMPUTER ? "Click to run" : "Click to select";
					statusBar.setText(symbol + "'s Turn (" + Player[B.currentPlayer()] + ") - " + msg);
					break;
				case DRAW:
					statusBar.setForeground(Color.RED);
					statusBar.setText("Draw! Click to play again.");
					break;
				case WINP1:
					statusBar.setForeground(Color.RED);
					statusBar.setText("X (" + Player[0] + ") Won! Click to play again.");
					break;
				case WINP2:
					statusBar.setForeground(Color.RED);
					statusBar.setText("O (" + Player[1] + ") Won! Click to play again.");
					break;
			}
		}
	}

	public static void main(String[] args) {
		if(args.length != 3 && args.length != 4) {
			System.err.println("Usage: MNKGame <M> <N> <K> [MNKPlayer class]");
			return;
		}	

		int M = Integer.parseInt(args[0]);
		int N = Integer.parseInt(args[1]);
		int K = Integer.parseInt(args[2]);
		
		// Parameters check
		if(M <= 0 || N <= 0 || K <= 0) {
			System.err.println("Error: M, N, K must be larger than 0");
			System.exit(1);
		}
		
		// Check if the class parameter exists and it is an MNKPlayer implementation
		if(args.length == 4) {
			try {
				ComPlayer = (MNKPlayer) Class.forName(args[3]).getDeclaredConstructor().newInstance();
			}
			catch(ClassNotFoundException e) {
				System.err.println("Error: \'" + args[3] + "\' class not found");
				System.exit(1);
			}
			catch(ClassCastException e) {
				System.err.println("Error: \'" + args[3] + "\' class does not implement the MNKPlayer interface");
				System.exit(1);
			}
			catch(NoSuchMethodException e) {
				System.err.println("Error: \'" + args[3] + "\' class constructor needs to be empty");
				System.exit(1);
			}
			catch (Exception e) {
				System.err.println("  " + e);
				System.exit(1);	
			}
		}

		// Select the game type
		MNKGameType type = args.length == 4 ? MNKGameType.HUMANvsCOMPUTER : MNKGameType.HUMANvsHUMAN;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// The constructor setups and runs the game
				new MNKGame(M,N,K,type); 
			}
		});
	}
}
