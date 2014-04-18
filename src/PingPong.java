import rice.p2p.commonapi.Message;


@SuppressWarnings("serial")
public class PingPong implements Message{
	
	private int type;
	public static enum Type{
		PING,
		PONG
	}
	public PingPong(int type) {
		this.setType(type);
	}
	@Override
	public int getPriority() {
		return 0;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}

}
