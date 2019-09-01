package cj.studio.network.node;

import cj.studio.network.Frame;

public interface INetworkNodeApp {
    /**
     * 如果验证侦不合法，抛出HandshakeException会关闭对端网络通道
     * @param frame
     * @throws VerifyException
     */
    void verifyFrame(Frame frame)throws VerifyException;
}