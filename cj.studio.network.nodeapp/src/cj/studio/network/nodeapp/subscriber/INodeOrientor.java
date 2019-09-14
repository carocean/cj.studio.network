package cj.studio.network.nodeapp.subscriber;

public interface INodeOrientor {
    String get(String nodeName);

    void set(String objkey, String nodeName);

}