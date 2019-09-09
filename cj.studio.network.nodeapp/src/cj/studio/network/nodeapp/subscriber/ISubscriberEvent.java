package cj.studio.network.nodeapp.subscriber;

import cj.studio.network.peer.IOnclose;
import cj.studio.network.peer.IOnerror;
import cj.studio.network.peer.IOnmessage;
import cj.studio.network.peer.IOnopen;

public interface ISubscriberEvent extends IOnopen, IOnclose, IOnmessage, IOnerror {
}
