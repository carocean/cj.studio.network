package cj.studio.network;


public interface IContentReciever {

	void recieve(byte[] b, int pos, int length) throws CircuitException;

	void done(byte[] b, int pos, int length) throws CircuitException;
	void begin(Frame frame);
}
