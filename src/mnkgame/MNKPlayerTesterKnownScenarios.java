/*
 *  Copyright (C) 2021 Pietro Di Lena
 *  
 *  This file is part of the MNKGame v2.0 software developed for the
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.*;


/**
 * Runs a game against two MNKPlayer classes and prints the game scores:
 * <ul>
 * <li> 3 if second player wins (and the first player is not interrupted)</li>
 * <li> 2 if first player wins or if the adversary is interrupted (illegal move or timeout interrupt)</li>
 * <li> 1 if the game ends in draw </li>
 * </ul>
 * <p>
 * Usage: MNKPlayerTester [OPTIONS] &lt;M&gt; &lt;N&gt; &lt;K&gt; &lt;MNKPlayer class name&gt; &lt;MNKPlayer class name&gt;<br/>
 * OPTIONS:<br>
 * &nbsp;&nbsp;-t &lt;timeout&gt; Timeout in seconds</br>
 * &nbsp;&nbsp;-r &lt;rounds&gt;  &nbsp;Number of rounds</br>
 * &nbsp;&nbsp;-v &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Verbose
 * </p>
 */
public class MNKPlayerTesterKnownScenarios {
	private static int     TIMEOUT = 10;
	private static int     ROUNDS  = 1;
	private static boolean VERBOSE = false;

	private static int     M;
	private static int     N;
	private static int     K;

	private static MNKBoard B;

	private static MNKPlayer[] Player = new MNKPlayer[2];


	/** Scoring system */
	private static int WINP1SCORE = 2;
	private static int WINP2SCORE = 3;
	private static int DRAWSCORE  = 1;
	private static int ERRSCORE   = 2;

	private enum GameState {
		WINP1, WINP2, DRAW, ERRP1, ERRP2;
	}


	private MNKPlayerTesterKnownScenarios() {
	}


	private static void initGame() {
		if(VERBOSE) System.out.println("Initializing " + M + "," + N + "," + K + " board");
		B = new MNKBoard(M,N,K);
		// Timed-out initializaton of the MNKPlayers
		for(int k = 0; k < 2; k++) {
			if(VERBOSE) if(VERBOSE) System.out.println("Initializing " + Player[k].playerName() + " as Player " + (k+1));
			final int i = k; // need to have a final variable here
			final Runnable initPlayer = new Thread() {
				@Override
				public void run() {
					Player[i].initPlayer(B.M,B.N,B.K,i == 0,TIMEOUT);
				}
			};

			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future future = executor.submit(initPlayer);
			executor.shutdown();
			try {
				future.get(TIMEOUT, TimeUnit.SECONDS);
			}
			catch (TimeoutException e) {
				System.err.println("Error: " + Player[i].playerName() + " interrupted: initialization takes too much time");
				System.exit(1);
			}
			catch (Exception e) {
				System.err.println(e);
				System.exit(1);
			}
			if (!executor.isTerminated())
				executor.shutdownNow();
		}
		if(VERBOSE) System.out.println();
	}

	private static class StoppablePlayer implements Callable<MNKCell> {
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

	private static GameState runGame() {
		while(B.gameState() == MNKGameState.OPEN) {
			int  curr = B.currentPlayer();
			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future<MNKCell> task     = executor.submit(new StoppablePlayer(Player[curr],B));
			executor.shutdown(); // Makes the  ExecutorService stop accepting new tasks

			MNKCell c = null;

			try {
				c = task.get(TIMEOUT, TimeUnit.SECONDS);
			}
			catch(TimeoutException ex) {
				int n = 3; // Wait some more time to see if it stops
				System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") interrupted due to timeout");
				while(!task.isDone() && n > 0) {
					System.err.println("Waiting for " + Player[curr].playerName() + " to stop ... (" + n + ")");
					try {Thread.sleep(TIMEOUT*1000);} catch(InterruptedException e) {}
					n--;
				}

				if(n == 0) {
					System.err.println("Player " + (curr+1) + " (" +Player[curr].playerName() + ") still running: game closed");
					System.exit(1);
				} else {
					System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") eventually stopped: round closed");
					return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
				}
			}
			catch (Exception ex) {
				int n = 3; // Wait some more time to see if it stops
				System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") interrupted due to exception");
				System.err.println(" " + ex);

				ex.printStackTrace();

				while(!task.isDone() && n > 0) {
					System.err.println("Waiting for " + Player[curr].playerName() + " to stop ... (" + n + ")");
					try {Thread.sleep(TIMEOUT*1000);} catch(InterruptedException e) {}
					n--;
				}
				System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") eventually stopped: round closed");
				return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
			}

			if (!executor.isTerminated())
				executor.shutdownNow();

			if(B.cellState(c.i,c.j) == MNKCellState.FREE) {
				if(VERBOSE) System.out.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") -> [" + c.i + "," + c.j + "]");
				B.markCell(c.i,c.j);
			} else {
				System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ")  selected an illegal move [" + c.i + "," + c.j + "]: round closed");
				return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
			}
		}

		return B.gameState() == MNKGameState.DRAW ? GameState.DRAW : (B.gameState() == MNKGameState.WINP1 ? GameState.WINP1 : GameState.WINP2);
	}

	public static void main(String[] args) {
		VERBOSE = true;

		int[][] configs = new int[][]{new int[]{3, 3, 3}, new int[]{3, 4, 3}, new int[]{4, 3, 3}, new int[]{4, 4, 4}, new int[]{4, 5, 4}, new int[]{5, 4, 4}, new int[]{6, 5, 4},new int[]{7, 6, 4}, new int[]{8, 4, 4}, new int[]{30, 4, 4} };
		GameState[] expectedResult = new GameState[]{GameState.DRAW, GameState.WINP1, GameState.WINP1, GameState.DRAW, GameState.DRAW, GameState.DRAW, GameState.WINP1, GameState.WINP1, GameState.DRAW, GameState.WINP1};

		HashMap<String, GameState[]> result = new HashMap<>();
		result.put("mnkgame.OurPlayer", new GameState[configs.length]);
		result.put("mnkgame.QuasiRandomPlayer", new GameState[configs.length]);
		result.put("mnkgame.RandomPlayer", new GameState[configs.length]);

		LinkedList<String[]> challenges = new LinkedList<>();
		challenges.add(new String[]{"mnkgame.OurPlayer", "mnkgame.OurPlayer"});
		challenges.add(new String[]{"mnkgame.OurPlayer", "mnkgame.QuasiRandomPlayer"});
		challenges.add(new String[]{"mnkgame.OurPlayer", "mnkgame.RandomPlayer"});

		for (int i=0; i< configs.length; i++) {
			M = configs[i][0]; N = configs[i][1]; K = configs[i][2];

			for (String pair[] : challenges) {
				String firstPlayer="", secondPlayer="";
				try {
					Player[0] = (MNKPlayer) Class.forName(pair[0]).getDeclaredConstructor().newInstance();
					Player[1] = (MNKPlayer) Class.forName(pair[1]).getDeclaredConstructor().newInstance();
					firstPlayer = pair[0];
					secondPlayer = pair[1];
				}
				catch(Exception e) { }

				System.out.println("Game type : " + M + "," + N + "," + K);
				System.out.println("Player1   : " + Player[0].playerName());
				System.out.println("Player2   : " + Player[1].playerName());

				initGame();
				GameState state = runGame();

				GameState[] poggia = result.get(secondPlayer);
				poggia[i] = state;
				result.put(secondPlayer, poggia);

				System.out.println("Game state: " + state);
				System.out.println("-------------------------");
				System.out.println();
			} // for (String pair[] : challenges)
		} // for (int[] config : configs)

		System.out.print(String.format("%30s\t", "OPPONENT"));
		for (int i=0; i< configs.length; i++) {
			System.out.print(String.format("%15s\t", ""+configs[i][0]+" "+configs[i][1]+" "+configs[i][2]+" (" + expectedResult[i] + ")" ));
		}
		System.out.println();
		for (String[] player : challenges) {
			System.out.print(String.format("%30s\t", player[1]));
			for (int i=0; i< configs.length; i++) {
				System.out.print(String.format("%15s\t", result.get(player[1])[i] == expectedResult[i] ? "ok" : result.get(player[1])[i]));
			}
			System.out.println();
		}
	}
}
