package patrolin.stablefps;

import com.mojang.blaze3d.platform.Window;
import net.neoforged.fml.earlydisplay.*;
import net.neoforged.fml.earlydisplay.render.*;
import net.neoforged.fml.earlydisplay.theme.Theme;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NeoforgeWindowAccessor {
  private static Object getPrivateObject(Class<?> classObj, Object obj, String key) throws NoSuchFieldException, IllegalAccessException {
    Field field = classObj.getDeclaredField(key);
    field.setAccessible(true);
    return field.get(obj);
  }
  private static void setPrivateObject(Class<?> classObj, Object obj, String key, Object value) throws NoSuchFieldException, IllegalAccessException {
    Field field = classObj.getDeclaredField(key);
    field.setAccessible(true);
    field.set(obj, value);
  }
  private static int getPrivateInt(Class<?> classObj, Object obj, String key) throws NoSuchFieldException, IllegalAccessException {
    Field field = classObj.getDeclaredField(key);
    field.setAccessible(true);
    return field.getInt(obj);
  }
  private static void setPrivateInt(Class<?> classObj, Object obj, String key, int value) throws NoSuchFieldException, IllegalAccessException {
    Field field = classObj.getDeclaredField(key);
    field.setAccessible(true);
    field.setInt(obj, value);
  }
  private static long getPrivateLong(Class<?> classObj, Object obj, String key) throws NoSuchFieldException, IllegalAccessException {
    Field field = classObj.getDeclaredField(key);
    field.setAccessible(true);
    return field.getLong(obj);
  }
  private static void setPrivateLong(Class<?> classObj, Object obj, String key, long value) throws NoSuchFieldException, IllegalAccessException {
    Field field = classObj.getDeclaredField(key);
    field.setAccessible(true);
    field.setLong(obj, value);
  }

  public static long recreateForgeWindow(String title, long share) throws NoSuchFieldException, IllegalAccessException {
    DisplayWindow displayWindow = getDisplayWindow();
    long window = getPrivateLong(DisplayWindow.class, displayWindow, "window");
    int[] width = new int[1];
    int[] height = new int[1];
    GLFW.glfwGetWindowSize(window, width, height);
    long monitor = GLFW.glfwGetWindowMonitor(window);
    int major = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CONTEXT_VERSION_MAJOR);
    int minor = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CONTEXT_VERSION_MINOR);
    GLFW.glfwMakeContextCurrent(0);
    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, major);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, minor);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    StableFPS.LOGGER.info("--- ayaya.window: {}, major: {}, minor: {}", window, major, minor);
    StableFPS.LOGGER.info("--- ayaya.width: {}, height: {}, title: {}, monitor: {}, share: {}", width, height, title, monitor, share);
    return GLFW.glfwCreateWindow(width[0], height[0], title, monitor, share);
  }
  private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
      Thread.ofPlatform()
          .name("stablefps-loadingscreen")
          .daemon()
          .uncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught error on background rendering thread: " + e);
            e.printStackTrace();
          })
          .factory());;
  private static CountDownLatch loadingScreenRenderer_ready = new CountDownLatch(1);
  private static LoadingScreenRenderer loadingScreenRenderer;
  public static void closeForgeEarlyWindow() {
    StableFPS.LOGGER.info("--- ayaya.closeForgeEarlyWindow()");
    try {
      DisplayWindow displayWindow = getDisplayWindow();
      // shutdown renderScheduler
      ScheduledExecutorService forgeScheduler = (ScheduledExecutorService)getPrivateObject(DisplayWindow.class, displayWindow, "renderScheduler");
      ScheduledFuture<LoadingScreenRenderer> rendererFuture = scheduler.schedule(() -> {
        loadingScreenRenderer_ready.await();
        return loadingScreenRenderer;
      }, 0, TimeUnit.SECONDS);
      setPrivateObject(DisplayWindow.class, displayWindow, "rendererFuture", rendererFuture);
      forgeScheduler.shutdown();
      boolean didShutdown = forgeScheduler.awaitTermination(60, TimeUnit.SECONDS);
      if (!didShutdown) throw new AssertionError("StableFPS: Failed to shutdown Forge loading-window threads.");
      GLFW.glfwMakeContextCurrent(0);

      // recreate SimpleBufferBuilder
      String[] arrayKeys = {"VERTEX_ARRAYS", "VERTEX_BUFFERS", "VERTEX_BUFFER_LENGTHS"};
      for (String key : arrayKeys) {
        int[] array = (int[])getPrivateObject(SimpleBufferBuilder.class, null, key);
        Arrays.fill(array, 0);
      }
      String[] intKeys = {"elementBuffer", "elementBufferVertexLength"};
      for (String key : intKeys) {
        setPrivateInt(SimpleBufferBuilder.class, null, key, 0);
      }

      // close Forge loading-window
      long forgeLoadingWindow = getWindow(displayWindow);
      GLFW.glfwDestroyWindow(forgeLoadingWindow);
    } catch (Exception err) {
      StableFPS.LOGGER.error("", err);
      throw new AssertionError(err.getMessage());
    }
  }
  public static void recreateForgeFramebuffer(long window) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InterruptedException, TimeoutException {
    StableFPS.LOGGER.info("--- ayaya.recreateForgeFramebuffer");
    // create GL context
    GLFW.glfwMakeContextCurrent(StableFPS.window);
    GL.createCapabilities();
    GL32C.glClearColor(0f, 0f, 0f, 1f);
    // swap to our window
    DisplayWindow displayWindow = getDisplayWindow();
    setPrivateLong(DisplayWindow.class, displayWindow, "window", window);
    // recreate the OpenGL program
    Theme theme = (Theme)getPrivateObject(DisplayWindow.class, displayWindow, "theme");
    var themePathGetter = DisplayWindow.class.getDeclaredMethod("getThemePath");
    themePathGetter.setAccessible(true);
    Path themePath = (Path)themePathGetter.invoke(null);
    String minecraftVersion = (String)getPrivateObject(DisplayWindow.class, displayWindow, "minecraftVersion");
    String neoForgeVersion = (String)getPrivateObject(DisplayWindow.class, displayWindow, "neoForgeVersion");
    loadingScreenRenderer = new LoadingScreenRenderer(scheduler, StableFPS.window, theme, themePath, () -> minecraftVersion, () -> neoForgeVersion);
    loadingScreenRenderer.stopAutomaticRendering();
    loadingScreenRenderer_ready.countDown();
    // set GL variables
    GL32C.glEnable(GL32C.GL_BLEND);
    GL32C.glBlendFunc(GL32C.GL_SRC_ALPHA, GL32C.GL_ONE_MINUS_SRC_ALPHA);
  }
  private static DisplayWindow getDisplayWindow() throws NoSuchFieldException, IllegalAccessException {
    return (DisplayWindow)getPrivateObject(ImmediateWindowHandler.class, null, "provider");
  }
  private static long getWindow(DisplayWindow displayWindow) throws NoSuchFieldException, IllegalAccessException {
    return getPrivateLong(DisplayWindow.class, displayWindow, "window");
  }
}
