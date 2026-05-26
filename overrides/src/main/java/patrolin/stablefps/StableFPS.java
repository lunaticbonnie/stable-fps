package patrolin.stablefps;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class StableFPS implements ModInitializer {
	public static final String MOD_ID = "stable-fps";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static final AtomicBoolean do_grabOrReleaseMouse = new AtomicBoolean(false);
	public static long mouse_window;
	public static int mouse_input_mode;
	public static double mouse_x;
	public static double mouse_y;

	@Override
	public void onInitialize() {}
	public static void grabOrReleaseMouse(long window, int input_mode, double x, double y) {
		do_grabOrReleaseMouse.set(false);
		mouse_window = window;
		mouse_input_mode = input_mode;
		mouse_x = x;
		mouse_y = y;
		do_grabOrReleaseMouse.set(true);
	}
}