package patrolin.stablefps;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ForgeWindowAccessor {
  public static void closeForgeLoadingWindow() {
    try {
      // get `ImmediateWindowHandler.provider`
      Field field = ImmediateWindowHandler.class.getDeclaredField("provider");
      field.setAccessible(true);
      DisplayWindow displayWindow = (DisplayWindow)field.get(null);

      // get `DisplayWindow.renderScheduler`
      field = DisplayWindow.class.getDeclaredField("renderScheduler");
      field.setAccessible(true);
      ScheduledExecutorService forgeScheduler = (ScheduledExecutorService)field.get(displayWindow);
      // shutdown renderScheduler
      forgeScheduler.shutdown();
      boolean didShutdown = forgeScheduler.awaitTermination(60, TimeUnit.SECONDS);
      if (!didShutdown) throw new AssertionError("StableFPS: Failed to shutdown Forge loading-window threads.");

      // get `DisplayWindow.window`
      field = DisplayWindow.class.getDeclaredField("window");
      field.setAccessible(true);
      long window = field.getLong(displayWindow);
      // close Forge loading-window
      GLFW.glfwDestroyWindow(window);
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
  }
}
