import java.util.logging.Level;

import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;

/**
 * This class implements liveness checking functions a separated thread is
 * executed to send keep alive messages at regular interval. If streaming server
 * is found dead it will look for an alternate server.
 * 
 * @author Abhi
 * 
 */
public class CheckLIveness extends Thread {
	private boolean running = false;

	public void run() {
		System.out.println("Liveness check is up...");
		try {
			while (running) {
				/**
				 * Conditions to enter: 1. Node should not be bootstrap node and
				 * surrogate node 2. It should not be already searching for
				 * server 3. Current streaming server should dead
				 */
				if (!RadioNode.isBootStrapNode && !RadioNode.isSurrogate
						&& !RadioApp.getRadioApp().isAlreadySearching
						&& RadioApp.getRadioApp().isServerAlive()) {
					System.out.println("Server found alive");
					RadioApp.getRadioApp().setServerAlive(false);
				}
				/**
				 * if ServerAlive is false i.e. if it has not received any heart
				 * beat from parent in last seven second
				 */
				else if (!RadioNode.isBootStrapNode && !RadioNode.isSurrogate
						&& !RadioApp.getRadioApp().isAlreadySearching) {
					System.out.println("Server dead");
					/**
					 * Set up searching parameter
					 */
					RadioApp.getRadioApp().setUpServerSearch();
					try {
						System.out.println("Looking for aleternate server");
						RadioApp.getRadioApp().sendStreamRequest();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Radio.logger.log(Level.SEVERE, e.getMessage());
						e.printStackTrace();
					}

				}
				/**
				 * If vlc media player is not receiving stream, try to reconnect
				 * with streaming server again. It is helpful when a node leaves
				 * the network and the descendent will not know about this and
				 * they won't receive any stream then if vlc will catch this as
				 * an error and the node then will keep trying to connect with
				 * its parent. Once the stream starts flowing it will receive it
				 * from its parent
				 */
				if (Player.mediaPlayer != null) {
					if (Player.mediaPlayer.getMediaState() == libvlc_state_t.libvlc_Ended
							|| Player.mediaPlayer.getMediaState() == libvlc_state_t.libvlc_Error) {
						Radio.logger.log(Level.SEVERE, "Streaming stopped");
						RadioApp.getRadioApp().setStream(
								RadioApp.getRadioApp().getVLCServerStream());
					}
				}

				/**
				 * Send heartbeat message to all the client nodes.
				 */
				Listeners.getListener().sendHeartBeat(HeartBeat.Type.ALIVE);

				try {
					/**
					 * Liveness check thread sleeps for 7 second
					 */
					Thread.sleep(7000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Radio.logger.log(Level.SEVERE, e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			Radio.logger.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Start liveness check thread
	 */
	public void startLivenessCheck() {
		if (!running) {
			running = true;
			this.start();
		}
	}

	/**
	 * Shut down liveness check.
	 */
	public void shutdown() {
		running = false;
	}
}
