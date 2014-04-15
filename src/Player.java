import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;

public class Player {

	private static MediaPlayer mediaPlayer = null;
	private static MediaPlayer Streamer = null;
	private static boolean isPlaying = false;

	private static String formatMmshStream(String serverAddress, int serverPort) {
		StringBuilder sb = new StringBuilder(120);
		sb.append(":sout=#transcode{vcodec=h264,acodec=mpga,ab=128,channels=2,samplerate=44100}");
		sb.append(":duplicate{dst=std{access=mmsh,mux=asfh,dst=");
		sb.append(serverAddress);
		sb.append(':');
		sb.append(serverPort);
		sb.append("}}");
		return sb.toString();
	}

	public static void startVLCStreaming(String StreamerIP, int StreamerPort,
			String Input) {
		// VLCpath = vlcPath;
		// System.out.println(VLCpath);
		// NativeLibrary
		// .addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLCpath);
		String options = formatMmshStream(StreamerIP, StreamerPort);
		System.out.println(options);
		AudioMediaPlayerComponent mediaPlayerComponent = new AudioMediaPlayerComponent();
		if (Streamer == null) {
			Streamer = mediaPlayerComponent.getMediaPlayer();
		}
		if (Streamer.playMedia(Input, options)) {
			System.out.println("Streaming Started");
		} else {
			System.out.println("Error in creating player");
		}
	}

	public static boolean Pause() {
		if (mediaPlayer != null) {
			mediaPlayer.pause();
			return true;
		}
		return false;
	}

	public static boolean Mute() {
		if (mediaPlayer != null) {
			mediaPlayer.mute(true);
			return true;
		}
		return false;
	}

	public static boolean UnMute() {
		if (mediaPlayer != null) {
			mediaPlayer.mute(false);
			return true;
		}
		return false;
	}

	public static void startListen(String src) {
		System.out.println("Start listening");
		AudioMediaPlayerComponent mediaPlayerComponent = new AudioMediaPlayerComponent();
		if (mediaPlayer == null) {
			mediaPlayer = mediaPlayerComponent.getMediaPlayer();
		}

		if (!mediaPlayer.playMedia(src)) {
			System.out.println("Error in starting");
		} else {
			System.out.println("VLC started");
		}
	}

	public static void stopServer() {
		RadioApp.getRadioApp().hasStream = false;
		if (Streamer != null && Streamer.isPlaying())
			Streamer.stop();
	}

	public static void stopListening() {
		if (mediaPlayer != null && mediaPlayer.isPlaying())
			mediaPlayer.stop();
	}
}
