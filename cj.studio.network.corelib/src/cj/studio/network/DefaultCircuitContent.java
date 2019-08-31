package cj.studio.network;

import io.netty.buffer.ByteBuf;

class DefaultCircuitContent implements ICircuitContent {
     ByteBuf buf;

    public DefaultCircuitContent(ByteBuf buf, int capacity) {
        this.buf = buf;
    }

    public DefaultCircuitContent(ByteBuf buf) {
        this(buf, 8192);// 默认8K
    }


    private byte[] readFully(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b, 0, b.length);
        buf.clear();
        return b;
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
