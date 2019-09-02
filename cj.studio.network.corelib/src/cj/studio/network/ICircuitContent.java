package cj.studio.network;

import io.netty.buffer.ByteBuf;

public interface ICircuitContent {
	public abstract void writeBytes(byte[] b);
	public abstract void writeBytes(byte[] b, int pos);
	public abstract void writeBytes(byte[] b, int pos, int len);
	void writeBytes(ByteBuf buf);

	public abstract long readableBytes();

	byte[] readFully();
}