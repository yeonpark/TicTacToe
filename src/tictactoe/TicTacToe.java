package tictactoe;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TicTacToe implements Runnable, ActionListener {

	JPanel jPanel = new JPanel();
	JLabel label = new JLabel();
	JTextField textField = new JTextField(18);
	JButton submitButton = new JButton("Submit");
	JMenu control = new JMenu("Control");
	JMenuBar controlBar = new JMenuBar();
	JMenu help = new JMenu("Help");
	JMenuItem exit = new JMenuItem("Exit");
	JMenuItem instruction = new JMenuItem("Instruction");
	JFrame dialogFrame = new JFrame("Message");

	private String ip = "127.0.0.1";
	private int port = 22222;
	private JFrame frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 620;
	private Thread thread;

	private Painter painter;
	private Socket socket;
	private DataOutputStream dataOutputStream;
	private DataInputStream dataInputStream;

	private ServerSocket serverSocket;

	private BufferedImage board;
	private BufferedImage o;
	private BufferedImage x;

	private String[] spaces = new String[9];

	private boolean yourTurn = false;
	private boolean circle = true;
	private boolean secondPlayer = false;
	private boolean accepted = false;
	private boolean unableToCommunicateWithOpponent = false;
	private boolean won = false;
	private boolean enemyWon = false;
	private boolean tie = false;

	private int lengthOfSpace = 160;
	private int errors = 0;

	private String wonString = "Congratulations. You win.";
	private String enemyWonString = "You lose.";
	private String tieString = "Draw";

	private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 },
			{ 0, 4, 8 }, { 2, 4, 6 } };

	/**
	 * <pre>
	 * 0, 1, 2 
	 * 3, 4, 5 
	 * 6, 7, 8
	 * </pre>
	 */

	public TicTacToe() {

		loadImages();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		if (!connect())
			initializeServer();

		frame = new JFrame("Tic Tac Toe");
		frame.setLayout(new BorderLayout());

		label.setText("Enter your player name...");

		control.add(exit);
		controlBar.add(control);
		help.add(instruction);
		controlBar.add(help);

		jPanel.add(textField);
		jPanel.add(submitButton);

		exit.addActionListener(this);
		instruction.addActionListener(this);
		submitButton.addActionListener(this);

		frame.setJMenuBar(controlBar);
		frame.add(painter, BorderLayout.CENTER);
		frame.add(label, BorderLayout.NORTH);
		frame.add(jPanel, BorderLayout.SOUTH);

		frame.setSize(WIDTH, HEIGHT);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		
		thread = new Thread(this, "TicTacToe");
		thread.start();
	}

	public void run() {
		while (true) {
			tick();
			painter.repaint();

			if (!circle && !accepted) {
				listenForServerRequest();
			}
		}
	}

	private void render(Graphics g) {
		g.drawImage(board, 0, 0, null);
		if (accepted) {
			for (int i = 0; i < spaces.length; i++) {
				if (spaces[i] != null) {
					if (spaces[i].equals("X")) {
						g.drawImage(x, (i % 3) * lengthOfSpace + 10 * (i % 3),
								(int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
					} else if (spaces[i].equals("O")) {
						g.drawImage(o, (i % 3) * lengthOfSpace + 10 * (i % 3),
								(int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
					}
				}
			}
		}
	}

	private void tick() {
		if (errors >= 10)
			unableToCommunicateWithOpponent = true;

		if (!yourTurn && !unableToCommunicateWithOpponent) {
			try {
				int space = dataInputStream.readInt();
				label.setText("Your opponent has moved, now is your turn");
				if (circle)
					spaces[space] = "X";
				else
					spaces[space] = "O";
				checkForEnemyWin();
				yourTurn = true;
				checkForTie();
				if (enemyWon) {
					JOptionPane.showMessageDialog(dialogFrame, enemyWonString);
					dialogFrame.setLocationRelativeTo(frame);
					
				}
				
				if (tie)
					JOptionPane.showMessageDialog(dialogFrame, tieString);	
					dialogFrame.setLocationRelativeTo(frame);
					
			} catch (IOException e) {
				e.printStackTrace();
				errors++;
			}
		}
	}

	private void checkForWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
					won = true;
				}
			} else {
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
					won = true;
				}
			}
		}
	}

	private void checkForEnemyWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
					enemyWon = true;
				}
			} else {
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
					enemyWon = true;
				}
			}
		}
	}

	private void checkForTie() {
		for (int i = 0; i < spaces.length; i++) {
			if (spaces[i] == null) {
				return;
			}
		}
		tie = true;
	}

	private void listenForServerRequest() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataInputStream = new DataInputStream(socket.getInputStream());
			accepted = true;

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean connect() {
		try {
			socket = new Socket(ip, port);
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataInputStream = new DataInputStream(socket.getInputStream());
			accepted = true;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private void initializeServer() {
		try {
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		yourTurn = true;
		circle = false;
	}

	private void loadImages() {
		try {
			board = ImageIO.read(new java.io.FileInputStream("res/board.png"));
			o = ImageIO.read(new java.io.FileInputStream("res/o.png"));
			x = ImageIO.read(new java.io.FileInputStream("res/x.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		TicTacToe ticTacToe = new TicTacToe();
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == exit) {
			System.exit(100);
		}

		if (e.getSource() == instruction) {
			JFrame DialogFrame = new JFrame("Message");
			JOptionPane.showMessageDialog(DialogFrame, "Some information about the game:\r\n"
					+ "Criteria for a vaild move:\r\n" + "-The move is not occcupied by any mark. \r\n"
					+ "-The move is made in the player's turn. \r\n " + "-The move is made within the 3 x 3 board. \r\n"
					+ " The game will continue and switch among the opposite player until it reaches either one of the following conditions: \r\n"
					+ "-Player 1 wins. \r\n" + "-Player 2 wins. \r\n" + "-Draw.");
		}

		if (e.getSource() == submitButton) {
			label.setText("WELCOME " + textField.getText());
			frame.setTitle("Tic Tac Toe - Player " + textField.getText());
			submitButton.setEnabled(false);
			textField.setEditable(false);
			secondPlayer = true;
		}
	}

	private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (accepted) {
				if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
					int x = e.getX() / lengthOfSpace;
					int y = e.getY() / lengthOfSpace;
					y *= 3;
					int position = x + y;

					if (spaces[position] == null) {
						if (!circle) {
							spaces[position] = "X";
							label.setText("Vaild move, wait for your opponent");
						} else {
							spaces[position] = "O";
							label.setText("Vaild move, wait for your opponent");
						}
						yourTurn = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dataOutputStream.writeInt(position);
							dataOutputStream.flush();
						} catch (IOException e1) {
							errors++;
							e1.printStackTrace();
						}
						checkForWin();
						if (won) {
							JOptionPane.showMessageDialog(dialogFrame, wonString);
							dialogFrame.setLocationRelativeTo(frame);
							System.exit(100);
							
						}
						checkForTie();
						if (tie && !enemyWon && !won) {
							JOptionPane.showMessageDialog(dialogFrame, tieString);
							dialogFrame.setLocationRelativeTo(frame);
							System.exit(100);
						}
					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	}

}
