
public class BlockColumnSetter implements Runnable {

	private final int x;
	private final int z;
	private final Server server;
	private final int[][][] initialState;
	private final int i;
	private final int k;

	public BlockColumnSetter(Server server, int[][][] initialState, int x, int z, int i, int k) {
		this.server = server;
		this.initialState = initialState;
		this.x = x;
		this.z = z;
		this.i = i;
		this.k = k;

	}

	@Override
	public void run() {
		for(int j = 0; j < 128; j++) {
			server.setBlockAt(initialState[i][j][k], x, j, z);
		}
		server.messageAll("Reset x=" + x + " z=" + z);
	}

}
