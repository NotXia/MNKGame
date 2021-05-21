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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
public class MNKPlayerTesterChallenge {
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


	private MNKPlayerTesterChallenge() {
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
				if(n == 0) {
					System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") still running: game closed");
					System.exit(1);
				} else {
					System.err.println("Player " + (curr+1) + " (" + Player[curr].playerName() + ") eventually stopped: round closed");
					return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
				}
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

	private static void parseArgs(String args[]) {
		List<String> L = new ArrayList<String>(); 
		for (int i = 0; i < args.length; i++) {
			switch(args[i].charAt(0)) {
				case '-':
					char c = (args[i].length() != 2 ? 'x' : args[i].charAt(1));
					switch(c) {
						case 't': 
							if(args.length < i+2)
								throw new IllegalArgumentException("Expected parameter after " + args[i]);
							
							try {
								TIMEOUT = Integer.parseInt(args[++i]);
							} catch(NumberFormatException e) {
								throw new IllegalArgumentException("Illegal integer format for " + args[i-1] + " argument: " + args[i]);
							}
							break;
						case 'r':
							if(args.length < i+2)
								throw new IllegalArgumentException("Expected parameter after " + args[i]);	
							
							try {
								ROUNDS = Integer.parseInt(args[++i]);
							} catch(NumberFormatException e) {
								throw new IllegalArgumentException("Illegal integer format for " + args[i-1] + " argument: " + args[i]);
							}
							break;
						case 'v':
							VERBOSE = true;
							break;
						default: 
							throw new IllegalArgumentException("Illegal argument:  " + args[i]);
					}
					break;
				default:
				  L.add(args[i]);
			}
		}

		int n = L.size();
		if(n != 5)
			throw new IllegalArgumentException("Missing arguments:" + (n < 1 ? " <M>" : "") + (n < 2 ? " <N>" : "") +
			  (n < 3 ? " <K>" : "") + (n < 4 ? " <MNKPlayer class>" : "") + (n < 5 ? " <MNKPlayer class>" : "")); 

		try {
			M  = Integer.parseInt(L.get(0));
		}
		catch(NumberFormatException e) {
			throw new IllegalArgumentException("Illegal integer format for M argument: " + M);
		}
		try {
			N  = Integer.parseInt(L.get(1));
		}
		catch(NumberFormatException e) {
			throw new IllegalArgumentException("Illegal integer format for N argument: " + N);
		}
		try {
			K  = Integer.parseInt(L.get(2));
		}
		catch(NumberFormatException e) {
			throw new IllegalArgumentException("Illegal integer format for N argument: " + K);
		}

		if(M <= 0 || N <= 0 || K <= 0)
			throw new IllegalArgumentException("Arguments  M, N, K must be larger than 0");

		String[] P = {L.get(3),L.get(4)};
		for(int i = 0; i < 2; i++) {
			try {
				Player[i] = (MNKPlayer) Class.forName(P[i]).getDeclaredConstructor().newInstance();
			}
			catch(ClassNotFoundException e) {
				throw new IllegalArgumentException("Illegal argument: \'" + P[i] + "\' class not found");
      }
      catch(ClassCastException e) {
				throw new IllegalArgumentException("Illegal argument: \'" + P[i] + "\' class does not implement the MNKPlayer interface");
      }
      catch(NoSuchMethodException e) {
				throw new IllegalArgumentException("Illegal argument: \'" + P[i] + "\' class constructor needs to be empty");
      }
			catch(Exception e) {
				throw new IllegalArgumentException("Illegal argument: \'" + P[i] + "\' class (unexpected exception) " + e);
			}
    }
	}

	private static void printUsage() {
		System.err.println("Usage: MNKPlayerTester [OPTIONS] <M> <N> <K> <MNKPlayer class> <MNKPlayer class>");
		System.err.println("OPTIONS:");
		System.err.println("  -t <timeout>  Timeout in seconds. Default: " + TIMEOUT);
		System.err.println("  -r <rounds>   Number of rounds. Default: " + ROUNDS);
		System.err.println("  -v            Verbose. Default: " + VERBOSE);
	}

	public static void main(String[] args) {
		//VERBOSE = true;
		String[] players = new String[]{"mnkgame.OurPlayer", "mnkgame.RandomPlayer", "mnkgame.QuasiRandomPlayer"};

		HashMap<String, Integer> scores = new HashMap<>();
		final int WIN = 0, LOSS = 1, DRAW = 2;
		HashMap<String, Integer[]> kda = new HashMap<>(); // WIN-LOSS-DRAW
		for (int i=0; i<players.length; i++) {
			scores.put(players[i], 0);
			kda.put(players[i], new Integer[]{0, 0, 0});
		}

		int[][] configs = new int[][]{
			new int[]{3, 3, 3}, new int[]{4, 3, 3}, new int[]{4, 4, 3}, new int[]{4, 4, 4}, new int[]{5, 4, 4}, new int[]{5, 5, 4},
			new int[]{5, 5, 5}, new int[]{6, 4, 4}, new int[]{6, 5, 4}, new int[]{6, 6, 4}, new int[]{6, 6, 5}, new int[]{6, 6, 6},
			new int[]{7, 4, 4}, new int[]{7, 5, 4}, new int[]{7, 6, 4}, new int[]{7, 7, 4}, new int[]{7, 5, 5}, new int[]{7, 6, 5},
			new int[]{7, 7, 5}, new int[]{7, 7, 6}, new int[]{7, 7, 7}, new int[]{8, 8, 4}, new int[]{10, 10, 5}, new int[]{50, 50, 10}, new int[]{70, 70, 10}
		};

		LinkedList<String[]> challenges = new LinkedList<>();
		for (int i=0; i<players.length; i++) {
			for (int j=i+1; j<players.length; j++) {
				challenges.add(new String[]{players[i], players[j]});
			}
		}

		for (int[] config : configs) {
			M = config[0]; N = config[1]; K = config[2];

			for (String pair[] : challenges) {
				for (int j=0; j<4; j++) {
					String firstPlayer="", secondPlayer="";
					try {
						if (j < 2) { // Due partite come primo giocatore
							Player[0] = (MNKPlayer) Class.forName(pair[0]).getDeclaredConstructor().newInstance();
							Player[1] = (MNKPlayer) Class.forName(pair[1]).getDeclaredConstructor().newInstance();
							firstPlayer = pair[0];
							secondPlayer = pair[1];
						}
						else { // Due partite come secondo giocatore
							Player[0] = (MNKPlayer) Class.forName(pair[1]).getDeclaredConstructor().newInstance();
							Player[1] = (MNKPlayer) Class.forName(pair[0]).getDeclaredConstructor().newInstance();
							firstPlayer = pair[1];
							secondPlayer = pair[0];
						}
					}
					catch(Exception e) { }

					System.out.println("Game type : " + M + "," + N + "," + K);
					System.out.println("Player1   : " + Player[0].playerName());
					System.out.println("Player2   : " + Player[1].playerName());

					initGame();
					GameState state = runGame();
					switch(state) {
						case WINP1:
							scores.put(firstPlayer, scores.get(firstPlayer)+WINP1SCORE);
							kda.put(firstPlayer, new Integer[]{kda.get(firstPlayer)[WIN]+1, kda.get(firstPlayer)[LOSS], kda.get(firstPlayer)[DRAW]});
							kda.put(secondPlayer, new Integer[]{kda.get(secondPlayer)[WIN], kda.get(secondPlayer)[LOSS]+1, kda.get(secondPlayer)[DRAW]});
							break;
						case WINP2:
							scores.put(secondPlayer, scores.get(secondPlayer)+WINP2SCORE);
							kda.put(firstPlayer, new Integer[]{kda.get(firstPlayer)[WIN], kda.get(firstPlayer)[LOSS]+1, kda.get(firstPlayer)[DRAW]});
							kda.put(secondPlayer, new Integer[]{kda.get(secondPlayer)[WIN]+1, kda.get(secondPlayer)[LOSS], kda.get(secondPlayer)[DRAW]});
							break;
						case ERRP1:
							scores.put(secondPlayer, scores.get(secondPlayer)+ERRSCORE);
							kda.put(firstPlayer, new Integer[]{kda.get(firstPlayer)[WIN], kda.get(firstPlayer)[LOSS]+1, kda.get(firstPlayer)[DRAW]});
							kda.put(secondPlayer, new Integer[]{kda.get(secondPlayer)[WIN]+1, kda.get(secondPlayer)[LOSS], kda.get(secondPlayer)[DRAW]});
							break;
						case ERRP2:
							scores.put(firstPlayer, scores.get(firstPlayer)+ERRSCORE);
							kda.put(firstPlayer, new Integer[]{kda.get(firstPlayer)[WIN]+1, kda.get(firstPlayer)[LOSS], kda.get(firstPlayer)[DRAW]});
							kda.put(secondPlayer, new Integer[]{kda.get(secondPlayer)[WIN], kda.get(secondPlayer)[LOSS]+1, kda.get(secondPlayer)[DRAW]});
							break;
						case DRAW :
							scores.put(firstPlayer, scores.get(firstPlayer)+DRAWSCORE);
							scores.put(secondPlayer, scores.get(secondPlayer)+DRAWSCORE);
							kda.put(firstPlayer, new Integer[]{kda.get(firstPlayer)[WIN], kda.get(firstPlayer)[LOSS], kda.get(firstPlayer)[DRAW]+1});
							kda.put(secondPlayer, new Integer[]{kda.get(secondPlayer)[WIN], kda.get(secondPlayer)[LOSS], kda.get(secondPlayer)[DRAW]+1});
							break;
					}
					System.out.println("Game state: " + state);
					System.out.println("-------------------------");
					System.out.println();
				} // for (int j=0; j<4; j++)
			} // for (String pair[] : challenges)
		} // for (int[] config : configs)

		System.out.println(String.format("%30s\t%5s\t%5s\t%5s\t%10s", "PLAYER", "WIN", "LOSS", "DRAW", "SCORE"));
		for (String player : players) {
			String out = String.format("%30s\t%5s\t%5s\t%5s\t%10s", player, kda.get(player)[WIN], kda.get(player)[LOSS], kda.get(player)[DRAW], scores.get(player));
			System.out.println(out);
		}

	}
}
