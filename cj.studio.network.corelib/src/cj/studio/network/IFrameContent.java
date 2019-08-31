package cj.studio.network;


public interface IFrameContent {
	byte[] readFully();
	public abstract long readableBytes();
	void read(byte[] buf,int pos,int len);
	void read(byte[] buf,int pos);
	void read(byte[] buf);
}
