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
	public static class InputThreadEvent {
		public static final int GRAB_MOUSE_EVENT = 0;
		public static final int SHOULD_CLOSE_EVENT = 1;
		public int type;
		InputThreadEvent(int type) {
			this.type = type;
		}
		void add() {
			inputThread_events.add(this);
		}
	}
	public static class GrabMouseEvent extends InputThreadEvent {
		public long window; public int input_mode; public double x; public double y;
		GrabMouseEvent(long window, int input_mode, double x, double y) {
			super(InputThreadEvent.GRAB_MOUSE_EVENT);
			this.window = window;
			this.input_mode = input_mode;
			this.x = x;
			this.y = y;
		}
	}
	public static class ShouldCloseEvent extends InputThreadEvent {
		ShouldCloseEvent() {
			super(InputThreadEvent.SHOULD_CLOSE_EVENT);
		}
		public AsyncResult result = new AsyncResult();
	}
	// renderThread events
	public static final BlockingQueue<RenderThreadEvent> renderThread_events = new LinkedBlockingQueue<>();
	public static class RenderThreadEvent {
		public static final int RESIZE_DISPLAY_EVENT = 0;
		public int type;
		RenderThreadEvent(int type) {
			this.type = type;
		}
		void add() {
			renderThread_events.add(this);
		}
	}
	public static class ResizeDisplayEvent extends RenderThreadEvent {
		public WindowEventHandler eventHandler;
		ResizeDisplayEvent(WindowEventHandler eventHandler) {
			super(RenderThreadEvent.RESIZE_DISPLAY_EVENT);
			this.eventHandler = eventHandler;
		}
	}

	// dispatch
	public static void grabOrReleaseMouse(long window, int input_mode, double x, double y) {
		GrabMouseEvent event = new GrabMouseEvent(window, input_mode, x, y);
		event.add();
	}
	public static void shouldClose() {
		ShouldCloseEvent event = new ShouldCloseEvent();
		event.add();
		event.result.await();
	}
	public static void resizeDisplay(WindowEventHandler eventHandler) {
		ResizeDisplayEvent event = new ResizeDisplayEvent(eventHandler);
		event.add();
	}
}