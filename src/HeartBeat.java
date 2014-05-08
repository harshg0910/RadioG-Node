import rice.p2p.commonapi.Message;

/**
 * Heartbeat- This class is used to keep alive messages on regural interval and
 * Dying message when a client leaves the network. 
 * 
 * @author Abhi
 * 
 */
@SuppressWarnings("serial")
public class HeartBeat implements Message {
	
	enum Type {
		ALIVE, DYING
	}

	Type type;
	
	public HeartBeat(Type type) {
		this.type = type;
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	}
}
