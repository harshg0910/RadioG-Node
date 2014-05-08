import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * These messages will update the bootstrap regarding the availability of the
 * streams
 * 
 * @author harsh
 */
public class StreamUpdateMessage implements Message {

	private static final long serialVersionUID = 1L;

	public static enum Type {
		/**
		 * This message is sent when a node has free slot either when 
		 * it has joined the network or it has become free after a 
		 * child left the network
		 */
		STREAM_FREE,
		
		/**
		 * When all slots are full for a node, this message is sent.
		 */
		STREAM_FULL,
		
		/**
		 * When the level of a node changes, this message updates the level
		 */
		LEVEL_UPDATE
	}

	public Type info;
	private NodeHandle node;
	private int level;

	public Type getInfo() {
		return info;
	}

	public void setInfo(Type inf) {
		this.info = inf;
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	}

	public NodeHandle getNode() {
		return node;
	}

	public void setNode(NodeHandle node) {
		this.node = node;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

}
