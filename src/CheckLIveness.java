public class CheckLIveness extends Thread {
	private boolean running = false;

	public void run() {
		System.out.println("Liveness check is up...");
		try {
			while (running) {
				// if(!RadioNode.isBootStrapeNode &&
				// !RadioApp.getRadioApp().checkServerLiveness() &&
				// !RadioApp.getRadioApp().isAlreadySearching){
				if (!RadioNode.isBootStrapeNode
						&& !RadioApp.getRadioApp().isAlreadySearching
						&& RadioApp.getRadioApp().isServerAlive()) {
					System.out.println("Server found alive");
					RadioApp.getRadioApp().setServerAlive(false);
				} else if (!RadioNode.isBootStrapeNode
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
				// Listeners.getListener().update();

				Listeners.getListener().sendHeartBeat();

				try {
					Thread.sleep(5000);
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
