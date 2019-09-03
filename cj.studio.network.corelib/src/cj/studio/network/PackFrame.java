package cj.studio.network;

/**
 * 包装frame，以增加传输协议，如是心跳包吗？是数据包吗？即加上了一些类型
 */
public class PackFrame {
    byte header;//头用一个字节表示包类型，0为无效，1为数据，2为心跳包
    NetworkFrame frame;

    public PackFrame(byte header, NetworkFrame frame) {
        this.header = header;
        this.frame = frame;
    }

    public PackFrame(byte[] pack) {
        header = pack[0];
        if (pack.length < 2) {
            return;
        }
        byte[] frameRaw = new byte[pack.length - 1];
        System.arraycopy(pack, 1, frameRaw, 0, frameRaw.length);
        frame=new NetworkFrame(frameRaw);
    }

    public byte[] toBytes() {
        byte[] b = null;
        if (frame == null) {
            b = new byte[1];
            b[0] = header;
        } else {
            byte[] frameRaw = frame.toBytes();
            b = new byte[frameRaw.length + 1];
            b[0] = header;
            System.arraycopy(frameRaw, 0, b, 1, frameRaw.length);
        }
        return b;
    }

    public boolean isInvalid() {
        return header < 1;
    }

    public boolean isFrame() {
        return header == 1;
    }

    public boolean isHeartbeat() {
        return header == 2;
    }

    public NetworkFrame getFrame() {
        return frame;
    }
}
