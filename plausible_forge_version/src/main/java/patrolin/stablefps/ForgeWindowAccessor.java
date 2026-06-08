package patrolin.stablefps;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.earlydisplay.SimpleBufferBuilder;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import net.minecraftforge.fml.loading.progress.StartupNotificationManager;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

public class ForgeWindowAccessor {
  public static DisplayWindow oldDisplayWindow;
  private static void shutdownOldDisplayWindow() {
    try {
      // shutdown oldDisplayWindow
      oldDisplayWindow = getDisplayWindow();
      Field field = DisplayWindow.class.getDeclaredField("renderScheduler");
      field.setAccessible(true);
      ScheduledExecutorService renderScheduler = (ScheduledExecutorService)field.get(oldDisplayWindow);
      renderScheduler.shutdown();
      GLFW.glfwMakeContextCurrent(0);
      // recreate SimpleBufferBuilder
      String[] arrayKeys = {"VERTEX_ARRAYS", "VERTEX_BUFFERS", "VERTEX_BUFFER_LENGTHS"};
      for (String key : arrayKeys) {
        field = SimpleBufferBuilder.class.getDeclaredField(key);
        field.setAccessible(true);
        int[] array = (int[])field.get(null);
        Arrays.fill(array, 0);
      }
      String[] intKeys = {"elementBuffer", "elementBufferVertexLength"};
      for (String key : intKeys) {
        field = SimpleBufferBuilder.class.getDeclaredField(key);
        field.setAccessible(true);
        field.setInt(null, 0);
      }
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
  }
  public static void closeOldWindow() throws InterruptedException {
    long window = getWindow(getDisplayWindow());
    GLFW.glfwHideWindow(window);
    GLFW.glfwPollEvents();
  }

  public static long recreateWindow() {
    shutdownOldDisplayWindow();
    ProgressMeter meter = StartupNotificationManager.getCurrentProgress().getFirst();
    StableFPS.LOGGER.info("getCurrentProgress(): {}", meter.progress());
    meter.complete();
    ImmediateWindowHandler.load("client", new String[]{});
    StableFPS.LOGGER.info("getCurrentProgress(): {}", meter.progress());
    long window = getWindow(getDisplayWindow());
    GLFW.glfwFocusWindow(window);
    return window;
  }
  private static DisplayWindow getDisplayWindow() {
    try {
      Field field = ImmediateWindowHandler.class.getDeclaredField("provider");
      field.setAccessible(true);
      return (DisplayWindow)field.get(null);
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
  }
  private static long getWindow(DisplayWindow displayWindow) {
    try {
      Field field = DisplayWindow.class.getDeclaredField("window");
      field.setAccessible(true);
      return field.getLong(displayWindow);
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
  }
}
