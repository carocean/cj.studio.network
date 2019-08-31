package cj.studio.network;


public interface IPrinter {
    /**
     * 将当前对象信息打印到sb中。<br>
     * 由于图可以动态构建，因此每次打印的图可能不同
     *
     * @return
     * @throws CircuitException
     */
    void print(StringBuffer sb) throws CircuitException;

    /**
     * 将当前对象信息打印到sb中。<br>
     * 由于图可以动态构建，因此每次打印的图可能不同
     *
     * <br><br>
     * 连通图的打印很复杂，先不实现了
     *
     * @param indent TODO 缩进
     * @return
     */
    void print(StringBuffer sb, String indent) throws CircuitException;
}
