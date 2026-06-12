package patrolin.stablefps;

import net.minecraftforge.fml.earlydisplay.*;
import net.minecraftforge.fml.earlydisplay.RenderElement.DisplayContext;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ForgeWindowAccessor {
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

  public static long recreateForgeWindow(int width, int height, String title, long monitor, long share) throws NoSuchFieldException, IllegalAccessException {
    DisplayWindow displayWindow = getDisplayWindow();
    String glVersionString = (String)getPrivateObject(DisplayWindow.class, displayWindow, "glVersion");
    String[] glVersion = glVersionString.split("\\.");
    int major = Integer.parseInt(glVersion[0]);
    int minor = Integer.parseInt(glVersion[1]);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, major);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, minor);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    return GLFW.glfwCreateWindow(width, height, title, monitor, share);
  }
  public static void closeForgeEarlyWindow() {
    try {
      DisplayWindow displayWindow = getDisplayWindow();
      // shutdown renderScheduler
      ScheduledExecutorService forgeScheduler = (ScheduledExecutorService)getPrivateObject(DisplayWindow.class, displayWindow, "renderScheduler");
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
  public static void recreateForgeFramebuffer(long window) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
    // create GL context
    GLFW.glfwMakeContextCurrent(StableFPS.window);
    GL.createCapabilities();
    GL32C.glClearColor(0f, 0f, 0f, 1f);
    // swap to our window
    DisplayWindow displayWindow = getDisplayWindow();
    setPrivateLong(DisplayWindow.class, displayWindow, "window", window);
    // recreate the OpenGL program
    int fbScale = getPrivateInt(DisplayWindow.class, displayWindow, "fbScale");
    ElementShader elementShader = new ElementShader();
    setPrivateObject(DisplayWindow.class, displayWindow, "elementShader", elementShader);
    elementShader.init();
    ColourScheme colourScheme = (ColourScheme)getPrivateObject(DisplayWindow.class, displayWindow, "colourScheme");
    PerformanceInfo performanceInfo = (PerformanceInfo)getPrivateObject(DisplayWindow.class, displayWindow, "performanceInfo");
    DisplayContext context = new RenderElement.DisplayContext(854, 480, fbScale, elementShader, colourScheme, performanceInfo);
    setPrivateObject(DisplayWindow.class, displayWindow, "context", context);
    // recreate the framebuffer
    Constructor<EarlyFramebuffer> framebufferConstructor = EarlyFramebuffer.class.getDeclaredConstructor(DisplayContext.class);
    framebufferConstructor.setAccessible(true);
    EarlyFramebuffer framebuffer = framebufferConstructor.newInstance(context);
    setPrivateObject(DisplayWindow.class, displayWindow, "framebuffer", framebuffer);
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
