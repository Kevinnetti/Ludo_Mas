package env;

import cartago.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ARTIFACT_INFO(outports = {})
public class LudoBoard extends Artifact {

    private int[][] positions;
    private int currentPlayer;
    private int diceValue;
    private boolean gameOver = false;
    private volatile boolean isPaused = false; 
    
    private LudoGUI gui; 

    void init() {
        positions = new int[4][4];
        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                positions[p][t] = -1;  
                defineObsProperty("token", p, t, -1);
            }
        }
        currentPlayer = 0;
        diceValue = 0;
        
        defineObsProperty("current_player", 0);
        defineObsProperty("dice", 0);
        defineObsProperty("board_state", buildStateString());

        gui = new LudoGUI();
        gui.setVisible(true);
        gui.updateBoard(positions, currentPlayer, diceValue); 
    }

    private void checkPause() {
        while (isPaused) {
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
    }

    @OPERATION
    public void rollDice() {
        if (gameOver) return;
        checkPause();

        diceValue = (int)(Math.random() * 6) + 1;
        updateObsProperty("dice", diceValue);
        gui.updateBoard(positions, currentPlayer, diceValue); 
        pausa(500); 
    }

    @OPERATION
    public void moveToken(int tokenIndex, OpFeedbackParam<Boolean> success) {
        if (gameOver) { success.set(false); return; }
        checkPause(); 

        int currentPos = positions[currentPlayer][tokenIndex];
        int targetPos = currentPos;

        if (currentPos == -1) {
            if (diceValue == 6) targetPos = 0;
        } else if (currentPos >= 0 && currentPos < 57) {
            targetPos = Math.min(57, currentPos + diceValue);
        }

        // MOSTRA LA FRECCIA CHE SEGUE IL PERCORSO
        if (currentPos != targetPos) {
            gui.showArrow(currentPlayer, tokenIndex, currentPos, targetPos);
            pausa(600); 
            gui.hideArrow(); // Cancella la freccia
        }

        if (targetPos != currentPos && targetPos >= 0 && targetPos <= 50) {
            int myAbsolutePos = (currentPlayer * 13 + targetPos) % 52;

            for (int p = 0; p < 4; p++) {
                if (p != currentPlayer) {
                    for (int t = 0; t < 4; t++) {
                        int enemyLogicalPos = positions[p][t];
                        if (enemyLogicalPos >= 0 && enemyLogicalPos <= 50) {
                            int enemyAbsolutePos = (p * 13 + enemyLogicalPos) % 52;
                            if (myAbsolutePos == enemyAbsolutePos) {
                                positions[p][t] = -1; 
                                ObsProperty eatenProp = getObsPropertyByTemplate("token", p, t, null);
                                if (eatenProp != null) eatenProp.updateValue(2, -1); 
                                System.out.println(" EATEN P" + currentPlayer + " has eaten a token from P" + p);
                            }
                        }
                    }
                }
            }
        }

        positions[currentPlayer][tokenIndex] = targetPos;
        
        ObsProperty myProp = getObsPropertyByTemplate("token", currentPlayer, tokenIndex, null);
        if (myProp != null) myProp.updateValue(2, targetPos); 

        updateObsProperty("board_state", buildStateString());
        gui.updateBoard(positions, currentPlayer, diceValue);
        
        pausa(500); 
        checkPause(); 

        int tokensAtHome = 0;
        for (int t = 0; t < 4; t++) {
            if (positions[currentPlayer][t] == 57) tokensAtHome++;
        }

        if (tokensAtHome == 4) {
            gameOver = true;
            if (!hasObsProperty("winner")) {
                defineObsProperty("winner", currentPlayer);
            } else {
                getObsProperty("winner").updateValue(currentPlayer);
            }
            
            System.out.println(" Player " + currentPlayer + " wins!");
            
            final int winnerId = currentPlayer;
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(gui, 
                    "Player " + gui.pNames[winnerId] + " has won!\n\nDo you want to play another game?", 
                    "Game Over", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
                
                if (choice == JOptionPane.YES_OPTION) {
                    execInternalOp("restartGameOp");
                } else {
                    System.exit(0); 
                }
            });

            success.set(true);
            return;
        }

        nextTurn();
        success.set(true);
    }

    @INTERNAL_OPERATION
    void restartGameOp() {
        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                positions[p][t] = -1;
                ObsProperty prop = getObsPropertyByTemplate("token", p, t, null);
                if (prop != null) prop.updateValue(2, -1);
            }
        }
        
        currentPlayer = 0;
        diceValue = 0;
        gameOver = false;
        isPaused = false; 
        gui.pauseBtn.setText("⏸ Pause");
        gui.hideArrow(); 
        
        updateObsProperty("dice", 0);
        updateObsProperty("board_state", buildStateString());
        
        try {
            if (hasObsProperty("winner")) removeObsProperty("winner");
            if (hasObsProperty("current_player")) removeObsProperty("current_player");
        } catch (Exception e) {}
        
        defineObsProperty("current_player", 0); 
        
        gui.updateBoard(positions, currentPlayer, diceValue);
        System.out.println("\n New game started!\n");
    }

    private String buildStateString() {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < 4; p++) {
            sb.append("p").append(p).append(":[");
            for (int t = 0; t < 4; t++) {
                sb.append(positions[p][t]);
                if (t < 3) sb.append(",");
            }
            sb.append("]");
            if (p < 3) sb.append(";");
        }
        return sb.toString();
    }

    private void nextTurn() {
        currentPlayer = (currentPlayer + 1) % 4;
        updateObsProperty("current_player", currentPlayer);
        updateObsProperty("dice", 0);
    }

    private void pausa(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }

    
    // INTERFACCIA GRAFICA
    class LudoGUI extends JFrame {
        private int[][] currentPos;
        private int turn = 0;
        private int lastDice = 0;
        public final String[] pNames = {"Aggressive (Red)", "Prudent (Green)", "Balanced (Yellow)", "Emotive (Blue)"}; 
        
        public JButton pauseBtn; 
        private BoardPanel boardPanel;

        public LudoGUI() { 
            setTitle("VEsNA Ludo Simulator - Traditional Board"); 
            setSize(700, 800); 
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
            setLocationRelativeTo(null); 
            setLayout(new BorderLayout());

            boardPanel = new BoardPanel();
            add(boardPanel, BorderLayout.CENTER);

            JPanel controlPanel = new JPanel();
            pauseBtn = new JButton("⏸ Pause");
            pauseBtn.setFont(new Font("Arial", Font.BOLD, 18));
            pauseBtn.setFocusPainted(false);
            
            pauseBtn.addActionListener(e -> {
                isPaused = !isPaused;
                if (isPaused) {
                    pauseBtn.setText("▶ Resume");
                    pauseBtn.setBackground(Color.YELLOW);
                } else {
                    pauseBtn.setText("⏸ Pause");
                    pauseBtn.setBackground(null);
                }
            });
            
            controlPanel.add(pauseBtn);
            add(controlPanel, BorderLayout.SOUTH);
        }

        public void updateBoard(int[][] pos, int playerTurn, int dice) { 
            this.currentPos = pos; 
            this.turn = playerTurn; 
            this.lastDice = dice; 
            boardPanel.repaint(); 
        }

        public void showArrow(int p, int t, int startPos, int endPos) {
            boardPanel.setArrow(p, t, startPos, endPos);
        }

        public void hideArrow() {
            boardPanel.clearArrow();
        }

        class BoardPanel extends JPanel {
            private final Color[] pColors = {new Color(255, 60, 60), new Color(60, 255, 60), new Color(255, 230, 0), new Color(60, 120, 255)};
            private final int cellSize = 40;
            private final int offsetX = 40;
            private final int offsetY = 70;

            private boolean isArrowVisible = false;
            private int arrowP, arrowT, arrowStart, arrowEnd;

            public void setArrow(int p, int t, int start, int end) {
                this.arrowP = p;
                this.arrowT = t;
                this.arrowStart = start;
                this.arrowEnd = end;
                this.isArrowVisible = true;
                repaint();
            }

            public void clearArrow() {
                this.isArrowVisible = false;
                repaint();
            }

            private final Point[] perimeter = {
                new Point(1,6), new Point(2,6), new Point(3,6), new Point(4,6), new Point(5,6),
                new Point(6,5), new Point(6,4), new Point(6,3), new Point(6,2), new Point(6,1), new Point(6,0),
                new Point(7,0),
                new Point(8,0), new Point(8,1), new Point(8,2), new Point(8,3), new Point(8,4), new Point(8,5),
                new Point(9,6), new Point(10,6), new Point(11,6), new Point(12,6), new Point(13,6), new Point(14,6),
                new Point(14,7),
                new Point(14,8), new Point(13,8), new Point(12,8), new Point(11,8), new Point(10,8), new Point(9,8),
                new Point(8,9), new Point(8,10), new Point(8,11), new Point(8,12), new Point(8,13), new Point(8,14),
                new Point(7,14),
                new Point(6,14), new Point(6,13), new Point(6,12), new Point(6,11), new Point(6,10), new Point(6,9),
                new Point(5,8), new Point(4,8), new Point(3,8), new Point(2,8), new Point(1,8), new Point(0,8),
                new Point(0,7), new Point(0,6)
            };

            private final Point[][] homeStraights = {
                {new Point(1,7), new Point(2,7), new Point(3,7), new Point(4,7), new Point(5,7), new Point(6,7)},
                {new Point(7,1), new Point(7,2), new Point(7,3), new Point(7,4), new Point(7,5), new Point(7,6)},
                {new Point(13,7), new Point(12,7), new Point(11,7), new Point(10,7), new Point(9,7), new Point(8,7)},
                {new Point(7,13), new Point(7,12), new Point(7,11), new Point(7,10), new Point(7,9), new Point(7,8)}
            };

            private final Point[][] bases = {
                {new Point(2,2), new Point(3,2), new Point(2,3), new Point(3,3)},
                {new Point(11,2), new Point(12,2), new Point(11,3), new Point(12,3)},
                {new Point(11,11), new Point(12,11), new Point(11,12), new Point(12,12)},
                {new Point(2,11), new Point(3,11), new Point(2,12), new Point(3,12)}
            };

            private Point getPixelCoords(int p, int t, int pos) {
                if (pos == -1) return new Point(bases[p][t].x * cellSize + cellSize/2, bases[p][t].y * cellSize + cellSize/2);
                if (pos == 57) return new Point(7 * cellSize + cellSize/2 + (p*10 - 15), 7 * cellSize + cellSize/2 + (t*10 - 15));
                Point grid = null;
                if (pos >= 0 && pos <= 50) grid = perimeter[(p * 13 + pos) % 52];
                else if (pos >= 51 && pos <= 56) grid = homeStraights[p][pos - 51];
                if (grid != null) return new Point(grid.x * cellSize + cellSize/2, grid.y * cellSize + cellSize/2);
                return new Point(0,0);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); 
                Graphics2D g2d = (Graphics2D) g; 
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (currentPos == null) return;

                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Round: " + pNames[turn] + " | Dice: " + lastDice, 40, 40);

                g2d.setColor(pColors[0]); g2d.fillRect(offsetX, offsetY, 6*cellSize, 6*cellSize);
                g2d.setColor(pColors[1]); g2d.fillRect(offsetX + 9*cellSize, offsetY, 6*cellSize, 6*cellSize);
                g2d.setColor(pColors[2]); g2d.fillRect(offsetX + 9*cellSize, offsetY + 9*cellSize, 6*cellSize, 6*cellSize);
                g2d.setColor(pColors[3]); g2d.fillRect(offsetX, offsetY + 9*cellSize, 6*cellSize, 6*cellSize);

                g2d.setColor(Color.WHITE);
                for(int p=0; p<4; p++) for(int t=0; t<4; t++) g2d.fillOval(offsetX + bases[p][t].x*cellSize + 5, offsetY + bases[p][t].y*cellSize + 5, 30, 30);

                g2d.setColor(Color.BLACK);
                for(Point pt : perimeter) g2d.drawRect(offsetX + pt.x*cellSize, offsetY + pt.y*cellSize, cellSize, cellSize);
                

                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(offsetX + 6*cellSize, offsetY + 6*cellSize, 3*cellSize, 3*cellSize);
                
                for (int p=0; p<4; p++) {
                    for (Point pt : homeStraights[p]) {
                        g2d.setColor(new Color(pColors[p].getRed(), pColors[p].getGreen(), pColors[p].getBlue(), 100)); 
                        g2d.fillRect(offsetX + pt.x*cellSize, offsetY + pt.y*cellSize, cellSize, cellSize);
                        g2d.setColor(Color.BLACK);
                        g2d.drawRect(offsetX + pt.x*cellSize, offsetY + pt.y*cellSize, cellSize, cellSize);
                    }
                }

                

                for(int p=0; p<4; p++) {
                    for(int t=0; t<4; t++) {
                        int posValue = currentPos[p][t];
                        Point coord = getPixelCoords(p, t, posValue);
                        
                        int ox = 0, oy = 0;
                        if (posValue >= 0 && posValue < 57) {
                            ox = (t % 2 == 0) ? -5 : 5;
                            oy = (t < 2) ? -5 : 5;
                        }

                        g2d.setColor(pColors[p]); 
                        g2d.fillOval(offsetX + coord.x + ox - 12, offsetY + coord.y + oy - 12, 24, 24);
                        g2d.setColor(Color.BLACK); 
                        g2d.drawOval(offsetX + coord.x + ox - 12, offsetY + coord.y + oy - 12, 24, 24);
                    }
                }

                // DISEGNO DELLA FRECCIA MULTI-SEGMENTO
                if (isArrowVisible) {
                    List<Point> pathPoints = new ArrayList<>();
                    
                    if (arrowStart == -1) {
                        pathPoints.add(getPixelCoords(arrowP, arrowT, arrowStart));
                        pathPoints.add(getPixelCoords(arrowP, arrowT, arrowEnd));
                    } else {
                        for (int pos = arrowStart; pos <= arrowEnd; pos++) {
                            pathPoints.add(getPixelCoords(arrowP, arrowT, pos));
                        }
                    }

                    if (pathPoints.size() > 1) {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        for (int i = 0; i < pathPoints.size() - 1; i++) {
                            Point p1 = pathPoints.get(i);
                            Point p2 = pathPoints.get(i+1);
                            g2d.drawLine(offsetX + p1.x, offsetY + p1.y, offsetX + p2.x, offsetY + p2.y);
                        }
                        
                        //  colore interno della freccia
                        g2d.setColor(pColors[arrowP]);
                        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        for (int i = 0; i < pathPoints.size() - 1; i++) {
                            Point p1 = pathPoints.get(i);
                            Point p2 = pathPoints.get(i+1);
                            g2d.drawLine(offsetX + p1.x, offsetY + p1.y, offsetX + p2.x, offsetY + p2.y);
                        }

                        // punta della freccia
                        Point p1 = pathPoints.get(pathPoints.size() - 2);
                        Point p2 = pathPoints.get(pathPoints.size() - 1); 
                        
                        int x1 = offsetX + p1.x;
                        int y1 = offsetY + p1.y;
                        int x2 = offsetX + p2.x;
                        int y2 = offsetY + p2.y;
                        
                        double angle = Math.atan2(y2 - y1, x2 - x1);
                        int arrowSize = 18;
                        
            
                        int tipX = x2;
                        int tipY = y2;

                        int ax1 = (int) (tipX - arrowSize * Math.cos(angle - Math.PI / 6));
                        int ay1 = (int) (tipY - arrowSize * Math.sin(angle - Math.PI / 6));
                        int ax2 = (int) (tipX - arrowSize * Math.cos(angle + Math.PI / 6));
                        int ay2 = (int) (tipY - arrowSize * Math.sin(angle + Math.PI / 6));

                        g2d.setColor(Color.BLACK);
                        g2d.fillPolygon(new int[]{tipX, ax1, ax2}, new int[]{tipY, ay1, ay2}, 3);
                        
                        g2d.setColor(pColors[arrowP]);
                        int innerSize = 14;
                        int inX1 = (int) (tipX - innerSize * Math.cos(angle - Math.PI / 6));
                        int inY1 = (int) (tipY - innerSize * Math.sin(angle - Math.PI / 6));
                        int inX2 = (int) (tipX - innerSize * Math.cos(angle + Math.PI / 6));
                        int inY2 = (int) (tipY - innerSize * Math.sin(angle + Math.PI / 6));
                        g2d.fillPolygon(new int[]{tipX, inX1, inX2}, new int[]{tipY, inY1, inY2}, 3);
                        
                        g2d.setStroke(new BasicStroke(1)); // Resetta
                    }
                }
            }
        }
    }
}