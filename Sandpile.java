/**
 * Sandpile.java
 * Written by Adam Jackson
 *
 * Simple Abelian sandpile simulation.
 *
 * Usage:
 *  javac Sandpile.java
 *  java Sandpile [cols] [rows] [scale] [delayMs]
 *  put "random" as the last argument to enable random dropping
 *
 * Examples:
 *  java Sandpile 250 250 random
 *    use a 250x250 grid with random dropping
 *  java Sandpile 100 100 4
 *    use a 100x100 grid and scale up 4 times
 *
 * Defaults are in constants at top of class definition
 */










import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.Arrays;










public class Sandpile extends JPanel implements Runnable
{
 private static final int DEFAULT_COLS = 50;
 private static final int DEFAULT_ROWS = 50;
 private static final int DEFAULT_SCALE = 6;
 private static final int DEFAULT_DELAY_MS = 5;
 private static final int DEFAULT_AVALANCHE_DECAY = 2;
 
 private final Color COLOURS[] =
 {
  new Color(0x00, 0x00, 0x00), // black for 0
  new Color(0x0F, 0x48, 0x7F), // blueish for 1
  new Color(0x7F, 0x7F, 0x7F), // white for 2
  new Color(0x7F, 0x6B, 0x00) // gold for 3
 };
 
 private final int cols;
 private final int rows;
 private final int scale;
 private final int threshold = 4;
 private final int[][] grid;
 private final int [][] lastAvalanche;
 private final BufferedImage image;
 private volatile boolean running = true;
 private final int delayMs;
 private final boolean dropRandomly;
 
 private boolean avalancheThisFrame;
 









 // Constructor
 public Sandpile(int cols, int rows, int scale, int delayMs, boolean dropRandomly)
 {
  this.cols = cols;
  this.rows = rows;
  this.scale = scale;
  this.delayMs = delayMs;
  this.dropRandomly = dropRandomly;
  this.grid = new int[rows][cols];
  this.lastAvalanche = new int[rows][cols];
  this.image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
  
  setPreferredSize(new Dimension(cols * scale, rows * scale));
  setFocusable(true);
  
  for (int i = 0; i < rows; ++i) Arrays.fill(this.lastAvalanche[i], 255);
  
  // Close on ESC key
  addKeyListener(new KeyAdapter() {
   @Override
   public void keyPressed(KeyEvent e)
   {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
    {
     stop();
    }
   }
  });
 }
 









 public void stop()
 {
  running = false;
  SwingUtilities.getWindowAncestor(this).dispose();
 }
 









 // Here is the simulation loop
 @Override
 public void run()
 {
  int dropX = cols / 2;
  int dropY = rows / 2;
  Random rand = new Random();
 
  while (running)
  {
   if (dropRandomly)
   {
    dropX = rand.nextInt(cols);
    dropY = rand.nextInt(rows);
   }

   synchronized (grid)
   {
    grid[dropX][dropY]++;
    relax();
    renderToImage();
   }
   repaint();
   
   try
   {
    Thread.sleep(delayMs);
   }
   catch (InterruptedException e)
   {
    Thread.currentThread().interrupt();
    break;
   }
  }
 }
 
 
 
 
 
 
 
 
 
 
 // Relax the sandpile demonstrating use of a queue.
 // Uncomment the lines in the queue loop to cause the simulation to cascade any avalanches completely in one step as opposed to letting them carry over. This would be useful if you wanted to calculate the size of each avalanche
 private void relax()
 {
  Queue<Point> q = new ArrayDeque<>();
  // Initially, find cells that need toppling and add them to the queue
  for (int y = 0; y < rows; y++)
  {
   for (int x = 0; x < cols; x++)
   {
    lastAvalanche[y][x] = Math.min(255, lastAvalanche[y][x] * DEFAULT_AVALANCHE_DECAY);
    if (grid[y][x] >= threshold)
    {
     q.add(new Point(x, y));
    }
   }
  }
  
  while (!q.isEmpty())
  {
   Point p = q.poll();
   int x = p.x, y = p.y;
   int val = grid[y][x];
   if (val < threshold) continue;
   int toppleCount = val / threshold;
   grid[y][x] -= toppleCount * threshold;
   lastAvalanche[y][x] = 1;
   
   // distribute to neighbors (4-neighborhood)
   if (x > 0)
   {
    grid[y][x - 1] += toppleCount;
//    if (grid[y][x - 1] >= threshold) q.add(new Point(x - 1, y));
   }
   if (x < cols - 1)
   {
    grid[y][x + 1] += toppleCount;
//    if (grid[y][x + 1] >= threshold) q.add(new Point(x + 1, y));
   }
   if (y > 0)
   {
    grid[y - 1][x] += toppleCount;
//    if (grid[y - 1][x] >= threshold) q.add(new Point(x, y - 1));
   }
   if (y < rows - 1)
   {
    grid[y + 1][x] += toppleCount;
//    if (grid[y + 1][x] >= threshold) q.add(new Point(x, y + 1));
   }
  }
 }
 









 // Map values to colors and write into the small image buffer
 private void renderToImage()
 {
  for (int y = 0; y < rows; y++)
  {
   for (int x = 0; x < cols; x++)
   {
    int v = grid[y][x] % COLOURS.length;
    
    Color c = new Color(Math.min(255, COLOURS[v].getRed() + Math.max(0, 255 - lastAvalanche[y][x])),
                        COLOURS[v].getGreen(),
                        COLOURS[v].getBlue());
    
    image.setRGB(x, y, c.getRGB());
   }
  }
 }
 









 @Override
 protected void paintComponent(Graphics g)
 {
  super.paintComponent(g);
  Graphics2D g2 = (Graphics2D) g;
  // Draw the buffered image scaled up using nearest-neighbor scaling
  g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
  g2.drawImage(image, 0, 0, cols * scale, rows * scale, null);
 }
 









 public static void main(String[] args)
 {
  int cols = DEFAULT_COLS;
  int rows = DEFAULT_ROWS;
  int scale = DEFAULT_SCALE;
  int delayMs = DEFAULT_DELAY_MS;
  boolean dropRandomly = false;

  if (args.length > 0 && args[args.length - 1].equals("random")) dropRandomly = true;
  
  if (args.length >= 2)
  {
   try
   {
    cols = Integer.parseInt(args[0]);
    rows = Integer.parseInt(args[1]);
   }
   catch (NumberFormatException ignored)
   {}
  }
  if (args.length >= 3)
  {
   try
   {
    scale = Math.max(1, Integer.parseInt(args[2]));
   }
   catch (NumberFormatException ignored)
   {}
  }
  if (args.length >= 4)
  {
   try
   {
    delayMs = Math.max(0, Integer.parseInt(args[3]));
   }
   catch (NumberFormatException ignored)
   {}
  }
  
  final int fCols = cols, fRows = rows, fScale = scale, fDelay = delayMs;
  final boolean fDropRandomly = dropRandomly;
  SwingUtilities.invokeLater(() -> {
   Sandpile panel = new Sandpile(fCols, fRows, fScale, fDelay, fDropRandomly);
   JFrame frame = new JFrame("Sandpile");
   frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   frame.setUndecorated(false); // keep normal window decoration so user can move and close it
   frame.setResizable(false);
   frame.setContentPane(panel);
   frame.pack();
   // center on screen
   frame.setLocationRelativeTo(null);
   frame.setVisible(true);
   
   // start simulation thread
   Thread simThread = new Thread(panel, "Sandpile-Sim");
   simThread.setDaemon(true);
   simThread.start();
   
   // ensure program exits when window is closed
   frame.addWindowListener(new WindowAdapter()
                           {
    @Override
    public void windowClosed(WindowEvent e)
    {
     panel.running = false;
     System.exit(0);
    }
   });
   
   // give initial focus to panel so ESC works immediately
   panel.requestFocusInWindow();
  });
 }
}

