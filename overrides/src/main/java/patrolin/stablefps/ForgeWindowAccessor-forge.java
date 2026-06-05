package patrolin.stablefps;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ForgeWindowAccessor {
  public static boolean isForgeLoadingWindowClosed = false;
  public static String getForgeOpenGLVersion() {
    DisplayWindow displayWindow = getDisplayWindow();
    try {
      Field field = DisplayWindow.class.getDeclaredField("glVersion");
      field.setAccessible(true);
      return (String)field.get(displayWindow);
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
  }
  public static void closeForgeLoadingWindow() {
    if (isForgeLoadingWindowClosed) return;
    isForgeLoadingWindowClosed = true;
    try {
      DisplayWindow displayWindow = getDisplayWindow();
      // get `DisplayWindow.renderScheduler`
      Field field = DisplayWindow.class.getDeclaredField("renderScheduler");
      field.setAccessible(true);
      ScheduledExecutorService forgeScheduler = (ScheduledExecutorService)field.get(displayWindow);
      // shutdown renderScheduler
      forgeScheduler.shutdown();
      boolean didShutdown = forgeScheduler.awaitTermination(60, TimeUnit.SECONDS);
      if (!didShutdown) throw new AssertionError("StableFPS: Failed to shutdown Forge loading-window threads.");

      // close Forge loading-window
      long forgeLoadingWindow = getWindow(displayWindow);
      GLFW.glfwDestroyWindow(forgeLoadingWindow);
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
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
