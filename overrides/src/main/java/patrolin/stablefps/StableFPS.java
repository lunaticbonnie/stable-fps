package patrolin.stablefps;

import com.mojang.blaze3d.platform.WindowEventHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StableFPS implements ModInitializer {
	@Override
	public void onInitialize() {}
	public static final String MOD_ID = "stable-fps";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	// window
	public static AtomicBoolean shouldClose = new AtomicBoolean(false);
	public static final CountDownLatch window_ready = new CountDownLatch(1);
	public static volatile long window;

	// events
	public static Thread inputThread = null;
	public static class AsyncResult {
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
	public sealed interface InputThreadEvent permits GrabMouseEvent {}
	public record GrabMouseEvent(long window, int input_mode, double x, double y) implements InputThreadEvent {}
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
}