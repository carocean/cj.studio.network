package cj.studio.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class DefaultCircuitContent implements ICircuitContent {
     ByteBuf buf;

    public DefaultCircuitContent( int capacity) {
        this(Unpooled.buffer(8192));// 默认8K
    }

    public DefaultCircuitContent(ByteBuf buf) {
        this.buf = buf;
    }


    private byte[] readFully(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b, 0, b.length);
        buf.clear();
        return b;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        this.buf.writeBytes(buf);
    }

    @Override
    public void writeBytes(byte[] b) {
        this.writeBytes(b, 0, b.length);
    }

    @Override
    public void writeBytes(byte[] b, int pos, int len) {
        buf.writeBytes(b, pos, len);
    }

    @Override
    public void writeBytes(byte[] b, int pos) {
        this.writeBytes(b, pos, b.length);
    }

    @Override
    public long readableBytes() {
        return this.buf.readableBytes();
    }

    @Override
    public byte[] readFully() {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b, 0, b.length);
        buf.clear();
        return b;
    }
}
