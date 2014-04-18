import rice.pastry.NodeHandle;
import rice.pastry.routing.RouteSet;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;

public class CheckLIveness extends Thread {
	private boolean running = false;

	public void run() {
		System.out.println("Liveness check is up...");
		try {
			while (running) {
				// if(!RadioNode.isBootStrapeNode &&
				// !RadioApp.getRadioApp().checkServerLiveness() &&
				// !RadioApp.getRadioApp().isAlreadySearching){
				if (!RadioNode.isBootStrapNode
						&& !RadioApp.getRadioApp().isAlreadySearching
						&& RadioApp.getRadioApp().isServerAlive()) {
					System.out.println("Server found alive");
					RadioApp.getRadioApp().setServerAlive(false);
				} else if (!RadioNode.isBootStrapNode
						&& !RadioApp.getRadioApp().isAlreadySearching) {
					System.out.println("Server dead");
					RadioApp.getRadioApp().hasStream = false;
					RadioApp.ServerFound = false;
					RadioApp.getRadioApp().setServerAlive(false);

					Player.stopServer();
					Player.stopListening();
					RadioNode.getRadioNode().updateLeafSet();
					try {
						System.out.println("Looking for aleternate server");
						RadioApp.getRadioApp().sendStreamRequest();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				if (Player.mediaPlayer != null) {
					if (Player.mediaPlayer.getMediaState() == libvlc_state_t.libvlc_Ended
							|| Player.mediaPlayer.getMediaState() == libvlc_state_t.libvlc_Error) {
						// Radio.logger.log(Level.SEVERE, "Streaming stopped");
						RadioApp.getRadioApp().setStream(
								RadioApp.getRadioApp().getVLCServerStream());
					}
				}
				// Listeners.getListener().update();
//				System.out.println("---------Routing Table---------------");
//				for (int i = 0; i < RadioNode.node.getRoutingTable().numRows(); i++) {
//					RouteSet rs[] = RadioNode.node.getRoutingTable().getRow(i);
//					System.out.println("---------Row "+ i +"---------------");
//					if (rs.length != 0) {
//						for (RouteSet r : rs) {
//							if (r != null)
//								System.out.println(r.toString());
//						}
//					}
//				}
//				System.out.println("---------Leaf Nodes---------------");
//				for(NodeHandle nh : RadioNode.node.getLeafSet()){
//					System.out.println(nh.toString());
//				}

				Listeners.getListener().sendHeartBeat();

				try {
					Thread.sleep(7000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void startLivenessCheck() {
		if (!running) {
			running = true;
			this.start();
		}
	}

	public void shutdown() {
		running = false;
	}
}
