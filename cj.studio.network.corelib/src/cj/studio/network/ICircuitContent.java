package cj.studio.network;

public interface ICircuitContent {
	public abstract void writeBytes(byte[] b);
	public abstract void writeBytes(byte[] b, int pos);
	public abstract void writeBytes(byte[] b, int pos, int len);


	public abstract long readableBytes();

	byte[] readFully();
}