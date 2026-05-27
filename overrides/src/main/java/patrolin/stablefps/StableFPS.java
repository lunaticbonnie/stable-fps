package patrolin.stablefps;

import com.mojang.blaze3d.platform.WindowEventHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class StableFPS implements ModInitializer {
	@Override
	public void onInitialize() {}
	public static final String MOD_ID = "stable-fps";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	// events
	public static Thread inputThread = null;
	public static class AsyncEventResult {
		Object value = null;
		final CountDownLatch ready = new CountDownLatch(1);
		private Object await() {
            try {
                ready.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
			return value;
        }
	}
	// inputThread events
	public static final BlockingQueue<InputThreadEvent> inputThread_events = new LinkedBlockingQueue<>();
	public sealed interface InputThreadEvent permits GrabMouseEvent, GLFW_GetWindowMonitorEvent, GLFW_SetWindowMonitorEvent {
		AsyncEventResult result = new AsyncEventResult();
	}
	public record GrabMouseEvent(long window, int input_mode, double x, double y) implements InputThreadEvent {}
	public record GLFW_GetWindowMonitorEvent(long window) implements InputThreadEvent {
		public void submit_result(long monitor) {
			result.value = monitor;
			result.ready.countDown();
		}
		public long wait_for_result() {
			return (long)result.await();
		}
	}
	public record GLFW_SetWindowMonitorEvent(long window, long monitor, int x, int y, int width, int height, int refresh_rate) implements InputThreadEvent {}
	// renderThread events
	public static final BlockingQueue<RenderThreadEvent> renderThread_events = new LinkedBlockingQueue<>();
	public sealed interface RenderThreadEvent permits ResizeDisplayEvent {}
	public record ResizeDisplayEvent(WindowEventHandler eventHandler) implements RenderThreadEvent {}

	// dispatch
	public static void grabOrReleaseMouse(long window, int input_mode, double x, double y) {
		GrabMouseEvent event = new GrabMouseEvent(window, input_mode, x, y);
		inputThread_events.add(event);
	}
	public static void resizeDisplay(WindowEventHandler eventHandler) {
		ResizeDisplayEvent event = new ResizeDisplayEvent(eventHandler);
		renderThread_events.add(event);
	}
	public static long glfwGetWindowMonitor(long window) {
		GLFW_GetWindowMonitorEvent event = new GLFW_GetWindowMonitorEvent(window);
		inputThread_events.add(event);
		return event.wait_for_result();
	}
	public static void glfwSetWindowMonitor(long window, long monitor, int x, int y, int width, int height, int refreshRate) {
		GLFW_SetWindowMonitorEvent event = new GLFW_SetWindowMonitorEvent(window, monitor, x, y, width, height, refreshRate);
		inputThread_events.add(event);
	}
}