package cj.studio.network;

import io.netty.buffer.ByteBuf;

//frame的内容可以完全异步，servlet3.0在httpservletrequest中无内容字段，tomcat,jetty等在该对象外做了异步接收器，各个厂商的实现均不同。
class DefaultFrameContent implements IFrameContent {
	ByteBuf buf;

	public DefaultFrameContent(ByteBuf buf) {
		this.buf=buf;
	}

	@Override
	public byte[] readFully() {
		byte[] b = new byte[buf.readableBytes()];
		buf.readBytes(b, 0, b.length);
		buf.clear();
		return b;
	}

	@Override
	public long readableBytes() {
		return buf.readableBytes();
	}

	@Override
	public void read(byte[] buf, int pos, int len) {
		this.buf.readBytes(buf,pos,len);
	}

	@Override
	public void read(byte[] buf, int pos) {
		this.buf.readBytes(buf,pos,buf.length);
	}

	@Override
	public void read(byte[] buf) {
		this.buf.readBytes(buf);
	}
}
