package cj.studio.network;

import cj.studio.ecm.EcmException;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 回路。它是一个执行序列
 *
 * @author carocean
 */
//https://www.cnblogs.com/ismallboy/p/6785328.html
public class Circuit implements IPrinter, IDisposable {
    private Map<String, String> headmap;
    private Map<String, Object> attributemap;
    protected transient ICircuitContent content;
    static transient final String CODE = "utf-8";

    public static void main(String...args){
        Circuit circuit=new Circuit("s/1.0 200 ok");
        circuit.head("xx","11");
        circuit.head("22","ll");
        circuit.content().writeBytes("xxxxxx".getBytes());
        byte[] b=circuit.toBytes();
        Circuit c=new Circuit(b);
        StringBuffer sb=new StringBuffer();
        c.print(sb);
        System.out.println(sb);
    }
    public Circuit(String frame_line) {
        this(frame_line, 8192);
    }

    public Circuit(String frame_line, int capacity) {
        headmap = new HashMap<>();
        attributemap = new HashMap<>();
        String[] arr = frame_line.split(" ");
        if (arr.length > 0)
            head("protocol", arr[0].toUpperCase());
        if (arr.length > 1)
            head("status", arr[1]);
        if (arr.length > 2)
            head("message", arr[2]);
        if (arr.length > 3)
            throw new RuntimeException("格式错误");
        content = new DefaultCircuitContent(capacity);
    }

    public Circuit(byte[] frameRaw) {
        headmap = new HashMap<String, String>(8);
        content  = new DefaultCircuitContent(8192);

        int up = 0;
        int down = 0;
        byte field = 0;// 0=heads;1=params;2=content

        while (down < frameRaw.length) {
            if (field < 2) {// 修改了当内容的头几行是连续空行的情况的bug因此使用了field<2
                if (frameRaw[up] == '\r' && (up + 1 < frameRaw.length && frameRaw[up + 1] == '\n')) {// 跳域
                    field++;
                    up += 2;
                    down += 2;
                    continue;
                }
            } else {
                down = frameRaw.length;// 非常变态，bytebuf数组总是在结尾入多一个0，因此其长度总是比写入的长度多1个字节
                byte[] b = new byte[down - up];
                System.arraycopy(frameRaw, up, b, 0, b.length);
                content.writeBytes(b, 0, b.length);
                break;
            }
            if (frameRaw[down] == '\r' && (down + 1 < frameRaw.length && frameRaw[down + 1] == '\n')) {// 跳行
                byte[] b = new byte[down - up];
                System.arraycopy(frameRaw, up, b, 0, b.length);
                try {
                    switch (field) {
                        case 0:
                            String kv = new String(b, CODE);
                            int at = kv.indexOf("=");
                            String k = kv.substring(0, at);
                            String v = kv.substring(at + 1, kv.length());
                            if ("protocol".equals(k)) {
                                if (v != null)
                                    v = v.toUpperCase();
                            }
                            headmap.put(k, v);
                            // if ("url".equals(k)
                            // && !StringUtil.isEmpty(queryString())) {
                            // String[] pair = queryString().split("&");
                            // for (String a : pair) {
                            // String[] t = a.split("=");
                            // String s = t.length > 1 ? t[1] : null;
                            // parametermap.put(t[0], s);
                            // }
                            // }
                            break;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                down += 2;
                up = down;
                continue;
            }
            down++;
        }
    }


    public boolean containAtrribute(String attr) {
        if (attributemap == null) {
            return false;
        }
        return attributemap.containsKey(attr);
    }

    @Override
    public void dispose() {
        headmap.clear();
        if (attributemap != null)
            attributemap.clear();
        ((DefaultCircuitContent) content).buf.clear();
    }


    public byte[] toBytes() {
        ByteBuf b = Unpooled.buffer();
        byte[] crcf = null;
        try {
            crcf = "\r\n".getBytes(CODE);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        headmap.put("Content-Length", Long.toString(content.readableBytes()));
        for (String key : headmap.keySet()) {
            String v = headmap.get(key);
            if (StringUtil.isEmpty(v)) {
                continue;
            }
            String tow = key + "=" + v + "\r\n";
            try {
                b.writeBytes(tow.getBytes(CODE));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        b.writeBytes(crcf);
//		b.writeBytes(crcf);
        if (content.readableBytes() > 0) {
            b.writeBytes(content.readFully());
        }
        byte[] newArr = new byte[b.readableBytes()];
        b.readBytes(newArr);
        b.release();
        return newArr;
    }

    public ByteBuf toByteBuf() {
        ByteBuf buf = Unpooled.copiedBuffer(toBytes());
        return buf;
    }

    /**
     * 从另一外填充到本回路。注：内容不被填充
     *
     * @param other
     */
    public void fillFrom(Circuit other) {
        if (other.attributemap != null) {
            for (String name : other.attributemap.keySet()) {
                attributemap.put(name, other.attribute(name));
            }
        }
        if (other.headmap != null) {
            for (String name : other.headmap.keySet()) {
                headmap.put(name, other.head(name));
            }
        }
        if (other.content().readableBytes() > 0) {
            content.writeBytes(other.content.readFully());
        }
    }

    public ICircuitContent content() {
        return content;
    }

    public String protocol() {
        return head("protocol");
    }

    /**
     * 状态
     *
     * <pre>
     *
     * </pre>
     *
     * @return
     */
    public String status() {
        return head("status");
    }

    public void status(String v) {
        head("status", v);
    }

    /**
     * 消息
     *
     * <pre>
     *
     * </pre>
     *
     * @param msg
     */
    public void message(String msg) {
        head("message", msg);
    }

    /**
     * 消息
     *
     * <pre>
     *
     * </pre>
     *
     * @return
     */
    public String message() {
        return head("message");
    }

    /**
     * 如发生错误，此值为错误原因，无错则为空
     *
     * <pre>
     *
     * </pre>
     *
     * @return
     */
    public String cause() {
        return (String) attribute("$cause");
    }

    public void cause(String cause) {
        attribute("$cause", cause);
    }

    public String contentChartset() {
        return head("content-chartset");
    }

    public void contentChartset(String chartset) {
        head("content-chartset", chartset);
    }

    public String[] enumAttributeName() {
        if (attributemap == null)
            return new String[0];
        return attributemap.keySet().toArray(new String[0]);
    }

    public Object attribute(String name) {
        if (attributemap == null)
            return null;
        return attributemap.get(name);
    }

    public void attribute(String key, Object v) {
        if (attributemap == null) {
            attributemap = new HashMap<String, Object>();
        }
        attributemap.put(key, v);
    }

    public void removeAttribute(String key) {
        if (attributemap == null)
            return;
        attributemap.remove(key);
    }

    public String[] enumHeadName() {
        return headmap.keySet().toArray(new String[0]);
    }

    public String head(String name) {
        return headmap.get(name);
    }

    public void head(String key, String v) {
        if (StringUtil.isEmpty(v)) {
            headmap.put(key, "");
            return;
        }
        if (!"message".equals(key) && !"cause".equals(key) && (v.contains("\r") || v.contains("\n"))) {
            throw new EcmException(String.format("不能包含\\r 或 \\n value is %s", v));
        }
        headmap.put(key, v);
    }

    public void removeHead(String key) {
        headmap.remove(key);
    }

    @Override
    public String toString() {
        return protocol() + " " + status() + " " + message();
    }

    public String contentType() {
        return head("Content-Type");
    }

    /**
     * min类型
     *
     * <pre>
     * 如frame/bin,frame/json,others
     * </pre>
     *
     * @param type
     */
    public void contentType(String type) {
        head("Content-Type", type);
    }

    public boolean containsContentType() {
        return headmap.containsKey("Content-Type");
    }

    /**
     *
     */
    @Override
    public void print(StringBuffer sb) {
        print(sb, null);
    }

    @Override
    public void print(StringBuffer sb, String indent) {
        sb.append(new String(toBytes()));
    }

    public boolean containsHead(String name) {
        // TODO Auto-generated method stub
        return headmap.containsKey(name);
    }

    public void protocol(String protocol) {
        head("protocol", protocol);
    }

    /**
     * 覆盖
     *
     * <pre>
     * </pre>
     *
     * @param by
     */
    public void coverFrom(Circuit by) {
        this.attributemap = by.attributemap;
        this.content = by.content;
        this.headmap = by.headmap;
    }

}
