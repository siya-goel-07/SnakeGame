package SnakeSync;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class SnakeGame extends JFrame {

    public SnakeGame() {
        setTitle("SnakeSync - Snake Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SnakeGame game = new SnakeGame();
            game.setVisible(true);
        });
    }
}

class GamePanel extends JPanel {

    private static final int CELL_SIZE = 24;
    private static final int COLS = 25;
    private static final int ROWS = 25;
    private static final int BOARD_WIDTH = CELL_SIZE * COLS;
    private static final int BOARD_HEIGHT = CELL_SIZE * ROWS;

    private final Deque<Point> snake = new ArrayDeque<>();
    private final Random random = new Random();
    private final Timer timer;

    private Point food;
    private Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private boolean gameOver = false;
    private int score = 0;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public GamePanel() {
        setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        setBackground(new Color(20, 24, 28));

        setupKeyBindings();
        resetGame();

        timer = new Timer(120, event -> {
            if (!gameOver) {
                moveSnake();
            }
            repaint();
        });
        timer.start();
    }

    private void setupKeyBindings() {
        bindKey("pressed UP", "up", () -> changeDirection(Direction.UP));
        bindKey("pressed W", "w", () -> changeDirection(Direction.UP));

        bindKey("pressed DOWN", "down", () -> changeDirection(Direction.DOWN));
        bindKey("pressed S", "s", () -> changeDirection(Direction.DOWN));

        bindKey("pressed LEFT", "left", () -> changeDirection(Direction.LEFT));
        bindKey("pressed A", "a", () -> changeDirection(Direction.LEFT));

        bindKey("pressed RIGHT", "right", () -> changeDirection(Direction.RIGHT));
        bindKey("pressed D", "d", () -> changeDirection(Direction.RIGHT));

        bindKey("pressed ENTER", "restart", () -> {
            if (gameOver) {
                resetGame();
            }
        });
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);

        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void changeDirection(Direction requested) {
        if (isOpposite(direction, requested)) {
            return;
        }
        nextDirection = requested;
    }

    private boolean isOpposite(Direction a, Direction b) {
        return (a == Direction.UP && b == Direction.DOWN)
                || (a == Direction.DOWN && b == Direction.UP)
                || (a == Direction.LEFT && b == Direction.RIGHT)
                || (a == Direction.RIGHT && b == Direction.LEFT);
    }

    private void resetGame() {
        snake.clear();

        int x = COLS / 2;
        int y = ROWS / 2;

        snake.addFirst(new Point(x, y));
        snake.addLast(new Point(x - 1, y));
        snake.addLast(new Point(x - 2, y));

        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        score = 0;
        gameOver = false;

        placeFood();
        repaint();
    }

    private void placeFood() {
        do {
            food = new Point(random.nextInt(COLS),
                    random.nextInt(ROWS));
        } while (snake.contains(food));
    }

    private void moveSnake() {
        direction = nextDirection;

        Point head = snake.peekFirst();
        Point newHead = new Point(head);

        switch (direction) {
            case UP -> newHead.y--;
            case DOWN -> newHead.y++;
            case LEFT -> newHead.x--;
            case RIGHT -> newHead.x++;
        }

        // Wall collision
        if (newHead.x < 0 || newHead.x >= COLS
                || newHead.y < 0 || newHead.y >= ROWS) {
            gameOver = true;
            return;
        }

        boolean eating = newHead.equals(food);

        // The tail moves away unless the snake eats food.
        if (!eating) {
            snake.removeLast();
        }

        // Self-collision
        if (snake.contains(newHead)) {
            gameOver = true;
            return;
        }

        snake.addFirst(newHead);

        if (eating) {
            score += 10;
            placeFood();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();

        // Grid
        g.setColor(new Color(37, 43, 47));

        for (int x = 0; x < BOARD_WIDTH; x += CELL_SIZE) {
            g.drawLine(x, 0, x, BOARD_HEIGHT);
        }

        for (int y = 0; y < BOARD_HEIGHT; y += CELL_SIZE) {
            g.drawLine(0, y, BOARD_WIDTH, y);
        }

        // Food
        g.setColor(new Color(255, 90, 90));
        g.fillOval(
                food.x * CELL_SIZE + 3,
                food.y * CELL_SIZE + 3,
                CELL_SIZE - 6,
                CELL_SIZE - 6
        );

        // Snake
        boolean head = true;

        for (Point segment : snake) {
            g.setColor(head
                    ? new Color(100, 255, 140)
                    : new Color(45, 190, 105));

            g.fillRoundRect(
                    segment.x * CELL_SIZE + 2,
                    segment.y * CELL_SIZE + 2,
                    CELL_SIZE - 4,
                    CELL_SIZE - 4,
                    7, 7
            );

            head = false;
        }

        // Score
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 22);

        // Game-over overlay
        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 175));
            g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            drawCentered(g, "GAME OVER", BOARD_HEIGHT / 2 - 15);

            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            drawCentered(g,
                    "Score: " + score + " | Press ENTER to restart",
                    BOARD_HEIGHT / 2 + 22);
        }

        g.dispose();
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = (BOARD_WIDTH - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}
