import java.util.logging.Level;

import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;

/**
 * VLC media player implementations
 * 
 * @author Abhi
 * 
 */
public class Player {

	/**
	 * MediaPlayer instance for playing received stream
	 */
	public static MediaPlayer mediaPlayer = null;
	/**
	 * MediaPlayer instance for streaming media
	 */
	private static MediaPlayer Streamer = null;
	
	/**
	 * format stream command for vlc  mms streaming
	 * @param serverAddress - address of the local machine for streaming 
	 * @param serverPort - port for streaming
	 * @return - returns vlc command for streaming 
	 */
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
	
	/**
	 * start vlc streaming from an address
	 * @param StreamerIP - Address of the streaming server -- local address
	 * @param StreamerPort - Streaming server port
	 * @param Input - Input stream to stream
	 */
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
		} else {
			Streamer.stop();
		}
		if (Streamer.playMedia(Input, options)) {
			RadioNode.getRadioNode();
			RadioApp.streamStartedAt = RadioNode.sts.currentTimeMillis();
			Radio.logger.log(Level.INFO, "Stream Started at "
					+ RadioApp.streamStartedAt);
			Radio.logger.log(Level.INFO, "Startup delay "
					+ (RadioApp.streamStartedAt - Radio.upTime) + "ms");
			System.out.println("Streaming Started at "
					+ RadioApp.streamStartedAt);
		} else {
			Radio.logger.log(Level.SEVERE, "Error in creating player");
			System.out.println("Error in creating player");
		}
	}
	
	/**
	 * pause media player
	 * @return - returns true if paused successfully
	 */
	public static boolean Pause() {
		if (mediaPlayer != null) {
			mediaPlayer.pause();
			return true;
		}
		return false;
	}
	
	/**
	 * Mute media player
	 * @return - returns true if successfully
	 */
	public static boolean Mute() {
		if (mediaPlayer != null) {
			mediaPlayer.mute(true);
			return true;
		}
		return false;
	}
	
	/**
	 * Unmute media player
	 * @return
	 */
	public static boolean UnMute() {
		if (mediaPlayer != null) {
			mediaPlayer.mute(false);
			return true;
		}
		return false;
	}
	
	/**
	 * Start listening from a given url
	 * @param src - url of the stream
	 */
	public static void startListen(String src) {
		System.out.println("Start listening");
		AudioMediaPlayerComponent mediaPlayerComponent = new AudioMediaPlayerComponent();
		if (mediaPlayer == null) {
			mediaPlayer = mediaPlayerComponent.getMediaPlayer();
			// mediaPlayer.addMediaPlayerEventListener(new
			// MediaPlayerEventAdapter() {
			// @Override
			// public void error(MediaPlayer mediaPlayer) {
			// Radio.logger.log(Level.SEVERE,"media error: isplayable" +
			// mediaPlayer.isPlayable() + " state "+
			// mediaPlayer.getMediaState());
			// };
			// });
		}

		if (!mediaPlayer.playMedia(src)) {
			System.out.println("Error in starting");
		} else {
			System.out.println("VLC started");

		}
	}
	
	/**
	 * Stop streaming server
	 */
	public static void stopServer() {
		RadioApp.getRadioApp().hasStream = false;
		if (Streamer != null && Streamer.isPlaying())
			Streamer.stop();
	}
	
	/**
	 * Stop listening from a streaming server
	 */
	public static void stopListening() {
		if (mediaPlayer != null && mediaPlayer.isPlaying())
			mediaPlayer.stop();
	}
	
	/**
	 * change volume level
	 * @param change 
	 */
	public static void setVolume(int change) {
		if (mediaPlayer != null)
			mediaPlayer.setVolume(change);

	}
}
