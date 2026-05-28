package patrolin.stablefps;

import com.mojang.blaze3d.platform.WindowEventHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class StableFPS implements ModInitializer {
	@Override
	public void onInitialize() {}
	public static final String MOD_ID = "stable-fps";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	// window
	public static volatile long window;
	public static final CountDownLatch window_ready = new CountDownLatch(1);
	public static volatile boolean shouldClose = false;

	// events
	public static Thread inputThread = null;
	public static class AsyncResult {
		private Object value = null;
		private final CountDownLatch ready = new CountDownLatch(1);
		public void submit(Object value) {
			ready.countDown();
			this.value = value;
		}
		@SuppressWarnings("UnusedReturnValue")
		public Object await() {
			try {
				ready.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return this.value;
		}
	}
	// inputThread events
	public static final BlockingQueue<InputThreadEvent> inputThread_events = new LinkedBlockingQueue<>();
	public sealed interface InputThreadEvent permits GrabMouseEvent {}
	public record GrabMouseEvent(long window, int input_mode, double x, double y) implements InputThreadEvent {}
	// renderThread events
	public static final BlockingQueue<RenderThreadEvent> renderThread_events = new LinkedBlockingQueue<>();
	public sealed interface RenderThreadEvent permits ResizeDisplayEvent, RunOnRenderThreadEvent {}
	public record ResizeDisplayEvent(WindowEventHandler eventHandler) implements RenderThreadEvent {}
	public record RunOnRenderThreadEvent(Runnable callback) implements RenderThreadEvent {}
	/*public static final class RunOnRenderThreadEvent implements RenderThreadEvent {
		public Runnable callback;
		RunOnRenderThreadEvent(Runnable callback) {
			this.callback = callback;
		}
		public AsyncResult result = new AsyncResult();
	}*/

	// dispatch
	public static void grabOrReleaseMouse(long window, int input_mode, double x, double y) {
		GrabMouseEvent event = new GrabMouseEvent(window, input_mode, x, y);
		inputThread_events.add(event);
	}
	public static void resizeDisplay(WindowEventHandler eventHandler) {
		ResizeDisplayEvent event = new ResizeDisplayEvent(eventHandler);
		renderThread_events.add(event);
	}
	public static void runOnRenderThread(Runnable callback) {
		if (Thread.currentThread() == inputThread) {
			RunOnRenderThreadEvent event = new RunOnRenderThreadEvent(callback);
			renderThread_events.add(event);
		} else {
			callback.run();
		}
	}
	/*public static void runOnRenderThreadSync(Runnable callback) {
		if (Thread.currentThread() == inputThread) {
			RunOnRenderThreadEvent event = new RunOnRenderThreadEvent(callback);
			renderThread_events.add(event);
			event.result.await();
		} else {
			callback.run();
		}
	}*/
}